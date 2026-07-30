package org.dromara.web3.wallet.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CommunityPostAdminVo {

    private Long postId;

    private Long userId;

    private String authorName;

    private Long topicId;

    private String topicName;

    private String topicKey;

    private String title;

    private String content;

    private String location;

    private Integer isFeatured;

    private Integer likeCount;

    private Integer commentCount;

    private Integer saveCount;

    private Integer shareCount;

    private Integer viewCount;

    private String status;

    private Date createTime;
}
