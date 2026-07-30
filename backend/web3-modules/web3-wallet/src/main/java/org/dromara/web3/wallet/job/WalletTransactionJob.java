package org.dromara.web3.wallet.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import conflux.web3j.Cfx;
import conflux.web3j.response.Receipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.web3.wallet.domain.WalletTransaction;
import org.dromara.web3.wallet.service.IWalletTransactionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * 钱包交易状态刷新任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletTransactionJob {

    private final IWalletTransactionService txService;

    @JobExecutor(name = "refreshWalletTxJob")
    public ExecuteResult refreshWalletTxJob(JobArgs jobArgs) {
        log.info("开始执行刷新钱包交易状态和Gas费任务");

        WalletTransaction query = new WalletTransaction();
        query.setStatus("pending");
        List<WalletTransaction> pendingTxs = txService.queryList(query);

        if (pendingTxs == null || pendingTxs.isEmpty()) {
            log.info("没有需要刷新的 pending 交易");
            return ExecuteResult.success("没有需要刷新的 pending 交易");
        }

        int successCount = 0;
        int failCount = 0;

        for (WalletTransaction tx : pendingTxs) {
            try {
                // 判断网络类型，默认假设非1029为testnet（根据项目实际情况调整）
                boolean isTestnet = !"1029".equals(tx.getChainId());
                String rpcUrl = isTestnet ? "https://test.confluxrpc.com" : "https://main.confluxrpc.com";
                Cfx cfx = Cfx.create(rpcUrl);

                Optional<Receipt> receiptOpt = cfx.getTransactionReceipt(tx.getTxHash()).sendAndGet();
                if (receiptOpt.isPresent()) {
                    Receipt receipt = receiptOpt.get();

                    Short outcomeStatus = receipt.getOutcomeStatus();
                    if (outcomeStatus != null) {
                        if (outcomeStatus == 0) {
                            tx.setStatus("SUCCESS");
                        } else {
                            tx.setStatus("FAILED");
                        }
                    } else {
                        // 如果无法获取outcomeStatus，但有了receipt，也暂时标记为SUCCESS
                        tx.setStatus("SUCCESS");
                    }

                    if (receipt.getGasFee() != null) {
                        // Conflux gas fee 单位是 drip，1 CFX = 10^18 drip
                        BigDecimal gasFeeCfx = new BigDecimal(receipt.getGasFee())
                                .divide(new BigDecimal("1000000000000000000"), 18, RoundingMode.HALF_UP);
                        tx.setGasFee(gasFeeCfx);
                    }

                    txService.updateByBo(tx);
                    successCount++;
                    log.info("交易 {} 状态刷新成功，状态: {}, gasFee: {}", tx.getTxHash(), tx.getStatus(), tx.getGasFee());
                } else {
                    log.info("交易 {} 仍在 pending，尚未打包", tx.getTxHash());
                }
            } catch (Exception e) {
                log.error("刷新交易 {} 状态失败", tx.getTxHash(), e);
                failCount++;
            }
        }

        String msg = String.format("刷新交易状态完成，成功刷新 %d 条，失败 %d 条", successCount, failCount);
        log.info(msg);
        return ExecuteResult.success(msg);
    }
}
