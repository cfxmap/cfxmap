package org.dromara.web3.wallet.kms;

import cn.hutool.crypto.digest.DigestUtil;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 兼容现有数据库密文的 AES 密钥实现。
 */
@Component
public class LegacyAesKmsService implements KmsService {

    private final String secretKey;
    private final String secretKeyPrivate;
    private final String secretKeyMnemonic;

    public LegacyAesKmsService(
        @Value("${wallet.secret-key:web3WalletAES16!}") String secretKey,
        @Value("${wallet.secret-key-private:}") String secretKeyPrivate,
        @Value("${wallet.secret-key-mnemonic:}") String secretKeyMnemonic) {
        this.secretKey = secretKey;
        this.secretKeyPrivate = secretKeyPrivate;
        this.secretKeyMnemonic = secretKeyMnemonic;
    }

    @Override
    public String provider() {
        return "legacy";
    }

    @Override
    public String encrypt(String plaintext, KmsKeyPurpose purpose) {
        return EncryptUtils.encryptByAes(plaintext, purposeKey(purpose));
    }

    @Override
    public String decrypt(String ciphertext, KmsKeyPurpose purpose) {
        try {
            String plaintext = EncryptUtils.decryptByAes(ciphertext, purposeKey(purpose));
            if (isExpectedValue(plaintext, purpose)) {
                return plaintext;
            }
        } catch (Exception ignored) {
            // 尝试历史单一主密钥，兼容存量数据。
        }
        return EncryptUtils.decryptByAes(ciphertext, legacyWalletAesKey());
    }

    @Override
    public boolean supports(String ciphertext) {
        return StringUtils.isNotBlank(ciphertext) && !ciphertext.startsWith("KMS_");
    }

    String encryptWithLegacyMasterKey(String plaintext) {
        return EncryptUtils.encryptByAes(plaintext, legacyWalletAesKey());
    }

    private String purposeKey(KmsKeyPurpose purpose) {
        return switch (purpose) {
            case PRIVATE_KEY -> privateKeyAesKey();
            case MNEMONIC -> mnemonicAesKey();
        };
    }

    private String legacyWalletAesKey() {
        return normalizeAesPassword(secretKey);
    }

    private String privateKeyAesKey() {
        if (StringUtils.isNotBlank(secretKeyPrivate)) {
            return normalizeAesPassword(secretKeyPrivate);
        }
        return DigestUtil.sha256Hex(secretKey + "|WEB3_WALLET_AES_PRIVATE_V1|").substring(0, 32);
    }

    private String mnemonicAesKey() {
        if (StringUtils.isNotBlank(secretKeyMnemonic)) {
            return normalizeAesPassword(secretKeyMnemonic);
        }
        return DigestUtil.sha256Hex(secretKey + "|WEB3_WALLET_AES_MNEMONIC_V1|").substring(0, 32);
    }

    private String normalizeAesPassword(String raw) {
        if (StringUtils.isBlank(raw)) {
            throw new IllegalStateException("钱包 AES 密钥不能为空");
        }
        String value = raw.trim();
        int length = value.length();
        if (length == 16 || length == 24 || length == 32) {
            return value;
        }
        return DigestUtil.sha256Hex(value).substring(0, 32);
    }

    private boolean isExpectedValue(String plaintext, KmsKeyPurpose purpose) {
        return switch (purpose) {
            case PRIVATE_KEY -> looksLikeHexPrivateKey(plaintext);
            case MNEMONIC -> looksLikeMnemonic(plaintext);
        };
    }

    private boolean looksLikeHexPrivateKey(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        String normalized = value.startsWith("0x") || value.startsWith("0X") ? value.substring(2) : value;
        return normalized.length() == 64 && normalized.matches("[0-9a-fA-F]+");
    }

    private boolean looksLikeMnemonic(String value) {
        return StringUtils.isNotBlank(value) && value.length() >= 16 && value.trim().split("\\s+").length >= 3;
    }
}
