package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import java.math.BigDecimal;

@Data
@TableName("wallet_transaction")
public class WalletTransaction extends BaseEntity {
    @TableId
    private Long txId;
    private Long userId;
    private String chainId;
    private String txHash;
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private String tokenSymbol;
    private String status;
    private BigDecimal gasFee;
}
