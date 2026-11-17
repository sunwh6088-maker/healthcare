package com.healthcare.api.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

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
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(phone, new Entry(code, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    public boolean verify(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        boolean[] matched = {false};
        codes.computeIfPresent(phone, (key, entry) -> {
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
}
