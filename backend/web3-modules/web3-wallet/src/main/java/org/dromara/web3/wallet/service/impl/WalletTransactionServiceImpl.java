package org.dromara.web3.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.web3.wallet.domain.WalletTransaction;
import org.dromara.web3.wallet.mapper.WalletTransactionMapper;
import org.dromara.web3.wallet.service.IWalletTransactionService;
import java.util.List;

@RequiredArgsConstructor
@Service
public class WalletTransactionServiceImpl implements IWalletTransactionService {
    
    private final WalletTransactionMapper baseMapper;

    @Override
    public WalletTransaction queryById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<WalletTransaction> queryList(WalletTransaction entity) {
        LambdaQueryWrapper<WalletTransaction> lqw = Wrappers.lambdaQuery();
        if (entity != null) {
            if (entity.getUserId() != null) {
                lqw.eq(WalletTransaction::getUserId, entity.getUserId());
            }
            if (entity.getStatus() != null && !entity.getStatus().isEmpty()) {
                lqw.eq(WalletTransaction::getStatus, entity.getStatus());
            }
            if (entity.getTxHash() != null && !entity.getTxHash().isEmpty()) {
                lqw.eq(WalletTransaction::getTxHash, entity.getTxHash());
            }
            if (entity.getChainId() != null && !entity.getChainId().isEmpty()) {
                lqw.eq(WalletTransaction::getChainId, entity.getChainId());
            }
            if (entity.getFromAddress() != null && !entity.getFromAddress().isEmpty()) {
                lqw.eq(WalletTransaction::getFromAddress, entity.getFromAddress());
            }
        }
        return baseMapper.selectList(lqw);
    }

    @Override
    public Boolean insertByBo(WalletTransaction entity) {
        return baseMapper.insert(entity) > 0;
    }

    @Override
    public Boolean updateByBo(WalletTransaction entity) {
        return baseMapper.updateById(entity) > 0;
    }
}
