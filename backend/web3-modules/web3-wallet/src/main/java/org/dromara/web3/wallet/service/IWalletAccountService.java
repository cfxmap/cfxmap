package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.WalletAccount;
import java.util.List;

public interface IWalletAccountService {
    WalletAccount queryById(Long id);
    List<WalletAccount> queryList(WalletAccount entity);
    Boolean insertByBo(WalletAccount entity);
    Boolean updateByBo(WalletAccount entity);

    /**
     * 物理删除钱包账户（需由调用方校验归属与业务规则）
     */
    Boolean deleteById(Long accountId);
}
