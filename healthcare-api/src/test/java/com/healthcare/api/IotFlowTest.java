package com.healthcare.api;

import com.healthcare.api.iot.IotDeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/catalog-test.sql")
class IotFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private IotDeviceService iotDeviceService;

    @Test
    void bindIngestAndUnbindDevice() {
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.nanoTime());
        String productKey = "pk-" + suffix;
        HttpHeaders headers = login("1360000" + suffix.substring(suffix.length() - 4));

        ResponseEntity<Map> bound = rest.exchange(
                base + "/api/iot/devices",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("productKey", productKey, "deviceName", "watch-1", "name", "测试手表"),
                        headers),
                Map.class);
        assertThat(bound.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> device = (Map<?, ?>) bound.getBody().get("data");
        long deviceId = ((Number) device.get("id")).longValue();
        assertThat(device.get("name")).isEqualTo("测试手表");

        ResponseEntity<Map> listed = rest.exchange(
                base + "/api/iot/devices", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat((List<?>) listed.getBody().get("data")).hasSize(1);

        // 模拟上报后健康数据更新
        ResponseEntity<Map> simulated = rest.exchange(
                base + "/api/iot/devices/" + deviceId + "/simulate",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(simulated.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> stateAfterSimulate = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> healthAfterSimulate =
                (Map<?, ?>) ((Map<?, ?>) stateAfterSimulate.getBody().get("data")).get("health");
        assertThat(((Number) healthAfterSimulate.get("steps")).intValue()).isPositive();
        assertThat(healthAfterSimulate.get("deviceConnected")).isEqualTo(true);

        // 连续模拟应在上次数据上累加，而不是从头随机
        rest.exchange(
                base + "/api/iot/devices/" + deviceId + "/simulate",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class);
        ResponseEntity<Map> stateAfterSecondSimulate = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> healthAfterSecondSimulate =
                (Map<?, ?>) ((Map<?, ?>) stateAfterSecondSimulate.getBody().get("data")).get("health");
        assertThat(((Number) healthAfterSecondSimulate.get("steps")).intValue())
                .isGreaterThan(((Number) healthAfterSimulate.get("steps")).intValue());
        assertThat(((Number) healthAfterSecondSimulate.get("deviceBattery")).intValue())
                .isLessThanOrEqualTo(((Number) healthAfterSimulate.get("deviceBattery")).intValue());

        // MQTT 路径（直接调用入库服务）解析 Alink 物模型
        iotDeviceService.ingestByKey(
                productKey,
                "watch-1",
                "sys/" + productKey + "/watch-1/thing/event/property/post",
                "{\"id\":\"1\",\"version\":\"1.0\",\"method\":\"thing.event.property.post\","
                        + "\"params\":{\"steps\":4321,\"waterMl\":1500,\"sleepMinutes\":450,"
                        + "\"weightKg\":61.5,\"battery\":88}}");
        ResponseEntity<Map> stateAfterIngest = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> healthAfterIngest =
                (Map<?, ?>) ((Map<?, ?>) stateAfterIngest.getBody().get("data")).get("health");
        assertThat(healthAfterIngest.get("steps")).isEqualTo(4321);
        assertThat(healthAfterIngest.get("waterMl")).isEqualTo(1500);
        assertThat(healthAfterIngest.get("sleepMinutes")).isEqualTo(450);
        assertThat(healthAfterIngest.get("deviceBattery")).isEqualTo(88);

        // 手机演示页提交指定数据
        ResponseEntity<Map> reported = rest.exchange(
                base + "/api/iot/devices/" + deviceId + "/report",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("steps", 7777, "waterMl", 999), headers),
                Map.class);
        assertThat(reported.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> stateAfterReport = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> healthAfterReport =
                (Map<?, ?>) ((Map<?, ?>) stateAfterReport.getBody().get("data")).get("health");
        assertThat(healthAfterReport.get("steps")).isEqualTo(7777);
        assertThat(healthAfterReport.get("waterMl")).isEqualTo(999);

        // 其他账号不能绑定同一台设备
        HttpHeaders otherUser = login("1350000" + suffix.substring(suffix.length() - 4));
        ResponseEntity<Map> conflict = rest.exchange(
                base + "/api/iot/devices",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("productKey", productKey, "deviceName", "watch-1", "name", "抢占"),
                        otherUser),
                Map.class);
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);

        // 解绑后列表为空
        ResponseEntity<Map> unbound = rest.exchange(
                base + "/api/iot/devices/" + deviceId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(unbound.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> afterUnbind = rest.exchange(
                base + "/api/iot/devices", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat((List<?>) afterUnbind.getBody().get("data")).isEmpty();
    }

    private HttpHeaders login(String phone) {
        String base = "http://localhost:" + port;
        ResponseEntity<Map> sendCode = rest.postForEntity(
                base + "/api/auth/send-code", Map.of("phone", phone), Map.class);
        String code = (String) ((Map<?, ?>) sendCode.getBody().get("data")).get("devCode");
        ResponseEntity<Map> login = rest.postForEntity(
                base + "/api/auth/login", Map.of("phone", phone, "code", code), Map.class);
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.substring(0, setCookie.indexOf(';')));
        return headers;
    }
}
