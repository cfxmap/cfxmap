package org.dromara.web3.wallet.kms;

public interface AliyunKmsClient {

    default void validateConfiguration() {
    }

    String encrypt(String keyId, String base64Plaintext) throws Exception;

    String decrypt(String ciphertextBlob) throws Exception;
}
