package com.healthcare.api.user;

import com.healthcare.api.auth.AuthDtos;
import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.auth.AuthService;
import com.healthcare.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号接口：当前账号资料、修改资料和密码。
 */
@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
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
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ApiResponse.ok();
    }
}
