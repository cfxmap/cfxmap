package org.dromara.web3.wallet.controller;

import cn.hutool.json.JSONUtil;
import conflux.web3j.response.Receipt;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.web3.wallet.cache.WalletAppRedisCache;
import org.springframework.web.bind.annotation.*;
import org.dromara.web3.wallet.domain.WalletTransaction;
import org.dromara.web3.wallet.domain.WalletAccount;
import org.dromara.web3.wallet.service.IWalletTransactionService;
import org.dromara.web3.wallet.service.IWalletAccountService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import conflux.web3j.Account;
import conflux.web3j.Cfx;
import conflux.web3j.types.Address;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import java.math.BigInteger;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import java.util.ArrayList;
import java.util.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tx")
public class WalletTxController extends BaseController {

    private static final BigDecimal DRIP_PER_CFX = new BigDecimal("1000000000000000000");
    private final IWalletTransactionService txService;
    private final IWalletAccountService accountService;

    private void refreshPendingTxStatus(WalletTransaction tx) {
        if (tx == null || tx.getTxHash() == null) {
            return;
        }
        String status = String.valueOf(tx.getStatus() == null ? "" : tx.getStatus()).trim().toLowerCase();
        if (!"pending".equals(status)) {
            return;
        }
        try {
            boolean isTestnet = !"1029".equals(tx.getChainId());
            String rpcUrl = isTestnet ? "https://test.confluxrpc.com" : "https://main.confluxrpc.com";
            Cfx cfx = Cfx.create(rpcUrl);
            Optional<Receipt> receiptOpt = cfx.getTransactionReceipt(tx.getTxHash()).sendAndGet();
            if (receiptOpt.isEmpty()) {
                return;
            }
            Receipt receipt = receiptOpt.get();
            Short outcomeStatus = receipt.getOutcomeStatus();
            if (outcomeStatus != null) {
                tx.setStatus(outcomeStatus == 0 ? "SUCCESS" : "FAILED");
            } else {
                tx.setStatus("SUCCESS");
            }
            if (receipt.getGasFee() != null) {
                BigDecimal gasFeeCfx = new BigDecimal(receipt.getGasFee())
                    .divide(new BigDecimal("1000000000000000000"), 18, RoundingMode.HALF_UP);
                tx.setGasFee(gasFeeCfx);
            }
            txService.updateByBo(tx);
        } catch (Exception ignored) {
        }
    }

    private String rpcUrlByChainId(String chainId) {
        return "1".equals(String.valueOf(chainId)) ? "https://test.confluxrpc.com" : "https://main.confluxrpc.com";
    }

    private JSONObject rpcObjectCall(String rpcUrl, String method, List<Object> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", 1);
        body.put("method", method);
        body.put("params", params);

        String response = HttpUtil.createPost(rpcUrl)
            .header("Content-Type", "application/json")
            .body(JSONUtil.toJsonStr(body))
            .timeout(5000)
            .execute()
            .body();
        JSONObject json = JSONUtil.parseObj(response);
        if (json.containsKey("error") && json.get("error") != null) {
            JSONObject error = json.getJSONObject("error");
            throw new IllegalStateException(error == null ? "RPC request failed" : error.getStr("message", "RPC request failed"));
        }
        Object result = json.get("result");
        if (result == null) {
            return null;
        }
        return JSONUtil.parseObj(result);
    }

