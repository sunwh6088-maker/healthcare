package com.healthcare.api.importdata;

import java.time.LocalDate;

/**
 * iPhone 快捷指令健康数据导入 DTO。
 */
public final class HealthImportDtos {

    private HealthImportDtos() {
    }

    public record ImportRequest(
            String date,
            Integer steps,
            Integer sleepMinutes,
            Integer activeMinutes,
            Double weightKg,
            Integer battery) {
    }

    public record ImportResult(
            LocalDate date,
            int steps,
            int sleepMinutes,
            int activeMinutes,
            Double weightKg,
            Integer battery) {
    }

    public record TokenResponse(String token) {
    }

    public record TokenStatus(boolean hasToken) {
    }
}
