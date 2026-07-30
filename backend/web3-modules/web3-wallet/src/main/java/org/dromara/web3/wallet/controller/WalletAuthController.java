package org.dromara.web3.wallet.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import conflux.web3j.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mail.service.AsyncVerifyMailSender;
import org.dromara.common.mail.template.CfxmapVerifyCodeMail;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.AppUserBiometricCredential;
import org.dromara.web3.wallet.domain.WalletAccount;
import org.dromara.web3.wallet.cache.WalletAppRedisCache;
import org.dromara.web3.wallet.mapper.AppUserBiometricCredentialMapper;
import org.dromara.web3.wallet.security.AppUserPasswords;
import org.dromara.web3.wallet.service.IAppUserService;
import org.dromara.web3.wallet.service.IWalletAccountService;
import org.dromara.web3.wallet.service.WalletRegisterAsyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class WalletAuthController extends BaseController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String EMAIL_SEND_COUNT_KEY = "wallet:email:send:count:";
    private static final String EMAIL_SEND_LOCK_KEY = "wallet:email:send:lock:";
    private static final String PWD_LOGIN_FAIL_KEY = "wallet:login:pwd_fail:";
    private final IWalletAccountService accountService;
    private final IAppUserService appUserService;
    private final AppUserBiometricCredentialMapper biometricCredentialMapper;
    private final ObjectProvider<AsyncVerifyMailSender> asyncVerifyMailSender;
    private final WalletRegisterAsyncService walletRegisterAsyncService;

    private String doLoginAndGetToken(AppUser appUser) {
        LoginUser loginUser = new LoginUser();
        loginUser.setTenantId(appUser.getTenantId());
        loginUser.setUserId(appUser.getUserId());
        loginUser.setUsername(appUser.getUserName());
        loginUser.setUserType(UserType.APP_USER.getUserType());
        loginUser.setClientKey("e5cd7e4891bf95d1d19206ce24a7b32e");
        loginUser.setDeviceType("app");

        SaLoginParameter model = new SaLoginParameter();
        model.setDevice("app");
        model.setExtra(LoginHelper.CLIENT_KEY, "e5cd7e4891bf95d1d19206ce24a7b32e");
        LoginHelper.login(loginUser, model);

        appUser.setLoginIp(ServletUtils.getClientIP());
        appUser.setLoginDate(DateUtils.getNowDate());
        appUserService.updateUser(appUser);

        return StpUtil.getTokenValue();
    }

    private void validateCaptcha(String uuid, String code) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            throw new IllegalArgumentException("图形验证码已过期");
        }
        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
            throw new IllegalArgumentException("图形验证码错误");
        }
    }

    private int getPwdLoginFailCount(String email) {
        Integer c = RedisUtils.getCacheObject(PWD_LOGIN_FAIL_KEY + email);
        return c == null ? 0 : c;
    }

    private void bumpPwdLoginFail(String email) {
        String k = PWD_LOGIN_FAIL_KEY + email;
        int n = getPwdLoginFailCount(email) + 1;
        RedisUtils.setCacheObject(k, n, Duration.ofHours(24));
    }

    private void clearPwdLoginFail(String email) {
        RedisUtils.deleteObject(PWD_LOGIN_FAIL_KEY + email);
    }

    /**
     * @return true 若本次为新用户补建了首钱包
     */
    private boolean ensureUserWallet(AppUser appUser) {
        long walletStart = System.currentTimeMillis();
        Long userId = appUser.getUserId();
        WalletAccount query = new WalletAccount();
        query.setUserId(userId);
        List<WalletAccount> existingWallets = accountService.queryList(query);
        if (existingWallets != null && !existingWallets.isEmpty()) {
            log.info(
                "App register wallet already exists. userId={}, walletCount={}, cost={}ms",
                userId,
                existingWallets.size(),
                System.currentTimeMillis() - walletStart
            );
            return false;
        }

        try {
            byte[] initialEntropy = new byte[16];
            new SecureRandom().nextBytes(initialEntropy);
            String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);

            int[] derivationPath = {44 | Bip32ECKeyPair.HARDENED_BIT, 60 | Bip32ECKeyPair.HARDENED_BIT, 0 | Bip32ECKeyPair.HARDENED_BIT, 0, 0};
            Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(MnemonicUtils.generateSeed(mnemonic, null));
            Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath);
            Credentials credentials = Credentials.create(derivedKeyPair);
            String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);

            Account cfxAccount = Account.create(conflux.web3j.Cfx.create("https://main.confluxrpc.com"), privateKey);
            String cfxAddress = cfxAccount.getAddress().getAddress();

            WalletAccount newWallet = new WalletAccount();
            newWallet.setUserId(userId);
            newWallet.setName("主钱包_" + System.currentTimeMillis());
            newWallet.setAddress(cfxAddress);
            newWallet.setChainId("cfx");
            newWallet.setPrivateKey(privateKey);
            newWallet.setMnemonic(mnemonic);
            newWallet.setHasPrivateKey(0);
            newWallet.setHasMnemonic(0);
            newWallet.setIsNewUserCreated(1);
            accountService.insertByBo(newWallet);
            log.info(
                "App register wallet created. userId={}, address={}, cost={}ms",
                userId,
                cfxAddress,
                System.currentTimeMillis() - walletStart
            );
            return true;
        } catch (Exception e) {
            log.error(
                "App register wallet create failed. userId={}, cost={}ms",
                userId,
                System.currentTimeMillis() - walletStart,
                e
            );
            throw new IllegalStateException("Failed to create wallet for new user");
        }
    }

    private String findUserCfxWalletAddress(Long userId) {
        if (userId == null) {
            return null;
        }
        WalletAccount query = new WalletAccount();
        query.setUserId(userId);
        query.setChainId("cfx");
        List<WalletAccount> wallets = accountService.queryList(query);
        if (wallets == null || wallets.isEmpty()) {
            return null;
        }
        return wallets.get(0).getAddress();
    }

    @PostMapping("/sms/send")
    public R<Void> sendSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        return R.ok();
    }

    @GetMapping("/email/code")
    public R<Void> sendEmailCode(
        @RequestParam String email,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String uuid
    ) {
        if (StringUtils.isBlank(email)) {
            return R.fail("邮箱不能为空");
        }

        String sendLockKey = EMAIL_SEND_LOCK_KEY + email;
        if (Boolean.TRUE.equals(RedisUtils.hasKey(sendLockKey))) {
            return R.fail("请60秒后再试");
        }

        String countKey = EMAIL_SEND_COUNT_KEY + email;
        Integer sendCount = RedisUtils.getCacheObject(countKey);
        int currentCount = sendCount == null ? 0 : sendCount;
        if (currentCount >= 3) {
            try {
                validateCaptcha(uuid, code);
            } catch (IllegalArgumentException e) {
                return R.fail(e.getMessage());
            }
        }

        String verifyCode = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email, verifyCode, Duration.ofMinutes(5));
        RedisUtils.setCacheObject(sendLockKey, 1, Duration.ofSeconds(60));
        RedisUtils.setCacheObject(countKey, currentCount + 1, Duration.ofDays(1));
        AsyncVerifyMailSender sender = asyncVerifyMailSender.getIfAvailable();
        if (sender != null) {
            sender.sendCfxmapVerifyCode(email, verifyCode, 5);
            return R.ok();
        }
        try {
            CfxmapVerifyCodeMail.send(email, verifyCode, 5);
            return R.ok();
        } catch (Exception e) {
            RedisUtils.deleteObject(sendLockKey);
            return R.fail("验证码邮件发送异常: " + e.getMessage());
        }
    }

    @PostMapping("/email/send")
    public R<Void> sendEmail(@RequestBody Map<String, String> body) {
        return R.ok();
    }

    /**
     * 未登录：通过邮箱验证码重置登录密码（验证码与登录/绑定共用 Redis 键）
     */
    @PutMapping("/password/reset")
    public R<Void> resetPasswordByEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        if (StringUtils.isAnyBlank(email, code, newPassword)) {
            return R.fail("邮箱、验证码与新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return R.fail("密码至少6位");
        }
        AppUser appUser = appUserService.selectUserByEmail(email.trim());
        if (ObjectUtil.isNull(appUser)) {
            return R.fail("该邮箱未注册");
        }
        if (!"0".equals(appUser.getStatus())) {
            return R.fail("账号已停用");
        }
        String cacheCode = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email.trim());
        if (StringUtils.isBlank(cacheCode)) {
            return R.fail("验证码已过期");
        }
        if (!StringUtils.equals(cacheCode, code)) {
            return R.fail("验证码错误");
        }
        RedisUtils.deleteObject(GlobalConstants.CAPTCHA_CODE_KEY + email.trim());
        appUser.setPassword(AppUserPasswords.encode(newPassword));
        boolean ok = appUserService.updateUser(appUser);
        if (ok) {
            WalletAppRedisCache.evictProfile(appUser.getUserId());
        }
        return ok ? R.ok() : R.fail("重置密码失败");
    }

    @PostMapping("/register/password")
    public R<Map<String, Object>> registerPassword(@RequestBody Map<String, String> body) {
        long registerStart = System.currentTimeMillis();
        String email = StringUtils.trim(body.get("email"));
        String password = body.get("password");
        log.info("App register request start. email={}", email);
        if (StringUtils.isAnyBlank(email, password)) {
            return R.fail("邮箱和密码不能为空");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return R.fail("邮箱格式不正确");
        }
        if (password.length() < 6) {
            return R.fail("密码至少6位");
        }

        AppUser existingUser = appUserService.selectUserByEmail(email);
        if (ObjectUtil.isNotNull(existingUser)) {
            if (!"0".equals(existingUser.getStatus())) {
                return R.fail("账号已停用");
            }
            return R.fail("该邮箱已注册，请直接登录");
        }

        String nickNameSeed = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setUserName(email);
        appUser.setNickName("用户" + nickNameSeed);
        appUser.setTenantId("000000");
        appUser.setStatus("0");
        appUser.setDelFlag("0");
        appUser.setPassword(AppUserPasswords.encode(password));
        long createUserStart = System.currentTimeMillis();
        appUserService.registerUser(appUser);
        log.info(
            "App register user created. email={}, cost={}ms",
            email,
            System.currentTimeMillis() - createUserStart
        );

        long reloadUserStart = System.currentTimeMillis();
        appUser = appUserService.selectUserByEmail(email);
        log.info(
            "App register user reload finished. email={}, cost={}ms",
            email,
            System.currentTimeMillis() - reloadUserStart
        );
        if (ObjectUtil.isNull(appUser)) {
            log.error(
                "App register user reload missing after insert. email={}, totalCost={}ms",
                email,
                System.currentTimeMillis() - registerStart
            );
            return R.fail("注册失败，请稍后重试");
        }

        long walletEnsureStart = System.currentTimeMillis();
        boolean walletCreated = ensureUserWallet(appUser);
        if (walletCreated) {
            WalletAppRedisCache.evictWalletList(appUser.getUserId());
        }
        String walletAddress = findUserCfxWalletAddress(appUser.getUserId());
        log.info(
            "App register wallet ensured. userId={}, email={}, walletCreated={}, walletAddress={}, cost={}ms",
            appUser.getUserId(),
            email,
            walletCreated,
            walletAddress,
            System.currentTimeMillis() - walletEnsureStart
        );

        long asyncDispatchStart = System.currentTimeMillis();
        walletRegisterAsyncService.handlePostRegister(appUser.getUserId(), email, walletAddress);
        log.info(
            "App register async post task dispatched. userId={}, email={}, cost={}ms",
            appUser.getUserId(),
            email,
            System.currentTimeMillis() - asyncDispatchStart
        );

        long loginStart = System.currentTimeMillis();
        String token = doLoginAndGetToken(appUser);
        log.info(
            "App register login finished. userId={}, email={}, cost={}ms",
            appUser.getUserId(),
            email,
            System.currentTimeMillis() - loginStart
        );
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);
        result.put("email", appUser.getEmail());
        log.info(
            "App register request finished. userId={}, email={}, totalCost={}ms",
            appUser.getUserId(),
            email,
            System.currentTimeMillis() - registerStart
        );
        return R.ok(result);
    }

    @PostMapping("/login/email")
    public R<Map<String, Object>> loginEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.getOrDefault("code", body.get("emailCode"));
        if (StringUtils.isAnyBlank(email, code)) {
            return R.fail("邮箱或验证码不能为空");
        }

        String cacheCode = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(cacheCode)) {
            return R.fail("验证码已过期");
        }
        if (!StringUtils.equals(cacheCode, code)) {
            return R.fail("验证码错误");
        }
        RedisUtils.deleteObject(GlobalConstants.CAPTCHA_CODE_KEY + email);

        AppUser appUser = appUserService.selectUserByEmail(email);
        if (ObjectUtil.isNull(appUser)) {
            return R.fail("该邮箱未注册，请先注册");
        }
        if (!"0".equals(appUser.getStatus())) {
            return R.fail("账号已停用");
        }

        if (ensureUserWallet(appUser)) {
            WalletAppRedisCache.evictWalletList(appUser.getUserId());
        }

        String token = doLoginAndGetToken(appUser);
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);
        result.put("email", appUser.getEmail());
        return R.ok(result);
    }

    /**
     * 邮箱 + 密码登录（须先在安全中心设置密码；存储方式与 /api/user/updatePwd 一致）
     */
    @PostMapping("/login/password")
    public R<Map<String, Object>> loginPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String uuid = body.get("uuid");
        String imgCode = body.get("code");
        if (StringUtils.isAnyBlank(email, password)) {
            return R.fail("邮箱和密码不能为空");
        }

        int fails = getPwdLoginFailCount(email);
        if (fails >= 3) {
            if (StringUtils.isAnyBlank(uuid, imgCode)) {
                return R.fail("登录失败次数过多，请输入图形验证码");
            }
            try {
                validateCaptcha(uuid, imgCode);
            } catch (IllegalArgumentException e) {
                return R.fail(e.getMessage());
            }
        }

        AppUser appUser = appUserService.selectUserByEmail(email);
        if (ObjectUtil.isNull(appUser)) {
            bumpPwdLoginFail(email);
            return R.fail("邮箱或密码错误");
        }
        if (!"0".equals(appUser.getStatus())) {
            return R.fail("账号已停用");
        }
        if (StringUtils.isBlank(appUser.getPassword())) {
            return R.fail("尚未设置登录密码，请先用验证码登录后在个人中心设置密码");
        }
        if (!AppUserPasswords.matches(password, appUser.getPassword())) {
            bumpPwdLoginFail(email);
            return R.fail("邮箱或密码错误");
        }

        if (!appUser.getPassword().startsWith("$2a$") && !appUser.getPassword().startsWith("$2b$")) {
            appUser.setPassword(AppUserPasswords.encode(password));
            appUserService.updateUser(appUser);
        }

        clearPwdLoginFail(email);
        if (ensureUserWallet(appUser)) {
            WalletAppRedisCache.evictWalletList(appUser.getUserId());
        }

        String token = doLoginAndGetToken(appUser);
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);
        result.put("email", appUser.getEmail());
        return R.ok(result);
    }

    @PostMapping("/login/biometric")
    public R<Map<String, Object>> loginBiometric(@RequestBody Map<String, String> body) {
        String biometricToken = body.get("biometricToken");
        if (StringUtils.isBlank(biometricToken)) {
            return R.fail("指纹登录凭证不能为空");
        }

        String credentialHash = SecureUtil.sha256(biometricToken);
        AppUserBiometricCredential credential = biometricCredentialMapper.selectOne(
            new LambdaQueryWrapper<AppUserBiometricCredential>()
                .eq(AppUserBiometricCredential::getCredentialHash, credentialHash)
                .eq(AppUserBiometricCredential::getStatus, "0")
                .last("limit 1")
        );
        if (credential == null) {
            return R.fail("指纹登录凭证已失效，请重新开启");
        }
        if (credential.getExpireTime() != null && credential.getExpireTime().before(new Date())) {
            return R.fail("指纹登录凭证已过期，请重新开启");
        }

        AppUser appUser = appUserService.selectUserById(credential.getUserId());
        if (appUser == null || !"0".equals(appUser.getStatus())) {
            return R.fail("当前用户不可用");
        }

        credential.setLastUsedTime(new Date());
        biometricCredentialMapper.updateById(credential);

        String token = doLoginAndGetToken(appUser);
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);
        result.put("email", appUser.getEmail());
        return R.ok(result);
    }

    @PostMapping("/login/phone")
    public R<String> loginPhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        return R.ok("jwt_token_placeholder_for_phone_" + phone);
    }

    @PostMapping("/login/apple")
    public R<String> loginApple(@RequestBody Map<String, String> body) {
        return R.ok("jwt_token_placeholder_for_apple");
    }
}
