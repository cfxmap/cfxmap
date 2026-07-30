package org.dromara.web3.wallet.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 首页统计数据
 */
@Data
public class Web3DashboardStatsVo {

    private Date beginTime;

    private Date endTime;

    private Long newUserCount;

    private Long activeUserCount;

    private Long newWalletCount;

    private Long periodTransactionCount;

    private Long cumulativeUserCount;

    private Long cumulativeWalletCount;

    private Long cumulativeTransactionCount;

    private List<String> trendDates;

    private List<Long> newUserTrend;

    private List<Long> newWalletTrend;

    private List<Long> transactionTrend;

    private List<Long> cumulativeUserTrend;

    private List<Long> cumulativeWalletTrend;

    private List<Long> cumulativeTransactionTrend;
}
