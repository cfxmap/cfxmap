package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.WalletNftTransfer;

public interface IWalletNftTransferService {
    boolean insert(WalletNftTransfer entity);
}
