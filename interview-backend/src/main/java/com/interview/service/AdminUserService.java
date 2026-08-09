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

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthService authService;

    public PageResult<VOs.UserVO> list(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
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
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRootPassword(dto.getPassword());
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : "USER");
        user.setStatus(1);
        userMapper.insert(user);
        return authService.toVO(user);
    }

    public VOs.UserVO update(Long id, Requests.UserUpdateDTO dto) {
        User user = getUser(id);
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        userMapper.updateById(user);
        return authService.toVO(user);
    }

    public void updateStatus(Long id, Integer status) {
        User user = getUser(id);
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == 0) {
            StpUtil.kickout(id);
        }
    }

    public void resetPassword(Long id, String newPassword) {
        User user = getUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRootPassword(newPassword);
        userMapper.updateById(user);
        StpUtil.kickout(id);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
        StpUtil.kickout(id);
    }

    private User getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
