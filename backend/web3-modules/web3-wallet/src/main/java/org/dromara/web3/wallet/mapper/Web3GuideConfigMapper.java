package org.dromara.web3.wallet.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.web3.wallet.domain.Web3GuideConfig;

@InterceptorIgnore(tenantLine = "true")
public interface Web3GuideConfigMapper extends BaseMapperPlus<Web3GuideConfig, Web3GuideConfig> {
}
