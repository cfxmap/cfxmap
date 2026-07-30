package org.dromara.web3.wallet.kms;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliyunKmsServiceTest {

    private static final String PRIVATE_KEY = "0x0123456789abcdef";
    private static final String MNEMONIC = "alpha beta gamma delta";

    @Test
    void shouldEncryptAndDecryptPrivateKeyWithDedicatedKey() {
        RecordingClient client = new RecordingClient();
        AliyunKmsService service = new AliyunKmsService(client, "key-private", "key-mnemonic");

        String ciphertext = service.encrypt(PRIVATE_KEY, KmsKeyPurpose.PRIVATE_KEY);

        assertEquals("key-private", client.lastKeyId);
        assertEquals(PRIVATE_KEY, decode(client.lastPlaintext));
        assertEquals(AliyunKmsService.CIPHER_PREFIX + "cipher-blob", ciphertext);
        assertEquals(PRIVATE_KEY, service.decrypt(ciphertext, KmsKeyPurpose.PRIVATE_KEY));
        assertEquals("cipher-blob", client.lastCiphertext);
    }

    @Test
    void shouldUseDedicatedMnemonicKey() {
        RecordingClient client = new RecordingClient();
        AliyunKmsService service = new AliyunKmsService(client, "key-private", "key-mnemonic");

        service.encrypt(MNEMONIC, KmsKeyPurpose.MNEMONIC);

        assertEquals("key-mnemonic", client.lastKeyId);
        assertEquals(MNEMONIC, decode(client.lastPlaintext));
    }

    @Test
    void shouldValidateAllConfigurationWhenAliyunProviderIsSelected() {
        AliyunKmsService missingPrivateKey = new AliyunKmsService(
            new RecordingClient(), "", "key-mnemonic");
        AliyunKmsService missingMnemonicKey = new AliyunKmsService(
            new RecordingClient(), "key-private", "");
        AliyunKmsService missingRegion = new AliyunKmsService(
            new RecordingClient(false), "key-private", "key-mnemonic");

        assertThrows(IllegalStateException.class, missingPrivateKey::validateConfiguration);
        assertThrows(IllegalStateException.class, missingMnemonicKey::validateConfiguration);
        assertThrows(IllegalStateException.class, missingRegion::validateConfiguration);
    }

    @Test
    void shouldRejectCiphertextWithoutAliyunPrefix() {
        AliyunKmsService service = new AliyunKmsService(new RecordingClient(), "key-private", "key-mnemonic");

        assertFalse(service.supports("cipher-blob"));
        assertThrows(IllegalArgumentException.class,
            () -> service.decrypt("cipher-blob", KmsKeyPurpose.PRIVATE_KEY));
    }

    @Test
    void shouldNotExposeSensitiveValuesWhenClientFails() {
        AliyunKmsClient client = new AliyunKmsClient() {
            @Override
            public String encrypt(String keyId, String base64Plaintext) throws Exception {
                throw new Exception("remote error");
            }

            @Override
            public String decrypt(String ciphertextBlob) throws Exception {
                throw new Exception("remote error");
            }
        };
        AliyunKmsService service = new AliyunKmsService(client, "key-private", "key-mnemonic");
        String ciphertext = AliyunKmsService.CIPHER_PREFIX + "sensitive-ciphertext";

        IllegalStateException encryptError = assertThrows(IllegalStateException.class,
            () -> service.encrypt(PRIVATE_KEY, KmsKeyPurpose.PRIVATE_KEY));
        IllegalStateException decryptError = assertThrows(IllegalStateException.class,
            () -> service.decrypt(ciphertext, KmsKeyPurpose.PRIVATE_KEY));

        assertFalse(encryptError.getMessage().contains(PRIVATE_KEY));
        assertFalse(encryptError.getMessage().contains("key-private"));
        assertFalse(decryptError.getMessage().contains(ciphertext));
        assertEquals(null, encryptError.getCause());
        assertEquals(null, decryptError.getCause());
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static final class RecordingClient implements AliyunKmsClient {
        private final boolean configured;
        private String lastKeyId;
        private String lastPlaintext;
        private String lastCiphertext;

        private RecordingClient() {
            this(true);
        }

        private RecordingClient(boolean configured) {
            this.configured = configured;
        }

        @Override
        public void validateConfiguration() {
            if (!configured) {
                throw new IllegalStateException("region missing");
            }
        }

        @Override
        public String encrypt(String keyId, String base64Plaintext) {
            lastKeyId = keyId;
            lastPlaintext = base64Plaintext;
            return "cipher-blob";
        }

        @Override
        public String decrypt(String ciphertextBlob) {
            lastCiphertext = ciphertextBlob;
            return Base64.getEncoder().encodeToString(PRIVATE_KEY.getBytes(StandardCharsets.UTF_8));
        }
    }
}
