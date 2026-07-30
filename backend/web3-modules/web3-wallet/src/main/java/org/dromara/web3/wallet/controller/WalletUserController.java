package org.dromara.web3.wallet.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.MimeTypeUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.web3.wallet.config.WalletNewUserRewardProperties;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.dromara.web3.wallet.domain.WalletAccount;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.AppUserBiometricCredential;
import org.dromara.web3.wallet.domain.WalletNewUserReward;
import org.dromara.web3.wallet.domain.WalletUserPreference;
import org.dromara.web3.wallet.cache.WalletAppRedisCache;
import org.dromara.web3.wallet.mapper.AppUserBiometricCredentialMapper;
import org.dromara.web3.wallet.security.AppUserPasswords;
import org.dromara.web3.wallet.service.IAppUserService;
import org.dromara.web3.wallet.service.IWalletAccountService;
import org.dromara.web3.wallet.service.IWalletNewUserRewardService;
import org.dromara.web3.wallet.service.IWalletUserPreferenceService;
import org.dromara.web3.wallet.service.WalletAvatarOssService;
import org.dromara.web3.wallet.service.WalletLocalAvatarStorage;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.WalletUtils;
import conflux.web3j.Account;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class WalletUserController extends BaseController {

    private static final int WALLET_NAME_MAX_LEN = 20;
    private static final int MNEMONIC_PREVIEW_LIMIT = 5;
    private static final int MAX_MNEMONIC_DERIVE_INDEX = 99;
    private static final String MAIN_WALLET_NAME_ZH = "主钱包";
    private static final String MAIN_WALLET_NAME_EN = "Main Wallet";
    private static final String MAIN_WALLET_PREFIX_ZH = MAIN_WALLET_NAME_ZH + "_";

    private final IWalletAccountService accountService;
    private final IWalletUserPreferenceService preferenceService;
    private final IAppUserService appUserService;
    private final WalletNewUserRewardProperties newUserRewardProperties;
    private final IWalletNewUserRewardService newUserRewardService;
    private final AppUserBiometricCredentialMapper biometricCredentialMapper;
    private final WalletLocalAvatarStorage localAvatarStorage;
    private final WalletAvatarOssService walletAvatarOssService;

    /**
     * 注册时系统自动创建的钱包：库里有 is_new_user_created=1，或历史数据仅有「主钱包_」前缀名称。
     */
    private static boolean isAppProvisionedDefaultWallet(WalletAccount acc) {
        if (acc.getIsNewUserCreated() != null && acc.getIsNewUserCreated() == 1) {
            return true;
        }
        String n = acc.getName();
        return n != null && n.startsWith(MAIN_WALLET_PREFIX_ZH);
    }

    /**
     * 仅允许移除非系统钱包：is_new_user_created 为 0 或历史空值，且非「主钱包_」/库内标记为注册下发。
     */
    private static boolean walletMayBeRemovedByUser(WalletAccount acc) {
        if (isAppProvisionedDefaultWallet(acc)) {
            return false;
        }
        Integer v = acc.getIsNewUserCreated();
        return v == null || v == 0;
    }

    private void appendNewUserRewardPopup(Map<String, Object> profile, Long userId) {
        if (userId == null) {
            return;
        }
        WalletNewUserReward reward = newUserRewardService.findPendingPopupReward(userId);
        if (reward == null) {
            return;
        }
        if (!newUserRewardService.markPopupShown(reward.getRewardId())) {
            return;
        }
        DecimalFormat seqFormat = new DecimalFormat("0");
        String rewardRange = newUserRewardProperties.getDisplayMinAmount() + "-" + newUserRewardProperties.getDisplayMaxAmount();
        Long userSequence = reward.getUserSequence() == null ? userId : reward.getUserSequence();
        Map<String, Object> popup = new HashMap<>();
        popup.put("title", "新用户奖励");
        popup.put(
            "content",
            "恭喜你是 CFXMAP 第 "
                + seqFormat.format(userSequence)
                + " 位用户，可获取 "
                + rewardRange
                + " 枚 CFX 代币，正在发放中。"
        );
        popup.put("userSequence", userSequence);
        popup.put("rewardRange", rewardRange);
        profile.put("newUserRewardPopup", popup);
    }

    /**
     * 可选钱包名称：空则跳过；非空则校验长度（最多 20 字符）。
     */
    private static R<Void> validateOptionalWalletName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        if (name.trim().length() > WALLET_NAME_MAX_LEN) {
            return R.fail("钱包名称最长" + WALLET_NAME_MAX_LEN + "个字符");
        }
        return null;
    }

    private static String currentLanguageTag() {
        return StringUtils.blankToDefault(ServletUtils.getRequest().getHeader("content-language"), "");
    }

    private static boolean isEnglishLanguage(String languageTag) {
        if (StringUtils.isBlank(languageTag)) {
            return false;
        }
        String normalized = languageTag.trim().toLowerCase().replace('-', '_');
        return normalized.startsWith("en");
    }

    private static String localizeWalletName(String walletName, boolean englishMode) {
        if (!englishMode || StringUtils.isBlank(walletName)) {
            return walletName;
        }
        if (MAIN_WALLET_NAME_ZH.equals(walletName)) {
            return MAIN_WALLET_NAME_EN;
        }
        if (walletName.startsWith(MAIN_WALLET_PREFIX_ZH)) {
            return MAIN_WALLET_NAME_EN + walletName.substring(MAIN_WALLET_NAME_ZH.length());
        }
        return walletName;
    }

    private static int[] buildDerivationPath(int addressIndex) {
        return new int[] {
            44 | Bip32ECKeyPair.HARDENED_BIT,
            60 | Bip32ECKeyPair.HARDENED_BIT,
            0 | Bip32ECKeyPair.HARDENED_BIT,
            0,
            addressIndex
        };
    }

    private static String buildDerivationPathLabel(int addressIndex) {
        return "m/44'/60'/0'/0/" + addressIndex;
    }

    private static Credentials deriveCredentialsFromMnemonic(String mnemonic, int addressIndex) {
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(MnemonicUtils.generateSeed(mnemonic, null));
        Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, buildDerivationPath(addressIndex));
        return Credentials.create(derivedKeyPair);
    }

    private static DerivedWalletAddresses deriveWalletAddresses(String privateKey) {
        Credentials credentials = Credentials.create(privateKey);
        String ethAddress = credentials.getAddress();

        Account cfxAccount = Account.create(conflux.web3j.Cfx.create("https://main.confluxrpc.com"), privateKey);
        String cfxAddress = cfxAccount.getAddress().getAddress();

        Account cfxTestAccount = Account.create(conflux.web3j.Cfx.create("https://test.confluxrpc.com", 1, 3), privateKey);
        String cfxTestAddress = cfxTestAccount.getAddress().getAddress();

        return new DerivedWalletAddresses(ethAddress, cfxAddress, cfxTestAddress);
    }

    private static String resolveAddressByChainId(String chainId, DerivedWalletAddresses addresses) {
        if ("cfxtest".equalsIgnoreCase(chainId)) {
            return addresses.cfxTestAddress();
        }
        if ("cfx".equalsIgnoreCase(chainId) || StringUtils.isBlank(chainId)) {
            return addresses.cfxAddress();
        }
        return addresses.ethAddress();
    }

    private boolean walletExistsForUser(Long userId, String address, String chainId) {
        if (userId == null || StringUtils.isBlank(address)) {
            return false;
        }
        WalletAccount query = new WalletAccount();
        query.setUserId(userId);
        query.setAddress(address);
        query.setChainId(chainId);
        List<WalletAccount> existingAccounts = accountService.queryList(query);
        return existingAccounts != null && !existingAccounts.isEmpty();
    }

    private static int parseDeriveIndex(Object rawDeriveIndex) {
        if (rawDeriveIndex == null) {
            return 0;
        }
        int deriveIndex;
        if (rawDeriveIndex instanceof Number number) {
            deriveIndex = number.intValue();
        } else {
            deriveIndex = Integer.parseInt(String.valueOf(rawDeriveIndex).trim());
        }
        if (deriveIndex < 0 || deriveIndex > MAX_MNEMONIC_DERIVE_INDEX) {
            throw new IllegalArgumentException("deriveIndex out of range");
        }
        return deriveIndex;
    }

    private record DerivedWalletAddresses(String ethAddress, String cfxAddress, String cfxTestAddress) {
    }

    /** 部分客户端上传临时文件无扩展名，仅能通过 Content-Type 判断格式 */
    private static String inferImageExtensionFromContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return "";
        }
        String ct = contentType.toLowerCase();
        if (ct.contains("jpeg")) {
            return "jpg";
        }
        if (ct.contains("png")) {
            return "png";
        }
        if (ct.contains("gif")) {
            return "gif";
        }
        if (ct.contains("bmp")) {
            return "bmp";
        }
        if (ct.contains("webp")) {
            return "webp";
        }
        if (ct.contains("heic") || ct.contains("heif")) {
            return "heic";
        }
        return "";
    }

    @GetMapping("/profile")
    public R<Map<String, Object>> getProfile() {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }

        Map<String, Object> cached = RedisUtils.getCacheObject(WalletAppRedisCache.profileKey(userId));
        if (cached != null) {
            Map<String, Object> profile = new HashMap<>(cached);
            appendNewUserRewardPopup(profile, userId);
            return R.ok(profile);
        }

        AppUser appUser = appUserService.selectUserById(userId);
        Map<String, Object> profile = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId.toString());
        user.put("nickName", appUser != null ? appUser.getNickName() : null);
        user.put("email", appUser != null ? appUser.getEmail() : null);
        user.put("phonenumber", appUser != null ? appUser.getPhonenumber() : null);
        user.put("avatar", appUser != null ? appUser.getAvatar() : null);
        user.put("userName", appUser != null ? appUser.getUserName() : null);
        profile.put("user", user);
        RedisUtils.setCacheObject(WalletAppRedisCache.profileKey(userId), profile, WalletAppRedisCache.ttl());
        appendNewUserRewardPopup(profile, userId);
        return R.ok(profile);
    }

    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody Map<String, Object> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        Object nickName = body.get("nickName");
        Object email = body.get("email");
        Object phonenumber = body.get("phonenumber");
        if (nickName != null) {
            appUser.setNickName(String.valueOf(nickName).trim());
        }
        if (email != null) {
            appUser.setEmail(String.valueOf(email).trim());
        }
        if (phonenumber != null) {
            appUser.setPhonenumber(String.valueOf(phonenumber).trim());
        }
        boolean ok = appUserService.updateUser(appUser);
        if (ok) {
            WalletAppRedisCache.evictProfile(userId);
        }
        return ok ? R.ok() : R.fail("Failed to update profile");
    }

    @PostMapping("/verifyEmailCode")
    public R<Void> verifyEmailCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        if (StringUtils.isAnyBlank(email, code)) {
            return R.fail("Email or code is empty");
        }
        String cacheCode = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(cacheCode)) {
            return R.fail("验证码已过期");
        }
        if (!StringUtils.equals(cacheCode, code)) {
            return R.fail("验证码错误");
        }
        return R.ok();
    }

    @PutMapping("/bindEmail")
    public R<Void> bindEmail(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        String email = body.get("email");
        String code = body.get("code");
        if (StringUtils.isAnyBlank(email, code)) {
            return R.fail("Email or code is empty");
        }
        String cacheCode = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(cacheCode)) {
            return R.fail("验证码已过期");
        }
        if (!StringUtils.equals(cacheCode, code)) {
            return R.fail("验证码错误");
        }
        appUser.setEmail(email.trim());
        boolean ok = appUserService.updateUser(appUser);
        if (ok) {
            WalletAppRedisCache.evictProfile(userId);
        }
        return ok ? R.ok() : R.fail("Failed to bind email");
    }

    @PutMapping("/updatePwd")
    public R<Void> updatePwd(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (StringUtils.isAnyBlank(oldPassword, newPassword)) {
            return R.fail("Password can not be empty");
        }
        if (StringUtils.isNotBlank(appUser.getPassword()) && !AppUserPasswords.matches(oldPassword, appUser.getPassword())) {
            return R.fail("旧密码错误");
        }
        appUser.setPassword(AppUserPasswords.encode(newPassword));
        boolean ok = appUserService.updateUser(appUser);
        if (ok) {
            WalletAppRedisCache.evictProfile(userId);
        }
        return ok ? R.ok() : R.fail("Failed to update password");
    }

    @PutMapping("/updatePwdByEmail")
    public R<Void> updatePwdByEmail(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        if (StringUtils.isAnyBlank(email, code, newPassword)) {
            return R.fail("Required fields can not be empty");
        }
        if (StringUtils.isNotBlank(appUser.getEmail()) && !StringUtils.equals(appUser.getEmail(), email)) {
            return R.fail("邮箱不匹配");
        }
        String cacheCode = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(cacheCode)) {
            return R.fail("验证码已过期");
        }
        if (!StringUtils.equals(cacheCode, code)) {
            return R.fail("验证码错误");
        }
        appUser.setPassword(AppUserPasswords.encode(newPassword));
        if (StringUtils.isBlank(appUser.getEmail())) {
            appUser.setEmail(email.trim());
        }
        boolean ok = appUserService.updateUser(appUser);
        if (ok) {
            WalletAppRedisCache.evictProfile(userId);
        }
        return ok ? R.ok() : R.fail("Failed to update password");
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<AvatarVo> avatar(@RequestPart("avatarfile") MultipartFile avatarfile) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        if (ObjectUtil.isNotNull(avatarfile) && !avatarfile.isEmpty()) {
            String extension = FileUtil.extName(avatarfile.getOriginalFilename());
            if (StringUtils.isBlank(extension)) {
                extension = inferImageExtensionFromContentType(avatarfile.getContentType());
            }
            if (!StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION)) {
                return R.fail("文件格式不正确，请上传" + Arrays.toString(MimeTypeUtils.IMAGE_EXTENSION) + "格式");
            }
            String imgUrl;
            try {
                imgUrl = localAvatarStorage.save(avatarfile, userId, extension);
            } catch (IOException e) {
                return R.fail("上传图片异常，请联系管理员");
            }
            appUser.setAvatar(imgUrl);
            if (appUserService.updateUser(appUser)) {
                WalletAppRedisCache.evictProfile(userId);
                return R.ok(new AvatarVo(imgUrl));
            }
        }
        return R.fail("上传图片异常，请联系管理员");
    }

    public record AvatarVo(String imgUrl) {}

    @GetMapping("/profile/avatar/policy")
    public R<WalletAvatarOssService.AvatarUploadPolicy> avatarPolicy(
        @RequestParam(value = "fileName", required = false) String fileName,
        @RequestParam(value = "contentType", required = false) String contentType
    ) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        if (!walletAvatarOssService.enabled()) {
            return R.fail("头像 OSS 上传未启用");
        }
        try {
            return R.ok(walletAvatarOssService.createPolicy(userId, fileName, contentType));
        } catch (IllegalStateException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("获取头像上传策略失败");
        }
    }

    @PutMapping("/profile/avatar")
    public R<AvatarVo> updateAvatarUrl(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        String avatarUrl = StringUtils.trim(body.get("avatarUrl"));
        if (StringUtils.isBlank(avatarUrl)) {
            return R.fail("头像地址不能为空");
        }
        if (!StringUtils.ishttp(avatarUrl)) {
            return R.fail("头像地址格式不正确");
        }
        appUser.setAvatar(avatarUrl);
        if (appUserService.updateUser(appUser)) {
            WalletAppRedisCache.evictProfile(userId);
            return R.ok(new AvatarVo(avatarUrl));
        }
        return R.fail("保存头像失败");
    }

    @PostMapping("/wallet/bind")
    public R<Void> bindWallet(@RequestBody WalletAccount account) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        R<Void> nameErr = validateOptionalWalletName(account.getName());
        if (nameErr != null) {
            return nameErr;
        }
        if (StringUtils.isNotBlank(account.getName())) {
            account.setName(account.getName().trim());
        }
        account.setUserId(userId);

        WalletAccount query = new WalletAccount();
        query.setUserId(userId);
        query.setAddress(account.getAddress());
        query.setChainId(account.getChainId());
        List<WalletAccount> existingAccounts = accountService.queryList(query);
        if (existingAccounts != null && !existingAccounts.isEmpty()) {
            return R.fail("Wallet already bound to the current user");
        }

        account.setHasPrivateKey(0);
        account.setHasMnemonic(0);
        account.setIsNewUserCreated(0);
        accountService.insertByBo(account);
        WalletAppRedisCache.evictWalletList(userId);
        return R.ok();
    }

    @PostMapping("/wallet/create")
    public R<Map<String, String>> createWallet(@RequestBody WalletAccount account) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        if (StringUtils.isBlank(account.getName())) {
            account.setName("钱包_" + System.currentTimeMillis());
        } else {
            R<Void> nameErr = validateOptionalWalletName(account.getName());
            if (nameErr != null) {
                return R.fail(nameErr.getMsg());
            }
            account.setName(account.getName().trim());
        }

        try {
            byte[] initialEntropy = new byte[16];
            new java.security.SecureRandom().nextBytes(initialEntropy);
            String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);

            int[] derivationPath = {44 | Bip32ECKeyPair.HARDENED_BIT, 60 | Bip32ECKeyPair.HARDENED_BIT, 0 | Bip32ECKeyPair.HARDENED_BIT, 0, 0};
            Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(MnemonicUtils.generateSeed(mnemonic, null));
            Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath);
            Credentials credentials = Credentials.create(derivedKeyPair);
            String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);

            String ethAddress = credentials.getAddress();

            Account cfxAccount = Account.create(conflux.web3j.Cfx.create("https://main.confluxrpc.com"), privateKey);
            String cfxAddress = cfxAccount.getAddress().getAddress();

            Account cfxTestAccount = Account.create(conflux.web3j.Cfx.create("https://test.confluxrpc.com", 1, 3), privateKey);
            String cfxTestAddress = cfxTestAccount.getAddress().getAddress();

            account.setUserId(userId);
            account.setPrivateKey(privateKey);
            account.setMnemonic(mnemonic);
            account.setHasPrivateKey(1);
            account.setHasMnemonic(1);

            String chainId = account.getChainId();
            if ("cfx".equalsIgnoreCase(chainId)) {
                account.setAddress(cfxAddress);
            } else if ("cfxtest".equalsIgnoreCase(chainId)) {
                account.setAddress(cfxTestAddress);
            } else {
                account.setAddress(ethAddress);
            }

            account.setIsNewUserCreated(0);
            accountService.insertByBo(account);
            WalletAppRedisCache.evictWalletList(userId);

            Map<String, String> result = new HashMap<>();
            result.put("ethAddress", ethAddress);
            result.put("cfxAddress", cfxAddress);
            result.put("cfxTestAddress", cfxTestAddress);
            return R.ok("Wallet created successfully", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("Failed to create wallet");
        }
    }

    @PostMapping("/wallet/import/preview")
    public R<List<Map<String, Object>>> previewImportWallet(@RequestBody Map<String, Object> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }

        String mnemonic = StringUtils.trim((String) body.get("mnemonic"));
        String chainId = StringUtils.blankToDefault(StringUtils.trim((String) body.get("chainId")), "cfx");
        if (StringUtils.isBlank(mnemonic)) {
            return R.fail("助记词不能为空");
        }
        if (!MnemonicUtils.validateMnemonic(mnemonic)) {
            return R.fail("助记词格式不正确");
        }

        try {
            List<Map<String, Object>> previews = new java.util.ArrayList<>();
            for (int i = 0; i < MNEMONIC_PREVIEW_LIMIT; i++) {
                Credentials credentials = deriveCredentialsFromMnemonic(mnemonic, i);
                String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
                DerivedWalletAddresses addresses = deriveWalletAddresses(privateKey);
                String selectedAddress = resolveAddressByChainId(chainId, addresses);

                Map<String, Object> item = new HashMap<>();
                item.put("deriveIndex", i);
                item.put("derivePath", buildDerivationPathLabel(i));
                item.put("selectedAddress", selectedAddress);
                item.put("ethAddress", addresses.ethAddress());
                item.put("cfxAddress", addresses.cfxAddress());
                item.put("cfxTestAddress", addresses.cfxTestAddress());
                item.put("alreadyImported", walletExistsForUser(userId, selectedAddress, chainId));
                previews.add(item);
            }
            return R.ok(previews);
        } catch (Exception e) {
            return R.fail("助记词预览失败");
        }
    }

    @PostMapping("/wallet/import")
    public R<Map<String, String>> importWallet(@RequestBody Map<String, Object> body) {
        WalletAccount account = new WalletAccount();
        account.setChainId(StringUtils.blankToDefault(StringUtils.trim((String) body.get("chainId")), "cfx"));
        account.setName(StringUtils.trim((String) body.get("name")));
        account.setPrivateKey(StringUtils.trim((String) body.get("privateKey")));
        account.setMnemonic(StringUtils.trim((String) body.get("mnemonic")));

        R<Void> nameErr = validateOptionalWalletName(account.getName());
        if (nameErr != null) {
            return R.fail(nameErr.getMsg());
        }
        if (StringUtils.isNotBlank(account.getName())) {
            account.setName(account.getName().trim());
        }

        String privateKey = account.getPrivateKey();
        String mnemonic = account.getMnemonic();
        Credentials credentials = null;
        int deriveIndex = 0;

        try {
            if (mnemonic != null && !mnemonic.trim().isEmpty()) {
                if (!MnemonicUtils.validateMnemonic(mnemonic)) {
                    return R.fail("Invalid mnemonic or private key");
                }
                deriveIndex = parseDeriveIndex(body.get("deriveIndex"));
                credentials = deriveCredentialsFromMnemonic(mnemonic, deriveIndex);
                privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
            } else if (privateKey != null && !privateKey.trim().isEmpty()) {
                credentials = Credentials.create(privateKey);
            } else {
                return R.fail("Private key or mnemonic is required");
            }
        } catch (Exception e) {
            return R.fail("Invalid mnemonic or private key");
        }

        DerivedWalletAddresses addresses = deriveWalletAddresses(privateKey);
        String ethAddress = addresses.ethAddress();
        String cfxAddress = addresses.cfxAddress();
        String cfxTestAddress = addresses.cfxTestAddress();

        // Get user id from auth context
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }

        account.setUserId(userId);
        account.setPrivateKey(privateKey);
        account.setMnemonic(mnemonic);
        account.setHasPrivateKey(1);
        account.setHasMnemonic((mnemonic != null && !mnemonic.trim().isEmpty()) ? 1 : 0);

        // Determine the address based on chainId selected by user
        String chainId = account.getChainId();
        account.setAddress(resolveAddressByChainId(chainId, addresses));

        // Check if the current user already has this wallet address on this chain
        if (walletExistsForUser(userId, account.getAddress(), account.getChainId())) {
            return R.fail("该钱包已导入，请勿重复恢复");
        }

        account.setIsNewUserCreated(0);
        accountService.insertByBo(account);
        WalletAppRedisCache.evictWalletList(userId);

        Map<String, String> result = new HashMap<>();
        result.put("ethAddress", ethAddress);
        result.put("cfxAddress", cfxAddress);
        result.put("cfxTestAddress", cfxTestAddress);
        result.put("selectedAddress", account.getAddress());
        result.put("deriveIndex", String.valueOf(deriveIndex));
        result.put("derivePath", buildDerivationPathLabel(deriveIndex));
        return R.ok("Wallet imported successfully", result);
    }

    @GetMapping("/wallet/export")
    public R<Map<String, String>> exportWallet(
        @RequestParam(value = "accountId", required = false) Long accountId,
        @RequestParam(value = "address", required = false) String address,
        @RequestParam(value = "exportType", required = false) String exportType
    ) {
        // Get user id from auth context
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }

        WalletAccount query = new WalletAccount();
        query.setUserId(userId);
        if (accountId != null) {
            query.setAccountId(accountId);
        } else if (address != null && !address.trim().isEmpty()) {
            query.setAddress(address.trim());
        } else {
            return R.fail("Missing wallet identifier");
        }

        List<WalletAccount> accounts = accountService.queryList(query);
        if (accounts == null || accounts.isEmpty()) {
            return R.fail("Wallet not found");
        }

        WalletAccount account = accounts.get(0);

        String type = (exportType == null || exportType.trim().isEmpty())
            ? "privateKey"
            : exportType.trim();

        Map<String, String> res = new HashMap<>();
        res.put("address", account.getAddress());

        boolean provisioned = isAppProvisionedDefaultWallet(account);

