package com.healthcare.api.iot;

import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 通用 MQTT 接入：订阅设备属性上报主题，交给 IotDeviceService 入库。
 *
 * <p>主题约定：<code>sys/{productKey}/{deviceName}/thing/event/property/post</code>，
 * 兼容阿里云 Alink 物模型格式。也可以连接 EMQX / Mosquitto / 阿里云 MQTT 版。</p>
 */
@Service
public class MqttIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MqttIngestionService.class);

    private final IotDeviceService iotDeviceService;
    private final boolean enabled;
    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private final String password;
    private final String topic;
    private final int qos;

    private MqttClient client;

    public MqttIngestionService(IotDeviceService iotDeviceService,
                                @Value("${app.iot.mqtt.enabled:false}") boolean enabled,
                                @Value("${app.iot.mqtt.broker-url:tcp://127.0.0.1:1883}") String brokerUrl,
                                @Value("${app.iot.mqtt.client-id:healthcare-api}") String clientId,
                                @Value("${app.iot.mqtt.username:}") String username,
                                @Value("${app.iot.mqtt.password:}") String password,
                                @Value("${app.iot.mqtt.topic:sys/+/+/thing/event/property/post}") String topic,
                                @Value("${app.iot.mqtt.qos:1}") int qos) {
        this.iotDeviceService = iotDeviceService;
        this.enabled = enabled;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.username = username;
        this.password = password;
        this.topic = topic;
        this.qos = qos;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (enabled) {
            connect();
        } else {
            log.info("MQTT ingestion disabled; device simulator endpoint remains available in dev mode");
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void ensureConnected() {
        if (enabled && (client == null || !client.isConnected())) {
            connect();
        }
    }

    private synchronized void connect() {
        try {
            if (client == null) {
                client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        try {
                            client.subscribe(topic, qos);
                            log.info("MQTT connected to {}, subscribed to {}", serverURI, topic);
                        } catch (Exception ex) {
                            log.error("Failed to subscribe MQTT topic {}", topic, ex);
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.toString());
                    }

                    @Override
                    public void messageArrived(String messageTopic, MqttMessage message) {
                        handle(messageTopic, new String(message.getPayload(), StandardCharsets.UTF_8));
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // 只订阅，不发布
                    }
                });
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(30);
            if (!username.isBlank()) {
                options.setUserName(username);
            }
            if (!password.isBlank()) {
                options.setPassword(password.toCharArray());
            }
            client.connect(options);
        } catch (Exception ex) {
            log.warn("MQTT connect to {} failed: {}", brokerUrl, ex.toString());
        }
    }

    private void handle(String messageTopic, String payload) {
        String normalized = messageTopic.startsWith("/") ? messageTopic.substring(1) : messageTopic;
        String[] parts = normalized.split("/");
        if (parts.length < 3 || !"sys".equals(parts[0])) {
            log.debug("Ignoring MQTT message on topic {}", messageTopic);
            return;
        }
        iotDeviceService.ingestByKey(parts[1], parts[2], messageTopic, payload);
    }

    @PreDestroy
    void shutdown() {
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            } catch (Exception ignored) {
                // 关闭失败不影响进程退出
            }
        }
    }
}
