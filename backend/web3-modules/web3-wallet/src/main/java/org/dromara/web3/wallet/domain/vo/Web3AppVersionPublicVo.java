package org.dromara.web3.wallet.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.web3.wallet.domain.Web3AppVersionConfig;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Web3AppVersionPublicVo {

    private String currentVersion;
    private String officialUrl;
    private String apkUrl;

    public static Web3AppVersionPublicVo from(Web3AppVersionConfig c) {
        if (c == null) {
            return Web3AppVersionPublicVo.builder()
                .currentVersion("")
                .officialUrl("")
                .apkUrl("")
                .build();
        }
        return Web3AppVersionPublicVo.builder()
            .currentVersion(c.getCurrentVersion() == null ? "" : c.getCurrentVersion().trim())
            .officialUrl(c.getOfficialUrl() == null ? "" : c.getOfficialUrl().trim())
            .apkUrl(c.getApkUrl() == null ? "" : c.getApkUrl().trim())
            .build();
    }
}
