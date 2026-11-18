-- 常用查询索引：按用户查记录、按用户查闹钟、清理过期 token
CREATE INDEX idx_food_logs_user_date ON food_logs (user_id, log_date);
CREATE INDEX idx_exercise_logs_user_date ON exercise_logs (user_id, log_date);
CREATE INDEX idx_alarms_user ON alarms (user_id);
CREATE INDEX idx_auth_tokens_expires ON auth_tokens (expires_at);
