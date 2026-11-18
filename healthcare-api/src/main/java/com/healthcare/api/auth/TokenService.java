package com.healthcare.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Token 服务：生成随机 token、哈希存储、过期校验和撤销。
 */
@Service
public class TokenService {

    private final AuthTokenRepository tokens;
    private final long ttlDays;
    private final SecureRandom random = new SecureRandom();

    public TokenService(AuthTokenRepository tokens,
                        @Value("${app.token-ttl-days:7}") long ttlDays) {
        this.tokens = tokens;
        this.ttlDays = ttlDays;
    }

    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthTokenEntity entity = new AuthTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS));
        tokens.save(entity);
        return rawToken;
    }

    public Optional<Long> resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(hash(rawToken))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .map(AuthTokenEntity::getUserId);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            tokens.deleteByTokenHash(hash(rawToken));
        }
    }

    /** 撤销该用户除当前 token 外的其他登录会话（修改密码后使用）。 */
    @Transactional
    public void revokeOthers(Long userId, String currentRawToken) {
        if (userId == null) {
            return;
        }
        if (currentRawToken == null || currentRawToken.isBlank()) {
            tokens.deleteByUserId(userId);
            return;
        }
        tokens.deleteByUserIdAndTokenHashNot(userId, hash(currentRawToken));
    }

    /** 撤销该用户全部登录会话（重置密码后使用）。 */
    @Transactional
    public void revokeAll(Long userId) {
        tokens.deleteByUserId(userId);
    }

    /** 每天清理过期 token，避免表无限增长。 */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        tokens.deleteByExpiresAtBefore(Instant.now());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
