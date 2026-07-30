package org.dromara.web3.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.web3.wallet.domain.WalletUserPreference;
import org.dromara.web3.wallet.mapper.WalletUserPreferenceMapper;
import org.dromara.web3.wallet.service.IWalletUserPreferenceService;
import java.util.List;

@RequiredArgsConstructor
@Service
public class WalletUserPreferenceServiceImpl implements IWalletUserPreferenceService {

    private final WalletUserPreferenceMapper baseMapper;

    @Override
    public WalletUserPreference queryById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<WalletUserPreference> queryList(WalletUserPreference entity) {
        LambdaQueryWrapper<WalletUserPreference> lqw = Wrappers.lambdaQuery();
        if (entity != null) {
            if (entity.getUserId() != null) {
                lqw.eq(WalletUserPreference::getUserId, entity.getUserId());
            }
            if (entity.getTheme() != null && !entity.getTheme().isEmpty()) {
                lqw.eq(WalletUserPreference::getTheme, entity.getTheme());
            }
            if (entity.getLanguage() != null && !entity.getLanguage().isEmpty()) {
                lqw.eq(WalletUserPreference::getLanguage, entity.getLanguage());
            }
        }
        return baseMapper.selectList(lqw);
    }

    @Override
    public Boolean insertByBo(WalletUserPreference entity) {
        return baseMapper.insert(entity) > 0;
    }

    @Override
    public Boolean updateByBo(WalletUserPreference entity) {
        return baseMapper.updateById(entity) > 0;
    }
}
