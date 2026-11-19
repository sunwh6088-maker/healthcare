package com.healthcare.api;

import com.healthcare.api.auth.RateLimitService;
import com.healthcare.api.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Test
    void sendCodeEnforcesCooldown() {
        RateLimitService limits = new RateLimitService(60, 10, 10, 5, 100, 30, 1000);
        assertThatCode(() -> limits.checkSendCode("13800000000")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.checkSendCode("13800000000"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("频繁");
    }

    @Test
    void sendCodeEnforcesHourlyLimit() {
        RateLimitService limits = new RateLimitService(0, 2, 10, 5, 100, 30, 1000);
        limits.checkSendCode("13800000001");
        limits.checkSendCode("13800000001");
        assertThatThrownBy(() -> limits.checkSendCode("13800000001"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("次数过多");
    }

    @Test
    void loginEnforcesAttemptLimit() {
        RateLimitService limits = new RateLimitService(0, 10, 2, 5, 100, 30, 1000);
        limits.checkLogin("13800000002");
        limits.checkLogin("13800000002");
        assertThatThrownBy(() -> limits.checkLogin("13800000002"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("频繁");
    }

    @Test
    void differentPhonesHaveSeparateBuckets() {
        RateLimitService limits = new RateLimitService(60, 10, 10, 5, 100, 30, 1000);
        limits.checkSendCode("13800000003");
        assertThatCode(() -> limits.checkSendCode("13800000004")).doesNotThrowAnyException();
    }

    @Test
    void ipLimitsProtectAgainstPhoneRotation() {
        RateLimitService limits = new RateLimitService(0, 1000, 1000, 5, 2, 1, 1000);
        limits.checkLoginIp("203.0.113.8");
        limits.checkLoginIp("203.0.113.8");
        assertThatThrownBy(() -> limits.checkLoginIp("203.0.113.8"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("频繁");

        limits.checkSendCodeIp("203.0.113.9");
        assertThatThrownBy(() -> limits.checkSendCodeIp("203.0.113.9"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("频繁");
    }
}
