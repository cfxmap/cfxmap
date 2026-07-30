package org.dromara.web3.wallet.domain.bo;

import lombok.Data;

@Data
public class CommunityTopicAdminBo {

    private Long topicId;

    private String topicKey;

    private String topicName;

    private String tone;

    private Integer sortOrder;

    private String status;
}
