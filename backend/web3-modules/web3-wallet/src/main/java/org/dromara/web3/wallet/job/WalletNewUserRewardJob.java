package org.dromara.web3.wallet.job;

import conflux.web3j.Account;
import conflux.web3j.Cfx;
import conflux.web3j.types.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.web3.wallet.config.WalletNewUserRewardProperties;
import org.dromara.web3.wallet.domain.WalletNewUserReward;
import org.dromara.web3.wallet.domain.WalletTransaction;
import org.dromara.web3.wallet.service.IWalletNewUserRewardService;
import org.dromara.web3.wallet.service.IWalletTransactionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 新用户 CFX 奖励发放任务，使用 Spring 自带调度，不依赖三方任务平台。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletNewUserRewardJob {

    private static final BigDecimal CFX_TO_DRIP = new BigDecimal("1000000000000000000");

    private final WalletNewUserRewardProperties properties;
    private final IWalletNewUserRewardService rewardService;
    private final IWalletTransactionService txService;

    @Scheduled(cron = "${web3.app.new-user-reward.cron:0 */1 * * * ?}")
    public void dispatchNewUserRewards() {
        if (!properties.isEnabled()) {
            return;
        }
        if (isPlaceholderConfig()) {
            log.warn("新用户奖励任务已跳过：请先在 yml 中配置发奖钱包地址与私钥");
            return;
        }

        List<WalletNewUserReward> rewards = rewardService.listDispatchCandidates(
            properties.getBatchSize(),
            properties.getMaxRetryCount()
        );
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        Cfx cfx = Cfx.create(properties.getRpcUrl());
        Account account = Account.create(cfx, properties.getPrivateKey());

        for (WalletNewUserReward reward : rewards) {
            if (!rewardService.claimForSending(reward.getRewardId())) {
                continue;
            }
            try {
                BigDecimal amount = randomRewardAmount();
                BigInteger amountInDrip = amount.multiply(CFX_TO_DRIP).toBigInteger();

                Account.Option option = new Account.Option();
                option.withChainId(properties.getChainId());

                String txHash = account.transfer(option, new Address(reward.getReceiveAddress()), amountInDrip);

                WalletTransaction txRecord = new WalletTransaction();
                txRecord.setUserId(reward.getUserId());
                txRecord.setChainId(String.valueOf(properties.getChainId()));
                txRecord.setTxHash(txHash);
                txRecord.setFromAddress(properties.getFromAddress());
                txRecord.setToAddress(reward.getReceiveAddress());
                txRecord.setAmount(amount);
                txRecord.setTokenSymbol("CFX");
                txRecord.setStatus("pending");
                txService.insertByBo(txRecord);

                rewardService.markSendSuccess(reward.getRewardId(), amount, txHash);
                log.info("新用户奖励发放成功, rewardId={}, userId={}, txHash={}", reward.getRewardId(), reward.getUserId(), txHash);
            } catch (Exception e) {
                rewardService.markSendFailed(reward.getRewardId(), e.getMessage());
                log.error("新用户奖励发放失败, rewardId={}, userId={}", reward.getRewardId(), reward.getUserId(), e);
            }
        }
    }

    private BigDecimal randomRewardAmount() {
        BigDecimal min = properties.getActualMinAmount();
        BigDecimal max = properties.getActualMaxAmount();
        if (min == null || max == null || min.compareTo(max) >= 0) {
            return new BigDecimal("0.1");
        }
        double randomValue = ThreadLocalRandom.current().nextDouble(min.doubleValue(), max.doubleValue());
        return BigDecimal.valueOf(randomValue).setScale(4, RoundingMode.DOWN);
    }

    private boolean isPlaceholderConfig() {
        return properties.getFromAddress() == null
            || properties.getPrivateKey() == null
            || properties.getFromAddress().contains("replace-with")
            || properties.getPrivateKey().contains("replace-with");
    }
}
