package com.healthcare.api.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务：6 位验证码、5 分钟有效、最多 5 次尝试。
 * 验证码按「用途 + 手机号」隔离，登录验证码不能用于修改手机号等其他场景。
 */
@Service
public class VerificationCodeService {

    public static final String PURPOSE_LOGIN = "login";
    public static final String PURPOSE_CHANGE_PHONE = "change-phone";
    public static final String PURPOSE_STEP_UP = "step-up";

    private static final int TTL_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final class Entry {
        private final String code;
        private final Instant expiresAt;
        private int failedAttempts;

        private Entry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    public String issue(String phone) {
        return issue(phone, PURPOSE_LOGIN);
    }

    public String issue(String phone, String purpose) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(key(phone, purpose), new Entry(code, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    public boolean verify(String phone, String code) {
        return verify(phone, code, PURPOSE_LOGIN);
    }

    public boolean verify(String phone, String code, String purpose) {
        if (phone == null || code == null) {
            return false;
        }
        boolean[] matched = {false};
        codes.computeIfPresent(key(phone, purpose), (key, entry) -> {
            if (entry.expiresAt.isBefore(Instant.now())) {
                return null;
            }
            if (entry.code.equals(code)) {
                matched[0] = true;
                return null;
            }
            entry.failedAttempts++;
            return entry.failedAttempts >= MAX_ATTEMPTS ? null : entry;
        });
        return matched[0];
    }

    private String key(String phone, String purpose) {
        return purpose + ":" + phone;
    }
}