//        if (provisioned) {
//            return R.fail("主钱包不支持导出");
//        }

        if ("mnemonic".equalsIgnoreCase(type)) {
            boolean allowMnemonic = (account.getHasMnemonic() != null && account.getHasMnemonic() == 1);
            if (!allowMnemonic) {
                return R.fail("Current wallet does not support mnemonic export");
            }
            String mnemonic = account.getMnemonic();
            if (mnemonic == null || mnemonic.trim().isEmpty()) {
                return R.fail("Mnemonic not found in database. Original mnemonic cannot be recovered from private key.");
            }
            res.put("mnemonic", mnemonic);
        } else {
            boolean allowPrivateKey = (account.getHasPrivateKey() != null && account.getHasPrivateKey() == 1);
//            if (!allowPrivateKey) {
//                return R.fail("Current wallet does not support private key export");
//            }
            if (account.getPrivateKey() == null || account.getPrivateKey().trim().isEmpty()) {
                return R.fail("Private key not found in database");
            }
            res.put("privateKey", account.getPrivateKey());
        }

        return R.ok(res);
    }

    @GetMapping("/wallet/list")
    public R<List<Map<String, Object>>> getWalletList() {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }

        String languageTag = currentLanguageTag();
        boolean englishMode = isEnglishLanguage(languageTag);
        String cacheKey = WalletAppRedisCache.walletListKey(userId, "all", languageTag);

        List<Map<String, Object>> cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return R.ok(cached);
        }

        WalletAccount query = new WalletAccount();
        query.setUserId(userId);

        List<WalletAccount> accounts = accountService.queryList(query);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (WalletAccount acc : accounts) {
            Map<String, Object> map = new HashMap<>();
            // Long 雪花 ID 超出 JS 安全整数，JSON 数字会被前端截断；统一用字符串
            map.put("accountId", acc.getAccountId() != null ? acc.getAccountId().toString() : null);
            map.put("userId", acc.getUserId() != null ? acc.getUserId().toString() : null);
            map.put("address", acc.getAddress());
            map.put("chainId", acc.getChainId());
            map.put("name", localizeWalletName(acc.getName(), englishMode));
            boolean hasPrivateKeyFlag = acc.getHasPrivateKey() != null && acc.getHasPrivateKey() == 1;
            boolean hasMnemonicFlag = acc.getHasMnemonic() != null && acc.getHasMnemonic() == 1;
            boolean provisioned = isAppProvisionedDefaultWallet(acc);
            boolean newUserCreatedDb = acc.getIsNewUserCreated() != null && acc.getIsNewUserCreated() == 1;
            map.put("hasPrivateKey", hasPrivateKeyFlag);
            map.put("hasMnemonic", hasMnemonicFlag);
            map.put("newUserCreatedDb", newUserCreatedDb);
            map.put("isNewUserCreated", provisioned);
            map.put("canExportPrivateKey", hasPrivateKeyFlag && !provisioned);
            map.put("canExportMnemonic", hasMnemonicFlag && !provisioned);
            map.put("canRemove", walletMayBeRemovedByUser(acc));
            result.add(map);
        }
        RedisUtils.setCacheObject(cacheKey, result, WalletAppRedisCache.ttl());
        return R.ok(result);
    }

    @GetMapping("/wallet/list2")
    public R<List<Map<String, Object>>> getWalletList2() {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }

        String languageTag = currentLanguageTag();
        boolean englishMode = isEnglishLanguage(languageTag);
        String cacheKey = WalletAppRedisCache.walletListKey(userId, "non_system", languageTag);

        List<Map<String, Object>> cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return R.ok(cached);
        }

        WalletAccount query = new WalletAccount();
        query.setUserId(userId);
        query.setIsNewUserCreated(0);

        List<WalletAccount> accounts = accountService.queryList(query);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (WalletAccount acc : accounts) {
            Map<String, Object> map = new HashMap<>();
            // Long 雪花 ID 超出 JS 安全整数，JSON 数字会被前端截断；统一用字符串
            map.put("accountId", acc.getAccountId() != null ? acc.getAccountId().toString() : null);
            map.put("userId", acc.getUserId() != null ? acc.getUserId().toString() : null);
            map.put("address", acc.getAddress());
            map.put("chainId", acc.getChainId());
            map.put("name", localizeWalletName(acc.getName(), englishMode));
            boolean hasPrivateKeyFlag = acc.getHasPrivateKey() != null && acc.getHasPrivateKey() == 1;
            boolean hasMnemonicFlag = acc.getHasMnemonic() != null && acc.getHasMnemonic() == 1;
            boolean provisioned = isAppProvisionedDefaultWallet(acc);
            boolean newUserCreatedDb = acc.getIsNewUserCreated() != null && acc.getIsNewUserCreated() == 1;
            map.put("hasPrivateKey", hasPrivateKeyFlag);
            map.put("hasMnemonic", hasMnemonicFlag);
            map.put("newUserCreatedDb", newUserCreatedDb);
            map.put("isNewUserCreated", provisioned);
            map.put("canExportPrivateKey", hasPrivateKeyFlag && !provisioned);
            map.put("canExportMnemonic", hasMnemonicFlag && !provisioned);
            map.put("canRemove", walletMayBeRemovedByUser(acc));
            result.add(map);
        }
        RedisUtils.setCacheObject(cacheKey, result, WalletAppRedisCache.ttl());
        return R.ok(result);
    }

    @DeleteMapping("/wallet")
    public R<Void> removeWallet(@RequestParam("accountId") Long accountId) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        if (accountId == null) {
            return R.fail("Missing accountId");
        }
        WalletAccount account = accountService.queryById(accountId);
        if (account == null || !userId.equals(account.getUserId())) {
            return R.fail("Wallet not found");
        }
        if (!walletMayBeRemovedByUser(account)) {
            return R.fail("仅可移除用户自行创建/导入的钱包，系统注册下发的钱包不可移除");
        }
        boolean removed = accountService.deleteById(accountId);
        if (removed) {
            WalletAppRedisCache.evictWalletList(userId);
        }
        return removed ? R.ok() : R.fail("移除失败");
    }

    @PutMapping("/settings")
    public R<Void> updateSettings(@RequestBody WalletUserPreference pref) {
        // TODO: update pref
        preferenceService.updateByBo(pref);
        return R.ok();
    }

    @PostMapping("/biometric/enable")
    public R<Map<String, String>> enableBiometricLogin(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }

        String deviceId = body.get("deviceId");
        String deviceName = body.get("deviceName");
        String platform = body.get("platform");
        String authMode = StringUtils.isBlank(body.get("authMode")) ? "fingerPrint" : body.get("authMode");
        if (StringUtils.isBlank(deviceId)) {
            return R.fail("deviceId can not be empty");
        }

        String rawToken = RandomUtil.randomString(64);
        String credentialHash = SecureUtil.sha256(rawToken);
        AppUserBiometricCredential credential = biometricCredentialMapper.selectOne(
            new LambdaQueryWrapper<AppUserBiometricCredential>()
                .eq(AppUserBiometricCredential::getUserId, userId)
                .eq(AppUserBiometricCredential::getDeviceId, deviceId)
                .eq(AppUserBiometricCredential::getAuthMode, authMode)
                .last("limit 1")
        );
        if (credential == null) {
            credential = new AppUserBiometricCredential();
            credential.setUserId(userId);
            credential.setCreateTime(new Date());
        }

        credential.setEmail(appUser.getEmail());
        credential.setDeviceId(deviceId);
        credential.setDeviceName(deviceName);
        credential.setPlatform(platform);
        credential.setAuthMode(authMode);
        credential.setCredentialHash(credentialHash);
        credential.setStatus("0");
        credential.setLastUsedTime(null);
        credential.setExpireTime(null);
        credential.setUpdateTime(new Date());

        if (credential.getCredentialId() == null) {
            biometricCredentialMapper.insert(credential);
        } else {
            biometricCredentialMapper.updateById(credential);
        }

        Map<String, String> result = new HashMap<>();
        result.put("biometricToken", rawToken);
        return R.ok(result);
    }

    @GetMapping("/tradePwd/status")
    public R<Map<String, Boolean>> tradePwdStatus() {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        Map<String, Boolean> m = new HashMap<>();
        m.put("hasTradePassword", appUser != null && StringUtils.isNotBlank(appUser.getTradePassword()));
        return R.ok(m);
    }

    /**
     * 首次设置交易密码（需邮箱验证码，与当前账号邮箱一致）
     */
    @PostMapping("/tradePwd/set")
    public R<Void> setTradePassword(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        if (StringUtils.isNotBlank(appUser.getTradePassword())) {
            return R.fail("已设置交易密码，请使用修改接口");
        }
        String tradePassword = body.get("tradePassword");
        String email = body.get("email");
        String code = body.get("code");
        if (StringUtils.isAnyBlank(tradePassword, email, code)) {
            return R.fail("交易密码、邮箱与验证码不能为空");
        }
        if (tradePassword.length() < 6) {
            return R.fail("交易密码至少6位");
        }
        if (StringUtils.isBlank(appUser.getEmail())) {
            return R.fail("请先绑定邮箱");
        }
        if (!StringUtils.equals(appUser.getEmail(), email.trim())) {
            return R.fail("邮箱不匹配");
        }
        String cacheCode = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(cacheCode)) {
            return R.fail("验证码已过期");
        }
        if (!StringUtils.equals(cacheCode, code)) {
            return R.fail("验证码错误");
        }
        appUser.setTradePassword(AppUserPasswords.encode(tradePassword));
        boolean ok = appUserService.updateUser(appUser);
        if (ok) {
            WalletAppRedisCache.evictProfile(userId);
        }
        return ok ? R.ok() : R.fail("保存失败");
    }

    /**
     * 修改交易密码（需原交易密码）
     */
    @PutMapping("/tradePwd/update")
    public R<Void> updateTradePassword(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        if (StringUtils.isBlank(appUser.getTradePassword())) {
            return R.fail("尚未设置交易密码");
        }
        String oldTradePassword = body.get("oldTradePassword");
        String newTradePassword = body.get("newTradePassword");
        if (StringUtils.isAnyBlank(oldTradePassword, newTradePassword)) {
            return R.fail("原密码与新密码不能为空");
        }
        if (newTradePassword.length() < 6) {
            return R.fail("新交易密码至少6位");
        }
        if (!AppUserPasswords.matches(oldTradePassword, appUser.getTradePassword())) {
            return R.fail("原交易密码错误");
        }
        appUser.setTradePassword(AppUserPasswords.encode(newTradePassword));
        boolean ok = appUserService.updateUser(appUser);
        if (ok) {
            WalletAppRedisCache.evictProfile(userId);
        }
        return ok ? R.ok() : R.fail("更新失败");
    }

    /**
     * 发送前短时解锁：交易密码或本机已绑定的指纹凭证（biometricToken）
     */
    @PostMapping("/tradePwd/unlock")
    public R<Map<String, Object>> unlockTradeSession(@RequestBody Map<String, String> body) {
        Long userId = org.dromara.common.satoken.utils.LoginHelper.getUserId();
        if (userId == null) {
            return R.fail("User not logged in");
        }
        AppUser appUser = appUserService.selectUserById(userId);
        if (appUser == null) {
            return R.fail("App user not found");
        }
        if (StringUtils.isBlank(appUser.getTradePassword())) {
            return R.fail("请先在个人中心设置交易密码");
        }
        String biometricToken = body.get("biometricToken");
        String tradePassword = body.get("tradePassword");
        boolean verified = false;
        if (StringUtils.isNotBlank(biometricToken)) {
            String credentialHash = SecureUtil.sha256(biometricToken);
            AppUserBiometricCredential credential = biometricCredentialMapper.selectOne(
                new LambdaQueryWrapper<AppUserBiometricCredential>()
                    .eq(AppUserBiometricCredential::getCredentialHash, credentialHash)
                    .eq(AppUserBiometricCredential::getUserId, userId)
                    .eq(AppUserBiometricCredential::getStatus, "0")
                    .last("limit 1")
            );
            if (credential != null
                && (credential.getExpireTime() == null || credential.getExpireTime().after(new Date()))) {
                verified = true;
                credential.setLastUsedTime(new Date());
                biometricCredentialMapper.updateById(credential);
            }
        } else if (StringUtils.isNotBlank(tradePassword)) {
            verified = AppUserPasswords.matches(tradePassword, appUser.getTradePassword());
        }
        if (!verified) {
            return R.fail("交易验证失败");
        }
        String sessionToken = RandomUtil.randomString(32);
        RedisUtils.setCacheObject(
            WalletAppRedisCache.tradeUnlockKey(userId, sessionToken),
            Boolean.TRUE,
            WalletAppRedisCache.tradeUnlockTtl()
        );
        Map<String, Object> res = new HashMap<>();
        res.put("tradeSessionToken", sessionToken);
        res.put("expiresIn", WalletAppRedisCache.tradeUnlockTtl().getSeconds());
        return R.ok(res);
    }
}
