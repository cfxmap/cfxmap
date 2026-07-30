package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 新用户 CFX 奖励记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wallet_new_user_reward")
public class WalletNewUserReward extends BaseEntity {

    @TableId
    private Long rewardId;

    private Long userId;

    /**
     * 第几位注册用户
     */
    private Long userSequence;

    /**
     * 奖励接收地址
     */
    private String receiveAddress;

    /**
     * 实际发放金额
     */
    private BigDecimal rewardAmount;

    /**
     * 任务状态：PENDING/SENDING/SUCCESS/FAILED
     */
    private String status;

    private String txHash;

    private Integer retryCount;

    private String lastError;

    /**
     * 登录弹框是否已展示
     */
    private Integer popupShown;

    private Date sendTime;
}
