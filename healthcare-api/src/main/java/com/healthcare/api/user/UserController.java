package com.healthcare.api.user;

import com.healthcare.api.auth.AuthCookieService;
import com.healthcare.api.auth.AuthDtos;
import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.auth.AuthService;
import com.healthcare.api.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 账号接口：当前账号资料、修改资料和密码。
 */
@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final AvatarService avatars;
    private final AccountExportService exportService;
    private final AuthCookieService authCookies;

    public UserController(UserService userService,
                          AuthService authService,
                          AvatarService avatars,
                          AccountExportService exportService,
                          AuthCookieService authCookies) {
        this.userService = userService;
        this.authService = authService;
        this.avatars = avatars;
        this.exportService = exportService;
        this.authCookies = authCookies;
    }

    @GetMapping
    public ApiResponse<UserDto> me(@RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(UserDto.from(userService.get(userId)));
    }

    @PatchMapping
    public ApiResponse<UserDto> update(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.ok(userService.update(userId, request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.changePassword(userId, request, currentToken(httpRequest));
        return ApiResponse.ok();
    }

    /** 当前账号的登录设备列表（不暴露 token）。 */
    @GetMapping("/sessions")
    public ApiResponse<List<AuthDtos.SessionDto>> sessions(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.sessions(userId, currentToken(httpRequest)));
    }

    /** 退出指定设备。 */
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> revokeSession(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long id) {
        authService.revokeSession(userId, id);
        return ApiResponse.ok();
    }

    /** 退出除当前设备外的所有登录会话。 */
    @DeleteMapping("/sessions")
    public ApiResponse<Void> revokeOtherSessions(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            HttpServletRequest httpRequest) {
        authService.revokeOtherSessions(userId, currentToken(httpRequest));
        return ApiResponse.ok();
    }

    /** 导出当前用户的全部个人数据（JSON 下载）。 */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"healthcare-export.json\"")
                .body(exportService.export(userId));
    }

    /** 读取当前用户头像；未设置时返回 404。 */
    @GetMapping("/avatar")
    public ResponseEntity<Resource> avatar(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        Path file = avatars.find(userId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(AvatarService.contentType(file)))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(new FileSystemResource(file));
    }

    /** 上传/替换头像：JPG、PNG、WebP，最大 2MB。 */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserDto> uploadAvatar(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(userService.updateAvatar(userId, file));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<UserDto> deleteAvatar(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(userService.removeAvatar(userId));
    }

    /** 修改手机号第 1 步：向新号码发送验证码（开发模式返回 devCode）。 */
    @PostMapping("/phone/send-code")
    public ApiResponse<UserDto.PhoneCodeResponse> sendPhoneChangeCode(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody UserDto.PhoneSendCodeRequest request) {
        String devCode = userService.sendPhoneChangeCode(userId, request.phone());
        return ApiResponse.ok(new UserDto.PhoneCodeResponse("验证码已发送", devCode));
    }

    /** 修改手机号第 2 步：验证新号码验证码并完成绑定。 */
    @PutMapping("/phone")
    public ApiResponse<UserDto> updatePhone(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody UserDto.PhoneUpdateRequest request) {
        return ApiResponse.ok(userService.updatePhone(userId, request.phone(), request.code()));
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAccount(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        userService.deleteAccount(userId);
        return ApiResponse.ok();
    }

    /** 当前请求使用的 token，用于修改密码时保留本机会话。 */
    private String currentToken(HttpServletRequest request) {
        String cookieToken = authCookies.readToken(request);
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith("Bearer ")
                ? header.substring("Bearer ".length())
                : null;
    }
}
