-- 以 MySQL root 身份执行一次：
--   mysql -u root -p < db/init.sql
CREATE DATABASE IF NOT EXISTS healthcare
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'healthcare'@'localhost' IDENTIFIED BY 'healthcare_dev';
CREATE USER IF NOT EXISTS 'healthcare'@'127.0.0.1' IDENTIFIED BY 'healthcare_dev';

GRANT ALL PRIVILEGES ON healthcare.* TO 'healthcare'@'localhost';
GRANT ALL PRIVILEGES ON healthcare.* TO 'healthcare'@'127.0.0.1';
FLUSH PRIVILEGES;
