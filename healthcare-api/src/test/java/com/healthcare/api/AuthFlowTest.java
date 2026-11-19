package com.healthcare.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/catalog-test.sql")
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
        // 登录 token 只通过 HttpOnly Cookie 下发，响应体不再包含明文 token
        assertThat(loginData.containsKey("token")).isFalse();
        assertThat(loginData.get("newUser")).isEqualTo(true);

        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("healthcare_token=").contains("HttpOnly");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.substring(0, setCookie.indexOf(';')));
        HttpEntity<Void> auth = new HttpEntity<>(headers);

        // 基础数据统一由数据库提供
        ResponseEntity<Map> catalog = rest.exchange(
                base + "/api/catalog", HttpMethod.GET, auth, Map.class);
        assertThat(catalog.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> catalogData = (Map<?, ?>) catalog.getBody().get("data");
        assertThat((List<?>) catalogData.get("foods")).hasSize(3);
        assertThat((List<?>) catalogData.get("exerciseTypes")).hasSize(2);
        assertThat((List<?>) catalogData.get("exerciseLessons")).hasSize(1);
        assertThat((List<?>) catalogData.get("sleepSounds")).hasSize(1);
        assertThat((List<?>) catalogData.get("devices")).hasSize(2);
        assertThat((List<?>) catalogData.get("waterPortions")).hasSize(2);
        assertThat((List<?>) catalogData.get("meals")).hasSize(4);

        ResponseEntity<Map> me = rest.exchange(base + "/api/me", HttpMethod.GET, auth, Map.class);
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> meData = (Map<?, ?>) me.getBody().get("data");
        assertThat(meData.get("name")).isEqualTo("用户1111");
        assertThat(meData.get("phone")).isEqualTo("13900001111");

        ResponseEntity<Map> state = rest.exchange(base + "/api/state", HttpMethod.GET, auth, Map.class);
        assertThat(state.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> stateData = (Map<?, ?>) state.getBody().get("data");
        Map<String, Object> health = new HashMap<>((Map<String, Object>) stateData.get("health"));
        assertThat(health.get("steps")).isEqualTo(0);
        assertThat(health.get("sleepMinutes")).isEqualTo(0);
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

        // 弱密码被拒绝
        for (String weak : List.of("short1", "abcdefgh", "12345678")) {
            ResponseEntity<Map> weakPassword = rest.exchange(
                    base + "/api/me/password",
                    HttpMethod.PUT,
                    new HttpEntity<>(Map.of("newPassword", weak), headers),
                    Map.class);
            assertThat(weakPassword.getStatusCode().value())
                    .as("weak password %s should be rejected", weak)
                    .isEqualTo(400);
        }

        // 首次设置密码：账号无密码，需校验当前手机号验证码
        ResponseEntity<Map> stepUpSend = rest.exchange(
                base + "/api/me/step-up/send-code",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(stepUpSend.getStatusCode().value()).isEqualTo(200);
        String stepUpCode =
                (String) ((Map<?, ?>) stepUpSend.getBody().get("data")).get("devCode");
        assertThat(stepUpCode).matches("\\d{6}");

        ResponseEntity<Map> changePassword = rest.exchange(
                base + "/api/me/password",
                HttpMethod.PUT,
                new HttpEntity<>(
                        Map.of("newPassword", "pass1234", "currentCode", stepUpCode), headers),
                Map.class);
        assertThat(changePassword.getStatusCode().value()).isEqualTo(200);

        // 密码登录
        ResponseEntity<Map> passwordLogin = rest.postForEntity(
                base + "/api/auth/login-password",
                Map.of("phone", "13900001111", "password", "pass1234"),
                Map.class);
        assertThat(passwordLogin.getStatusCode().value()).isEqualTo(200);
        String passwordSetCookie = passwordLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(passwordSetCookie).contains("healthcare_token=").contains("HttpOnly");

        HttpHeaders passwordHeaders = new HttpHeaders();
        passwordHeaders.add(
                HttpHeaders.COOKIE, passwordSetCookie.substring(0, passwordSetCookie.indexOf(';')));

        // 登录设备列表：当前会话 + 密码登录产生的第二个会话
        ResponseEntity<Map> sessionsResp = rest.exchange(
                base + "/api/me/sessions", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(sessionsResp.getStatusCode().value()).isEqualTo(200);
        List<?> sessions = (List<?>) sessionsResp.getBody().get("data");
        assertThat(sessions).hasSize(2);
        List<Map<?, ?>> sessionMaps = sessions.stream()
                .<Map<?, ?>>map(session -> (Map<?, ?>) session)
                .toList();
        assertThat(sessionMaps.stream()
                .filter(session -> Boolean.TRUE.equals(session.get("current")))
                .toList()).hasSize(1);
        Map<?, ?> otherSession = sessionMaps.stream()
                .filter(session -> !Boolean.TRUE.equals(session.get("current")))
                .findFirst()
                .orElseThrow();
        long otherSessionId = ((Number) otherSession.get("id")).longValue();

        // 退出其他设备后，该会话的 Cookie 立即失效
        ResponseEntity<Map> revokeOther = rest.exchange(
                base + "/api/me/sessions/" + otherSessionId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(revokeOther.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<String> otherAfterRevoke = rest.exchange(
                base + "/api/state",
                HttpMethod.GET,
                new HttpEntity<>(passwordHeaders),
                String.class);
        assertThat(otherAfterRevoke.getStatusCode().value()).isEqualTo(401);

        // 服务端退出登录，该 Cookie 失效
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

        // 修改手机号：必须通过新号码的短信验证码
        String newPhone = "13700002222";
        ResponseEntity<Map> phoneCodeResp = rest.exchange(
                base + "/api/me/phone/send-code",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", newPhone), headers),
                Map.class);
        assertThat(phoneCodeResp.getStatusCode().value()).isEqualTo(200);
        String phoneCode =
                (String) ((Map<?, ?>) phoneCodeResp.getBody().get("data")).get("devCode");
        assertThat(phoneCode).matches("\\d{6}");

        // 当前密码错误时拒绝改绑（不会消耗新号码验证码）
        ResponseEntity<Map> wrongCurrentPassword = rest.exchange(
                base + "/api/me/phone",
                HttpMethod.PUT,
                new HttpEntity<>(
                        Map.of("phone", newPhone, "code", phoneCode,
                                "currentPassword", "wrong1234"),
                        headers),
                Map.class);
        assertThat(wrongCurrentPassword.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map> wrongPhoneCode = rest.exchange(
                base + "/api/me/phone",
                HttpMethod.PUT,
                new HttpEntity<>(
                        Map.of("phone", newPhone, "code", "000000",
                                "currentPassword", "pass1234"),
                        headers),
                Map.class);
        assertThat(wrongPhoneCode.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map> phoneUpdated = rest.exchange(
                base + "/api/me/phone",
                HttpMethod.PUT,
                new HttpEntity<>(
                        Map.of("phone", newPhone, "code", phoneCode,
                                "currentPassword", "pass1234"),
                        headers),
                Map.class);
        assertThat(phoneUpdated.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> phoneUpdatedData = (Map<?, ?>) phoneUpdated.getBody().get("data");
        assertThat(phoneUpdatedData.get("phone")).isEqualTo(newPhone);

        // 旧的 PATCH 接口不能再绕过验证码修改手机号
        ResponseEntity<Map> patchPhone = rest.exchange(
                base + "/api/me",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("phone", "13600003333"), headers),
                Map.class);
        assertThat(patchPhone.getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) patchPhone.getBody().get("data")).get("phone"))
                .isEqualTo(newPhone);

        // 头像：上传 PNG、读取、删除（手工构造 multipart 请求体，避免测试客户端缺 boundary）
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        String boundary = "----healthcareTestBoundary";
        byte[] prefix = ("--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"avatar.png\""
                + "\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] multipart = new byte[prefix.length + png.length + suffix.length];
        System.arraycopy(prefix, 0, multipart, 0, prefix.length);
        System.arraycopy(png, 0, multipart, prefix.length, png.length);
        System.arraycopy(suffix, 0, multipart, prefix.length + png.length, suffix.length);

        HttpHeaders avatarHeaders = new HttpHeaders();
        avatarHeaders.setContentType(
                MediaType.parseMediaType("multipart/form-data; boundary=" + boundary));
        avatarHeaders.add(HttpHeaders.COOKIE, headers.getFirst(HttpHeaders.COOKIE));
        ResponseEntity<Map> avatarUpload = rest.exchange(
                base + "/api/me/avatar",
                HttpMethod.POST,
                new HttpEntity<>(multipart, avatarHeaders),
                Map.class);
        assertThat(avatarUpload.getStatusCode().value()).isEqualTo(200);
        String avatarUrl =
                (String) ((Map<?, ?>) avatarUpload.getBody().get("data")).get("avatarUrl");
        assertThat(avatarUrl).startsWith("/api/me/avatar?v=");

        ResponseEntity<byte[]> avatarBytes = rest.exchange(
                base + "/api/me/avatar",
                HttpMethod.GET,
                new HttpEntity<>(avatarHeaders),
                byte[].class);
        assertThat(avatarBytes.getStatusCode().value()).isEqualTo(200);
        assertThat(avatarBytes.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(avatarBytes.getBody()).hasSize(png.length);

        ResponseEntity<Map> avatarDelete = rest.exchange(
                base + "/api/me/avatar",
                HttpMethod.DELETE,
                new HttpEntity<>(avatarHeaders),
                Map.class);
        assertThat(avatarDelete.getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) avatarDelete.getBody().get("data")).get("avatarUrl")).isNull();

        ResponseEntity<String> avatarAfterDelete = rest.exchange(
                base + "/api/me/avatar",
                HttpMethod.GET,
                new HttpEntity<>(avatarHeaders),
                String.class);
        assertThat(avatarAfterDelete.getStatusCode().value()).isEqualTo(404);

        // Web Push：公钥、订阅与退订
        ResponseEntity<Map> pushKey = rest.exchange(
                base + "/api/push/public-key", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(pushKey.getStatusCode().value()).isEqualTo(200);
        String vapidPublicKey = (String) ((Map<?, ?>) pushKey.getBody().get("data"))
                .get("publicKey");
        assertThat(vapidPublicKey).isNotBlank();

        String fakeEndpoint = "https://fcm.googleapis.com/fcm/send/test-" + System.nanoTime();
        Map<String, Object> subscription = Map.of(
                "endpoint", fakeEndpoint,
                "keys", Map.of("p256dh", "test-p256dh", "auth", "test-auth"));
        ResponseEntity<Map> subscribed = rest.exchange(
                base + "/api/push/subscribe",
                HttpMethod.POST,
                new HttpEntity<>(subscription, headers),
                Map.class);
        assertThat(subscribed.getStatusCode().value()).isEqualTo(200);

        Map<String, Object> insecureSubscription = Map.of(
                "endpoint", "http://fcm.googleapis.com/fcm/send/insecure",
                "keys", Map.of("p256dh", "test", "auth", "test"));
        ResponseEntity<Map> rejected = rest.exchange(
                base + "/api/push/subscribe",
                HttpMethod.POST,
                new HttpEntity<>(insecureSubscription, headers),
                Map.class);
        assertThat(rejected.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map> unsubscribed = rest.exchange(
                base + "/api/push/unsubscribe",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("endpoint", fakeEndpoint), headers),
                Map.class);
        assertThat(unsubscribed.getStatusCode().value()).isEqualTo(200);

        // 验证码重置密码（使用修改后的手机号）
        ResponseEntity<Map> resetSendCode = rest.postForEntity(
                base + "/api/auth/send-code", Map.of("phone", newPhone), Map.class);
        String resetCode = (String) ((Map<?, ?>) resetSendCode.getBody().get("data")).get("devCode");
        ResponseEntity<Map> reset = rest.postForEntity(
                base + "/api/auth/reset-password",
                Map.of("phone", newPhone, "code", resetCode, "newPassword", "newpass123"),
                Map.class);
        assertThat(reset.getStatusCode().value()).isEqualTo(200);
        // 重置密码会使旧会话全部失效，改用新下发的 Cookie 继续
        String resetSetCookie = reset.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(resetSetCookie).contains("healthcare_token=");
        headers.clear();
        headers.add(
                HttpHeaders.COOKIE, resetSetCookie.substring(0, resetSetCookie.indexOf(';')));

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
        Map<?, ?> createdExercise = (Map<?, ?>) exerciseCreate.getBody().get("data");
        long exerciseId = ((Number) createdExercise.get("id")).longValue();

        ResponseEntity<Map> exerciseList = rest.exchange(
                base + "/api/exercise-logs", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat((List<?>) exerciseList.getBody().get("data")).hasSize(1);

        // 编辑运动记录，并验证活动时长/次数按记录重新汇总
        Map<String, Object> exerciseUpdate = new HashMap<>();
        exerciseUpdate.put("date", today);
        exerciseUpdate.put("type", "walk");
        exerciseUpdate.put("minutes", 45);
        exerciseUpdate.put("calories", 280);
        ResponseEntity<Map> exerciseUpdated = rest.exchange(
                base + "/api/exercise-logs/" + exerciseId,
                HttpMethod.PUT,
                new HttpEntity<>(exerciseUpdate, headers),
                Map.class);
        assertThat(exerciseUpdated.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> updatedExercise = (Map<?, ?>) exerciseUpdated.getBody().get("data");
        assertThat(updatedExercise.get("type")).isEqualTo("walk");
        assertThat(updatedExercise.get("minutes")).isEqualTo(45);
        assertThat(updatedExercise.get("calories")).isEqualTo(280);

        ResponseEntity<Map> stateAfterExercise = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> exerciseHealth =
                (Map<?, ?>) ((Map<?, ?>) stateAfterExercise.getBody().get("data")).get("health");
        assertThat(exerciseHealth.get("activeMinutes")).isEqualTo(45);
        assertThat(exerciseHealth.get("exerciseCount")).isEqualTo(1);

        // 删除后汇总归零
        ResponseEntity<Map> exerciseDelete = rest.exchange(
                base + "/api/exercise-logs/" + exerciseId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(exerciseDelete.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map> exerciseListAfterDelete = rest.exchange(
                base + "/api/exercise-logs", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat((List<?>) exerciseListAfterDelete.getBody().get("data")).isEmpty();
        ResponseEntity<Map> stateAfterExerciseDelete = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> healthAfterExerciseDelete =
                (Map<?, ?>) ((Map<?, ?>) stateAfterExerciseDelete.getBody().get("data")).get("health");
        assertThat(healthAfterExerciseDelete.get("activeMinutes")).isEqualTo(0);
        assertThat(healthAfterExerciseDelete.get("exerciseCount")).isEqualTo(0);

        ResponseEntity<Map> stateAfterFood = rest.exchange(
                base + "/api/state", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<?, ?> stateAfterFoodData = (Map<?, ?>) stateAfterFood.getBody().get("data");
        Map<?, ?> healthAfterFood = (Map<?, ?>) stateAfterFoodData.get("health");
        assertThat(healthAfterFood.get("foodKcal")).isEqualTo(100);

        // 数据导出：包含账号、健康状态、食物和运动记录
        ResponseEntity<Map> export = rest.exchange(
                base + "/api/me/export", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(export.getStatusCode().value()).isEqualTo(200);
        assertThat(export.getBody()).containsKeys("account", "health", "foodLogs", "exerciseLogs");
        assertThat((List<?>) export.getBody().get("foodLogs")).hasSize(1);

        // 注销账号需当前密码；缺少凭证时拒绝
        ResponseEntity<Map> deleteWithoutCredential = rest.exchange(
                base + "/api/me", HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
        assertThat(deleteWithoutCredential.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map> deleteAccount = rest.exchange(
                base + "/api/me",
                HttpMethod.DELETE,
                new HttpEntity<>(Map.of("password", "newpass123"), headers),
                Map.class);
        assertThat(deleteAccount.getStatusCode().value()).isEqualTo(200);
        ResponseEntity<String> afterDelete = rest.exchange(
                base + "/api/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(afterDelete.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void rejectsNonMainlandPhone() {
        String base = "http://localhost:" + port;

        for (String phone : List.of("12345678901", "12000000000", "1380000111", "+1 234 567 8901")) {
            ResponseEntity<Map> response = rest.postForEntity(
                    base + "/api/auth/send-code", Map.of("phone", phone), Map.class);
            assertThat(response.getStatusCode().value())
                    .as("phone %s should be rejected", phone)
                    .isEqualTo(400);
        }
    }

    @Test
    void rejectsCrossSiteWrites() {
        String base = "http://localhost:" + port;

        HttpHeaders evil = new HttpHeaders();
        evil.setOrigin("https://evil.example");
        ResponseEntity<String> blocked = rest.exchange(
                base + "/api/auth/send-code",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", "13800009901"), evil),
                String.class);
        assertThat(blocked.getStatusCode().value()).isEqualTo(403);

        HttpHeaders opaque = new HttpHeaders();
        opaque.setOrigin("null");
        ResponseEntity<String> opaqueBlocked = rest.exchange(
                base + "/api/auth/send-code",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", "13800009904"), opaque),
                String.class);
        assertThat(opaqueBlocked.getStatusCode().value()).isEqualTo(403);

        HttpHeaders allowed = new HttpHeaders();
        allowed.setOrigin("http://localhost:5173");
        ResponseEntity<Map> passed = rest.exchange(
                base + "/api/auth/send-code",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("phone", "13800009902"), allowed),
                Map.class);
        assertThat(passed.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void rejectsPasswordChangeWithoutLetterAndDigit() {
        String base = "http://localhost:" + port;

        ResponseEntity<Map> sendCode = rest.postForEntity(
                base + "/api/auth/send-code", Map.of("phone", "13800009903"), Map.class);
        String code = (String) ((Map<?, ?>) sendCode.getBody().get("data")).get("devCode");
        ResponseEntity<Map> login = rest.postForEntity(
                base + "/api/auth/login", Map.of("phone", "13800009903", "code", code), Map.class);
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.substring(0, setCookie.indexOf(';')));

        for (String weak : List.of("abc1234", "abcdefgh", "12345678", "abcd 1234")) {
            ResponseEntity<Map> response = rest.exchange(
                    base + "/api/me/password",
                    HttpMethod.PUT,
                    new HttpEntity<>(Map.of("newPassword", weak), headers),
                    Map.class);
            assertThat(response.getStatusCode().value())
                    .as("password %s should be rejected", weak)
                    .isEqualTo(400);
        }

        ResponseEntity<Map> stepUpSend = rest.exchange(
                base + "/api/me/step-up/send-code",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class);
        String stepUpCode =
                (String) ((Map<?, ?>) stepUpSend.getBody().get("data")).get("devCode");

        ResponseEntity<Map> accepted = rest.exchange(
                base + "/api/me/password",
                HttpMethod.PUT,
                new HttpEntity<>(
                        Map.of("newPassword", "Strong123", "currentCode", stepUpCode), headers),
                Map.class);
        assertThat(accepted.getStatusCode().value()).isEqualTo(200);
    }
}
