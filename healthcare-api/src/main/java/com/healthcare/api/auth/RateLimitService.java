package com.healthcare.api.auth;

import com.healthcare.api.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内限流：验证码发送冷却/小时上限，以及登录尝试频率。
 * 单实例部署足够；多实例部署时应换成 Redis 等共享存储。
 */
@Service
public class RateLimitService {

    private final int sendCodeCooldownSeconds;
    private final int sendCodeMaxPerHour;
    private final int loginMaxAttemptsPerTenMinutes;
    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();

    public RateLimitService(
            @Value("${app.rate-limit.send-code-cooldown-seconds:60}") int sendCodeCooldownSeconds,
            @Value("${app.rate-limit.send-code-max-per-hour:10}") int sendCodeMaxPerHour,
            @Value("${app.rate-limit.login-max-attempts-per-10-min:10}") int loginMaxAttemptsPerTenMinutes) {
        this.sendCodeCooldownSeconds = sendCodeCooldownSeconds;
        this.sendCodeMaxPerHour = sendCodeMaxPerHour;
        this.loginMaxAttemptsPerTenMinutes = loginMaxAttemptsPerTenMinutes;
    }

    public void checkSendCode(String phone) {
        Instant now = Instant.now();
        events.compute("send-code:" + phone, (key, queue) -> {
            Deque<Instant> recent = purge(queue, now.minus(Duration.ofHours(1)));
            if (sendCodeCooldownSeconds > 0 && !recent.isEmpty()
                    && recent.peekLast().plusSeconds(sendCodeCooldownSeconds).isAfter(now)) {
                throw new ApiException(429, "验证码发送过于频繁，请稍后再试");
            }
            if (recent.size() >= sendCodeMaxPerHour) {
                throw new ApiException(429, "验证码发送次数过多，请稍后再试");
            }
            recent.addLast(now);
            return recent;
        });
    }

    public void checkLogin(String phone) {
        Instant now = Instant.now();
        events.compute("login:" + phone, (key, queue) -> {
            Deque<Instant> recent = purge(queue, now.minus(Duration.ofMinutes(10)));
            if (recent.size() >= loginMaxAttemptsPerTenMinutes) {
                throw new ApiException(429, "登录尝试过于频繁，请稍后再试");
            }
            recent.addLast(now);
            return recent;
        });
    }

    private Deque<Instant> purge(Deque<Instant> queue, Instant cutoff) {
        Deque<Instant> recent = queue == null ? new ArrayDeque<>() : queue;
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.pollFirst();
        }
        return recent;
    }
}
