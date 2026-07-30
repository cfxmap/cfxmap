package org.dromara.common.mail.template;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.mail.utils.MailUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * CFXMAP 登录/验证邮件：HTML 模板 + 内嵌 logo（classpath mail-assets/cfxmap.png）
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CfxmapVerifyCodeMail {

    private static final String LOGO_RESOURCE = "/mail-assets/cfxmap.png";
    private static final String LOGO_CID = "cfxmapLogo";

    /**
     * 发送带品牌视觉的验证码邮件
     *
     * @param to            收件人
     * @param verifyCode    验证码（仅数字等安全字符）
     * @param validMinutes  有效分钟数（与 Redis 中 TTL 一致）
     * @return message-id
     */
    public static String send(String to, String verifyCode, int validMinutes) {
        String safeCode = escapeHtml(verifyCode);
        byte[] logo = readLogoBytes();
        boolean hasLogo = logo != null && logo.length > 0;
        String html = buildHtml(safeCode, validMinutes, hasLogo);

        if (hasLogo) {
            Map<String, InputStream> images = new HashMap<>(1);
            images.put(LOGO_CID, new ByteArrayInputStream(logo));
            return MailUtils.sendHtml(to, subject(), html, images);
        }
        return MailUtils.sendHtml(to, subject(), html);
    }

    private static String subject() {
        return "CFXMAP · 安全登录验证";
    }

    private static byte[] readLogoBytes() {
        try (InputStream in = CfxmapVerifyCodeMail.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static String buildHtml(String safeCode, int validMinutes, boolean withLogo) {
        String logoImg = withLogo
            ? ("<img src=\"cid:" + LOGO_CID + "\" alt=\"CFXMAP\" width=\"112\" "
            + "style=\"display:block;margin:0 auto 20px;border:0;height:auto;\" />")
            : "";

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"margin:0;padding:32px 16px;background:#050810;"
            + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;\">"
            + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" "
            + "style=\"max-width:520px;margin:0 auto;background:#0b101c;"
            + "border-radius:18px;border:1px solid rgba(91,140,255,0.28);"
            + "box-shadow:0 24px 64px rgba(0,0,0,0.45),0 0 1px rgba(0,212,255,0.15);\">"
            + "<tr><td style=\"padding:40px 36px 12px;text-align:center;\">"
            + logoImg
            + "<p style=\"margin:0 0 10px;color:#6b8ab8;font-size:12px;letter-spacing:3px;"
            + "text-transform:uppercase;\">Security Gate · 身份网关</p>"
            + "<h1 style=\"margin:0;color:#eef4ff;font-size:21px;font-weight:600;"
            + "letter-spacing:0.4px;\">动态访问令牌</h1>"
            + "<p style=\"margin:10px 0 0;color:#5b8cff;font-size:11px;letter-spacing:2px;"
            + "text-transform:uppercase;\">Dynamic Access Token</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:16px 36px 28px;text-align:center;\">"
            + "<div style=\"display:inline-block;padding:22px 40px;margin-top:8px;"
            + "background:linear-gradient(145deg,rgba(0,212,255,0.08) 0%,rgba(91,140,255,0.06) 100%);"
            + "border:1px solid rgba(0,212,255,0.4);border-radius:14px;\">"
            + "<span style=\"font-family:Consolas,'SF Mono',Monaco,'Courier New',monospace;"
            + "font-size:34px;font-weight:700;letter-spacing:14px;color:#5cf0ff;"
            + "text-shadow:0 0 28px rgba(92,240,255,0.45);\">"
            + safeCode
            + "</span></div>"
            + "<p style=\"margin:26px 0 0;color:#8a9bb8;font-size:14px;line-height:1.75;\">"
            + "本次 <strong style=\"color:#c5d4ec;\">一次性校验密钥</strong> 用于完成登录鉴权，"
            + "<strong style=\"color:#7ec8ff;\">" + validMinutes + " 分钟</strong> 内有效。"
            + "<br/>请勿回复本邮件，切勿向他人泄露验证码。</p>"
            + "<p style=\"margin:14px 0 0;color:#5a6a85;font-size:12px;line-height:1.65;\">"
            + "One-time cryptographic token for session authentication. "
            + "Valid for " + validMinutes + " minute(s). "
            + "If you did not request this, please ignore.</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:0 36px 32px;text-align:center;"
            + "border-top:1px solid rgba(91,140,255,0.15);\">"
            + "<p style=\"margin:22px 0 0;color:#4a5a78;font-size:11px;letter-spacing:0.5px;\">"
            + "CFXMAP · Web3 Wallet · Encrypted Channel</p>"
            + "</td></tr></table></body></html>";
    }
}