    private Long parseHexLong(String hexValue) {
        if (StringUtils.isBlank(hexValue)) {
            return null;
        }
        try {
            return Long.decode(hexValue);
        } catch (Exception e) {
            try {
                return new BigInteger(hexValue.replaceFirst("^0x", ""), 16).longValue();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private BigDecimal parseHexToCfx(String hexValue) {
        if (StringUtils.isBlank(hexValue)) {
            return null;
        }
        try {
            BigInteger drip = new BigInteger(hexValue.replaceFirst("^0x", ""), 16);
            return new BigDecimal(drip).divide(DRIP_PER_CFX, 18, RoundingMode.HALF_UP).stripTrailingZeros();
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeStatus(String existingStatus, Long txStatus, Long outcomeStatus) {
        if (outcomeStatus != null) {
            return outcomeStatus == 0L ? "SUCCESS" : "FAILED";
        }
        if (txStatus != null) {
            return txStatus == 0L ? "SUCCESS" : "FAILED";
        }
        String raw = String.valueOf(existingStatus == null ? "" : existingStatus).trim().toLowerCase();
        if ("success".equals(raw) || "completed".equals(raw)) {
            return "SUCCESS";
        }
        if ("failed".equals(raw) || "failure".equals(raw)) {
            return "FAILED";
        }
        return "PENDING";
    }

    private Map<String, Object> buildTxDetail(String txHash, String chainId, WalletTransaction baseTx) {
        Map<String, Object> detail = new HashMap<>();
        if (baseTx != null) {
            detail.put("txId", baseTx.getTxId());
            detail.put("userId", baseTx.getUserId());
            detail.put("chainId", baseTx.getChainId());
            detail.put("txHash", baseTx.getTxHash());
            detail.put("fromAddress", baseTx.getFromAddress());
            detail.put("toAddress", baseTx.getToAddress());
            detail.put("amount", baseTx.getAmount());
            detail.put("tokenSymbol", baseTx.getTokenSymbol());
            detail.put("status", baseTx.getStatus());
            detail.put("gasFee", baseTx.getGasFee());
            detail.put("createTime", baseTx.getCreateTime());
        }

        String resolvedChainId = StringUtils.blankToDefault(chainId, baseTx != null ? baseTx.getChainId() : "1029");
        detail.put("txHash", txHash);
        detail.put("chainId", resolvedChainId);

        String rpcUrl = rpcUrlByChainId(resolvedChainId);
        JSONObject txObj = rpcObjectCall(rpcUrl, "cfx_getTransactionByHash", Collections.singletonList(txHash));
        JSONObject receiptObj = rpcObjectCall(rpcUrl, "cfx_getTransactionReceipt", Collections.singletonList(txHash));

        if (txObj != null) {
            if (StringUtils.isNotBlank(txObj.getStr("from"))) {
                detail.put("fromAddress", txObj.getStr("from"));
            }
            if (StringUtils.isNotBlank(txObj.getStr("to"))) {
                detail.put("toAddress", txObj.getStr("to"));
            }
            if (!detail.containsKey("amount") || detail.get("amount") == null || "".equals(detail.get("amount"))) {
                BigDecimal value = parseHexToCfx(txObj.getStr("value"));
                if (value != null) {
                    detail.put("amount", value);
                }
            }
            if (StringUtils.isBlank((String) detail.get("tokenSymbol")) && "0x".equals(txObj.getStr("data"))) {
                detail.put("tokenSymbol", "CFX");
            }
            detail.put("blockHash", txObj.getStr("blockHash"));
            detail.put("blockHeight", parseHexLong(txObj.getStr("epochHeight")));
            detail.put("transactionIndex", parseHexLong(txObj.getStr("transactionIndex")));
            BigDecimal gasPrice = parseHexToCfx(txObj.getStr("gasPrice"));
            if (gasPrice != null) {
                detail.put("gasPrice", gasPrice);
            }
        }

        Long txStatus = txObj == null ? null : parseHexLong(txObj.getStr("status"));
        Long outcomeStatus = null;
        if (receiptObj != null) {
            outcomeStatus = parseHexLong(receiptObj.getStr("outcomeStatus"));
            detail.put("blockHash", StringUtils.blankToDefault(receiptObj.getStr("blockHash"), (String) detail.get("blockHash")));
            detail.put("blockHeight", parseHexLong(receiptObj.getStr("epochNumber")));
            detail.put("transactionIndex", parseHexLong(receiptObj.getStr("index")));
            BigDecimal gasFee = parseHexToCfx(receiptObj.getStr("gasFee"));
            if (gasFee != null) {
                detail.put("gasFee", gasFee);
            }
            BigDecimal gasUsed = parseHexToCfx(receiptObj.getStr("gasUsed"));
            if (gasUsed != null) {
                detail.put("gasUsed", gasUsed);
            }
            String errorMessage = receiptObj.getStr("txExecErrorMsg");
            if (StringUtils.isNotBlank(errorMessage)) {
                detail.put("errorMessage", errorMessage);
            }
        }

        String blockHash = (String) detail.get("blockHash");
        if (StringUtils.isNotBlank(blockHash)) {
            JSONObject blockObj = rpcObjectCall(rpcUrl, "cfx_getBlockByHash", Arrays.asList(blockHash, false));
            if (blockObj != null) {
                Long timestamp = parseHexLong(blockObj.getStr("timestamp"));
                if (timestamp != null) {
                    detail.put("blockTime", timestamp * 1000);
                    if (detail.get("createTime") == null) {
                        detail.put("createTime", new Date(timestamp * 1000));
                    }
                }
                if (detail.get("blockHeight") == null) {
                    detail.put("blockHeight", parseHexLong(blockObj.getStr("epochNumber")));
                }
            }
        }

        detail.put("status", normalizeStatus((String) detail.get("status"), txStatus, outcomeStatus));
        return detail;
    }

    private Map<String, Object> buildTxDetailFallback(String txHash, String chainId, WalletTransaction baseTx) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("txHash", txHash);
        detail.put("chainId", StringUtils.blankToDefault(chainId, baseTx != null ? baseTx.getChainId() : "1029"));
        if (baseTx != null) {
            detail.put("txId", baseTx.getTxId());
            detail.put("userId", baseTx.getUserId());
            detail.put("fromAddress", baseTx.getFromAddress());
            detail.put("toAddress", baseTx.getToAddress());
            detail.put("amount", baseTx.getAmount());
            detail.put("tokenSymbol", baseTx.getTokenSymbol());
            detail.put("status", baseTx.getStatus());
            detail.put("gasFee", baseTx.getGasFee());
            detail.put("createTime", baseTx.getCreateTime());
        } else {
            detail.put("status", "PENDING");
        }
        return detail;
    }

    private R<String> verifyTradeUnlock(Long userId, Map<String, String> body) {
        if (userId == null) {
            return R.fail("User not logged in");
        }
        String sessionToken = body.get("tradeSessionToken");
        if (StringUtils.isBlank(sessionToken)) {
            return R.fail("请先完成交易密码或指纹验证");
        }
        Boolean ok = RedisUtils.getCacheObject(WalletAppRedisCache.tradeUnlockKey(userId, sessionToken));
        if (!Boolean.TRUE.equals(ok)) {
            return R.fail("交易验证无效或已过期，请重新验证");
        }
        return null;
    }

    @GetMapping("/gas")
    public R<Void> getGas() {
        // TODO: get gas estimation
        return R.ok();
    }

    @PostMapping("/broadcast")
    public R<String> broadcast(@RequestBody java.util.Map<String, String> body) {
        String signedTx = body.get("signedTx");
        // TODO: broadcast tx, save tx record
        return R.ok("tx_hash_placeholder");
    }

    @PostMapping("/sendCfx")
    public R<String> sendCfx(@RequestBody Map<String, String> body) {
        String fromAddress = body.get("fromAddress");
        String toAddress = body.get("toAddress");
        String amountStr = body.get("amount");

        if (fromAddress == null || toAddress == null || amountStr == null) {
            return R.fail("Missing required parameters");
        }

        try {
            Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
            R<String> tradeGate = verifyTradeUnlock(userId, body);
            if (tradeGate != null) {
                return tradeGate;
            }

            // Find private key for fromAddress
            WalletAccount query = new WalletAccount();
            query.setUserId(userId);
            query.setAddress(fromAddress);
            List<WalletAccount> accounts = accountService.queryList(query);
            if (accounts == null || accounts.isEmpty()) {
                return R.fail("Wallet not found or not imported");
            }

            String privateKey = accounts.get(0).getPrivateKey();

            boolean isTestnet = fromAddress.toLowerCase().startsWith("cfxtest");
            int chainId = isTestnet ? 1 : 1029;
            Cfx cfx = Cfx.create(isTestnet ? "https://test.confluxrpc.com" : "https://main.confluxrpc.com");
            Account account = Account.create(cfx, privateKey);

            Address to = new Address(toAddress);

            java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
            java.math.BigDecimal cfx2drip = new java.math.BigDecimal("1000000000000000000");
            BigInteger cfxInt = amount.multiply(cfx2drip).toBigInteger();

            Account.Option option = new Account.Option();
            option.withChainId(chainId);

            String txHash = account.transfer(option, to, cfxInt);

            // Save transaction record
            WalletTransaction txRecord = new WalletTransaction();
            txRecord.setUserId(accounts.get(0).getUserId());
            txRecord.setChainId(String.valueOf(chainId));
            txRecord.setTxHash(txHash);
            txRecord.setFromAddress(fromAddress);
            txRecord.setToAddress(toAddress);
            txRecord.setAmount(amount);
            txRecord.setTokenSymbol("CFX");
            txRecord.setStatus("pending");
            txService.insertByBo(txRecord);

            return R.ok(txHash);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            String friendlyMsg = "发送CFX失败：" + errorMsg;
            if (errorMsg != null && errorMsg.contains("transaction is reverted")) {
                friendlyMsg = "链上交易被回滚：账户余额可能不足以支付转账金额及 Gas 费用。";
            }
            return R.fail(friendlyMsg);
        }
    }

    @PostMapping("/sendNft")
    public R<String> sendNft(@RequestBody Map<String, String> body) {
        String fromAddress = body.get("fromAddress");
        String toAddress = body.get("toAddress");
        String contract = body.get("contract");
        String tokenIdStr = body.get("tokenId");
        String standard = body.get("standard");

        if (fromAddress == null || toAddress == null || contract == null || tokenIdStr == null) {
            return R.fail("Missing required parameters");
        }

        try {
            Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
            R<String> tradeGate = verifyTradeUnlock(userId, body);
            if (tradeGate != null) {
                return tradeGate;
            }

            // Find private key for fromAddress
            WalletAccount query = new WalletAccount();
            query.setUserId(userId);
            query.setAddress(fromAddress);
            List<WalletAccount> accounts = accountService.queryList(query);
            if (accounts == null || accounts.isEmpty()) {
                return R.fail("Wallet not found or not imported");
            }

            String privateKey = accounts.get(0).getPrivateKey();

            boolean isTestnet = fromAddress.toLowerCase().startsWith("cfxtest");
            int chainId = isTestnet ? 1 : 1029;
            Cfx cfx = Cfx.create(isTestnet ? "https://test.confluxrpc.com" : "https://main.confluxrpc.com");
            Account account = Account.create(cfx, privateKey);

//            Address to = new Address(toAddress);
//            Address from = new Address(fromAddress);
            org.web3j.abi.datatypes.Address to = new org.web3j.abi.datatypes.Address(conflux.web3j.types.Address.decode(toAddress));
            org.web3j.abi.datatypes.Address from = new org.web3j.abi.datatypes.Address(conflux.web3j.types.Address.decode(fromAddress));
            conflux.web3j.types.CfxAddress contractAddr = new conflux.web3j.types.CfxAddress(contract);

            Account.Option option = new Account.Option();
            option.withChainId(chainId);

            String txHash;
            if (standard != null && (standard.contains("1155") || standard.equalsIgnoreCase("CRC1155") || standard.equalsIgnoreCase("ERC1155"))) {
                txHash = account.call(option, contractAddr, "safeTransferFrom", from, to, new Uint256(new BigInteger(tokenIdStr)), new Uint256(BigInteger.ONE), new org.web3j.abi.datatypes.DynamicBytes(new byte[0]));
            } else {
                txHash = account.call(option, contractAddr, "transferFrom", from, to, new Uint256(new BigInteger(tokenIdStr)));
            }

            // Save transaction record
            WalletTransaction txRecord = new WalletTransaction();
            txRecord.setUserId(accounts.get(0).getUserId());
            txRecord.setChainId(String.valueOf(chainId));
            txRecord.setTxHash(txHash);
            txRecord.setFromAddress(fromAddress);
            txRecord.setToAddress(toAddress);
            txRecord.setAmount(java.math.BigDecimal.ZERO);
            txRecord.setTokenSymbol("NFT");
            txRecord.setStatus("pending");
            txService.insertByBo(txRecord);

            return R.ok(txHash);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            String friendlyMsg = "转移NFT失败：" + errorMsg;
            if (errorMsg != null) {
                if (errorMsg.contains("transfer caller is not owner nor approved")) {
                    friendlyMsg = "链上交易被回滚：当前账户不是该 NFT 的拥有者，或未获得授权转移！";
                } else if (errorMsg.contains("transfer of token that is not own")) {
                    friendlyMsg = "链上交易被回滚：即将转移的 TokenId 不属于发送方账户！";
                } else if (errorMsg.contains("transaction is reverted")) {
                    friendlyMsg = "链上交易被回滚：账户余额可能不足以支付 Gas 费用，或合约内部错误。";
                }
            }
            return R.fail(friendlyMsg);
        }
    }

    @PostMapping("/sendToken")
    public R<String> sendToken(@RequestBody Map<String, String> body) {
        String fromAddress = body.get("fromAddress");
        String toAddress = body.get("toAddress");
        String amountStr = body.get("amount");
        String contract = body.get("contract");
        String decimalsStr = body.get("decimals");

        if (fromAddress == null || toAddress == null || amountStr == null || contract == null || decimalsStr == null) {
            return R.fail("Missing required parameters");
        }

        try {
            Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
            R<String> tradeGate = verifyTradeUnlock(userId, body);
            if (tradeGate != null) {
                return tradeGate;
            }

            WalletAccount query = new WalletAccount();
            query.setUserId(userId);
            query.setAddress(fromAddress);
            List<WalletAccount> accounts = accountService.queryList(query);
            if (accounts == null || accounts.isEmpty()) {
                return R.fail("Wallet not found or not imported");
            }

            String privateKey = accounts.get(0).getPrivateKey();

            boolean isTestnet = fromAddress.toLowerCase().startsWith("cfxtest");
            int chainId = isTestnet ? 1 : 1029;
            Cfx cfx = Cfx.create(isTestnet ? "https://test.confluxrpc.com" : "https://main.confluxrpc.com");
            Account account = Account.create(cfx, privateKey);

            java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
            int decimals = Integer.parseInt(decimalsStr);
            BigInteger wei = amount.multiply(new java.math.BigDecimal(BigInteger.TEN.pow(decimals))).toBigInteger();

            org.web3j.abi.datatypes.Address to = new org.web3j.abi.datatypes.Address(conflux.web3j.types.Address.decode(toAddress));
            conflux.web3j.types.CfxAddress contractAddr = new conflux.web3j.types.CfxAddress(contract);

            Account.Option option = new Account.Option();
            option.withChainId(chainId);

            String txHash = account.call(option, contractAddr, "transfer", to, new Uint256(wei));

            WalletTransaction txRecord = new WalletTransaction();
            txRecord.setUserId(accounts.get(0).getUserId());
            txRecord.setChainId(String.valueOf(chainId));
            txRecord.setTxHash(txHash);
            txRecord.setFromAddress(fromAddress);
            txRecord.setToAddress(toAddress);
            txRecord.setAmount(amount);
            String sym = body.get("tokenSymbol");
            txRecord.setTokenSymbol(sym != null && !sym.isEmpty() ? sym : "CRC20");
            txRecord.setStatus("pending");
            txService.insertByBo(txRecord);

            return R.ok(txHash);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            String friendlyMsg = "发送代币失败：" + errorMsg;
            if (errorMsg != null && errorMsg.contains("transaction is reverted")) {
                friendlyMsg = "链上交易被回滚：账户余额可能不足以支付转账及 Gas。";
            }
            return R.fail(friendlyMsg);
        }
    }

    @GetMapping("/detail")
    public R<Map<String, Object>> getDetail(@RequestParam String txHash, @RequestParam(required = false) String chainId) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        if (StringUtils.isBlank(txHash)) {
            return R.fail("txHash不能为空");
        }

        WalletTransaction query = new WalletTransaction();
        query.setUserId(userId);
        query.setTxHash(txHash);
        List<WalletTransaction> list = txService.queryList(query);
        WalletTransaction tx = (list == null || list.isEmpty()) ? null : list.get(0);
        if (tx != null) {
            refreshPendingTxStatus(tx);
        }
        try {
            return R.ok(buildTxDetail(txHash, chainId, tx));
        } catch (Exception e) {
            if (tx != null) {
                return R.ok(buildTxDetailFallback(txHash, chainId, tx));
            }
            return R.fail("交易详情获取失败");
        }
    }

    @GetMapping("/history")
    public R<List<WalletTransaction>> getHistory(@RequestParam(required = false) String address, @RequestParam(required = false) String chainId) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }

        if (address == null || address.isEmpty()) {
            // Fallback to local DB if no address provided
            WalletTransaction query = new WalletTransaction();
            query.setUserId(userId);
            return R.ok(txService.queryList(query));
        }

        List<WalletTransaction> txList = new ArrayList<>();
        try {
            boolean isTestnet = address.toLowerCase().startsWith("cfxtest");
            String baseUrl = isTestnet ? "https://api-testnet.confluxscan.net" : "https://api.confluxscan.net";

            // 1. Fetch token and CFX transactions
            String url = baseUrl + "/account/transactions?account=" + address + "&limit=20&skip=0";
            String response = HttpUtil.get(url, 5000);
            JSONObject jsonObj = JSONUtil.parseObj(response);

            // 2. Fetch NFT transfer events (CRC721/CRC1155)
            String crc721Url = baseUrl + "/account/crc721/transfers?account=" + address + "&limit=20&skip=0";
            String crc721Response = HttpUtil.get(crc721Url, 5000);
            JSONObject crc721JsonObj = JSONUtil.parseObj(crc721Response);

            String crc1155Url = baseUrl + "/account/crc1155/transfers?account=" + address + "&limit=20&skip=0";
            String crc1155Response = HttpUtil.get(crc1155Url, 5000);
            JSONObject crc1155JsonObj = JSONUtil.parseObj(crc1155Response);

            // Process normal transactions
            if (jsonObj.getInt("code") == 0) {
                JSONObject data = jsonObj.getJSONObject("data");
                if (data != null) {
                    JSONArray list = data.getJSONArray("list");
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            JSONObject txObj = list.getJSONObject(i);
                            WalletTransaction tx = new WalletTransaction();
                            tx.setTxHash(txObj.getStr("hash"));
                            tx.setFromAddress(txObj.getStr("from"));
                            tx.setToAddress(txObj.getStr("to"));

                            // Convert drip to CFX (1 CFX = 10^18 drip)
                            String valueStr = txObj.getStr("value");
                            if (valueStr != null && !valueStr.isEmpty()) {
                                BigDecimal valueDrip = new BigDecimal(valueStr);
                                BigDecimal valueCfx = valueDrip.divide(new BigDecimal("1000000000000000000"));
                                tx.setAmount(valueCfx);
                            } else {
                                tx.setAmount(BigDecimal.ZERO);
                            }

                            // Parse token info from tokenTransfer (usually for CRC20 here)
                            JSONArray tokenTransfers = txObj.getJSONArray("tokenTransfer");
                            if (tokenTransfers != null && tokenTransfers.size() > 0) {
                                JSONObject tokenTransfer = tokenTransfers.getJSONObject(0);
                                if (tokenTransfer != null) {
                                    String symbol = tokenTransfer.getStr("symbol");
                                    if (symbol != null && !symbol.isEmpty()) {
                                        tx.setTokenSymbol(symbol);
                                    } else {
                                        tx.setTokenSymbol("Token");
                                    }

                                    String tValueStr = tokenTransfer.getStr("value");
                                    if (tValueStr != null && !tValueStr.isEmpty()) {
                                        BigDecimal decimals = new BigDecimal("10").pow(tokenTransfer.getInt("decimals", 18));
                                        tx.setAmount(new BigDecimal(tValueStr).divide(decimals));
                                    }

                                    String tokenFrom = tokenTransfer.getStr("from");
                                    String tokenTo = tokenTransfer.getStr("to");
                                    if (tokenFrom != null) tx.setFromAddress(tokenFrom);
                                    if (tokenTo != null) tx.setToAddress(tokenTo);
                                }
                            } else {
                                tx.setTokenSymbol("CFX");
                            }
                            tx.setChainId(isTestnet ? "1" : "1029");

                            Integer status = txObj.getInt("status");
                            if (status != null) {
                                tx.setStatus(status == 0 ? "Completed" : "Failed");
                            } else {
                                tx.setStatus("Pending");
                            }

                            Long timestamp = txObj.getLong("timestamp");
                            if (timestamp != null) {
                                tx.setCreateTime(new Date(timestamp * 1000));
                            }

                            txList.add(tx);
                        }
                    }
                }
            }

            // Process NFT transfers
            JSONObject[] nftJsonObjs = {crc721JsonObj, crc1155JsonObj};
            for (JSONObject nftJsonObj : nftJsonObjs) {
                if (nftJsonObj.getInt("code") == 0) {
                    JSONObject data = nftJsonObj.getJSONObject("data");
                    if (data != null) {
                        JSONArray list = data.getJSONArray("list");
                        if (list != null) {
                            for (int i = 0; i < list.size(); i++) {
                                JSONObject transferObj = list.getJSONObject(i);
                                WalletTransaction tx = new WalletTransaction();
                                tx.setTxHash(transferObj.getStr("transactionHash"));
                                tx.setFromAddress(transferObj.getStr("from"));
                                tx.setToAddress(transferObj.getStr("to"));
                                tx.setAmount(BigDecimal.ONE); // Usually 1 for NFT transfers

                                String name = transferObj.getStr("name");
                                String symbol = transferObj.getStr("symbol");
                                String tokenId = transferObj.getStr("tokenId");

                                if (name != null && !name.isEmpty()) {
                                    tx.setTokenSymbol("NFT: " + name + (tokenId != null ? " #" + tokenId : ""));
                                } else if (symbol != null && !symbol.isEmpty()) {
                                    tx.setTokenSymbol("NFT: " + symbol + (tokenId != null ? " #" + tokenId : ""));
                                } else {
                                    tx.setTokenSymbol("NFT" + (tokenId != null ? " #" + tokenId : ""));
                                }

                                tx.setChainId(isTestnet ? "1" : "1029");
                                tx.setStatus("Completed"); // Transfer events are from confirmed blocks

                                Long timestamp = transferObj.getLong("timestamp");
                                if (timestamp != null) {
                                    tx.setCreateTime(new Date(timestamp * 1000));
                                }

                                // Check if we already have this tx from the account/transactions call to avoid duplicates
                                boolean exists = false;
                                for (WalletTransaction existingTx : txList) {
                                    if (existingTx.getTxHash() != null && existingTx.getTxHash().equals(tx.getTxHash())) {
                                        // If the existing one is just a CFX wrapper for this NFT transfer, replace or skip
                                        // We'll remove the old one and use the more detailed NFT one
                                        txList.remove(existingTx);
                                        break;
                                    }
                                }
                                txList.add(tx);
                            }
                        }
                    }
                }
            }

            // Sort by timestamp descending
            txList.sort((t1, t2) -> {
                if (t1.getCreateTime() == null) return 1;
                if (t2.getCreateTime() == null) return -1;
                return t2.getCreateTime().compareTo(t1.getCreateTime());
            });

            return R.ok(txList);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to DB query on error
            WalletTransaction query = new WalletTransaction();
            query.setUserId(userId);
            query.setFromAddress(address);
            return R.ok(txService.queryList(query));
        }
    }
}
