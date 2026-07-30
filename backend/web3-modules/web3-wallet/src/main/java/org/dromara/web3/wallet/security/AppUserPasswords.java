package org.dromara.web3.wallet.security;

import cn.hutool.crypto.digest.BCrypt;
import org.dromara.common.core.utils.StringUtils;

/**
 * App 用户登录密码：BCrypt 存储；兼容历史明文，校验成功后由业务层写回哈希。
 */
public final class AppUserPasswords {

    private AppUserPasswords() {
    }

    public static boolean matches(String plain, String stored) {
        if (StringUtils.isBlank(plain) || StringUtils.isBlank(stored)) {
            return false;
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$")) {
            return BCrypt.checkpw(plain, stored);
        }
        return StringUtils.equals(stored, plain);
    }

    public static String encode(String plain) {
        return BCrypt.hashpw(plain);
    }
}
