package com.healthcare.api.auth;

import com.healthcare.api.user.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SendCodeRequest(@NotBlank(message = "手机号不能为空") String phone) {
    }

    public record LoginRequest(
            @NotBlank(message = "手机号不能为空") String phone,
            @NotBlank(message = "验证码不能为空") String code) {
    }

    public record LoginPasswordRequest(
            @NotBlank(message = "手机号不能为空") String phone,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "手机号不能为空") String phone,
            @NotBlank(message = "验证码不能为空") String code,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间") String newPassword) {
    }

    public record ChangePasswordRequest(
            @Size(max = 64, message = "当前密码过长") String oldPassword,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间") String newPassword) {
    }

    public record LoginResponse(String token, UserDto user, boolean newUser) {
    }
}
