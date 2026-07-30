package org.dromara.web3.wallet.crypto;

import org.dromara.common.core.constant.Constants;
import org.dromara.web3.wallet.domain.WalletAccount;
import org.dromara.web3.wallet.kms.AliyunKmsClient;
import org.dromara.web3.wallet.kms.AliyunKmsService;
import org.dromara.web3.wallet.kms.LegacyAesKmsService;
import org.dromara.web3.wallet.kms.WalletKmsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletSecretCryptoTest {

    private static final String PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

    @Test
    void aliyunProviderMustEncryptEntityOnlyOnceAndDecryptIt() {
        WalletSecretCrypto crypto = crypto(aliyunClient());
        WalletAccount account = account();

        crypto.encryptIfNeeded(account);
        String encryptedPrivateKey = account.getPrivateKey();
        String encryptedMnemonic = account.getMnemonic();
        crypto.encryptIfNeeded(account);

        assertTrue(encryptedPrivateKey.startsWith(Constants.ENCRYPT_HEADER + AliyunKmsService.CIPHER_PREFIX));
        assertEquals(encryptedPrivateKey, account.getPrivateKey());
        assertEquals(encryptedMnemonic, account.getMnemonic());

        crypto.decryptIfNeeded(account);
        assertEquals(PRIVATE_KEY, account.getPrivateKey());
        assertEquals(MNEMONIC, account.getMnemonic());
    }

    @Test
    void aliyunProviderMustStillDecryptLegacyEntityCiphertext() {
        LegacyAesKmsService legacy = new LegacyAesKmsService("web3WalletAES16!", "", "");
        WalletSecretCrypto legacyCrypto = new WalletSecretCrypto(
            new WalletKmsService("legacy", legacy, aliyunService(aliyunClient())));
        WalletAccount account = account();
        legacyCrypto.encryptIfNeeded(account);

        crypto(aliyunClient()).decryptIfNeeded(account);

        assertEquals(PRIVATE_KEY, account.getPrivateKey());
        assertEquals(MNEMONIC, account.getMnemonic());
    }

    @Test
    void decryptionFailureMustBeAtomicAndFailClosed() {
        WalletSecretCrypto correctCrypto = crypto(aliyunClient());
        WalletAccount account = account();
        correctCrypto.encryptIfNeeded(account);
        String encryptedPrivateKey = account.getPrivateKey();
        String encryptedMnemonic = account.getMnemonic();

        AliyunKmsClient failingClient = new AliyunKmsClient() {
            @Override
            public String encrypt(String keyId, String base64Plaintext) {
                return base64Plaintext;
            }

            @Override
            public String decrypt(String ciphertextBlob) {
                throw new IllegalStateException("KMS request failed");
            }
        };
        WalletSecretCrypto failingCrypto = crypto(failingClient);

        assertThrows(IllegalStateException.class, () -> failingCrypto.decryptIfNeeded(account));
        assertEquals(encryptedPrivateKey, account.getPrivateKey());
        assertEquals(encryptedMnemonic, account.getMnemonic());
    }

    private WalletSecretCrypto crypto(AliyunKmsClient client) {
        LegacyAesKmsService legacy = new LegacyAesKmsService("web3WalletAES16!", "", "");
        return new WalletSecretCrypto(new WalletKmsService("aliyun", legacy, aliyunService(client)));
    }

    private AliyunKmsService aliyunService(AliyunKmsClient client) {
        return new AliyunKmsService(client, "key-private", "key-mnemonic");
    }

    private AliyunKmsClient aliyunClient() {
        return new AliyunKmsClient() {
            @Override
            public String encrypt(String keyId, String base64Plaintext) {
                return base64Plaintext;
            }

            @Override
            public String decrypt(String ciphertextBlob) {
                return ciphertextBlob;
            }
        };
    }

    private WalletAccount account() {
        WalletAccount account = new WalletAccount();
        account.setPrivateKey(PRIVATE_KEY);
        account.setMnemonic(MNEMONIC);
        return account;
    }
}
