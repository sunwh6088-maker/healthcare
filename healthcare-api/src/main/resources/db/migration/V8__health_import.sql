-- iPhone 快捷指令导入：令牌与每日健康数据
ALTER TABLE users ADD COLUMN import_token_hash VARCHAR(64) DEFAULT NULL;
ALTER TABLE users ADD COLUMN import_token_created_at DATETIME(6) DEFAULT NULL;
ALTER TABLE users ADD UNIQUE KEY uk_users_import_token (import_token_hash);

CREATE TABLE daily_health_metrics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    metric_date DATE NOT NULL,
    steps INT NOT NULL DEFAULT 0,
    sleep_minutes INT NOT NULL DEFAULT 0,
    active_minutes INT NOT NULL DEFAULT 0,
    weight_kg DOUBLE DEFAULT NULL,
    phone_battery INT DEFAULT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'shortcut',
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_metrics_user_date (user_id, metric_date),
    KEY idx_daily_metrics_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
