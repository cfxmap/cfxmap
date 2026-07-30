package org.dromara.web3.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.web3.wallet.domain.Web3AppVersionConfig;
import org.dromara.web3.wallet.mapper.Web3AppVersionConfigMapper;
import org.dromara.web3.wallet.service.IWeb3AppVersionConfigService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class Web3AppVersionConfigServiceImpl implements IWeb3AppVersionConfigService {

    private final Web3AppVersionConfigMapper baseMapper;

    @Override
    public Web3AppVersionConfig getSingleton() {
        Web3AppVersionConfig row = baseMapper.selectById(Web3AppVersionConfig.SINGLETON_ID);
        if (row == null) {
            row = new Web3AppVersionConfig();
            row.setId(Web3AppVersionConfig.SINGLETON_ID);
            row.setCurrentVersion("1.0.0");
            row.setOfficialUrl("");
            row.setApkUrl("");
            baseMapper.insert(row);
            return baseMapper.selectById(Web3AppVersionConfig.SINGLETON_ID);
        }
        return row;
    }

    @Override
    public boolean saveSingleton(Web3AppVersionConfig config) {
        if (config == null || StringUtils.isBlank(config.getCurrentVersion())) {
            return false;
        }
        String url = config.getOfficialUrl() == null ? "" : config.getOfficialUrl().trim();
        if (StringUtils.isNotBlank(url) && !url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        Web3AppVersionConfig exist = baseMapper.selectById(Web3AppVersionConfig.SINGLETON_ID);
        Web3AppVersionConfig toSave = new Web3AppVersionConfig();
        toSave.setId(Web3AppVersionConfig.SINGLETON_ID);
        toSave.setCurrentVersion(config.getCurrentVersion().trim());
        toSave.setOfficialUrl(url);
        toSave.setApkUrl(config.getApkUrl() == null ? "" : config.getApkUrl().trim());
        if (exist == null) {
            return baseMapper.insert(toSave) > 0;
        }
        return baseMapper.updateById(toSave) > 0;
    }
}
