package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.common.AccountValidator;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.RedisKeys;
import com.interview.entity.User;
import com.interview.enums.CodeScene;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final UserMapper userMapper;

    @Value("${app.code.ttl-minutes:5}")
    private long ttlMinutes;

    @Value("${app.code.max-tries:5}")
    private int maxTries;

    @Value("${app.code.resend-seconds:60}")
    private long resendSeconds;

    @Value("${app.code.daily-limit:10}")
    private int dailyLimit;

    @Value("${app.code.mock:true}")
    private boolean mock;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送验证码：scene 支持 login / register / reset。
     * login/reset 要求邮箱已注册；register 要求邮箱未注册（避免重复注册）。
     * 开发环境（mock=true）不真实发信，验证码打印到日志并返回。
     *
     * @return mock 模式下返回验证码，生产返回空 Map
     */
    public Map<String, String> send(String email, String scene) {
        String mail = email == null ? "" : email.trim();
        String sc = scene == null ? "" : scene.trim();
        if (!AccountValidator.isValidEmail(mail)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        CodeScene codeScene = CodeScene.fromValue(sc);
        if (codeScene == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "场景不合法");
        }
        boolean registered = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, mail)) > 0;
        if ("register".equals(sc)) {
            if (registered) {
                throw new BizException(ErrorCode.CONFLICT, "该邮箱已注册，请直接登录");
            }
        } else if (!registered) {
            throw new BizException(ErrorCode.NOT_FOUND, "该邮箱未注册，请先注册");
        }

        String cooldownKey = RedisKeys.emailCodeCooldown(mail, codeScene.getValue());
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            Long remain = redis.getExpire(cooldownKey, TimeUnit.SECONDS);
            throw new BizException(ErrorCode.PARAM_ERROR, "发送太频繁，请 " + (remain == null ? resendSeconds : remain) + " 秒后再试");
        }

        String dailyKey = RedisKeys.emailDailyRate(mail);
        Long daily = redis.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1) {
            redis.expire(dailyKey, Duration.ofHours(24));
        }
        if (daily != null && daily > dailyLimit) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该邮箱今日发送次数已达上限");
        }

        String code = String.valueOf(RANDOM.nextInt(900000) + 100000);
        redis.opsForValue().set(RedisKeys.emailCode(mail, codeScene.getValue()), code, Duration.ofMinutes(ttlMinutes));
        redis.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(resendSeconds));
        // 重发即重置错误计数，避免旧 tries key 残留
        redis.delete(RedisKeys.emailCodeTries(mail, codeScene.getValue()));

        if (mock) {
            log.info("[MOCK EMAIL] 收件人={}, 场景={}, 验证码={}", mail, sc, code);
            return Map.of("debugCode", code);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(StringUtils.hasText(from) ? from : mail);
        message.setTo(mail);
        message.setSubject("【面试题知识库】邮箱验证码");
        message.setText("您的验证码是：" + code + "，5 分钟内有效，请勿泄露给他人。");
        mailSender.send(message);
        return Map.of();
    }

    /**
     * 校验验证码：一次性使用、5 分钟过期、错误超过次数自动作废。
     */
    public void verify(String email, String scene, String code) {
        String mail = email == null ? "" : email.trim();
        String sc = scene == null ? "" : scene.trim();
        if (!AccountValidator.isValidEmail(mail)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "邮箱格式不正确");
        }
        CodeScene codeScene = CodeScene.fromValue(sc);
        if (codeScene == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "场景不合法");
        }
        if (!StringUtils.hasText(code)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请输入验证码");
        }
        String key = RedisKeys.emailCode(mail, codeScene.getValue());
        String triesKey = RedisKeys.emailCodeTries(mail, codeScene.getValue());
        String saved = redis.opsForValue().get(key);
        if (saved == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码不存在或已过期，请重新获取");
        }
        Long tries = redis.opsForValue().increment(triesKey);
        redis.expire(triesKey, Duration.ofMinutes(ttlMinutes));
        if (tries != null && tries > maxTries) {
            redis.delete(key);
            redis.delete(triesKey);
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码错误次数过多，请重新获取");
        }
        if (!saved.equals(code.trim())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "验证码错误");
        }
        redis.delete(key);
        redis.delete(triesKey);
    }
}
