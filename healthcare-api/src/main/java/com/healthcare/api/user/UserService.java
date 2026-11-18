package com.healthcare.api.user;

import com.healthcare.api.auth.RateLimitService;
import com.healthcare.api.auth.VerificationCodeService;
import com.healthcare.api.common.ApiException;
import com.healthcare.api.common.PhoneNumbers;
import com.healthcare.api.sms.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 账号业务：个人信息、手机号绑定（短信二次验证）、密码和注销。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final VerificationCodeService codes;
    private final RateLimitService rateLimits;
    private final SmsSender smsSender;
    private final AvatarService avatars;
    private final boolean devMode;

    public UserService(UserRepository users,
                       JdbcTemplate jdbc,
                       VerificationCodeService codes,
                       RateLimitService rateLimits,
                       SmsSender smsSender,
                       AvatarService avatars,
                       @Value("${app.dev-mode:true}") boolean devMode) {
        this.users = users;
        this.jdbc = jdbc;
        this.codes = codes;
        this.rateLimits = rateLimits;
        this.smsSender = smsSender;
        this.avatars = avatars;
        this.devMode = devMode;
    }

    public User get(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    @Transactional
    public UserDto update(Long userId, UserDto.UpdateRequest request) {
        User user = get(userId);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            String email = request.email().trim();
            user.setEmail(email.isEmpty() ? null : email);
        }

        return UserDto.from(users.save(user));
    }

    /**
     * 修改手机号第 1 步：校验新号码可用后发送验证码。
     *
     * @return 开发模式下返回验证码，生产环境返回 null
     */
    public String sendPhoneChangeCode(Long userId, String rawPhone) {
        User user = get(userId);
        String phone = PhoneNumbers.normalizeAndValidate(rawPhone);
        if (phone.equals(user.getPhone())) {
            throw new ApiException(400, "新手机号与当前手机号相同");
        }
        if (users.existsByPhoneAndIdNot(phone, userId)) {
            throw new ApiException(400, "该手机号已被其他账号使用");
        }
        rateLimits.checkPhoneChangeSendCode(userId);
        rateLimits.checkSendCode(phone);
        String code = codes.issue(phone, VerificationCodeService.PURPOSE_CHANGE_PHONE);
        if (devMode) {
            log.info("Dev phone-change code for user {} -> {}: {}", userId, phone, code);
            return code;
        }
        smsSender.sendVerificationCode(phone, code, VerificationCodeService.PURPOSE_CHANGE_PHONE);
        return null;
    }

    /** 修改手机号第 2 步：验证新号码收到的验证码后完成绑定。 */
    @Transactional
    public UserDto updatePhone(Long userId, String rawPhone, String code) {
        User user = get(userId);
        String phone = PhoneNumbers.normalizeAndValidate(rawPhone);
        if (phone.equals(user.getPhone())) {
            throw new ApiException(400, "新手机号与当前手机号相同");
        }
        if (users.existsByPhoneAndIdNot(phone, userId)) {
            throw new ApiException(400, "该手机号已被其他账号使用");
        }
        if (!codes.verify(phone, code, VerificationCodeService.PURPOSE_CHANGE_PHONE)) {
            throw new ApiException(400, "验证码错误或已过期");
        }
        user.setPhone(phone);
        return UserDto.from(users.save(user));
    }

    /** 上传或替换头像。 */
    @Transactional
    public UserDto updateAvatar(Long userId, MultipartFile file) {
        User user = get(userId);
        avatars.store(userId, file);
        user.setHasAvatar(true);
        user.setAvatarVersion(user.getAvatarVersion() + 1);
        return UserDto.from(users.save(user));
    }

    /** 删除头像（版本号继续自增，避免浏览器复用旧缓存）。 */
    @Transactional
    public UserDto removeAvatar(Long userId) {
        User user = get(userId);
        avatars.delete(userId);
        user.setHasAvatar(false);
        user.setAvatarVersion(user.getAvatarVersion() + 1);
        return UserDto.from(users.save(user));
    }

    /**
     * 注销账号：删除该用户的登录凭证、健康数据、闹钟、食物和运动记录。
     */
    @Transactional
    public void deleteAccount(Long userId) {
        get(userId);
        avatars.delete(userId);
        jdbc.update("DELETE FROM auth_tokens WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM alarms WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM food_logs WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM exercise_logs WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM health_states WHERE user_id = ?", userId);
        users.deleteById(userId);
    }
}
