package com.healthcare.api.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private static final int TTL_SECONDS = 300;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private record Entry(String code, Instant expiresAt) {
    }

    public String issue(String phone) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(phone, new Entry(code, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    public boolean verify(String phone, String code) {
        Entry entry = codes.get(phone);
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            codes.remove(phone);
            return false;
        }
        if (!entry.code().equals(code)) {
            return false;
        }
        codes.remove(phone);
        return true;
    }
}
