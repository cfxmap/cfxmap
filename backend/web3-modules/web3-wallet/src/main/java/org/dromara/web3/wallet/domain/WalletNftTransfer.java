package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

@Data
@TableName("wallet_nft_transfer")
public class WalletNftTransfer extends BaseEntity {

    @TableId(value = "nft_transfer_id", type = IdType.AUTO)
    private Long nftTransferId;
    private Long userId;
    private Long accountId;
    private String chainId;
    private String txHash;
    private String contractAddress;
    private String tokenId;
    private String nftStandard;
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private String tokenName;
    private String tokenSymbol;
    private String status;
    private BigDecimal gasFee;
    private Long blockNumber;
    private Integer logIndex;
    private String remark;
}
