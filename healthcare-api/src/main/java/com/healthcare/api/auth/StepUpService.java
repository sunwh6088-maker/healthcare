package com.healthcare.api.auth;

import com.healthcare.api.common.ApiException;
import com.healthcare.api.sms.SmsSender;
import com.healthcare.api.user.User;
import com.healthcare.api.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 敏感操作二次验证：修改手机号、注销账号、首次设置密码前，
 * 有密码时校验当前密码，无密码时校验当前手机号收到的验证码。
 */
@Service
public class StepUpService {

    private static final Logger log = LoggerFactory.getLogger(StepUpService.class);

    private final UserRepository users;
    private final VerificationCodeService codes;
    private final RateLimitService rateLimits;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final boolean devMode;

    public StepUpService(UserRepository users,
                         VerificationCodeService codes,
                         RateLimitService rateLimits,
                         SmsSender smsSender,
                         @Value("${app.dev-mode:true}") boolean devMode) {
        this.users = users;
        this.codes = codes;
        this.rateLimits = rateLimits;
        this.smsSender = smsSender;
        this.devMode = devMode;
    }

    /**
     * 向当前绑定手机号发送二次验证码。
     *
     * @return 开发模式下返回验证码，生产环境返回 null
     */
    public String sendCode(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
        // 已按用户限制每小时次数，不再叠加面向登录验证码的 60 秒冷却，
        // 否则用户刚登录就改手机号会被误伤。
        rateLimits.checkStepUpSendCode(userId);
        String code = codes.issue(user.getPhone(), VerificationCodeService.PURPOSE_STEP_UP);
        if (devMode) {
            log.info("Dev step-up code for user {}: {}", userId, code);
            return code;
        }
        smsSender.sendVerificationCode(
                user.getPhone(), code, VerificationCodeService.PURPOSE_STEP_UP);
        return null;
    }

    /** 校验当前密码（已设置密码时）或当前手机号验证码（未设置密码时）。 */
    public void requireCredential(User user, String password, String code) {
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            if (password == null || password.isBlank()
                    || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new ApiException(400, "当前密码不正确");
            }
            return;
        }
        if (!codes.verify(user.getPhone(), code, VerificationCodeService.PURPOSE_STEP_UP)) {
            throw new ApiException(400, "验证码错误或已过期");
        }
    }
}
