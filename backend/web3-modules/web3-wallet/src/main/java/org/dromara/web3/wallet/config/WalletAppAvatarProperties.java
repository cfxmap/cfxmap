package org.dromara.web3.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * APP 用户头像配置，支持本地存储与阿里云 OSS 直传。
 */
@Data
@Component
@ConfigurationProperties(prefix = "web3.app.avatar")
public class WalletAppAvatarProperties {

    /**
     * 物理存储根目录（须可写）
     */
    private String storagePath = System.getProperty("java.io.tmpdir") + File.separator + "web3-app-avatar";

    /**
     * 对外访问路径前缀，须与 {@code security.excludes} 中放行规则一致
     */
    private String urlPathPrefix = "/static/app-avatar";

    /**
     * 可选：反向代理场景下填写对外完整根地址（无尾斜杠），例如 https://api.example.com；
     * 留空则从当前请求推断完整头像 URL。
     */
    private String publicBaseUrl = "";

    /**
     * 是否启用阿里云 OSS 直传头像。
     */
    private boolean ossEnabled = false;

    /**
     * 阿里云 OSS AccessKeyId。
     */
    private String ossAccessKeyId = "";

    /**
     * 阿里云 OSS AccessKeySecret。
     */
    private String ossAccessKeySecret = "";

    /**
     * 阿里云 OSS Endpoint，例如 oss-cn-hangzhou.aliyuncs.com。
     */
    private String ossEndpoint = "";

    /**
     * 阿里云 OSS 上传域名，留空则使用 bucket + endpoint 拼接。
     */
    private String ossUploadBaseUrl = "";

    /**
     * 阿里云 OSS Bucket 名称。
     */
    private String ossBucketName = "";

    /**
     * 阿里云 OSS 对外访问根地址，留空则使用 bucket + endpoint 拼接。
     */
    private String ossPublicBaseUrl = "";

    /**
     * 阿里云 OSS 内对象前缀。
     */
    private String ossDirPrefix = "app-avatar";

    /**
     * 阿里云 OSS 直传策略有效期，单位秒。
     */
    private long ossPolicyExpireSeconds = 300;

    /**
     * 头像最大上传字节数，默认 5MB。
     */
    private long maxSize = 5L * 1024 * 1024;
}
