package com.healthcare.api.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Web Push 请求/响应 DTO。
 */
public final class PushDtos {

    private PushDtos() {
    }

    public record Keys(
            @NotBlank(message = "p256dh 不能为空") String p256dh,
            @NotBlank(message = "auth 不能为空") String auth) {
    }

    public record SubscribeRequest(
            @NotBlank(message = "endpoint 不能为空") String endpoint,
            @NotNull(message = "keys 不能为空") @Valid Keys keys) {
    }

    public record UnsubscribeRequest(@NotBlank(message = "endpoint 不能为空") String endpoint) {
    }

    public record PublicKeyResponse(String publicKey) {
    }
}
