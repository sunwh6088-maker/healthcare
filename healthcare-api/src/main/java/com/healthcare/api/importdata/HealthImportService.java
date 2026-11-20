package com.healthcare.api.importdata;

import com.healthcare.api.auth.RateLimitService;
import com.healthcare.api.common.ApiException;
import com.healthcare.api.state.HealthStateEntity;
import com.healthcare.api.state.HealthStateRepository;
import com.healthcare.api.user.User;
import com.healthcare.api.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 外部健康数据导入：生成导入令牌、校验令牌、写入每日汇总与当前状态。
 */
@Service
public class HealthImportService {

    private static final Logger log = LoggerFactory.getLogger(HealthImportService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final UserRepository users;
    private final DailyMetricRepository metrics;
    private final HealthStateRepository healthStates;
    private final RateLimitService rateLimits;
    private final SecureRandom random = new SecureRandom();

    public HealthImportService(UserRepository users,
                               DailyMetricRepository metrics,
                               HealthStateRepository healthStates,
                               RateLimitService rateLimits) {
        this.users = users;
        this.metrics = metrics;
        this.healthStates = healthStates;
        this.rateLimits = rateLimits;
    }

    /** 生成/轮换导入令牌，返回明文（只此一次）。 */
    @Transactional
    public String generateToken(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.setImportTokenHash(hash(rawToken));
        user.setImportTokenCreatedAt(Instant.now());
        users.save(user);
        return rawToken;
    }

    public boolean hasToken(Long userId) {
        return users.findById(userId)
                .map(user -> user.getImportTokenHash() != null
                        && !user.getImportTokenHash().isBlank())
                .orElse(false);
    }

    @Transactional
    public HealthImportDtos.ImportResult importData(
            String rawToken, HealthImportDtos.ImportRequest request) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(401, "缺少导入令牌");
        }
        User user = users.findByImportTokenHash(hash(rawToken.trim()))
                .orElseThrow(() -> new ApiException(401, "导入令牌无效"));
        rateLimits.checkHealthImport(user.getId());

        LocalDate date = parseDate(request.date());
        validate(request);
        if (request.steps() == null && request.sleepMinutes() == null
                && request.activeMinutes() == null && request.weightKg() == null) {
            throw new ApiException(400, "请至少提供一项健康数据");
        }

        DailyMetricEntity metric = metrics.findByUserIdAndMetricDate(user.getId(), date)
                .orElseGet(() -> {
                    DailyMetricEntity created = new DailyMetricEntity();
                    created.setUserId(user.getId());
                    created.setMetricDate(date);
                    created.setSource("shortcut");
                    return created;
                });
        if (request.steps() != null) metric.setSteps(request.steps());
        if (request.sleepMinutes() != null) metric.setSleepMinutes(request.sleepMinutes());
        if (request.activeMinutes() != null) metric.setActiveMinutes(request.activeMinutes());
        if (request.weightKg() != null) metric.setWeightKg(request.weightKg());
        if (request.battery() != null) metric.setPhoneBattery(request.battery());
        metrics.save(metric);

        HealthStateEntity state = healthStates.findById(user.getId())
                .orElseGet(() -> HealthStateEntity.defaults(user.getId()));
        if (request.steps() != null) state.setSteps(request.steps());
        if (request.sleepMinutes() != null) state.setSleepMinutes(request.sleepMinutes());
        if (request.activeMinutes() != null) state.setActiveMinutes(request.activeMinutes());
        if (request.weightKg() != null) state.setWeightKg(request.weightKg());
        state.setDeviceLastSyncAt(Instant.now());
        healthStates.save(state);

        log.info("Imported health data for user {} on {}: steps={}, sleep={}, active={}",
                user.getId(), date, metric.getSteps(), metric.getSleepMinutes(),
                metric.getActiveMinutes());
        return new HealthImportDtos.ImportResult(
                date,
                metric.getSteps(),
                metric.getSleepMinutes(),
                metric.getActiveMinutes(),
                metric.getWeightKg(),
                metric.getPhoneBattery());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now(ZONE);
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ApiException(400, "日期格式应为 yyyy-MM-dd");
        }
    }

    private void validate(HealthImportDtos.ImportRequest request) {
        if (request.steps() != null && (request.steps() < 0 || request.steps() > 200_000)) {
            throw new ApiException(400, "步数超出范围");
        }
        if (request.sleepMinutes() != null
                && (request.sleepMinutes() < 0 || request.sleepMinutes() > 1440)) {
            throw new ApiException(400, "睡眠分钟数超出范围");
        }
        if (request.activeMinutes() != null
                && (request.activeMinutes() < 0 || request.activeMinutes() > 1440)) {
            throw new ApiException(400, "活动分钟数超出范围");
        }
        if (request.weightKg() != null
                && (request.weightKg() < 20 || request.weightKg() > 300)) {
            throw new ApiException(400, "体重超出范围");
        }
        if (request.battery() != null && (request.battery() < 0 || request.battery() > 100)) {
            throw new ApiException(400, "电量超出范围");
        }
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
