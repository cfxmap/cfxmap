package org.dromara.web3.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.web3.wallet.domain.Web3GuideConfig;
import org.dromara.web3.wallet.mapper.Web3GuideConfigMapper;
import org.dromara.web3.wallet.service.IWeb3GuideConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class Web3GuideConfigServiceImpl implements IWeb3GuideConfigService {

    private final Web3GuideConfigMapper baseMapper;

    @Override
    public Web3GuideConfig getConfigByLanguage(String language) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Web3GuideConfig>()
            .eq(Web3GuideConfig::getLanguage, language));
    }

    @Override
    public List<Web3GuideConfig> listAll() {
        return baseMapper.selectList();
    }

    @Override
    public boolean saveOrUpdateConfig(Web3GuideConfig config) {
        Web3GuideConfig exist = getConfigByLanguage(config.getLanguage());
        if (exist != null) {
            config.setId(exist.getId());
            return baseMapper.updateById(config) > 0;
        }
        return baseMapper.insert(config) > 0;
    }
}
