package org.dromara.web3.wallet.kms;

/**
 * 钱包密钥管理服务契约。
 *
 * <p>云 KMS、HSM 或其他远程密钥服务可通过实现该接口接入。</p>
 */
public interface KmsService {

    String provider();

    String encrypt(String plaintext, KmsKeyPurpose purpose);

    String decrypt(String ciphertext, KmsKeyPurpose purpose);

    boolean supports(String ciphertext);
}
