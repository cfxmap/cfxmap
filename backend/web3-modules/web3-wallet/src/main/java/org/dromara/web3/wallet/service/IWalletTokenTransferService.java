package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.WalletTokenTransfer;

public interface IWalletTokenTransferService {
    boolean insert(WalletTokenTransfer entity);
}
