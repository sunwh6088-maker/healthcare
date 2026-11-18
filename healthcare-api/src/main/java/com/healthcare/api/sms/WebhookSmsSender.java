package com.healthcare.api.sms;

import com.healthcare.api.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 通用短信 Webhook 发送器：向配置的地址 POST {"phone","code","scene"}。
 * 这样可以对接任意短信网关（阿里云/腾讯云函数、自建转发服务等），
 * 而不把供应商 SDK 绑进项目。
 */
@Service
public class WebhookSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSmsSender.class);

    private final RestClient restClient;
    private final String webhookUrl;
    private final String webhookToken;

    public WebhookSmsSender(@Value("${app.sms.webhook-url:}") String webhookUrl,
                            @Value("${app.sms.webhook-token:}") String webhookToken) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.webhookToken = webhookToken == null ? "" : webhookToken.trim();
    }

    @Override
    public void sendVerificationCode(String phone, String code, String scene) {
        if (webhookUrl.isBlank()) {
            log.error("SMS webhook is not configured; cannot send verification code");
            throw new ApiException(503, "短信服务未配置，请联系管理员");
        }
        try {
            var request = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("phone", phone, "code", code, "scene", scene));
            if (!webhookToken.isBlank()) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + webhookToken);
            }
            request.retrieve().toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to send verification code via SMS webhook", ex);
            throw new ApiException(503, "短信发送失败，请稍后重试");
        }
    }
}
