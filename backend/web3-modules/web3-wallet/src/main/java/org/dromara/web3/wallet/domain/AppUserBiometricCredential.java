package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * APP 用户生物登录凭证对象 app_user_biometric_credential
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user_biometric_credential")
public class AppUserBiometricCredential extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "credential_id")
    private Long credentialId;

    private Long userId;

    private String email;

    private String deviceId;

    private String deviceName;

    private String platform;

    private String authMode;

    private String credentialHash;

    private String status;

    private Date lastUsedTime;

    private Date expireTime;
}
