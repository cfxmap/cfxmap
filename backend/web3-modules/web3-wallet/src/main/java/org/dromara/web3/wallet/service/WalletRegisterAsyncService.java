package org.dromara.web3.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.mail.service.AsyncVerifyMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletRegisterAsyncService {

    private final IWalletNewUserRewardService newUserRewardService;
    private final ObjectProvider<AsyncVerifyMailSender> asyncVerifyMailSender;

    @Async
    public void handlePostRegister(Long userId, String email, String walletAddress) {
        long asyncStart = System.currentTimeMillis();
        log.info("App register async task start. userId={}, email={}, walletAddress={}", userId, email, walletAddress);

        long rewardStart = System.currentTimeMillis();
        try {
            newUserRewardService.initRewardForNewUser(userId, walletAddress);
            log.info(
                "App register async reward init finished. userId={}, cost={}ms",
                userId,
                System.currentTimeMillis() - rewardStart
            );
        } catch (Exception e) {
            log.error(
                "App register async reward init failed. userId={}, cost={}ms",
                userId,
                System.currentTimeMillis() - rewardStart,
                e
            );
        }

        long mailStart = System.currentTimeMillis();
        try {
            AsyncVerifyMailSender sender = asyncVerifyMailSender.getIfAvailable();
            if (sender != null) {
                sender.sendCfxmapRegisterSuccess(email);
                log.info(
                    "App register async mail delegated. userId={}, email={}, cost={}ms",
                    userId,
                    email,
                    System.currentTimeMillis() - mailStart
                );
            } else {
                log.warn(
                    "App register async mail sender unavailable. userId={}, email={}, cost={}ms",
                    userId,
                    email,
                    System.currentTimeMillis() - mailStart
                );
            }
        } catch (Exception e) {
            log.error(
                "App register async mail dispatch failed. userId={}, email={}, cost={}ms",
                userId,
                email,
                System.currentTimeMillis() - mailStart,
                e
            );
        }

        log.info(
            "App register async task finished. userId={}, email={}, totalCost={}ms",
            userId,
            email,
            System.currentTimeMillis() - asyncStart
        );
    }
}
