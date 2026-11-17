package com.healthcare.api.sms;

/**
 * 短信发送抽象：开发环境用日志输出验证码，生产环境通过实现类接入具体短信服务。
 */
public interface SmsSender {

    void sendVerificationCode(String phone, String code);
}
