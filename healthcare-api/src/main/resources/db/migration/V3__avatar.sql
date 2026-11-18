-- 头像版本号：每次上传/删除自增，用于让浏览器缓存失效
ALTER TABLE users ADD COLUMN avatar_version INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN has_avatar BIT(1) NOT NULL DEFAULT 0;
