package org.dromara.web3.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.web3.wallet.config.WalletNewUserRewardProperties;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.WalletNewUserReward;
import org.dromara.web3.wallet.mapper.AppUserMapper;
import org.dromara.web3.wallet.mapper.WalletNewUserRewardMapper;
import org.dromara.web3.wallet.service.IWalletNewUserRewardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletNewUserRewardServiceImpl implements IWalletNewUserRewardService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENDING = "SENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final WalletNewUserRewardMapper baseMapper;
    private final AppUserMapper appUserMapper;
    private final WalletNewUserRewardProperties properties;

    @Override
    public void initRewardForNewUser(Long userId, String receiveAddress) {
        if (userId == null || receiveAddress == null || receiveAddress.isBlank()) {
            return;
        }
        Long exists = baseMapper.selectCount(
            Wrappers.<WalletNewUserReward>lambdaQuery()
                .eq(WalletNewUserReward::getUserId, userId)
        );
        if (exists != null && exists > 0) {
            return;
        }
        Integer totalLimit = properties.getTotalLimit();
        if (totalLimit != null && totalLimit > 0) {
            Long currentRewardCount = baseMapper.selectCount(Wrappers.<WalletNewUserReward>lambdaQuery());
            if (currentRewardCount != null && currentRewardCount >= totalLimit) {
                return;
            }
        }

        Long userSequence = appUserMapper.selectCount(
            Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getDelFlag, "0")
                .le(AppUser::getUserId, userId)
        );

        WalletNewUserReward reward = new WalletNewUserReward();
        reward.setUserId(userId);
        reward.setUserSequence(userSequence);
        reward.setReceiveAddress(receiveAddress);
        reward.setStatus(STATUS_PENDING);
        reward.setRetryCount(0);
        reward.setPopupShown(0);
        baseMapper.insert(reward);
    }

    @Override
    public WalletNewUserReward findPendingPopupReward(Long userId) {
        return baseMapper.selectOne(
            new LambdaQueryWrapper<WalletNewUserReward>()
                .eq(WalletNewUserReward::getUserId, userId)
                .eq(WalletNewUserReward::getPopupShown, 0)
                .in(WalletNewUserReward::getStatus, STATUS_PENDING, STATUS_SENDING, STATUS_FAILED)
                .last("limit 1")
        );
    }

    @Override
    public boolean markPopupShown(Long rewardId) {
        if (rewardId == null) {
            return false;
        }
        return baseMapper.update(
            null,
            new LambdaUpdateWrapper<WalletNewUserReward>()
                .set(WalletNewUserReward::getPopupShown, 1)
                .eq(WalletNewUserReward::getRewardId, rewardId)
                .eq(WalletNewUserReward::getPopupShown, 0)
        ) > 0;
    }

    @Override
    public List<WalletNewUserReward> listDispatchCandidates(int limit, int maxRetryCount) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<WalletNewUserReward>()
                .and(wrapper -> wrapper
                    .eq(WalletNewUserReward::getStatus, STATUS_PENDING)
                    .or()
                    .eq(WalletNewUserReward::getStatus, STATUS_FAILED))
                .lt(WalletNewUserReward::getRetryCount, maxRetryCount)
                .orderByAsc(WalletNewUserReward::getRewardId)
                .last("limit " + Math.max(limit, 1))
        );
    }

    @Override
    public TableDataInfo<WalletNewUserReward> queryPageList(WalletNewUserReward entity, PageQuery pageQuery) {
        LambdaQueryWrapper<WalletNewUserReward> lqw = Wrappers.lambdaQuery();
        if (entity != null) {
            lqw.eq(entity.getRewardId() != null, WalletNewUserReward::getRewardId, entity.getRewardId());
            lqw.eq(entity.getUserId() != null, WalletNewUserReward::getUserId, entity.getUserId());
            lqw.eq(entity.getUserSequence() != null, WalletNewUserReward::getUserSequence, entity.getUserSequence());
            lqw.eq(StringUtils.isNotBlank(entity.getStatus()), WalletNewUserReward::getStatus, entity.getStatus());
            lqw.like(StringUtils.isNotBlank(entity.getReceiveAddress()), WalletNewUserReward::getReceiveAddress, entity.getReceiveAddress());
            lqw.like(StringUtils.isNotBlank(entity.getTxHash()), WalletNewUserReward::getTxHash, entity.getTxHash());
        }
        lqw.orderByDesc(WalletNewUserReward::getCreateTime);
        Page<WalletNewUserReward> page = baseMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public boolean claimForSending(Long rewardId) {
        if (rewardId == null) {
            return false;
        }
        return baseMapper.update(
            null,
            new LambdaUpdateWrapper<WalletNewUserReward>()
                .set(WalletNewUserReward::getStatus, STATUS_SENDING)
                .set(WalletNewUserReward::getLastError, null)
                .eq(WalletNewUserReward::getRewardId, rewardId)
                .in(WalletNewUserReward::getStatus, STATUS_PENDING, STATUS_FAILED)
        ) > 0;
    }

    @Override
    public void markSendSuccess(Long rewardId, BigDecimal rewardAmount, String txHash) {
        if (rewardId == null) {
            return;
        }
        baseMapper.update(
            null,
            new LambdaUpdateWrapper<WalletNewUserReward>()
                .set(WalletNewUserReward::getStatus, STATUS_SUCCESS)
                .set(WalletNewUserReward::getRewardAmount, rewardAmount)
                .set(WalletNewUserReward::getTxHash, txHash)
                .set(WalletNewUserReward::getLastError, null)
                .set(WalletNewUserReward::getSendTime, new Date())
                .eq(WalletNewUserReward::getRewardId, rewardId)
        );
    }

    @Override
    public void markSendFailed(Long rewardId, String lastError) {
        if (rewardId == null) {
            return;
        }
        WalletNewUserReward reward = baseMapper.selectById(rewardId);
        if (reward == null) {
            return;
        }
        int nextRetry = reward.getRetryCount() == null ? 1 : reward.getRetryCount() + 1;
        baseMapper.update(
            null,
            new LambdaUpdateWrapper<WalletNewUserReward>()
                .set(WalletNewUserReward::getStatus, STATUS_FAILED)
                .set(WalletNewUserReward::getRetryCount, nextRetry)
                .set(WalletNewUserReward::getLastError, truncateError(lastError))
                .eq(WalletNewUserReward::getRewardId, rewardId)
        );
    }

    private String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 500 ? error : error.substring(0, 500);
    }
}
