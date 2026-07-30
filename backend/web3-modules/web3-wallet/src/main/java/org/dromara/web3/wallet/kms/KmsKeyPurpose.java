package org.dromara.web3.wallet.kms;

/**
 * 钱包 KMS 密钥用途。不同用途必须使用相互隔离的加密上下文。
 */
public enum KmsKeyPurpose {
    PRIVATE_KEY,
    MNEMONIC
}
