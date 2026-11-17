-- 测试环境的基础数据表（生产由 Flyway V2 创建）
CREATE TABLE IF NOT EXISTS foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    portion VARCHAR(64) NOT NULL,
    calories INT NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    category VARCHAR(16) NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS exercise_types (
    type_key VARCHAR(16) PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    bg_color VARCHAR(16) NOT NULL,
    bar_color VARCHAR(16) NOT NULL,
    kcal INT NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS exercise_lessons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_key VARCHAR(16) NOT NULL,
    title VARCHAR(64) NOT NULL,
    level VARCHAR(32) NOT NULL,
    minutes INT NOT NULL,
    calories INT NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS sleep_sounds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    audio_url VARCHAR(255),
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    icon VARCHAR(16) NOT NULL,
    capabilities VARCHAR(128) NOT NULL,
    is_primary BOOLEAN NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS water_portions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    size_label VARCHAR(32) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    ml INT NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS meal_types (
    meal_key VARCHAR(16) PRIMARY KEY,
    label VARCHAR(32) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL
);

DELETE FROM foods;
DELETE FROM exercise_types;
DELETE FROM exercise_lessons;
DELETE FROM sleep_sounds;
DELETE FROM devices;
DELETE FROM water_portions;
DELETE FROM meal_types;

INSERT INTO foods (name, portion, calories, emoji, category, is_custom, sort_order) VALUES
('苹果', '1 个苹果', 52, '🍎', 'snack', FALSE, 1),
('鸡胸肉', '100 克鸡胸肉', 165, '🍗', 'main', FALSE, 2),
('米饭', '1 碗米饭', 116, '🍚', 'main', TRUE, 3);

INSERT INTO exercise_types (type_key, name, emoji, bg_color, bar_color, kcal, sort_order) VALUES
('yoga', '瑜伽', '🧘', '#bacac2', '#2a6049', 320, 1),
('walk', '快走', '🚶', '#ffe2d3', '#ec744a', 560, 2);

INSERT INTO exercise_lessons (type_key, title, level, minutes, calories, emoji, sort_order) VALUES
('yoga', '瑜伽 #1', '初级', 32, 320, '🧘', 1);

INSERT INTO sleep_sounds (name, emoji, audio_url, sort_order) VALUES
('潜鸟鸣声', '🎵', NULL, 1);

INSERT INTO devices (name, icon, capabilities, is_primary, sort_order) VALUES
('智能手表', '⌚', '心率 · 睡眠 · 步数', TRUE, 1),
('智能手机', '📱', '步数 · 睡眠', FALSE, 2);

INSERT INTO water_portions (name, size_label, emoji, ml, sort_order) VALUES
('1 杯', '(130 毫升)', '🥛', 130, 1),
('1 大瓶', '(520 毫升)', '🫗', 520, 2);

INSERT INTO meal_types (meal_key, label, emoji, sort_order) VALUES
('breakfast', '早餐', '🍳', 1),
('lunch', '午餐', '🍱', 2),
('dinner', '晚餐', '🍲', 3),
('snack', '零食', '🍪', 4);
