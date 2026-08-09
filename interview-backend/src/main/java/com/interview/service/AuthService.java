package com.interview.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.User;
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

    public void register(Requests.RegisterDTO dto) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRootPassword(dto.getPassword());
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
    }

    public VOs.LoginResultVO login(Requests.LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            String rateKey = "rate:login:" + dto.getUsername();
            Long attempts = redis.opsForValue().increment(rateKey);
            if (attempts != null && attempts == 1) {
                redis.expire(rateKey, Duration.ofMinutes(1));
            }
            if (attempts != null && attempts > 5) {
                throw new BizException(40000, "尝试次数过多，请 1 分钟后再试");
            }
            throw new BizException(40000, "账号或密码错误");
        }
        redis.delete("rate:login:" + dto.getUsername());
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
        StpUtil.login(user.getId());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return VOs.LoginResultVO.builder()
                .token(StpUtil.getTokenValue())
                .userInfo(toVO(user))
                .build();
    }

    public void logout() {
        StpUtil.logout();
    }

    public VOs.UserVO profile() {
        return toVO(currentUser());
    }

    public void changePassword(Requests.ChangePasswordDTO dto) {
        User user = currentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(40000, "旧密码错误");
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
                .username(user.getUsername())
                .nickname(user.getNickname())
                .rootPassword(user.getRootPassword())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
