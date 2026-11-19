package com.healthcare.api.iot;

import com.healthcare.api.auth.AuthInterceptor;
import com.healthcare.api.common.ApiException;
import com.healthcare.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IoT 设备接口：绑定、解绑、列表和开发环境模拟上报。
 */
@RestController
@RequestMapping("/api/iot")
public class IotController {

    private final IotDeviceService deviceService;
    private final boolean devMode;

    public IotController(IotDeviceService deviceService,
                         @Value("${app.dev-mode:true}") boolean devMode) {
        this.deviceService = deviceService;
        this.devMode = devMode;
    }

    @GetMapping("/devices")
    public ApiResponse<List<IotDtos.DeviceDto>> devices(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId) {
        return ApiResponse.ok(deviceService.list(userId));
    }

    @PostMapping("/devices")
    public ApiResponse<IotDtos.DeviceDto> bind(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @Valid @RequestBody IotDtos.BindRequest request) {
        return ApiResponse.ok(deviceService.bind(userId, request));
    }

    @DeleteMapping("/devices/{id}")
    public ApiResponse<Void> unbind(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long id) {
        deviceService.unbind(userId, id);
        return ApiResponse.ok();
    }

    /** 无真实设备时模拟一次 Alink 属性上报。仅开发环境开放。 */
    @PostMapping("/devices/{id}/simulate")
    public ApiResponse<IotDtos.DeviceDto> simulate(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long id) {
        if (!devMode) {
            throw new ApiException(404, "接口不存在");
        }
        return ApiResponse.ok(deviceService.simulate(userId, id));
    }

    /** 手机演示页提交指定数据（开发环境）。 */
    @PostMapping("/devices/{id}/report")
    public ApiResponse<IotDtos.DeviceDto> report(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTR) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody IotDtos.ReportRequest request) {
        if (!devMode) {
            throw new ApiException(404, "接口不存在");
        }
        return ApiResponse.ok(deviceService.report(userId, id, request));
    }
}
