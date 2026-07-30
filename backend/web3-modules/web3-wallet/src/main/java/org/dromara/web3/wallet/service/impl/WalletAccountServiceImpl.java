package org.dromara.web3.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.web3.wallet.crypto.WalletSecretCrypto;
import org.dromara.web3.wallet.domain.WalletAccount;
import org.dromara.web3.wallet.mapper.WalletAccountMapper;
import org.dromara.web3.wallet.service.IWalletAccountService;
import java.util.List;

@RequiredArgsConstructor
@Service
public class WalletAccountServiceImpl implements IWalletAccountService {

    private final WalletAccountMapper baseMapper;
    private final WalletSecretCrypto walletSecretCrypto;

    @Override
    public WalletAccount queryById(Long id) {
        WalletAccount row = baseMapper.selectById(id);
        if (row != null) {
            walletSecretCrypto.decryptIfNeeded(row);
        }
        return row;
    }

    @Override
    public List<WalletAccount> queryList(WalletAccount entity) {
        LambdaQueryWrapper<WalletAccount> lqw = Wrappers.lambdaQuery();
        if (entity != null) {
            if (entity.getUserId() != null){
                lqw.eq(WalletAccount::getUserId, entity.getUserId());
            }
            if (entity.getAccountId() != null){
                lqw.eq(WalletAccount::getAccountId, entity.getAccountId());
            }
            if (StringUtils.isNotBlank(entity.getAddress())){
                lqw.eq(WalletAccount::getAddress, entity.getAddress());
            }
            if (StringUtils.isNotBlank(entity.getChainId())){
                lqw.eq(WalletAccount::getChainId, entity.getChainId());
            }
            if (StringUtils.isNotBlank(entity.getName())){
                lqw.like(WalletAccount::getName, entity.getName());
            }
            if (entity.getIsNewUserCreated() != null){
                lqw.eq(WalletAccount::getIsNewUserCreated, entity.getIsNewUserCreated());
            }
        }
        List<WalletAccount> list = baseMapper.selectList(lqw);
        if (list != null) {
            list.forEach(walletSecretCrypto::decryptIfNeeded);
        }
        return list;
    }

    @Override
    public Boolean insertByBo(WalletAccount entity) {
        walletSecretCrypto.encryptIfNeeded(entity);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    public Boolean updateByBo(WalletAccount entity) {
        walletSecretCrypto.encryptIfNeeded(entity);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteById(Long accountId) {
        if (accountId == null) {
            return false;
        }
        return baseMapper.deleteById(accountId) > 0;
    }
}
