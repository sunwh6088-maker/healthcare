package com.healthcare.api.push;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web Push 接口：获取公钥、订阅/退订、发送测试通知。
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final WebPushService pushService;

    public PushController(WebPushService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/public-key")
    public ApiResponse<PushDtos.PublicKeyResponse> publicKey() {
        return ApiResponse.ok(new PushDtos.PublicKeyResponse(pushService.publicKey()));
    }

    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody PushDtos.SubscribeRequest request) {
        pushService.subscribe(userId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/unsubscribe")
    public ApiResponse<Void> unsubscribe(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody PushDtos.UnsubscribeRequest request) {
        pushService.unsubscribe(userId, request.endpoint());
        return ApiResponse.ok();
    }

    @PostMapping("/test")
    public ApiResponse<Integer> test(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(pushService.sendTest(userId));
    }
}
