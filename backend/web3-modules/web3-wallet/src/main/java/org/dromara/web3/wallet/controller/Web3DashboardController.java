package org.dromara.web3.wallet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.WalletAccount;
import org.dromara.web3.wallet.domain.WalletTransaction;
import org.dromara.web3.wallet.domain.bo.Web3DashboardStatsBo;
import org.dromara.web3.wallet.domain.vo.Web3DashboardStatsVo;
import org.dromara.web3.wallet.mapper.AppUserMapper;
import org.dromara.web3.wallet.mapper.WalletAccountMapper;
import org.dromara.web3.wallet.mapper.WalletTransactionMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端首页统计
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/web3/dashboard")
public class Web3DashboardController {

    private static final int DEFAULT_TREND_DAYS = 30;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final AppUserMapper appUserMapper;
    private final WalletAccountMapper walletAccountMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    @GetMapping("/stats")
    public R<Web3DashboardStatsVo> stats(Web3DashboardStatsBo bo) {
        Date endTime = bo.getEndTime() != null ? bo.getEndTime() : endOfDay(new Date());
        Date beginTime = bo.getBeginTime() != null ? bo.getBeginTime() : defaultBeginTime(endTime);
        if (beginTime.after(endTime)) {
            return R.fail("开始时间不能晚于结束时间");
        }
        Web3DashboardStatsVo vo = new Web3DashboardStatsVo();
        vo.setBeginTime(beginTime);
        vo.setEndTime(endTime);
        vo.setNewUserCount(countNewUsers(beginTime, endTime));
        vo.setActiveUserCount(countActiveUsers(beginTime, endTime));
        vo.setNewWalletCount(countNewWallets(beginTime, endTime));
        vo.setPeriodTransactionCount(countTransactions(beginTime, endTime));
        vo.setCumulativeUserCount(countCumulativeUsers(endTime));
        vo.setCumulativeWalletCount(countCumulativeWallets(endTime));
        vo.setCumulativeTransactionCount(countCumulativeTransactions(endTime));
        fillTrendSeries(vo, beginTime, endTime);
        return R.ok(vo);
    }

    private Long countNewUsers(Date beginTime, Date endTime) {
        LambdaQueryWrapper<AppUser> lqw = Wrappers.lambdaQuery();
        lqw.ne(AppUser::getDelFlag, "2");
        applyRange(lqw, AppUser::getCreateTime, beginTime, endTime);
        return appUserMapper.selectCount(lqw);
    }

    private Long countActiveUsers(Date beginTime, Date endTime) {
        LambdaQueryWrapper<AppUser> lqw = Wrappers.lambdaQuery();
        lqw.ne(AppUser::getDelFlag, "2")
            .isNotNull(AppUser::getLoginDate);
        applyRange(lqw, AppUser::getLoginDate, beginTime, endTime);
        return appUserMapper.selectCount(lqw);
    }

    private Long countNewWallets(Date beginTime, Date endTime) {
        LambdaQueryWrapper<WalletAccount> lqw = Wrappers.lambdaQuery();
        applyRange(lqw, WalletAccount::getCreateTime, beginTime, endTime);
        return walletAccountMapper.selectCount(lqw);
    }

    private Long countTransactions(Date beginTime, Date endTime) {
        LambdaQueryWrapper<WalletTransaction> lqw = Wrappers.lambdaQuery();
        applyRange(lqw, WalletTransaction::getCreateTime, beginTime, endTime);
        return walletTransactionMapper.selectCount(lqw);
    }

    private Long countCumulativeUsers(Date endTime) {
        LambdaQueryWrapper<AppUser> lqw = Wrappers.lambdaQuery();
        lqw.ne(AppUser::getDelFlag, "2")
            .le(endTime != null, AppUser::getCreateTime, endTime);
        return appUserMapper.selectCount(lqw);
    }

    private Long countCumulativeWallets(Date endTime) {
        LambdaQueryWrapper<WalletAccount> lqw = Wrappers.lambdaQuery();
        lqw.le(endTime != null, WalletAccount::getCreateTime, endTime);
        return walletAccountMapper.selectCount(lqw);
    }

    private Long countCumulativeTransactions(Date endTime) {
        LambdaQueryWrapper<WalletTransaction> lqw = Wrappers.lambdaQuery();
        lqw.le(endTime != null, WalletTransaction::getCreateTime, endTime);
        return walletTransactionMapper.selectCount(lqw);
    }

