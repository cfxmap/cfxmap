package org.dromara.common.mail.service;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.mail.template.CfxmapRegisterSuccessMail;
import org.dromara.common.mail.template.CfxmapVerifyCodeMail;
import org.springframework.scheduling.annotation.Async;

/**
 * 异步发送验证码邮件，避免 HTTP 请求阻塞在 SMTP 握手与投递上。
 */
@Slf4j
public class AsyncVerifyMailSender {

    @Async
    public void sendCfxmapVerifyCode(String to, String code, int validMinutes) {
        try {
            CfxmapVerifyCodeMail.send(to, code, validMinutes);
        } catch (Exception e) {
            log.error("异步验证码邮件发送失败 to={} => {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendCfxmapRegisterSuccess(String to) {
        try {
            CfxmapRegisterSuccessMail.send(to);
        } catch (Exception e) {
            log.error("异步注册成功邮件发送失败 to={} => {}", to, e.getMessage(), e);
        }
    }
}
