package com.youhu.caregiver.auth.sms;

/**
 * 短信发送抽象：开发环境用 {@link MockSmsSender}，生产可替换为阿里云/腾讯云实现。
 */
public interface SmsSender {

    /** 发送验证码；实现方需保证不抛出让业务失败的非关键异常 */
    void send(String phone, String code);
}
