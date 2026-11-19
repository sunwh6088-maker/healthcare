package com.healthcare.api.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.api.common.ApiException;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Utils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Push：管理浏览器订阅并发送 VAPID 推送。
 *
 * <p>未配置 VAPID 密钥时会在启动时生成临时密钥（仅适合开发环境）；
 * 生产环境应通过 VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY 配置固定密钥。</p>
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository subscriptions;
    private final ObjectMapper objectMapper;
    private final String configuredPublicKey;
    private final String configuredPrivateKey;
    private final String subject;

    private String publicKey;
    private nl.martijndwars.webpush.PushService pushService;

    public WebPushService(PushSubscriptionRepository subscriptions,
                          ObjectMapper objectMapper,
                          @Value("${app.push.public-key:}") String publicKey,
                          @Value("${app.push.private-key:}") String privateKey,
                          @Value("${app.push.subject:mailto:support@healthcare.example}") String subject) {
        this.subscriptions = subscriptions;
        this.objectMapper = objectMapper;
        this.configuredPublicKey = publicKey == null ? "" : publicKey.trim();
        this.configuredPrivateKey = privateKey == null ? "" : privateKey.trim();
        this.subject = subject;
    }

    @PostConstruct
    void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (configuredPublicKey.isBlank() || configuredPrivateKey.isBlank()) {
            generateEphemeralKeys();
            log.warn("未配置 VAPID 密钥，已生成临时密钥；重启后旧订阅将失效。"
                    + "生产环境请设置 VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY");
        } else {
            publicKey = configuredPublicKey;
            createPushService(configuredPublicKey, configuredPrivateKey);
        }
    }

    public String publicKey() {
        return publicKey;
    }

    @Transactional
    public void subscribe(Long userId, PushDtos.SubscribeRequest request) {
        String endpoint = request.endpoint().trim();
        validateEndpoint(endpoint);
        PushSubscriptionEntity entity = subscriptions.findByEndpoint(endpoint)
                .orElseGet(PushSubscriptionEntity::new);
        entity.setUserId(userId);
        entity.setEndpoint(endpoint);
        entity.setP256dh(request.keys().p256dh());
        entity.setAuthKey(request.keys().auth());
        subscriptions.save(entity);
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            subscriptions.deleteByUserIdAndEndpoint(userId, endpoint.trim());
        }
    }

    /** 向用户所有设备推送，返回成功数量。失效订阅会被清理。 */
    public int sendToUser(Long userId, String title, String body, String url, String tag) {
        List<PushSubscriptionEntity> entities = subscriptions.findByUserId(userId);
        if (entities.isEmpty()) {
            return 0;
        }
        String payload = buildPayload(title, body, url, tag);
        int sent = 0;
        for (PushSubscriptionEntity entity : entities) {
            try {
                Subscription subscription = new Subscription(
                        entity.getEndpoint(),
                        new Subscription.Keys(entity.getP256dh(), entity.getAuthKey()));
                Notification notification = new Notification(subscription, payload);
                HttpResponse response = pushService.send(notification);
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    sent++;
                } else if (status == 404 || status == 410) {
                    subscriptions.delete(entity);
                    log.info("Removed expired push subscription {}", entity.getId());
                } else {
                    log.warn("Push to subscription {} failed with status {}", entity.getId(), status);
                }
            } catch (Exception ex) {
                log.warn("Push to subscription {} failed: {}", entity.getId(), ex.toString());
            }
        }
        return sent;
    }

    public int sendTest(Long userId) {
        return sendToUser(
                userId,
                "健康追踪",
                "通知已开启，之后会在这里提醒你。",
                "/#/settings",
                "test");
    }

    private String buildPayload(String title, String body, String url, String tag) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            payload.put("url", url);
            if (tag != null) {
                payload.put("tag", tag);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("无法序列化推送内容", ex);
        }
    }

    private void generateEphemeralKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
            generator.initialize(256);
            KeyPair pair = generator.generateKeyPair();
            byte[] pub = Utils.encode((ECPublicKey) pair.getPublic());
            byte[] priv = Utils.encode((ECPrivateKey) pair.getPrivate());
            publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(pub);
            String privateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(priv);
            createPushService(publicKey, privateKey);
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成 VAPID 密钥", ex);
        }
    }

    private void createPushService(String pub, String priv) {
        try {
            this.pushService = new nl.martijndwars.webpush.PushService(pub, priv, subject);
        } catch (Exception ex) {
            throw new IllegalStateException("VAPID 密钥配置不正确", ex);
        }
    }

    /** 只允许主流推送服务地址，避免把订阅 endpoint 变成 SSRF 入口。 */
    private void validateEndpoint(String endpoint) {
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "推送地址格式不正确");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new ApiException(400, "推送地址必须是 HTTPS");
        }
        String host = uri.getHost().toLowerCase();
        boolean allowed = host.endsWith(".googleapis.com")
                || host.endsWith(".notify.windows.com")
                || host.endsWith(".push.services.mozilla.com")
                || host.equals("web.push.apple.com")
                || host.endsWith(".push.apple.com");
        if (!allowed) {
            throw new ApiException(400, "不支持的推送服务");
        }
    }
}
