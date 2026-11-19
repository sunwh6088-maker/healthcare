package com.healthcare.api.push;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 判断闹钟在当前分钟是否需要触发。
 * repeatDays 使用前端约定：0 = 周日，1 = 周一 … 6 = 周六；空表示仅一次。
 */
public final class AlarmMatcher {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private AlarmMatcher() {
    }

    public static boolean matches(String alarmTime, String repeatDays, ZonedDateTime now) {
        if (alarmTime == null || !alarmTime.equals(now.toLocalTime().format(HH_MM))) {
            return false;
        }
        if (repeatDays == null || repeatDays.isBlank()) {
            return true;
        }
        int day = now.getDayOfWeek().getValue() % 7;
        return Arrays.stream(repeatDays.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .anyMatch(value -> value.equals(String.valueOf(day)));
    }
}
