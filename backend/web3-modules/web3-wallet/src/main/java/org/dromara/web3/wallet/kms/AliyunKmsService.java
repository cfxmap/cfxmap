package org.dromara.web3.wallet.kms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AliyunKmsService implements KmsService {

    public static final String CIPHER_PREFIX = "KMS_ALIYUN_V1:";

    private final AliyunKmsClient client;
    private final String privateKeyId;
    private final String mnemonicKeyId;

    public AliyunKmsService(AliyunKmsClient client,
                     @Value("${wallet.kms.aliyun.private-key-id:}") String privateKeyId,
                     @Value("${wallet.kms.aliyun.mnemonic-key-id:}") String mnemonicKeyId) {
        this.client = client;
        this.privateKeyId = privateKeyId;
        this.mnemonicKeyId = mnemonicKeyId;
    }

    @Override
    public String provider() {
        return "aliyun";
    }

    @Override
    public String encrypt(String plaintext, KmsKeyPurpose purpose) {
        try {
            String encodedPlaintext = Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
            return CIPHER_PREFIX + client.encrypt(keyId(purpose), encodedPlaintext);
        } catch (Exception e) {
            throw new IllegalStateException("阿里云 KMS 加密失败，密钥用途: " + purpose.name());
        }
    }

    @Override
    public String decrypt(String ciphertext, KmsKeyPurpose purpose) {
        if (!supports(ciphertext)) {
            throw new IllegalArgumentException("不支持的阿里云 KMS 密文格式");
        }
        try {
            keyId(purpose);
            String encodedPlaintext = client.decrypt(ciphertext.substring(CIPHER_PREFIX.length()));
            return new String(Base64.getDecoder().decode(encodedPlaintext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("阿里云 KMS 解密失败，密钥用途: " + purpose.name());
        }
    }

    @Override
    public boolean supports(String ciphertext) {
        return StringUtils.hasText(ciphertext) && ciphertext.startsWith(CIPHER_PREFIX);
    }

    public void validateConfiguration() {
        client.validateConfiguration();
        keyId(KmsKeyPurpose.PRIVATE_KEY);
        keyId(KmsKeyPurpose.MNEMONIC);
    }

    private String keyId(KmsKeyPurpose purpose) {
        String keyId = switch (purpose) {
            case PRIVATE_KEY -> privateKeyId;
            case MNEMONIC -> mnemonicKeyId;
        };
        if (!StringUtils.hasText(keyId)) {
            throw new IllegalStateException("阿里云 KMS 密钥未配置，密钥用途: " + purpose.name());
        }
        return keyId.trim();
    }
}
