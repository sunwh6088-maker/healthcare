package com.healthcare.api.auth;

import com.healthcare.api.user.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 认证 DTO：登录、验证码和密码相关请求/响应。
 */
public final class AuthDtos {

    /** 8-64 位、不含空白，且同时包含字母和数字。 */
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,64}$";
    public static final String PASSWORD_MESSAGE = "密码需为 8-64 位，且同时包含字母和数字";

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
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record ChangePasswordRequest(
            @Size(max = 64, message = "当前密码过长") String oldPassword,
            /** 未设置密码时，用当前手机号验证码完成首次设置。 */
            String currentCode,
            @NotBlank(message = "新密码不能为空")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String newPassword) {
    }

    /** 接口返回给浏览器的登录结果：token 只通过 HttpOnly Cookie 下发。 */
    public record LoginResponse(UserDto user, boolean newUser) {
    }

    /** 服务内部使用的登录结果，token 由 Controller 写入 Cookie。 */
    public record IssuedLogin(String token, UserDto user, boolean newUser) {
    }

    /** 登录设备列表项：不暴露 token 本身。 */
    public record SessionDto(
            Long id,
            String device,
            String ip,
            Instant createdAt,
            Instant expiresAt,
            boolean current) {
    }
}
