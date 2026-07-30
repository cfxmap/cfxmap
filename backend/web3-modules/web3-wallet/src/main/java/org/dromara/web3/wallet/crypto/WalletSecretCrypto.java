package org.dromara.web3.wallet.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.web3.wallet.domain.WalletAccount;
import org.dromara.web3.wallet.kms.KmsKeyPurpose;
import org.dromara.web3.wallet.kms.WalletKmsService;
import org.springframework.stereotype.Component;

/**
 * 钱包敏感数据持久化适配器，具体加解密方式由 KMS 路由服务决定。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSecretCrypto {

    private final WalletKmsService walletKmsService;

    public void encryptIfNeeded(WalletAccount account) {
        if (account == null) {
            return;
        }
        try {
            if (StringUtils.isNotBlank(account.getPrivateKey())
                && !StringUtils.startsWith(account.getPrivateKey(), Constants.ENCRYPT_HEADER)) {
                account.setPrivateKey(Constants.ENCRYPT_HEADER
                    + walletKmsService.encrypt(account.getPrivateKey(), KmsKeyPurpose.PRIVATE_KEY));
            }
            if (StringUtils.isNotBlank(account.getMnemonic())
                && !StringUtils.startsWith(account.getMnemonic(), Constants.ENCRYPT_HEADER)) {
                account.setMnemonic(Constants.ENCRYPT_HEADER
                    + walletKmsService.encrypt(account.getMnemonic(), KmsKeyPurpose.MNEMONIC));
            }
        } catch (Exception e) {
            log.error("wallet secret encrypt failed with provider {}", walletKmsService.activeProvider(), e);
            throw new IllegalStateException("钱包敏感数据加密失败", e);
        }
    }

    public void decryptIfNeeded(WalletAccount account) {
        if (account == null) {
            return;
        }
        try {
            String privateKey = decryptIfEncrypted(account.getPrivateKey(), KmsKeyPurpose.PRIVATE_KEY);
            String mnemonic = decryptIfEncrypted(account.getMnemonic(), KmsKeyPurpose.MNEMONIC);
            account.setPrivateKey(privateKey);
            account.setMnemonic(mnemonic);
        } catch (Exception e) {
            log.error("wallet secret decrypt failed", e);
            throw new IllegalStateException("钱包敏感数据解密失败", e);
        }
    }

    private String decryptIfEncrypted(String value, KmsKeyPurpose purpose) {
        if (StringUtils.isBlank(value) || !StringUtils.startsWith(value, Constants.ENCRYPT_HEADER)) {
            return value;
        }
        String ciphertext = StringUtils.removeStart(value, Constants.ENCRYPT_HEADER);
        return walletKmsService.decrypt(ciphertext, purpose);
    }
}
