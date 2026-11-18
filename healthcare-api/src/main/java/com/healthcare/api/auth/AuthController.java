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

/**
 * 认证接口：验证码/密码登录、重置密码和退出登录。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookies;
    private final RateLimitService rateLimits;
    private final boolean devMode;

    public AuthController(AuthService authService,
                          AuthCookieService authCookies,
                          RateLimitService rateLimits,
                          @Value("${app.dev-mode:true}") boolean devMode) {
        this.authService = authService;
        this.authCookies = authCookies;
        this.rateLimits = rateLimits;
        this.devMode = devMode;
    }

    @PostMapping("/send-code")
    public ApiResponse<Map<String, Object>> sendCode(
            @Valid @RequestBody AuthDtos.SendCodeRequest request,
            HttpServletRequest httpRequest) {
        rateLimits.checkSendCodeIp(clientIp(httpRequest));
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
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        rateLimits.checkLoginIp(clientIp(httpRequest));
        AuthDtos.IssuedLogin result = authService.login(
                request.phone(), request.code(), userAgent(httpRequest), clientIp(httpRequest));
        authCookies.writeToken(response, result.token());
        return ApiResponse.ok(new AuthDtos.LoginResponse(result.user(), result.newUser()));
    }

    @PostMapping("/login-password")
    public ApiResponse<AuthDtos.LoginResponse> loginWithPassword(
            @Valid @RequestBody AuthDtos.LoginPasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        rateLimits.checkLoginIp(clientIp(httpRequest));
        AuthDtos.IssuedLogin result = authService.loginWithPassword(
                request.phone(), request.password(), userAgent(httpRequest), clientIp(httpRequest));
        authCookies.writeToken(response, result.token());
        return ApiResponse.ok(new AuthDtos.LoginResponse(result.user(), result.newUser()));
    }

    @PostMapping("/reset-password")
    public ApiResponse<AuthDtos.LoginResponse> resetPassword(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        rateLimits.checkLoginIp(clientIp(httpRequest));
        AuthDtos.IssuedLogin result = authService.resetPassword(
                request.phone(), request.code(), request.newPassword(),
                userAgent(httpRequest), clientIp(httpRequest));
        authCookies.writeToken(response, result.token());
        return ApiResponse.ok(new AuthDtos.LoginResponse(result.user(), result.newUser()));
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

    /** 反向代理后取 X-Forwarded-For 的第一个地址；直连时使用 remoteAddr。 */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
