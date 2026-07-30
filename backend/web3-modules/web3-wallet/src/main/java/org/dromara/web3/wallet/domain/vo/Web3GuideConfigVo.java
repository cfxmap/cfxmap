package org.dromara.web3.wallet.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.dromara.web3.wallet.domain.Web3GuideConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 指南配置对外展示：三个列表字段为 JSON 数组，避免 data 内再嵌一层转义字符串。
 */
@Data
public class Web3GuideConfigVo implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Web3GuideConfigVo.class);
    private static final int MAX_JSON_UNWRAP_DEPTH = 8;

    private static final long serialVersionUID = 1L;

    private Long id;
    private String language;
    private String name;
    private String eco;
    private String tool;
    private String heroTitle;
    private String heroSubtitle;
    private String heroBtnText;
    private String firstTitle;
    private String firstSubtitle;
    private String secondTitle;
    private String secondSubtitle;
    private JsonNode contentList;
    private JsonNode menuList;
    private JsonNode toolList;
    private Date createTime;
    private Date updateTime;

    public static Web3GuideConfigVo from(Web3GuideConfig src, ObjectMapper om) {
        if (src == null) {
            return null;
        }
        Web3GuideConfigVo vo = new Web3GuideConfigVo();
        BeanUtils.copyProperties(src, vo, "contentList", "menuList", "toolList");
        vo.setContentList(parseListJson(om, src.getContentList()));
        vo.setMenuList(parseListJson(om, src.getMenuList()));
        vo.setToolList(parseListJson(om, src.getToolList()));
        return vo;
    }

    /**
     * 库里常见错误：HTML 写成 &lt;span class="rate"&gt; 未在 JSON 字符串内转义，导致 readTree 失败。
     * 已合法的数据为 class=\"rate\"（物理字符反斜杠+引号），不会匹配本替换。
     */
    private static String repairUnescapedSpanRateQuotes(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        return json.replace("<span class=\"rate\">", "<span class=\\\"rate\\\">");
    }

    private static JsonNode parseListJson(ObjectMapper om, String json) {
        if (json == null || json.isBlank()) {
            return om.createArrayNode();
        }
        String trimmed = stripBom(json.trim());
        JsonNode parsed = tryReadListTree(om, trimmed);
        if (parsed != null) {
            return parsed;
        }
        parsed = tryReadListTree(om, repairUnescapedSpanRateQuotes(trimmed));
        if (parsed == null) {
            String preview = trimmed.length() > 160 ? trimmed.substring(0, 160) + "…" : trimmed;
            log.warn("指南列表字段无法解析为 JSON 数组，已返回 []。片段: {}", preview);
        }
        return parsed != null ? parsed : om.createArrayNode();
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static JsonNode tryReadListTree(ObjectMapper om, String json) {
        try {
            JsonNode node = om.readTree(json);
            return unwrapToArrayNode(om, node, 0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根节点为数组 / 单对象 / 文本（整段再包一层 JSON 字符串）时统一成数组；无法识别时返回 null，禁止误返回空数组。
     */
    private static JsonNode unwrapToArrayNode(ObjectMapper om, JsonNode node, int depth) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (depth > MAX_JSON_UNWRAP_DEPTH) {
            return null;
        }
        if (node.isArray()) {
            return node;
        }
        if (node.isObject()) {
            return om.createArrayNode().add(node);
        }
        if (node.isTextual()) {
            String t = node.asText();
            if (t == null) {
                return null;
            }
            String s = t.trim();
            if (s.isEmpty()) {
                return null;
            }
            if (!s.startsWith("[") && !s.startsWith("{")) {
                return null;
            }
            try {
                JsonNode inner = om.readTree(s);
                return unwrapToArrayNode(om, inner, depth + 1);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
