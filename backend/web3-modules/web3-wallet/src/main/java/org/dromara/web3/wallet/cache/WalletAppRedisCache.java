package org.dromara.web3.wallet.cache;

import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;

import java.time.Duration;

/**
 * APP 用户信息、钱包列表 Redis 缓存键与失效。
 */
public final class WalletAppRedisCache {

    /** v3：钱包列表缓存按接口类型和语言区分，避免串缓存 */
    private static final String PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "app_wallet:v3:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private WalletAppRedisCache() {
    }

    public static String profileKey(Long userId) {
        return PREFIX + "profile:" + userId;
    }

    public static String walletListKey(Long userId, String listType, String language) {
        return PREFIX + "wallets:" + normalizeListType(listType) + ":" + userId + ":" + normalizeLanguage(language);
    }

    /** 交易发送前短时解锁凭证（指纹或交易密码校验通过后写入） */
    public static String tradeUnlockKey(Long userId, String token) {
        return PREFIX + "trade_unlock:" + userId + ":" + token;
    }

    public static Duration tradeUnlockTtl() {
        return Duration.ofMinutes(5);
    }

    public static Duration ttl() {
        return DEFAULT_TTL;
    }

    public static void evictProfile(Long userId) {
        if (userId != null) {
            RedisUtils.deleteObject(profileKey(userId));
        }
    }

    public static void evictWalletList(Long userId) {
        if (userId != null) {
            RedisUtils.deleteKeys(PREFIX + "wallets:*:" + userId + ":*");
        }
    }

    private static String normalizeListType(String listType) {
        if (StringUtils.isBlank(listType)) {
            return "default";
        }
        return listType.trim().toLowerCase();
    }

    private static String normalizeLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            return "default";
        }
        return language.trim().toLowerCase().replace('-', '_');
    }
}
