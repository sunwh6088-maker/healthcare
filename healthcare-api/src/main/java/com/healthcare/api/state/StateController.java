package com.healthcare.api.state;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康状态接口：读取/整体保存健康数据和闹钟。
 */
@RestController
@RequestMapping("/api/state")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    public ApiResponse<StateDto> get(@RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(stateService.get(userId));
    }

    @PutMapping
    public ApiResponse<StateDto> save(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @RequestBody StateDto dto) {
        return ApiResponse.ok(stateService.save(userId, dto));
    }
}
