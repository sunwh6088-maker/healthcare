package com.healthcare.api.iot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * IoT 设备绑定与上报 DTO。
 */
public final class IotDtos {

    private IotDtos() {
    }

    public record DeviceDto(
            Long id,
            String productKey,
            String deviceName,
            String name,
            boolean connected,
            int battery,
            Instant lastSeenAt) {

        public static DeviceDto from(IotDeviceEntity entity) {
            return new DeviceDto(
                    entity.getId(),
                    entity.getProductKey(),
                    entity.getDeviceName(),
                    entity.getDisplayName(),
                    entity.isConnected(),
                    entity.getBattery(),
                    entity.getLastSeenAt());
        }
    }

    public record BindRequest(
            @NotBlank(message = "productKey 不能为空")
            @Size(max = 64, message = "productKey 过长") String productKey,
            @NotBlank(message = "deviceName 不能为空")
            @Size(max = 64, message = "deviceName 过长") String deviceName,
            @Size(max = 64, message = "设备名称过长") String name) {
    }

    /** 手机演示页提交的一次设备数据。 */
    public record ReportRequest(
            Integer steps,
            Integer waterMl,
            Integer sleepMinutes,
            Double weightKg,
            Integer battery) {
    }

    public record RenameRequest(
            @NotBlank(message = "设备名称不能为空")
            @Size(max = 64, message = "设备名称过长") String name) {
    }
}
