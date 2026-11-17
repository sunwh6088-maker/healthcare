package com.healthcare.api.state;

import java.time.Instant;
import java.util.List;

/**
 * 健康状态和闹钟的请求/响应 DTO。
 */
public record StateDto(HealthDto health, List<AlarmDto> alarms) {

    public record HealthDto(
            int steps,
            int stepsGoal,
            int waterMl,
            int waterGoalMl,
            int activeMinutes,
            int activeGoalMinutes,
            int sleepMinutes,
            int sleepStartMinutes,
            int foodKcal,
            int exerciseCount,
            boolean notifyEnabled,
            boolean privacyEnabled,
            boolean deviceConnected,
            boolean deviceAdded,
            int deviceBattery,
            Instant deviceLastSyncAt,
            String gender,
            int age,
            int heightCm,
            double weightKg) {
    }

    public record AlarmDto(Long id, String time, String label, List<Integer> repeat, boolean on) {
    }
}