    private void fillTrendSeries(Web3DashboardStatsVo vo, Date beginTime, Date endTime) {
        List<LocalDate> dateAxis = buildDateAxis(beginTime, endTime);
        Date dayStart = atStartOfDay(beginTime);
        Date dayEnd = endOfDay(endTime);

        Map<LocalDate, Long> newUserMap = buildUserDailyCountMap(dayStart, dayEnd);
        Map<LocalDate, Long> newWalletMap = buildWalletDailyCountMap(dayStart, dayEnd);
        Map<LocalDate, Long> transactionMap = buildTransactionDailyCountMap(dayStart, dayEnd);

        long cumulativeUsers = countUsersBefore(dayStart);
        long cumulativeWallets = countWalletsBefore(dayStart);
        long cumulativeTransactions = countTransactionsBefore(dayStart);

        List<String> trendDates = new ArrayList<>(dateAxis.size());
        List<Long> newUserTrend = new ArrayList<>(dateAxis.size());
        List<Long> newWalletTrend = new ArrayList<>(dateAxis.size());
        List<Long> transactionTrend = new ArrayList<>(dateAxis.size());
        List<Long> cumulativeUserTrend = new ArrayList<>(dateAxis.size());
        List<Long> cumulativeWalletTrend = new ArrayList<>(dateAxis.size());
        List<Long> cumulativeTransactionTrend = new ArrayList<>(dateAxis.size());

        for (LocalDate date : dateAxis) {
            long userCount = newUserMap.getOrDefault(date, 0L);
            long walletCount = newWalletMap.getOrDefault(date, 0L);
            long transactionCount = transactionMap.getOrDefault(date, 0L);

            cumulativeUsers += userCount;
            cumulativeWallets += walletCount;
            cumulativeTransactions += transactionCount;

            trendDates.add(date.toString());
            newUserTrend.add(userCount);
            newWalletTrend.add(walletCount);
            transactionTrend.add(transactionCount);
            cumulativeUserTrend.add(cumulativeUsers);
            cumulativeWalletTrend.add(cumulativeWallets);
            cumulativeTransactionTrend.add(cumulativeTransactions);
        }

        vo.setTrendDates(trendDates);
        vo.setNewUserTrend(newUserTrend);
        vo.setNewWalletTrend(newWalletTrend);
        vo.setTransactionTrend(transactionTrend);
        vo.setCumulativeUserTrend(cumulativeUserTrend);
        vo.setCumulativeWalletTrend(cumulativeWalletTrend);
        vo.setCumulativeTransactionTrend(cumulativeTransactionTrend);
    }

    private Map<LocalDate, Long> buildUserDailyCountMap(Date beginTime, Date endTime) {
        LambdaQueryWrapper<AppUser> lqw = Wrappers.lambdaQuery();
        lqw.select(AppUser::getCreateTime)
            .ne(AppUser::getDelFlag, "2");
        applyRange(lqw, AppUser::getCreateTime, beginTime, endTime);
        return groupByDate(appUserMapper.selectList(lqw), AppUser::getCreateTime);
    }

    private Map<LocalDate, Long> buildWalletDailyCountMap(Date beginTime, Date endTime) {
        LambdaQueryWrapper<WalletAccount> lqw = Wrappers.lambdaQuery();
        lqw.select(WalletAccount::getCreateTime);
        applyRange(lqw, WalletAccount::getCreateTime, beginTime, endTime);
        return groupByDate(walletAccountMapper.selectList(lqw), WalletAccount::getCreateTime);
    }

    private Map<LocalDate, Long> buildTransactionDailyCountMap(Date beginTime, Date endTime) {
        LambdaQueryWrapper<WalletTransaction> lqw = Wrappers.lambdaQuery();
        lqw.select(WalletTransaction::getCreateTime);
        applyRange(lqw, WalletTransaction::getCreateTime, beginTime, endTime);
        return groupByDate(walletTransactionMapper.selectList(lqw), WalletTransaction::getCreateTime);
    }

    private Long countUsersBefore(Date boundary) {
        LambdaQueryWrapper<AppUser> lqw = Wrappers.lambdaQuery();
        lqw.ne(AppUser::getDelFlag, "2")
            .lt(AppUser::getCreateTime, boundary);
        return appUserMapper.selectCount(lqw);
    }

    private Long countWalletsBefore(Date boundary) {
        LambdaQueryWrapper<WalletAccount> lqw = Wrappers.lambdaQuery();
        lqw.lt(WalletAccount::getCreateTime, boundary);
        return walletAccountMapper.selectCount(lqw);
    }

    private Long countTransactionsBefore(Date boundary) {
        LambdaQueryWrapper<WalletTransaction> lqw = Wrappers.lambdaQuery();
        lqw.lt(WalletTransaction::getCreateTime, boundary);
        return walletTransactionMapper.selectCount(lqw);
    }

    private <T> Map<LocalDate, Long> groupByDate(List<T> records, SFunction<T, Date> getter) {
        Map<LocalDate, Long> result = new HashMap<>();
        for (T record : records) {
            Date date = getter.apply(record);
            if (date == null) {
                continue;
            }
            LocalDate localDate = date.toInstant().atZone(ZONE_ID).toLocalDate();
            result.merge(localDate, 1L, Long::sum);
        }
        return result;
    }

    private List<LocalDate> buildDateAxis(Date beginTime, Date endTime) {
        LocalDate beginDate = beginTime.toInstant().atZone(ZONE_ID).toLocalDate();
        LocalDate endDate = endTime.toInstant().atZone(ZONE_ID).toLocalDate();
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = beginDate;
        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private Date defaultBeginTime(Date endTime) {
        LocalDate endDate = endTime.toInstant().atZone(ZONE_ID).toLocalDate();
        return Date.from(endDate.minusDays(DEFAULT_TREND_DAYS - 1L).atStartOfDay(ZONE_ID).toInstant());
    }

    private Date atStartOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZONE_ID).toLocalDate();
        return Date.from(localDate.atStartOfDay(ZONE_ID).toInstant());
    }

    private Date endOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZONE_ID).toLocalDate();
        return Date.from(localDate.plusDays(1).atStartOfDay(ZONE_ID).minusNanos(1).toInstant());
    }

    private <T> void applyRange(LambdaQueryWrapper<T> lqw, SFunction<T, ?> column, Date beginTime, Date endTime) {
        lqw.ge(beginTime != null, column, beginTime)
            .le(endTime != null, column, endTime);
    }
}
