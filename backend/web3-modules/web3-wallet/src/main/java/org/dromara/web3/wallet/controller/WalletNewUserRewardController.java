package org.dromara.web3.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.web3.wallet.domain.WalletNewUserReward;
import org.dromara.web3.wallet.service.IWalletNewUserRewardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 新用户奖励记录管理
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/web3/newUserReward")
public class WalletNewUserRewardController {

    private final IWalletNewUserRewardService walletNewUserRewardService;

    /**
     * 查询新用户奖励记录列表
     */
    @SaCheckPermission("web3:newUserReward:list")
    @GetMapping("/list")
    public TableDataInfo<WalletNewUserReward> list(WalletNewUserReward entity, PageQuery pageQuery) {
        return walletNewUserRewardService.queryPageList(entity, pageQuery);
    }
}
