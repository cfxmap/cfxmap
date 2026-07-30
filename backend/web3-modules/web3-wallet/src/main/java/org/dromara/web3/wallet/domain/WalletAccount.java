package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@TableName("wallet_account")
public class WalletAccount extends BaseEntity {
    @TableId
    private Long accountId;
    private Long userId;
    private String name;
    private String address;
    private String chainId;
    private String privateKey;
    private String mnemonic;
    private Integer hasPrivateKey;
    private Integer hasMnemonic;
    private Integer isNewUserCreated;
}
