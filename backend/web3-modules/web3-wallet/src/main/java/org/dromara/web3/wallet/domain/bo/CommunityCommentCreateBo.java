package org.dromara.web3.wallet.domain.bo;

import lombok.Data;

@Data
public class CommunityCommentCreateBo {

    private String content;

    private String parentId;

    private String replyTo;
}
