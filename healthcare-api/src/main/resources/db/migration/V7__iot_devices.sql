-- IoT 设备绑定与上报事件（通用 MQTT / Alink 物模型）
CREATE TABLE iot_devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_key VARCHAR(64) NOT NULL,
    device_name VARCHAR(64) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    connected BIT(1) NOT NULL DEFAULT b'0',
    battery INT NOT NULL DEFAULT 0,
    last_seen_at DATETIME(6) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_device (product_key, device_name),
    KEY idx_iot_devices_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE iot_device_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    topic VARCHAR(255) DEFAULT NULL,
    payload TEXT NOT NULL,
    received_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_iot_events_device (device_id, received_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
