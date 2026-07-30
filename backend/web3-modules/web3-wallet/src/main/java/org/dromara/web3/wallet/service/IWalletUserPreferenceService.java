package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.WalletUserPreference;
import java.util.List;

public interface IWalletUserPreferenceService {
    WalletUserPreference queryById(Long id);
    List<WalletUserPreference> queryList(WalletUserPreference entity);
    Boolean insertByBo(WalletUserPreference entity);
    Boolean updateByBo(WalletUserPreference entity);
}
