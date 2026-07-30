package org.dromara.web3.wallet.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CommunityTopicAdminVo {

    private Long topicId;

    private String topicKey;

    private String topicName;

    private String tone;

    private Integer sortOrder;

    private String status;

    private Date createTime;
}
