package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

@Data
@TableName("wallet_token_transfer")
public class WalletTokenTransfer extends BaseEntity {

    @TableId(value = "transfer_id", type = IdType.AUTO)
    private Long transferId;
    private Long userId;
    private Long accountId;
    private String chainId;
    private String txHash;
    private String contractAddress;
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private String amountRaw;
    private String tokenSymbol;
    private Integer tokenDecimals;
    private String status;
    private BigDecimal gasFee;
    private Long blockNumber;
    private Integer logIndex;
    private String remark;
}
