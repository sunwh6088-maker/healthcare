package com.healthcare.api;

import com.healthcare.api.importdata.DailyMetricRepository;
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

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/catalog-test.sql")
class HealthImportTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DailyMetricRepository metrics;

    @Test
    void shortcutImportUpdatesHealthState() {
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.nanoTime());
        String phone = "1370000" + suffix.substring(suffix.length() - 4);
        HttpHeaders headers = login(base, phone);

        ResponseEntity<Map> tokenResponse = rest.exchange(
                base + "/api/me/import-token",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(tokenResponse.getStatusCode().value()).isEqualTo(200);
        String token = (String) ((Map<?, ?>) tokenResponse.getBody().get("data")).get("token");
        assertThat(token).isNotBlank();

        String today = LocalDate.now().toString();
        Map<String, Object> payload = Map.of(
                "date", today,
                "steps", 8888,
                "sleepMinutes", 480,
                "activeMinutes", 45,
                "weightKg", 63.2,
                "battery", 77);
        HttpHeaders importHeaders = new HttpHeaders();
        importHeaders.add("X-Import-Token", token);
        ResponseEntity<Map> imported = rest.exchange(
                base + "/api/import/health",
                HttpMethod.POST,
                new HttpEntity<>(payload, importHeaders),
                Map.class);
        assertThat(imported.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map> state = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> health = (Map<?, ?>) ((Map<?, ?>) state.getBody().get("data")).get("health");
        assertThat(health.get("steps")).isEqualTo(8888);
        assertThat(health.get("sleepMinutes")).isEqualTo(480);
        assertThat(health.get("activeMinutes")).isEqualTo(45);
        assertThat(((Number) health.get("weightKg")).doubleValue()).isEqualTo(63.2);

        Long userId = metrics.findAll().stream()
                .filter(metric -> metric.getSteps() == 8888)
                .findFirst()
                .orElseThrow()
                .getUserId();
        assertThat(metrics.findByUserIdAndMetricDate(userId, LocalDate.parse(today)))
                .hasValueSatisfying(metric -> {
                    assertThat(metric.getPhoneBattery()).isEqualTo(77);
                    assertThat(metric.getSource()).isEqualTo("shortcut");
                });

        // 错误令牌不能导入
        HttpHeaders badHeaders = new HttpHeaders();
        badHeaders.add("X-Import-Token", "invalid-token");
        ResponseEntity<Map> rejected = rest.exchange(
                base + "/api/import/health",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("steps", 1), badHeaders),
                Map.class);
        assertThat(rejected.getStatusCode().value()).isEqualTo(401);

        // 无令牌不能导入
        ResponseEntity<Map> missing = rest.exchange(
                base + "/api/import/health",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("steps", 1)),
                Map.class);
        assertThat(missing.getStatusCode().value()).isEqualTo(401);

        // 每日指标可按区间查询（历史页/步骤页使用）
        ResponseEntity<Map> metricResponse = rest.exchange(
                base + "/api/health-metrics?from=" + today + "&to=" + today,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(metricResponse.getStatusCode().value()).isEqualTo(200);
        java.util.List<Map<String, Object>> metrics =
                (java.util.List<Map<String, Object>>) metricResponse.getBody().get("data");
        assertThat(metrics).hasSize(1);
        assertThat(metrics.get(0).get("date")).isEqualTo(today);
        assertThat(metrics.get(0).get("steps")).isEqualTo(8888);
        assertThat(metrics.get(0).get("sleepMinutes")).isEqualTo(480);
        assertThat(metrics.get(0).get("battery")).isEqualTo(77);

        // 非法区间返回 400；未登录返回 401
        ResponseEntity<Map> badRange = rest.exchange(
                base + "/api/health-metrics?from=" + today + "&to=2020-01-01",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(badRange.getStatusCode().value()).isEqualTo(400);
        ResponseEntity<Map> unauthenticated = rest.getForEntity(
                base + "/api/health-metrics", Map.class);
        assertThat(unauthenticated.getStatusCode().value()).isEqualTo(401);
    }

    private HttpHeaders login(String base, String phone) {
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
