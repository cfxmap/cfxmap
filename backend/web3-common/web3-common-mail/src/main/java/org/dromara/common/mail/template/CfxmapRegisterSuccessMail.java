package org.dromara.common.mail.template;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.mail.utils.MailUtils;

/**
 * CFXMAP 注册成功通知邮件。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CfxmapRegisterSuccessMail {

    public static String send(String to) {
        return MailUtils.sendHtml(to, subject(), buildHtml());
    }

    private static String subject() {
        return "CFXMAP · 注册成功通知";
    }

    private static String buildHtml() {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"margin:0;padding:32px 16px;background:#050810;"
            + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;\">"
            + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" "
            + "style=\"max-width:520px;margin:0 auto;background:#0b101c;"
            + "border-radius:18px;border:1px solid rgba(91,140,255,0.28);"
            + "box-shadow:0 24px 64px rgba(0,0,0,0.45),0 0 1px rgba(0,212,255,0.15);\">"
            + "<tr><td style=\"padding:40px 36px 14px;text-align:center;\">"
            + "<p style=\"margin:0 0 10px;color:#6b8ab8;font-size:12px;letter-spacing:3px;text-transform:uppercase;\">"
            + "Account Activated</p>"
            + "<h1 style=\"margin:0;color:#eef4ff;font-size:24px;font-weight:700;\">CFXMAP 账号注册成功</h1>"
            + "<p style=\"margin:12px 0 0;color:#8a9bb8;font-size:14px;line-height:1.8;\">"
            + "你的邮箱账号已成功创建，现在可以直接使用邮箱和密码登录 CFXMAP。"
            + "</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:0 36px 28px;\">"
            + "<div style=\"padding:20px 22px;background:linear-gradient(145deg,rgba(0,212,255,0.08) 0%,rgba(91,140,255,0.06) 100%);"
            + "border:1px solid rgba(0,212,255,0.24);border-radius:14px;\">"
            + "<p style=\"margin:0;color:#dce7fb;font-size:14px;line-height:1.8;\">"
            + "安全提示：请妥善保管登录密码，不要将账号信息透露给他人。"
            + "</p>"
            + "<p style=\"margin:10px 0 0;color:#8a9bb8;font-size:13px;line-height:1.8;\">"
            + "If this registration was not initiated by you, please change your password immediately or contact support."
            + "</p>"
            + "</div>"
            + "</td></tr>"
            + "<tr><td style=\"padding:0 36px 32px;text-align:center;border-top:1px solid rgba(91,140,255,0.15);\">"
            + "<p style=\"margin:22px 0 0;color:#4a5a78;font-size:11px;letter-spacing:0.5px;\">"
            + "CFXMAP · Web3 Wallet · Registration Notice</p>"
            + "</td></tr></table></body></html>";
    }
}
