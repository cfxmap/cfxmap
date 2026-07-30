package org.dromara.web3.wallet.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.web3.wallet.domain.Web3AppVersionConfig;

@InterceptorIgnore(tenantLine = "true")
public interface Web3AppVersionConfigMapper extends BaseMapperPlus<Web3AppVersionConfig, Web3AppVersionConfig> {
}
