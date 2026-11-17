package com.healthcare.api;

import com.healthcare.api.auth.VerificationCodeService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeServiceTest {

    @Test
    void correctCodeCanOnlyBeUsedOnce() {
        VerificationCodeService codes = new VerificationCodeService();
        String code = codes.issue("13800000010");

        assertThat(codes.verify("13800000010", code)).isTrue();
        assertThat(codes.verify("13800000010", code)).isFalse();
    }

    @Test
    void codeIsInvalidatedAfterFiveFailedAttempts() {
        VerificationCodeService codes = new VerificationCodeService();
        String code = codes.issue("13800000011");
        String wrong = "000000".equals(code) ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThat(codes.verify("13800000011", wrong)).isFalse();
        }

        assertThat(codes.verify("13800000011", code)).isFalse();
    }

    @Test
    void issuingAgainReplacesOldCode() {
        VerificationCodeService codes = new VerificationCodeService();
        String first = codes.issue("13800000012");
        String second = codes.issue("13800000012");

        assertThat(codes.verify("13800000012", first)).isFalse();
        assertThat(codes.verify("13800000012", second)).isTrue();
    }
}
