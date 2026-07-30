package org.dromara.web3.wallet.domain.bo;

import lombok.Data;

@Data
public class CommunityCommentQueryBo {

    private Long postId;

    private String authorName;

    private String content;

    private String status;
}
