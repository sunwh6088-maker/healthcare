-- 业务基础数据：食物、运动类型、课程、助眠声音、设备、饮水份量、餐次
-- 同时为用户设备补充电量与最近同步时间

CREATE TABLE IF NOT EXISTS foods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    portion VARCHAR(64) NOT NULL,
    calories INT NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    category VARCHAR(16) NOT NULL,
    is_custom BIT(1) NOT NULL DEFAULT b'0',
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_foods_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercise_types (
    type_key VARCHAR(16) NOT NULL,
    name VARCHAR(32) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    bg_color VARCHAR(16) NOT NULL,
    bar_color VARCHAR(16) NOT NULL,
    kcal INT NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (type_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercise_lessons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type_key VARCHAR(16) NOT NULL,
    title VARCHAR(64) NOT NULL,
    level VARCHAR(32) NOT NULL,
    minutes INT NOT NULL,
    calories INT NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sleep_sounds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    audio_url VARCHAR(255) DEFAULT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    icon VARCHAR(16) NOT NULL,
    capabilities VARCHAR(128) NOT NULL,
    is_primary BIT(1) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS water_portions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    size_label VARCHAR(32) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    ml INT NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meal_types (
    meal_key VARCHAR(16) NOT NULL,
    label VARCHAR(32) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (meal_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

ALTER TABLE health_states
    ADD COLUMN device_battery INT NOT NULL DEFAULT 86,
    ADD COLUMN device_last_sync_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

INSERT INTO foods (name, portion, calories, emoji, category, is_custom, sort_order) VALUES
('萝卜', '1 根胡萝卜', 29, '🥕', 'main', FALSE, 1),
('苹果', '1 个苹果', 52, '🍎', 'snack', FALSE, 2),
('鸡胸肉', '100 克鸡胸肉', 165, '🍗', 'main', FALSE, 3),
('鸡蛋', '1 个鸡蛋', 78, '🥚', 'main', FALSE, 4),
('米饭', '1 碗米饭', 116, '🍚', 'main', FALSE, 5),
('牛奶', '1 杯牛奶', 42, '🥛', 'main', FALSE, 6),
('西兰花', '100 克西兰花', 34, '🥦', 'main', FALSE, 7),
('香蕉', '1 根香蕉', 89, '🍌', 'main', FALSE, 8),
('酸奶', '1 杯酸奶', 95, '🥛', 'snack', FALSE, 9),
('糙米饭', '1 碗糙米饭', 150, '🍚', 'main', FALSE, 10),
('全麦面包', '1 片全麦面包', 70, '🍞', 'main', FALSE, 11),
('燕麦粥', '1 碗燕麦粥', 150, '🥣', 'main', FALSE, 12),
('面条', '1 碗面条', 220, '🍜', 'main', FALSE, 13),
('意大利面', '1 份意大利面', 300, '🍝', 'main', FALSE, 14),
('红薯', '1 个红薯', 112, '🍠', 'main', FALSE, 15),
('玉米', '1 根玉米', 96, '🌽', 'main', FALSE, 16),
('土豆', '1 个土豆', 130, '🥔', 'main', TRUE, 17),
('包子', '1 个包子', 220, '🍥', 'main', TRUE, 18),
('饺子', '6 个饺子', 300, '🥟', 'main', TRUE, 19),
('三明治', '1 个三明治', 250, '🥪', 'main', TRUE, 20),
('寿司', '6 贯寿司', 300, '🍣', 'main', TRUE, 21),
('牛肉', '100 克牛肉', 250, '🥩', 'main', TRUE, 22),
('三文鱼', '100 克三文鱼', 208, '🐟', 'main', TRUE, 23),
('虾', '100 克虾', 99, '🍤', 'main', TRUE, 24),
('豆腐', '100 克豆腐', 76, '🍲', 'main', TRUE, 25),
('豆浆', '1 杯豆浆', 80, '🥛', 'main', TRUE, 26),
('菠菜', '100 克菠菜', 23, '🥬', 'main', TRUE, 27),
('生菜', '100 克生菜', 15, '🥬', 'main', TRUE, 28),
('番茄', '1 个番茄', 22, '🍅', 'main', TRUE, 29),
('黄瓜', '1 根黄瓜', 16, '🥒', 'main', TRUE, 30),
('南瓜', '100 克南瓜', 26, '🎃', 'main', FALSE, 31),
('蘑菇', '100 克蘑菇', 22, '🍄', 'main', FALSE, 32),
('蔬菜沙拉', '1 份蔬菜沙拉', 120, '🥗', 'main', FALSE, 33),
('芝士', '1 片芝士', 70, '🧀', 'main', FALSE, 34),
('烤鸡腿', '1 个烤鸡腿', 280, '🍗', 'main', FALSE, 35),
('炒饭', '1 碗炒饭', 330, '🍛', 'main', FALSE, 36),
('白粥', '1 碗白粥', 90, '🥣', 'main', FALSE, 37),
('橙子', '1 个橙子', 62, '🍊', 'snack', FALSE, 38),
('蓝莓', '100 克蓝莓', 57, '🫐', 'snack', FALSE, 39),
('草莓', '100 克草莓', 32, '🍓', 'snack', FALSE, 40),
('葡萄', '100 克葡萄', 69, '🍇', 'snack', FALSE, 41),
('西瓜', '1 块西瓜', 46, '🍉', 'snack', FALSE, 42),
('芒果', '100 克芒果', 60, '🥭', 'snack', FALSE, 43),
('猕猴桃', '1 个猕猴桃', 42, '🥝', 'snack', FALSE, 44),
('梨', '1 个梨', 57, '🍐', 'snack', FALSE, 45),
('桃子', '1 个桃子', 39, '🍑', 'snack', FALSE, 46),
('牛油果', '半个牛油果', 120, '🥑', 'snack', FALSE, 47),
('混合坚果', '1 小把混合坚果', 170, '🥜', 'snack', FALSE, 48),
('杏仁', '10 颗杏仁', 70, '🌰', 'snack', FALSE, 49),
('花生', '30 克花生', 170, '🥜', 'snack', FALSE, 50),
('黑巧克力', '1 块黑巧克力', 60, '🍫', 'snack', FALSE, 51),
('全麦饼干', '2 片全麦饼干', 80, '🍪', 'snack', FALSE, 52),
('薯片', '1 小包薯片', 150, '🍟', 'snack', FALSE, 53),
('冰淇淋', '1 球冰淇淋', 140, '🍦', 'snack', FALSE, 54),
('蛋糕', '1 块蛋糕', 250, '🍰', 'snack', FALSE, 55),
('蜂蜜', '1 勺蜂蜜', 64, '🍯', 'snack', FALSE, 56),
('美式咖啡', '1 杯美式咖啡', 5, '☕', 'snack', FALSE, 57),
('绿茶', '1 杯绿茶', 0, '🍵', 'snack', FALSE, 58),
('蛋白棒', '1 根蛋白棒', 200, '🍫', 'snack', FALSE, 59),
('爆米花', '1 小份爆米花', 120, '🍿', 'snack', FALSE, 60),
('葡萄干', '30 克葡萄干', 90, '🍇', 'snack', FALSE, 61),
('拿铁', '1 杯拿铁', 120, '☕', 'snack', FALSE, 62),
('橙汁', '1 杯橙汁', 110, '🧃', 'snack', FALSE, 63),
('可乐', '1 罐可乐', 140, '🥤', 'snack', FALSE, 64),
('椰子水', '1 杯椰子水', 45, '🥥', 'snack', FALSE, 65);

INSERT INTO exercise_types (type_key, name, emoji, bg_color, bar_color, kcal, sort_order) VALUES
('yoga', '瑜伽', '🧘', '#bacac2', '#2a6049', 320, 1),
('walk', '快走', '🚶', '#ffe2d3', '#ec744a', 560, 2),
('run', '跑步', '🏃', '#d2d0e8', '#655eb0', 210, 3),
('ride', '骑车', '🚴', '#fdedd0', '#f8c060', 180, 4);

INSERT INTO exercise_lessons (type_key, title, level, minutes, calories, emoji, sort_order) VALUES
('yoga', '瑜伽 #1', '初级', 32, 320, '🧘', 1),
('walk', '快走 #1', '中级', 45, 560, '🚶', 2);

INSERT INTO sleep_sounds (name, emoji, audio_url, sort_order) VALUES
('潜鸟鸣声', '🎵', NULL, 1);

INSERT INTO devices (name, icon, capabilities, is_primary, sort_order) VALUES
('智能手表', '⌚', '心率 · 睡眠 · 步数', TRUE, 1),
('智能手机', '📱', '步数 · 睡眠', FALSE, 2),
('智能体脂秤', '⚖️', '体重 · 体脂', FALSE, 3);

INSERT INTO water_portions (name, size_label, emoji, ml, sort_order) VALUES
('1 杯', '(130 毫升)', '🥛', 130, 1),
('1 瓶', '(260 毫升)', '🍶', 260, 2),
('1 大瓶', '(520 毫升)', '🫗', 520, 3);

INSERT INTO meal_types (meal_key, label, emoji, sort_order) VALUES
('breakfast', '早餐', '🍳', 1),
('lunch', '午餐', '🍱', 2),
('dinner', '晚餐', '🍲', 3),
('snack', '零食', '🍪', 4);
