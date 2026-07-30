package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.WalletTransaction;
import java.util.List;

public interface IWalletTransactionService {
    WalletTransaction queryById(Long id);
    List<WalletTransaction> queryList(WalletTransaction entity);
    Boolean insertByBo(WalletTransaction entity);
    Boolean updateByBo(WalletTransaction entity);
}
