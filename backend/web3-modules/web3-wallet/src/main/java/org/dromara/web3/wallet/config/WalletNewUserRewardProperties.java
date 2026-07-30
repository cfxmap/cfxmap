package org.dromara.web3.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "web3.app.new-user-reward")
public class WalletNewUserRewardProperties {

    private boolean enabled = true;

    private String cron = "0 */1 * * * ?";

    private String rpcUrl = "https://main.confluxrpc.com";

    private Integer chainId = 1029;

    private String fromAddress = "cfx:replace-with-your-reward-wallet-address";

    private String privateKey = "replace-with-your-reward-wallet-private-key";

    private BigDecimal displayMinAmount = new BigDecimal("0.1");

    private BigDecimal displayMaxAmount = new BigDecimal("888");

    private BigDecimal actualMinAmount = new BigDecimal("0.1");

    private BigDecimal actualMaxAmount = new BigDecimal("2");

    private Integer batchSize = 20;

    private Integer maxRetryCount = 10;

    /**
     * 总共最多发放多少次
     */
    private Integer totalLimit = 500;
}
