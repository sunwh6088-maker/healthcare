package com.healthcare.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动自检：对不安全的配置输出醒目告警，避免误把开发配置部署到生产环境。
 */
@Component
public class StartupChecks implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    private final boolean devMode;
    private final boolean cookieSecure;
    private final String smsWebhookUrl;

    public StartupChecks(@Value("${app.dev-mode:true}") boolean devMode,
                         @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
                         @Value("${app.sms.webhook-url:}") String smsWebhookUrl) {
        this.devMode = devMode;
        this.cookieSecure = cookieSecure;
        this.smsWebhookUrl = smsWebhookUrl == null ? "" : smsWebhookUrl.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (devMode) {
            log.warn("APP_DEV_MODE=true：验证码会写入日志并返回前端，仅限开发环境使用");
            return;
        }
        if (smsWebhookUrl.isBlank()) {
            log.warn("生产模式未配置 SMS_WEBHOOK_URL，用户将无法接收验证码");
        }
        if (!cookieSecure) {
            log.warn("生产模式未启用 AUTH_COOKIE_SECURE，请务必通过 HTTPS 部署并设为 true");
        }
    }
}
