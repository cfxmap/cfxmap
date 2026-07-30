package org.dromara.web3.wallet.kms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 钱包 KMS 路由服务：配置决定新数据的加密提供方，密文格式决定解密提供方。
 */
@Primary
@Component
public class WalletKmsService implements KmsService {

    private final KmsService activeProvider;
    private final LegacyAesKmsService legacyProvider;
    private final AliyunKmsService aliyunProvider;

    public WalletKmsService(
        @Value("${wallet.kms.provider:legacy}") String provider,
        LegacyAesKmsService legacyProvider,
        AliyunKmsService aliyunProvider) {
        this.legacyProvider = legacyProvider;
        this.aliyunProvider = aliyunProvider;
        this.activeProvider = switch (provider.trim().toLowerCase()) {
            case "legacy" -> legacyProvider;
            case "aliyun" -> {
                aliyunProvider.validateConfiguration();
                yield aliyunProvider;
            }
            default -> throw new IllegalArgumentException("不支持的钱包 KMS provider: " + provider);
        };
    }

    @Override
    public String provider() {
        return activeProvider.provider();
    }

    @Override
    public String encrypt(String plaintext, KmsKeyPurpose purpose) {
        return activeProvider.encrypt(plaintext, purpose);
    }

    @Override
    public String decrypt(String ciphertext, KmsKeyPurpose purpose) {
        if (aliyunProvider.supports(ciphertext)) {
            return aliyunProvider.decrypt(ciphertext, purpose);
        }
        if (legacyProvider.supports(ciphertext)) {
            return legacyProvider.decrypt(ciphertext, purpose);
        }
        throw new IllegalArgumentException("无法识别的钱包 KMS 密文格式");
    }

    @Override
    public boolean supports(String ciphertext) {
        return aliyunProvider.supports(ciphertext) || legacyProvider.supports(ciphertext);
    }

    public String activeProvider() {
        return provider();
    }
}
