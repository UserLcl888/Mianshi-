package com.interview.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.common.AccountValidator;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.RedisKeys;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.User;
import com.interview.enums.UserRole;
import com.interview.enums.UserStatus;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final EmailCodeService emailCodeService;

    public void register(Requests.RegisterDTO dto) {
        String email = StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : "";
        String phone = StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : "";
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
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : defaultNickname(email, phone));
        user.setEmail(StringUtils.hasText(email) ? email : null);
        user.setPhone(StringUtils.hasText(phone) ? phone : null);
        user.setRole(UserRole.USER.getCode());
        user.setStatus(UserStatus.NORMAL.getCode());
        userMapper.insert(user);
    }

    public VOs.LoginResultVO login(Requests.LoginDTO dto) {
        String account = dto.getAccount().trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, account)
                .or()
                .eq(User::getPhone, account));
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "账号不存在");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            String rateKey = RedisKeys.loginRate(account);
            Long attempts = redis.opsForValue().increment(rateKey);
            if (attempts != null && attempts == 1) {
                redis.expire(rateKey, Duration.ofMinutes(1));
            }
            if (attempts != null && attempts > 5) {
                throw new BizException(ErrorCode.PARAM_ERROR, "尝试次数过多，请 1 分钟后再试");
            }
            throw new BizException(ErrorCode.PARAM_ERROR, "密码错误");
        }
        redis.delete(RedisKeys.loginRate(account));
        if (user.getStatus() == null || user.getStatus() != UserStatus.NORMAL.getCode()) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        StpUtil.login(user.getId());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return VOs.LoginResultVO.builder()
                .token(StpUtil.getTokenValue())
                .userInfo(toVO(user))
                .build();
    }

    public VOs.LoginResultVO loginByCode(Requests.LoginByCodeDTO dto) {
        String email = dto.getEmail().trim();
        if (!AccountValidator.isValidEmail(email)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        emailCodeService.verify(email, "login", dto.getCode());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该邮箱未注册，请先注册");
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.NORMAL.getCode()) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        StpUtil.login(user.getId());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return VOs.LoginResultVO.builder()
                .token(StpUtil.getTokenValue())
                .userInfo(toVO(user))
                .build();
    }

    public void resetPasswordByCode(Requests.ResetByCodeDTO dto) {
        String email = dto.getEmail().trim();
        if (!AccountValidator.isValidEmail(email)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        emailCodeService.verify(email, "reset", dto.getCode());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该邮箱未注册");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setRootPassword(dto.getNewPassword());
        userMapper.updateById(user);
        StpUtil.kickout(user.getId());
    }

    public void logout() {
        StpUtil.logout();
    }

    public VOs.UserVO updateNickname(Requests.UpdateNicknameDTO dto) {
        User user = currentUser();
        String nickname = dto.getNickname() == null ? "" : dto.getNickname().trim();
        if (!StringUtils.hasText(nickname)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "昵称不能为空");
        }
        user.setNickname(nickname);
        userMapper.updateById(user);
        return toVO(user);
    }

    public VOs.UserVO profile() {
        return toVO(currentUser());
    }

    public void changePassword(Requests.ChangePasswordDTO dto) {
        User user = currentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setRootPassword(dto.getNewPassword());
        userMapper.updateById(user);
        StpUtil.logout();
    }

    public User currentUser() {
        Long id = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public VOs.UserVO toVO(User user) {
        return VOs.UserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String defaultNickname(String email, String phone) {
        return StringUtils.hasText(email) ? email.split("@")[0] : phone;
    }
}
