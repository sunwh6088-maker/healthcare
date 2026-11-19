package com.healthcare.api;

import com.healthcare.api.push.AlarmMatcher;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmMatcherTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void matchesTimeAndWeekday() {
        // 2026-11-16 是周一
        ZonedDateTime monday = ZonedDateTime.of(2026, 11, 16, 8, 30, 0, 0, ZONE);
        assertThat(AlarmMatcher.matches("08:30", "1,2,3,4,5", monday)).isTrue();
        assertThat(AlarmMatcher.matches("08:31", "1,2,3,4,5", monday)).isFalse();
        assertThat(AlarmMatcher.matches("08:30", "0,6", monday)).isFalse();
        assertThat(AlarmMatcher.matches("08:30", "", monday)).isTrue();
    }

    @Test
    void sundayUsesZero() {
        // 2026-11-22 是周日
        ZonedDateTime sunday = ZonedDateTime.of(2026, 11, 22, 9, 0, 0, 0, ZONE);
        assertThat(AlarmMatcher.matches("09:00", "0,6", sunday)).isTrue();
        assertThat(AlarmMatcher.matches("09:00", "1,2,3,4,5", sunday)).isFalse();
    }
}
