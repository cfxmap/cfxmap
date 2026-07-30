package org.dromara.web3.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.dromara.web3.wallet.domain.WalletNftTransfer;
import org.dromara.web3.wallet.mapper.WalletNftTransferMapper;
import org.dromara.web3.wallet.service.IWalletNftTransferService;

@RequiredArgsConstructor
@Service
public class WalletNftTransferServiceImpl implements IWalletNftTransferService {

    private final WalletNftTransferMapper baseMapper;

    @Override
    public boolean insert(WalletNftTransfer entity) {
        return baseMapper.insert(entity) > 0;
    }
}
