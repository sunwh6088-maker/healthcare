-- 登录会话附加信息：设备标识与来源 IP，用于「登录设备」管理
ALTER TABLE auth_tokens ADD COLUMN user_agent VARCHAR(255) DEFAULT NULL;
ALTER TABLE auth_tokens ADD COLUMN ip_address VARCHAR(45) DEFAULT NULL;
