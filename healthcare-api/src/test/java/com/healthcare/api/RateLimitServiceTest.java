package com.healthcare.api;

import com.healthcare.api.auth.RateLimitService;
import com.healthcare.api.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Test
    void sendCodeEnforcesCooldown() {
        RateLimitService limits = new RateLimitService(60, 10, 10);
        assertThatCode(() -> limits.checkSendCode("13800000000")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.checkSendCode("13800000000"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("频繁");
    }

    @Test
    void sendCodeEnforcesHourlyLimit() {
        RateLimitService limits = new RateLimitService(0, 2, 10);
        limits.checkSendCode("13800000001");
        limits.checkSendCode("13800000001");
        assertThatThrownBy(() -> limits.checkSendCode("13800000001"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("次数过多");
    }

    @Test
    void loginEnforcesAttemptLimit() {
        RateLimitService limits = new RateLimitService(0, 10, 2);
        limits.checkLogin("13800000002");
        limits.checkLogin("13800000002");
        assertThatThrownBy(() -> limits.checkLogin("13800000002"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("频繁");
    }

    @Test
    void differentPhonesHaveSeparateBuckets() {
        RateLimitService limits = new RateLimitService(60, 10, 10);
        limits.checkSendCode("13800000003");
        assertThatCode(() -> limits.checkSendCode("13800000004")).doesNotThrowAnyException();
    }
}
