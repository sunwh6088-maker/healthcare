package com.healthcare.api.auth;

import com.healthcare.api.common.ApiException;
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
import java.util.List;
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
        return issue(userId, null, null);
    }

    public String issue(Long userId, String userAgent, String ipAddress) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthTokenEntity entity = new AuthTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS));
        entity.setUserAgent(truncate(userAgent, 255));
        entity.setIpAddress(truncate(ipAddress, 45));
        tokens.save(entity);
        return rawToken;
    }

    /** 当前用户的全部有效会话；current 标记本次请求使用的会话。 */
    public List<AuthDtos.SessionDto> sessions(Long userId, String currentRawToken) {
        String currentHash = currentRawToken == null || currentRawToken.isBlank()
                ? null
                : hash(currentRawToken);
        Instant now = Instant.now();
        return tokens.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(token -> token.getExpiresAt().isAfter(now))
                .map(token -> new AuthDtos.SessionDto(
                        token.getId(),
                        DeviceNames.describe(token.getUserAgent()),
                        displayIp(token.getIpAddress()),
                        token.getCreatedAt(),
                        token.getExpiresAt(),
                        token.getTokenHash().equals(currentHash)))
                .toList();
    }

    /** 本机回环地址统一显示为「本机」。 */
    private String displayIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "本机";
        }
        return ip;
    }

    /** 撤销指定会话；只能操作自己的会话。 */
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        AuthTokenEntity token = tokens.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ApiException(404, "登录会话不存在"));
        tokens.delete(token);
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

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
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
