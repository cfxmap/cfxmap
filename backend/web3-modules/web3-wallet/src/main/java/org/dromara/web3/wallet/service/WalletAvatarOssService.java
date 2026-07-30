package org.dromara.web3.wallet.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.web3.wallet.config.WalletAppAvatarProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * APP 头像阿里云 OSS 直传策略服务。
 */
@Service
@RequiredArgsConstructor
public class WalletAvatarOssService {

    private final WalletAppAvatarProperties props;

    public boolean enabled() {
        return props.isOssEnabled();
    }

    public void validateConfigured() {
        if (!enabled()) {
            throw new IllegalStateException("头像 OSS 直传未启用");
        }
        if (StringUtils.isAnyBlank(
            props.getOssAccessKeyId(),
            props.getOssAccessKeySecret(),
            props.getOssEndpoint(),
            props.getOssBucketName()
        )) {
            throw new IllegalStateException("头像 OSS 配置不完整");
        }
    }

    public AvatarUploadPolicy createPolicy(Long userId, String originalFileName, String contentType) {
        validateConfigured();
        String suffix = normalizeSuffix(originalFileName, contentType);
        String dir = buildDir(userId);
        String objectKey = dir + IdUtil.fastSimpleUUID() + suffix;
        long expireSeconds = Math.max(props.getOssPolicyExpireSeconds(), 60L);
        Date expiration = DateUtil.offsetSecond(new Date(), (int) expireSeconds);

        PolicyConditions policyConditions = new PolicyConditions();
        policyConditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);
        policyConditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, props.getMaxSize());

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                buildEndpointUrl(props.getOssEndpoint()),
                props.getOssAccessKeyId(),
                props.getOssAccessKeySecret()
            );
            String postPolicy = ossClient.generatePostPolicy(expiration, policyConditions);
            String policyBase64 = java.util.Base64.getEncoder()
                .encodeToString(postPolicy.getBytes(StandardCharsets.UTF_8));
            String signature = ossClient.calculatePostSignature(postPolicy);

            Map<String, String> formData = new LinkedHashMap<>();
            formData.put("key", objectKey);
            formData.put("policy", policyBase64);
            formData.put("OSSAccessKeyId", props.getOssAccessKeyId());
            formData.put("success_action_status", "200");
            formData.put("signature", signature);
            if (StringUtils.isNotBlank(contentType)) {
                formData.put("Content-Type", contentType);
            }

            String host = buildUploadHost();
            String url = buildPublicUrl(objectKey);
            return new AvatarUploadPolicy(host, objectKey, url, expireSeconds, formData);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public String buildPublicUrl(String objectKey) {
        String base = StringUtils.isNotBlank(props.getOssPublicBaseUrl())
            ? props.getOssPublicBaseUrl().trim()
            : buildDefaultUploadHost();
        return StringUtils.stripEnd(base, "/") + "/" + StringUtils.removeStart(objectKey, "/");
    }

    private String buildUploadHost() {
        if (StringUtils.isNotBlank(props.getOssUploadBaseUrl())) {
            return StringUtils.stripEnd(props.getOssUploadBaseUrl().trim(), "/");
        }
        return buildDefaultUploadHost();
    }

    private String buildDefaultUploadHost() {
        String endpoint = props.getOssEndpoint().trim();
        endpoint = StringUtils.removeStart(endpoint, "https://");
        endpoint = StringUtils.removeStart(endpoint, "http://");
        return "https://" + props.getOssBucketName() + "." + endpoint;
    }

    private String buildDir(Long userId) {
        String prefix = StringUtils.blankToDefault(props.getOssDirPrefix(), "app-avatar").trim();
        prefix = StringUtils.removeStart(prefix, "/");
        prefix = StringUtils.removeEnd(prefix, "/");
        return prefix + "/" + userId + "/";
    }

    private static String buildEndpointUrl(String endpoint) {
        String trimmed = endpoint.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private static String normalizeSuffix(String originalFileName, String contentType) {
        String ext = "";
        if (StringUtils.isNotBlank(originalFileName) && originalFileName.contains(".")) {
            ext = originalFileName.substring(originalFileName.lastIndexOf('.')).trim();
        }
        if (StringUtils.isBlank(ext)) {
            String ct = StringUtils.blankToDefault(contentType, "").toLowerCase();
            if (ct.contains("png")) {
                ext = ".png";
            } else if (ct.contains("gif")) {
                ext = ".gif";
            } else if (ct.contains("webp")) {
                ext = ".webp";
            } else if (ct.contains("heic") || ct.contains("heif")) {
                ext = ".heic";
            } else {
                ext = ".jpg";
            }
        }
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        return ext.toLowerCase();
    }

    public record AvatarUploadPolicy(
        String host,
        String objectKey,
        String url,
        long expireSeconds,
        Map<String, String> formData
    ) {}
}
