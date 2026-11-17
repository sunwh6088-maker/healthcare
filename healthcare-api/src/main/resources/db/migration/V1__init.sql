-- 初始表结构（与当前 JPA 实体保持一致）
-- 已有数据库通过 baseline-on-migrate + baseline-version=0 记录此版本，不会重复建表。

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    password_hash VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS health_states (
    user_id BIGINT NOT NULL,
    active_goal_minutes INT NOT NULL,
    active_minutes INT NOT NULL,
    age INT NOT NULL,
    device_added BIT(1) NOT NULL,
    device_connected BIT(1) NOT NULL,
    exercise_count INT NOT NULL,
    food_kcal INT NOT NULL,
    gender VARCHAR(255) DEFAULT NULL,
    height_cm INT NOT NULL,
    notify_enabled BIT(1) NOT NULL,
    privacy_enabled BIT(1) NOT NULL,
    sleep_minutes INT NOT NULL,
    sleep_start_minutes INT NOT NULL,
    steps INT NOT NULL,
    steps_goal INT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    water_goal_ml INT NOT NULL,
    water_ml INT NOT NULL,
    weight_kg DOUBLE NOT NULL,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alarms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enabled BIT(1) NOT NULL,
    label VARCHAR(64) NOT NULL,
    repeat_days VARCHAR(32) NOT NULL,
    alarm_time VARCHAR(5) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_tokens_hash (token_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS food_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calories INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    emoji VARCHAR(16) DEFAULT NULL,
    log_date DATE NOT NULL,
    meal VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    portion VARCHAR(64) DEFAULT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercise_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calories INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    log_date DATE NOT NULL,
    minutes INT NOT NULL,
    type VARCHAR(16) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
