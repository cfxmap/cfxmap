package org.dromara.web3.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.web3.wallet.domain.Web3AppVersionConfig;
import org.dromara.web3.wallet.domain.vo.Web3AppVersionPublicVo;
import org.dromara.web3.wallet.service.IWeb3AppVersionConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * APP 版本与官网配置：公开 GET 供客户端校验；POST 需后台权限。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/app/version")
public class Web3AppVersionConfigController {

    private final IWeb3AppVersionConfigService appVersionConfigService;

    @SaIgnore
    @GetMapping
    public R<Web3AppVersionPublicVo> getPublic() {
        return R.ok(Web3AppVersionPublicVo.from(appVersionConfigService.getSingleton()));
    }

    @SaCheckPermission("web3:appVersion:edit")
    @PostMapping
    public R<Void> save(@RequestBody Web3AppVersionPublicVo body) {
        Web3AppVersionConfig c = new Web3AppVersionConfig();
        if (body != null) {
            c.setCurrentVersion(body.getCurrentVersion());
            c.setOfficialUrl(body.getOfficialUrl());
            c.setApkUrl(body.getApkUrl());
        }
        return appVersionConfigService.saveSingleton(c) ? R.ok() : R.fail("保存失败：请检查版本号非空且官网地址为 http(s) 链接");
    }
}
