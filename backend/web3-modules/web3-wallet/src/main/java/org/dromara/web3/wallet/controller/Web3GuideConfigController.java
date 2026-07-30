package org.dromara.web3.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.web3.wallet.domain.Web3GuideConfig;
import org.dromara.web3.wallet.domain.vo.Web3GuideConfigVo;
import org.dromara.web3.wallet.service.IWeb3GuideConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/guide/config")
public class Web3GuideConfigController {

    private final IWeb3GuideConfigService guideConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有语言的配置列表
     */
    @SaCheckPermission("web3:guideConfig:list")
    @GetMapping("/list")
    public R<List<Web3GuideConfigVo>> list() {
        return R.ok(guideConfigService.listAll().stream()
            .map(c -> Web3GuideConfigVo.from(c, objectMapper))
            .toList());
    }

    /**
     * 获取指定语言的配置（列表字段为 JSON 数组，非字符串）
     */
    @GetMapping("/{language}")
    public R<Web3GuideConfigVo> getInfo(@PathVariable("language") String language) {
        return R.ok(Web3GuideConfigVo.from(guideConfigService.getConfigByLanguage(language), objectMapper));
    }

    /**
     * 保存或更新配置
     */
    @SaCheckPermission("web3:guideConfig:edit")
    @PostMapping
    public R<Void> save(@RequestBody Web3GuideConfig config) {
        return guideConfigService.saveOrUpdateConfig(config) ? R.ok() : R.fail();
    }
}
