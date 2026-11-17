package com.healthcare.api.auth;

import com.healthcare.api.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookies;
    private final boolean devMode;

    public AuthController(AuthService authService,
                          AuthCookieService authCookies,
                          @Value("${app.dev-mode:true}") boolean devMode) {
        this.authService = authService;
        this.authCookies = authCookies;
        this.devMode = devMode;
    }

    @PostMapping("/send-code")
    public ApiResponse<Map<String, Object>> sendCode(
            @Valid @RequestBody AuthDtos.SendCodeRequest request) {
        String code = authService.sendCode(request.phone());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "验证码已发送");
        if (devMode) {
            data.put("devCode", code);
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletResponse response) {
        AuthDtos.LoginResponse result = authService.login(request.phone(), request.code());
        authCookies.writeToken(response, result.token());
        return ApiResponse.ok(result);
    }

    @PostMapping("/login-password")
    public ApiResponse<AuthDtos.LoginResponse> loginWithPassword(
            @Valid @RequestBody AuthDtos.LoginPasswordRequest request,
            HttpServletResponse response) {
        AuthDtos.LoginResponse result =
                authService.loginWithPassword(request.phone(), request.password());
        authCookies.writeToken(response, result.token());
        return ApiResponse.ok(result);
    }

    @PostMapping("/reset-password")
    public ApiResponse<AuthDtos.LoginResponse> resetPassword(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request,
            HttpServletResponse response) {
        AuthDtos.LoginResponse result =
                authService.resetPassword(request.phone(), request.code(), request.newPassword());
        authCookies.writeToken(response, result.token());
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request,
            HttpServletResponse response) {
        String cookieToken = authCookies.readToken(request);
        String headerToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length())
                : null;
        authService.logout(cookieToken);
        if (headerToken != null && !headerToken.equals(cookieToken)) {
            authService.logout(headerToken);
        }
        authCookies.clearToken(response);
        return ApiResponse.ok();
    }
}
