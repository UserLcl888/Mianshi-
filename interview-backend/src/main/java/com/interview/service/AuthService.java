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
import com.interview.enums.CodeScene;
import com.interview.enums.UserRole;
import com.interview.enums.UserStatus;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final EmailCodeService emailCodeService;
    private final MarkdownImageService markdownImageService;

    /** 头像仅支持 png/jpg */
    private static final Set<String> AVATAR_EXT = Set.of("png", "jpg", "jpeg");

    public void register(Requests.RegisterDTO dto) {
        String email = StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : "";
        String code = dto.getCode() == null ? "" : dto.getCode().trim();
        String password = dto.getPassword() == null ? "" : dto.getPassword();
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请填写邮箱");
        }
        if (!AccountValidator.isValidEmail(email)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请输入邮箱验证码");
        }
        if (!StringUtils.hasText(password)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请设置登录密码");
        }
        if (password.length() < 6 || password.length() > 12) {
            throw new BizException(ErrorCode.PARAM_ERROR, "密码长度为 6~12 位");
        }
        // 首次注册仅支持邮箱验证码：凭验证码证明邮箱归属，避免“账号是否已存在”的歧义
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该邮箱已注册，请直接登录");
        }
        emailCodeService.verify(email, CodeScene.REGISTER.getValue(), code);
        User user = new User();
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRootPassword(password);
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : defaultNickname(email, ""));
        user.setEmail(email);
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

    public VOs.UserVO updateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择头像图片");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BizException(ErrorCode.PARAM_ERROR, "头像不能超过 10MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!AVATAR_EXT.contains(ext)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持 png/jpg 格式");
        }
        try {
            String url = markdownImageService.storeImage(file.getBytes(), ext, "avatar");
            User user = currentUser();
            String oldAvatar = user.getAvatar();
            user.setAvatar(url);
            userMapper.updateById(user);
            // 替换头像后删除旧的 MinIO 头像（最佳努力）
            if (StringUtils.hasText(oldAvatar) && !oldAvatar.equals(url)) {
                markdownImageService.removeObjectByUrl(oldAvatar);
            }
            return toVO(user);
        } catch (IOException e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "头像读取失败，请重试");
        }
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
