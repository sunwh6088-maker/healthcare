package com.healthcare.api.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.api.common.ApiException;
import com.healthcare.api.state.HealthStateEntity;
import com.healthcare.api.state.HealthStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * IoT 设备绑定、模拟上报与数据入库。
 * 接收 Alink 物模型属性上报，把步数、饮水、睡眠、体重、电量写入健康状态。
 */
@Service
public class IotDeviceService {

    private static final Logger log = LoggerFactory.getLogger(IotDeviceService.class);

    private final IotDeviceRepository devices;
    private final IotDeviceEventRepository events;
    private final HealthStateRepository healthStates;
    private final ObjectMapper objectMapper;

    public IotDeviceService(IotDeviceRepository devices,
                            IotDeviceEventRepository events,
                            HealthStateRepository healthStates,
                            ObjectMapper objectMapper) {
        this.devices = devices;
        this.events = events;
        this.healthStates = healthStates;
        this.objectMapper = objectMapper;
    }

    public List<IotDtos.DeviceDto> list(Long userId) {
        return devices.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(IotDtos.DeviceDto::from)
                .toList();
    }

    @Transactional
    public IotDtos.DeviceDto bind(Long userId, IotDtos.BindRequest request) {
        String productKey = request.productKey().trim();
        String deviceName = request.deviceName().trim();
        String displayName = request.name() == null || request.name().isBlank()
                ? deviceName
                : request.name().trim();

        IotDeviceEntity entity = devices.findByProductKeyAndDeviceName(productKey, deviceName)
                .orElseGet(IotDeviceEntity::new);
        if (entity.getId() != null && !entity.getUserId().equals(userId)) {
            throw new ApiException(409, "该设备已被其他账号绑定");
        }
        entity.setUserId(userId);
        entity.setProductKey(productKey);
        entity.setDeviceName(deviceName);
        entity.setDisplayName(displayName);
        return IotDtos.DeviceDto.from(devices.save(entity));
    }

    @Transactional
    public void unbind(Long userId, Long deviceId) {
        IotDeviceEntity entity = devices.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ApiException(404, "设备不存在"));
        events.deleteByDeviceId(entity.getId());
        devices.delete(entity);
    }

    /** 开发环境模拟一次设备上报：在现有数据上累加，模拟真实设备的连续上报。 */
    @Transactional
    public IotDtos.DeviceDto simulate(Long userId, Long deviceId) {
        IotDeviceEntity device = devices.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ApiException(404, "设备不存在"));
        HealthStateEntity current = healthStates.findById(userId)
                .orElseGet(() -> HealthStateEntity.defaults(userId));
        ThreadLocalRandom random = ThreadLocalRandom.current();

        int steps = current.getSteps() == 0
                ? 3200 + random.nextInt(1800)
                : Math.min(200_000, current.getSteps() + 100 + random.nextInt(800));
        int waterMl = current.getWaterMl() == 0
                ? 800 + random.nextInt(400)
                : Math.min(20_000, current.getWaterMl() + 80 + random.nextInt(220));
        int sleepMinutes = current.getSleepMinutes() > 0
                ? current.getSleepMinutes()
                : 400 + random.nextInt(80);
        double weightKg = current.getWeightKg() > 0
                ? Math.round((current.getWeightKg() + (random.nextDouble() - 0.5) * 0.4) * 10) / 10.0
                : Math.round((58 + random.nextDouble() * 10) * 10) / 10.0;
        int battery = current.getDeviceBattery() > 0
                ? Math.max(20, current.getDeviceBattery() - random.nextInt(2))
                : 85;

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("steps", steps);
        params.put("waterMl", waterMl);
        params.put("sleepMinutes", sleepMinutes);
        params.put("weightKg", weightKg);
        params.put("battery", battery);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(System.currentTimeMillis()));
        payload.put("version", "1.0");
        payload.put("method", "thing.event.property.post");
        payload.put("params", params);
        try {
            apply(device, "simulator", objectMapper.writeValueAsString(payload),
                    objectMapper.valueToTree(payload));
        } catch (Exception ex) {
            throw new ApiException(500, "模拟上报失败");
        }
        return IotDtos.DeviceDto.from(device);
    }

    /** 开发环境手机演示页提交数据：直接走同一套 Alink 解析与入库逻辑。 */
    @Transactional
    public IotDtos.DeviceDto report(Long userId, Long deviceId, IotDtos.ReportRequest request) {
        IotDeviceEntity device = devices.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ApiException(404, "设备不存在"));
        Map<String, Object> params = new LinkedHashMap<>();
        if (request.steps() != null) params.put("steps", request.steps());
        if (request.waterMl() != null) params.put("waterMl", request.waterMl());
        if (request.sleepMinutes() != null) params.put("sleepMinutes", request.sleepMinutes());
        if (request.weightKg() != null) params.put("weightKg", request.weightKg());
        if (request.battery() != null) params.put("battery", request.battery());
        if (params.isEmpty()) {
            throw new ApiException(400, "请至少填写一项设备数据");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", String.valueOf(System.currentTimeMillis()));
        payload.put("version", "1.0");
        payload.put("method", "thing.event.property.post");
        payload.put("params", params);
        try {
            apply(device, "phone-demo",
                    objectMapper.writeValueAsString(payload), objectMapper.valueToTree(payload));
        } catch (Exception ex) {
            throw new ApiException(500, "设备数据保存失败");
        }
        return IotDtos.DeviceDto.from(device);
    }

    /** MQTT 消息入口：topic 约定为 sys/{productKey}/{deviceName}/...。 */
    @Transactional
    public void ingestByKey(String productKey, String deviceName, String topic, String payload) {
        IotDeviceEntity device = devices.findByProductKeyAndDeviceName(productKey, deviceName)
                .orElse(null);
        if (device == null) {
            log.info("Ignoring data from unbound device {}/{}", productKey, deviceName);
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            apply(device, topic, payload, node);
        } catch (Exception ex) {
            log.warn("Invalid IoT payload from {}/{}: {}", productKey, deviceName, ex.toString());
        }
    }

    private void apply(IotDeviceEntity device, String topic, String rawPayload, JsonNode node) {
        IotPayloadParser.HealthSample sample = IotPayloadParser.parse(node);
        HealthStateEntity state = healthStates.findById(device.getUserId())
                .orElseGet(() -> HealthStateEntity.defaults(device.getUserId()));

        if (valid(sample.steps(), 0, 200_000)) state.setSteps(sample.steps());
        if (valid(sample.waterMl(), 0, 20_000)) state.setWaterMl(sample.waterMl());
        if (valid(sample.sleepMinutes(), 0, 1440)) state.setSleepMinutes(sample.sleepMinutes());
        if (sample.weightKg() != null && sample.weightKg() >= 20 && sample.weightKg() <= 300) {
            state.setWeightKg(sample.weightKg());
        }
        int battery = valid(sample.battery(), 0, 100) ? sample.battery() : device.getBattery();
        state.setDeviceBattery(battery);
        state.setDeviceConnected(true);
        state.setDeviceLastSyncAt(Instant.now());
        healthStates.save(state);

        device.setConnected(true);
        device.setBattery(battery);
        device.setLastSeenAt(Instant.now());
        devices.save(device);

        IotDeviceEventEntity event = new IotDeviceEventEntity();
        event.setDeviceId(device.getId());
        event.setTopic(topic != null && topic.length() > 255 ? topic.substring(0, 255) : topic);
        event.setPayload(rawPayload);
        event.setReceivedAt(Instant.now());
        events.save(event);
    }

    private boolean valid(Integer value, int min, int max) {
        return value != null && value >= min && value <= max;
    }
}
