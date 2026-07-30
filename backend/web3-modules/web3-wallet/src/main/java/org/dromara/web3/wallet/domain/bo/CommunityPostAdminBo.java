package org.dromara.web3.wallet.domain.bo;

import lombok.Data;

@Data
public class CommunityPostAdminBo {

    private Long postId;

    private Long topicId;

    private String title;

    private String content;

    private String location;

    private Integer isFeatured;

    private String status;
}
