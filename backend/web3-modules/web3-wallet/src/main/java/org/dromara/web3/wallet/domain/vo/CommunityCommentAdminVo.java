package org.dromara.web3.wallet.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CommunityCommentAdminVo {

    private Long commentId;

    private Long postId;

    private String postTitle;

    private Long userId;

    private String authorName;

    private Long parentCommentId;

    private Long replyToUserId;

    private String replyToUserName;

    private String content;

    private Integer likeCount;

    private String status;

    private Date createTime;
}
