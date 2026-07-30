package org.dromara.web3.wallet.kms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletKmsServiceTest {

    private static final String PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

    @Test
    void legacyProviderMustRoundTripBothPurposesAndReadHistoricalCiphertext() {
        LegacyAesKmsService service = new LegacyAesKmsService("web3WalletAES16!", "", "");

        String privateCipher = service.encrypt(PRIVATE_KEY, KmsKeyPurpose.PRIVATE_KEY);
        String mnemonicCipher = service.encrypt(MNEMONIC, KmsKeyPurpose.MNEMONIC);

        assertEquals(PRIVATE_KEY, service.decrypt(privateCipher, KmsKeyPurpose.PRIVATE_KEY));
        assertEquals(MNEMONIC, service.decrypt(mnemonicCipher, KmsKeyPurpose.MNEMONIC));
        assertEquals(PRIVATE_KEY,
            service.decrypt(service.encryptWithLegacyMasterKey(PRIVATE_KEY), KmsKeyPurpose.PRIVATE_KEY));
    }

    @Test
    void routerMustReadAliyunAndHistoricalCiphertextWhenAliyunProviderIsEnabled() {
        LegacyAesKmsService legacy = new LegacyAesKmsService("web3WalletAES16!", "", "");
        AliyunKmsService aliyun = aliyunService();
        WalletKmsService router = new WalletKmsService("aliyun", legacy, aliyun);
        String legacyCipher = legacy.encrypt(PRIVATE_KEY, KmsKeyPurpose.PRIVATE_KEY);

        String aliyunCipher = router.encrypt(PRIVATE_KEY, KmsKeyPurpose.PRIVATE_KEY);

        assertEquals("aliyun", router.activeProvider());
        assertTrue(aliyun.supports(aliyunCipher));
        assertFalse(legacy.supports(aliyunCipher));
        assertEquals(PRIVATE_KEY, router.decrypt(aliyunCipher, KmsKeyPurpose.PRIVATE_KEY));
        assertEquals(PRIVATE_KEY, router.decrypt(legacyCipher, KmsKeyPurpose.PRIVATE_KEY));
    }

    @Test
    void routerMustRejectUnknownVersionedCiphertext() {
        LegacyAesKmsService legacy = new LegacyAesKmsService("web3WalletAES16!", "", "");
        WalletKmsService router = new WalletKmsService("legacy", legacy, aliyunService());
        String unknownCiphertext = "KMS_ALIYUN_V2:unknown";

        assertFalse(legacy.supports(unknownCiphertext));
        assertFalse(router.supports(unknownCiphertext));
        assertThrows(IllegalArgumentException.class,
            () -> router.decrypt(unknownCiphertext, KmsKeyPurpose.PRIVATE_KEY));
    }

    @Test
    void routerMustRejectUnknownProvider() {
        LegacyAesKmsService legacy = new LegacyAesKmsService("web3WalletAES16!", "", "");

        assertThrows(IllegalArgumentException.class,
            () -> new WalletKmsService("cloud", legacy, aliyunService()));
    }

    private AliyunKmsService aliyunService() {
        return new AliyunKmsService(new AliyunKmsClient() {
            @Override
            public String encrypt(String keyId, String base64Plaintext) {
                return base64Plaintext;
            }

            @Override
            public String decrypt(String ciphertextBlob) {
                return ciphertextBlob;
            }
        }, "key-private", "key-mnemonic");
    }
}
