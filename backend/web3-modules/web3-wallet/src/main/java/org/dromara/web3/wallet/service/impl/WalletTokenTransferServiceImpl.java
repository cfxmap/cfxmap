package org.dromara.web3.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.web3.wallet.domain.WalletTokenTransfer;
import org.dromara.web3.wallet.mapper.WalletTokenTransferMapper;
import org.dromara.web3.wallet.service.IWalletTokenTransferService;

@RequiredArgsConstructor
@Service
public class WalletTokenTransferServiceImpl implements IWalletTokenTransferService {

    private final WalletTokenTransferMapper baseMapper;

    @Override
    public boolean insert(WalletTokenTransfer entity) {
        return baseMapper.insert(entity) > 0;
    }
}
