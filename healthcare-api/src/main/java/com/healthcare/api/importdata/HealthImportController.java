package com.healthcare.api.importdata;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * iPhone 快捷指令导入接口。
 * 外部调用使用 X-Import-Token 或 Authorization: Bearer <token>，不使用登录 Cookie。
 */
@RestController
public class HealthImportController {

    private final HealthImportService importService;

    public HealthImportController(HealthImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/api/me/import-token")
    public ApiResponse<HealthImportDtos.TokenResponse> generateToken(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(new HealthImportDtos.TokenResponse(
                importService.generateToken(userId)));
    }

    @GetMapping("/api/me/import-token")
    public ApiResponse<HealthImportDtos.TokenStatus> tokenStatus(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(new HealthImportDtos.TokenStatus(importService.hasToken(userId)));
    }

    @PostMapping("/api/import/health")
    public ApiResponse<HealthImportDtos.ImportResult> importHealth(
            @RequestHeader(value = "X-Import-Token", required = false) String headerToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody HealthImportDtos.ImportRequest request) {
        String token = headerToken;
        if ((token == null || token.isBlank())
                && authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring("Bearer ".length());
        }
        return ApiResponse.ok(importService.importData(token, request));
    }
}
