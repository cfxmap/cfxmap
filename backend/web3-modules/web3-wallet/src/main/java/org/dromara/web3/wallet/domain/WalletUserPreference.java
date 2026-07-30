package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@TableName("wallet_user_preference")
public class WalletUserPreference extends BaseEntity {
    @TableId
    private Long userId;
    private String fiatCurrency;
    private String language;
    private String theme;
}
