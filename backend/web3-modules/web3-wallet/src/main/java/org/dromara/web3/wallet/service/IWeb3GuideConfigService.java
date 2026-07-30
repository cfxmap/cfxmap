package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.Web3GuideConfig;
import java.util.List;

public interface IWeb3GuideConfigService {
    
    Web3GuideConfig getConfigByLanguage(String language);
    
    List<Web3GuideConfig> listAll();
    
    boolean saveOrUpdateConfig(Web3GuideConfig config);
}
