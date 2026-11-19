package com.healthcare.api.auth;

import com.healthcare.api.common.ApiException;
import com.healthcare.api.common.PhoneNumbers;
import com.healthcare.api.sms.SmsSender;
import com.healthcare.api.state.AlarmEntity;
import com.healthcare.api.state.AlarmRepository;
import com.healthcare.api.state.HealthStateEntity;
import com.healthcare.api.state.HealthStateRepository;
import com.healthcare.api.user.User;
import com.healthcare.api.user.UserDto;
import com.healthcare.api.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证业务：验证码校验、用户创建、密码处理和 token 签发。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final HealthStateRepository healthStates;
    private final AlarmRepository alarms;
    private final VerificationCodeService codes;
    private final TokenService tokens;
    private final RateLimitService rateLimits;
    private final SmsSender smsSender;
    private final StepUpService stepUp;
    private final boolean devMode;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository users,
                       HealthStateRepository healthStates,
                       AlarmRepository alarms,
                       VerificationCodeService codes,
                       TokenService tokens,
                       RateLimitService rateLimits,
                       SmsSender smsSender,
                       StepUpService stepUp,
                       @Value("${app.dev-mode:true}") boolean devMode) {
        this.users = users;
        this.healthStates = healthStates;
        this.alarms = alarms;
        this.codes = codes;
        this.tokens = tokens;
        this.rateLimits = rateLimits;
        this.smsSender = smsSender;
        this.stepUp = stepUp;
        this.devMode = devMode;
    }

    public String sendCode(String phone) {
        String normalized = PhoneNumbers.normalizeAndValidate(phone);
        rateLimits.checkSendCode(normalized);
        String code = codes.issue(normalized, VerificationCodeService.PURPOSE_LOGIN);
        if (devMode) {
            log.info("Dev verification code for {}: {}", normalized, code);
        } else {
            smsSender.sendVerificationCode(normalized, code, VerificationCodeService.PURPOSE_LOGIN);
        }
        return code;
    }

    @Transactional
    public AuthDtos.IssuedLogin login(String phone, String code, String userAgent, String ip) {
        String normalized = PhoneNumbers.normalizeAndValidate(phone);
        rateLimits.checkLogin(normalized);
        if (!codes.verify(normalized, code, VerificationCodeService.PURPOSE_LOGIN)) {
            throw new ApiException(400, "验证码错误或已过期");
        }

        boolean newUser = false;
        User user = users.findByPhone(normalized).orElse(null);
        if (user == null) {
            user = new User();
            user.setPhone(normalized);
            user.setName(defaultName(normalized));
            user = users.save(user);
            newUser = true;
        }
        ensureState(user);

        String token = tokens.issue(user.getId(), userAgent, ip);
        return new AuthDtos.IssuedLogin(token, UserDto.from(user), newUser);
    }

    @Transactional
    public AuthDtos.IssuedLogin loginWithPassword(
            String phone, String password, String userAgent, String ip) {
        String normalized = PhoneNumbers.normalizeAndValidate(phone);
        rateLimits.checkLogin(normalized);
        User user = users.findByPhone(normalized)
                .orElseThrow(() -> new ApiException(400, "手机号或密码错误"));
        if (user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(400, "手机号或密码错误");
        }
        ensureState(user);
        String token = tokens.issue(user.getId(), userAgent, ip);
        return new AuthDtos.IssuedLogin(token, UserDto.from(user), false);
    }

    @Transactional
    public AuthDtos.IssuedLogin resetPassword(
            String phone, String code, String newPassword, String userAgent, String ip) {
        String normalized = PhoneNumbers.normalizeAndValidate(phone);
        rateLimits.checkLogin(normalized);
        if (!codes.verify(normalized, code, VerificationCodeService.PURPOSE_LOGIN)) {
            throw new ApiException(400, "验证码错误或已过期");
        }
        User user = users.findByPhone(normalized)
                .orElseThrow(() -> new ApiException(404, "用户不存在，请先登录"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        // 重置密码后让所有旧会话失效，再签发新 token
        tokens.revokeAll(user.getId());
        ensureState(user);
        String token = tokens.issue(user.getId(), userAgent, ip);
        return new AuthDtos.IssuedLogin(token, UserDto.from(user), false);
    }

    /** 登录设备列表。 */
    public List<AuthDtos.SessionDto> sessions(Long userId, String currentRawToken) {
        return tokens.sessions(userId, currentRawToken);
    }

    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        tokens.revokeSession(userId, sessionId);
    }

    @Transactional
    public void revokeOtherSessions(Long userId, String currentRawToken) {
        tokens.revokeOthers(userId, currentRawToken);
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.ChangePasswordRequest request,
                               String currentRawToken) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
        // 有密码时校验当前密码；首次设置密码时校验当前手机号验证码
        stepUp.requireCredential(user, request.oldPassword(), request.currentCode());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        // 修改密码后保留当前会话，其他设备上的登录全部失效
        tokens.revokeOthers(userId, currentRawToken);
    }

    public void logout(String rawToken) {
        tokens.revoke(rawToken);
    }

    private void ensureState(User user) {
        if (healthStates.findById(user.getId()).isEmpty()) {
            healthStates.save(HealthStateEntity.defaults(user.getId()));
            alarms.saveAll(defaultAlarms(user.getId()));
        }
    }

    private List<AlarmEntity> defaultAlarms(Long userId) {
        return List.of(
                alarm(userId, "08:30", "工作日", "1,2,3,4,5", true),
                alarm(userId, "09:00", "周末", "0,6", false));
    }

    private AlarmEntity alarm(Long userId, String time, String label, String repeatDays, boolean enabled) {
        AlarmEntity entity = new AlarmEntity();
        entity.setUserId(userId);
        entity.setTime(time);
        entity.setLabel(label);
        entity.setRepeatDays(repeatDays);
        entity.setEnabled(enabled);
        return entity;
    }

    /** 新用户默认昵称：用户 + 手机号后四位。 */
    private String defaultName(String phone) {
        return "用户" + phone.substring(phone.length() - 4);
    }
}
