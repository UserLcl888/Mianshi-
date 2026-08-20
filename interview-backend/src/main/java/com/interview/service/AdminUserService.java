package com.interview.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.AccountValidator;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.PageResult;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.User;
import com.interview.enums.AdminLogAction;
import com.interview.enums.UserRole;
import com.interview.enums.UserStatus;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AdminLogService adminLogService;

    public PageResult<VOs.UserVO> list(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (status != null) {
            qw.eq(User::getStatus, status);
        }
        qw.orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), qw);
        List<VOs.UserVO> list = result.getRecords().stream().map(authService::toVO).toList();
        return PageResult.of(page, size, result.getTotal(), list);
    }

    public VOs.UserVO get(Long id) {
        return authService.toVO(getUser(id));
    }

    public VOs.UserVO create(Requests.UserCreateDTO dto) {
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        if (!StringUtils.hasText(email) && !StringUtils.hasText(phone)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请填写邮箱或手机号");
        }
        if (StringUtils.hasText(email)) {
            if (!AccountValidator.isValidEmail(email)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
            }
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
                throw new BizException(ErrorCode.CONFLICT, "邮箱已被注册");
            }
        }
        if (StringUtils.hasText(phone)) {
            if (!AccountValidator.isValidPhone(phone)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "手机号格式不正确");
            }
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
                throw new BizException(ErrorCode.CONFLICT, "手机号已被注册");
            }
        }
        User user = new User();
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRootPassword(dto.getPassword());
        user.setNickname(StringUtils.hasText(dto.getNickname())
                ? dto.getNickname().trim()
                : defaultNickname(email, phone));
        user.setEmail(StringUtils.hasText(email) ? email : null);
        user.setPhone(StringUtils.hasText(phone) ? phone : null);
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : UserRole.USER.getCode());
        user.setStatus(UserStatus.NORMAL.getCode());
        userMapper.insert(user);
        adminLogService.write(AdminLogAction.USER_CREATE, user.getId(), "创建用户 " + user.getNickname());
        return authService.toVO(user);
    }

    private String defaultNickname(String email, String phone) {
        return StringUtils.hasText(email) ? email.split("@")[0] : phone;
    }

    public VOs.UserVO update(Long id, Requests.UserUpdateDTO dto) {
        User user = getUser(id);
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        userMapper.updateById(user);
        adminLogService.write(AdminLogAction.USER_UPDATE, id, "编辑用户 " + user.getNickname());
        return authService.toVO(user);
    }

    public void updateStatus(Long id, Integer status) {
        User user = getUser(id);
        boolean disabled = UserStatus.fromCode(status) == UserStatus.DISABLED;
        user.setStatus(status);
        userMapper.updateById(user);
        if (disabled) {
            StpUtil.kickout(id);
        }
        adminLogService.write(disabled ? AdminLogAction.USER_DISABLE : AdminLogAction.USER_ENABLE,
                id, disabled ? "禁用用户" : "启用用户");
    }

    public void resetPassword(Long id, String newPassword) {
        User user = getUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRootPassword(newPassword);
        userMapper.updateById(user);
        StpUtil.kickout(id);
        adminLogService.write(AdminLogAction.USER_RESET_PASSWORD, id, "重置用户密码");
    }

    public void delete(Long id) {
        if (StpUtil.getLoginIdAsLong() == id) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能删除当前登录账号");
        }
        userMapper.deleteById(id);
        StpUtil.kickout(id);
        adminLogService.write(AdminLogAction.USER_DELETE, id, "删除用户");
    }

    private User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
