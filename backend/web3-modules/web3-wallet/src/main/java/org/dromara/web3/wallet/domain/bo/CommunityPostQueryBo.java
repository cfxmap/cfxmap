package org.dromara.web3.wallet.domain.bo;

import lombok.Data;

@Data
public class CommunityPostQueryBo {

    private String title;

    private String topicKey;

    private String authorName;

    private String status;

    private Integer isFeatured;
}
