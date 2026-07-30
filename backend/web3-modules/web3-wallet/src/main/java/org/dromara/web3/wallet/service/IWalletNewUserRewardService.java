package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.WalletNewUserReward;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.math.BigDecimal;
import java.util.List;

public interface IWalletNewUserRewardService {

    void initRewardForNewUser(Long userId, String receiveAddress);

    WalletNewUserReward findPendingPopupReward(Long userId);

    boolean markPopupShown(Long rewardId);

    List<WalletNewUserReward> listDispatchCandidates(int limit, int maxRetryCount);

    TableDataInfo<WalletNewUserReward> queryPageList(WalletNewUserReward entity, PageQuery pageQuery);

    boolean claimForSending(Long rewardId);

    void markSendSuccess(Long rewardId, BigDecimal rewardAmount, String txHash);

    void markSendFailed(Long rewardId, String lastError);
}
