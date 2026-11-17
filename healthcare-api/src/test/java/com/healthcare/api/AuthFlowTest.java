package com.healthcare.api;

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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void loginAndStateFlow() {
        String base = "http://localhost:" + port;

        ResponseEntity<Map> sendCode = rest.postForEntity(
                base + "/api/auth/send-code", Map.of("phone", "139 0000 1111"), Map.class);
        assertThat(sendCode.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> sendData = (Map<?, ?>) sendCode.getBody().get("data");
        String code = (String) sendData.get("devCode");
        assertThat(code).matches("\\d{6}");

        ResponseEntity<Map> login = rest.postForEntity(
                base + "/api/auth/login",
                Map.of("phone", "139 0000 1111", "code", code),
                Map.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> loginData = (Map<?, ?>) login.getBody().get("data");
        String token = (String) loginData.get("token");
        assertThat(token).isNotBlank();
        assertThat(loginData.get("newUser")).isEqualTo(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> auth = new HttpEntity<>(headers);

        ResponseEntity<Map> me = rest.exchange(base + "/api/me", HttpMethod.GET, auth, Map.class);
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> meData = (Map<?, ?>) me.getBody().get("data");
        assertThat(meData.get("name")).isEqualTo("杰西·比伯");
        assertThat(meData.get("phone")).isEqualTo("13900001111");

        ResponseEntity<Map> state = rest.exchange(base + "/api/state", HttpMethod.GET, auth, Map.class);
        assertThat(state.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> stateData = (Map<?, ?>) state.getBody().get("data");
        Map<String, Object> health = new HashMap<>((Map<String, Object>) stateData.get("health"));
        assertThat(health.get("steps")).isEqualTo(4204);
        assertThat(health.get("sleepMinutes")).isEqualTo(500);
        assertThat((List<?>) stateData.get("alarms")).hasSize(2);

        health.put("steps", 5000);
        health.put("waterMl", 1200);
        Map<String, Object> alarm = new HashMap<>();
        alarm.put("time", "07:30");
        alarm.put("label", "测试闹钟");
        alarm.put("repeat", List.of(1, 3, 5));
        alarm.put("on", true);
        Map<String, Object> body = new HashMap<>();
        body.put("health", health);
        body.put("alarms", List.of(alarm));

        ResponseEntity<Map> saved = rest.exchange(
                base + "/api/state", HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
        assertThat(saved.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> savedData = (Map<?, ?>) saved.getBody().get("data");
        Map<?, ?> savedHealth = (Map<?, ?>) savedData.get("health");
        assertThat(savedHealth.get("steps")).isEqualTo(5000);
        List<?> alarms = (List<?>) savedData.get("alarms");
        assertThat(alarms).hasSize(1);
        Map<?, ?> savedAlarm = (Map<?, ?>) alarms.get(0);
        assertThat(savedAlarm.get("time")).isEqualTo("07:30");
        assertThat(((List<?>) savedAlarm.get("repeat")).stream()
                .map(value -> ((Number) value).intValue())
                .toList()).containsExactly(1, 3, 5);

        Map<String, Object> patch = Map.of("email", "new@example.com");
        ResponseEntity<Map> updated = rest.exchange(
                base + "/api/me", HttpMethod.PATCH, new HttpEntity<>(patch, headers), Map.class);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> updatedData = (Map<?, ?>) updated.getBody().get("data");
        assertThat(updatedData.get("email")).isEqualTo("new@example.com");

        ResponseEntity<String> unauthorized = rest.getForEntity(base + "/api/state", String.class);
        assertThat(unauthorized.getStatusCode().value()).isEqualTo(401);

        // 设置密码（账号此前没有密码，oldPassword 可省略）
        ResponseEntity<Map> changePassword = rest.exchange(
                base + "/api/me/password",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("newPassword", "pass1234"), headers),
                Map.class);
        assertThat(changePassword.getStatusCode().value()).isEqualTo(200);

        // 密码登录
        ResponseEntity<Map> passwordLogin = rest.postForEntity(
                base + "/api/auth/login-password",
                Map.of("phone", "13900001111", "password", "pass1234"),
                Map.class);
        assertThat(passwordLogin.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> passwordLoginData = (Map<?, ?>) passwordLogin.getBody().get("data");
        String passwordToken = (String) passwordLoginData.get("token");
        assertThat(passwordToken).isNotBlank();

        // 服务端退出登录，旧 token 失效
        HttpHeaders passwordHeaders = new HttpHeaders();
        passwordHeaders.setBearerAuth(passwordToken);
        ResponseEntity<Map> logout = rest.exchange(
                base + "/api/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(passwordHeaders),
                Map.class);
        assertThat(logout.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<String> afterLogout = rest.exchange(
                base + "/api/state",
                HttpMethod.GET,
                new HttpEntity<>(passwordHeaders),
                String.class);
        assertThat(afterLogout.getStatusCode().value()).isEqualTo(401);

        // 验证码重置密码
        ResponseEntity<Map> resetSendCode = rest.postForEntity(
                base + "/api/auth/send-code", Map.of("phone", "13900001111"), Map.class);
        String resetCode = (String) ((Map<?, ?>) resetSendCode.getBody().get("data")).get("devCode");
        ResponseEntity<Map> reset = rest.postForEntity(
                base + "/api/auth/reset-password",
                Map.of("phone", "13900001111", "code", resetCode, "newPassword", "newpass123"),
                Map.class);
        assertThat(reset.getStatusCode().value()).isEqualTo(200);

        // 食物记录
        String today = LocalDate.now().toString();
        Map<String, Object> food = new HashMap<>();
        food.put("date", today);
        food.put("meal", "breakfast");
        food.put("name", "测试食物");
        food.put("portion", "1 份");
        food.put("calories", 100);
        food.put("emoji", "🍎");
        ResponseEntity<Map> foodCreate = rest.exchange(
                base + "/api/food-logs",
                HttpMethod.POST,
                new HttpEntity<>(food, headers),
                Map.class);
        assertThat(foodCreate.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map> foodList = rest.exchange(
                base + "/api/food-logs", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> foodData = (Map<?, ?>) foodList.getBody().get("data");
        assertThat(foodData.get("totalCalories")).isEqualTo(100);
        assertThat((List<?>) foodData.get("logs")).hasSize(1);

        // 运动记录
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("date", today);
        exercise.put("type", "yoga");
        exercise.put("minutes", 30);
        exercise.put("calories", 320);
        ResponseEntity<Map> exerciseCreate = rest.exchange(
                base + "/api/exercise-logs",
                HttpMethod.POST,
                new HttpEntity<>(exercise, headers),
                Map.class);
        assertThat(exerciseCreate.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map> exerciseList = rest.exchange(
                base + "/api/exercise-logs", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat((List<?>) exerciseList.getBody().get("data")).hasSize(1);

        ResponseEntity<Map> stateAfterFood = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> stateAfterFoodData = (Map<?, ?>) stateAfterFood.getBody().get("data");
        Map<?, ?> healthAfterFood = (Map<?, ?>) stateAfterFoodData.get("health");
        assertThat(healthAfterFood.get("foodKcal")).isEqualTo(100);
    }
}
