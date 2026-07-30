package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.Web3AppVersionConfig;

public interface IWeb3AppVersionConfigService {

    Web3AppVersionConfig getSingleton();

    boolean saveSingleton(Web3AppVersionConfig config);
}
