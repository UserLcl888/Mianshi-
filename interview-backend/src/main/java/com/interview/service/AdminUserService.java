package com.interview.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.PageResult;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.User;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

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
            throw new BizException(40000, "请填写邮箱或手机号");
        }
        if (StringUtils.hasText(email)) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new BizException(40000, "邮箱格式不正确");
            }
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
                throw new BizException(ErrorCode.CONFLICT, "邮箱已被注册");
            }
        }
        if (StringUtils.hasText(phone)) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new BizException(40000, "手机号格式不正确");
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
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : "USER");
        user.setStatus(1);
        userMapper.insert(user);
        adminLogService.write("USER_CREATE", "USER", user.getId(), "创建用户 " + user.getNickname());
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
        adminLogService.write("USER_UPDATE", "USER", id, "编辑用户 " + user.getNickname());
        return authService.toVO(user);
    }

    public void updateStatus(Long id, Integer status) {
        User user = getUser(id);
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == 0) {
            StpUtil.kickout(id);
        }
        adminLogService.write(status == 0 ? "USER_DISABLE" : "USER_ENABLE", "USER", id,
                status == 0 ? "禁用用户" : "启用用户");
    }

    public void resetPassword(Long id, String newPassword) {
        User user = getUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRootPassword(newPassword);
        userMapper.updateById(user);
        StpUtil.kickout(id);
        adminLogService.write("USER_RESET_PASSWORD", "USER", id, "重置用户密码");
    }

    public void delete(Long id) {
        if (StpUtil.getLoginIdAsLong() == id) {
            throw new BizException(40000, "不能删除当前登录账号");
        }
        userMapper.deleteById(id);
        StpUtil.kickout(id);
        adminLogService.write("USER_DELETE", "USER", id, "删除用户");
    }

    private User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
