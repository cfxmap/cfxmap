package org.dromara.web3.wallet.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityCommentVo {

    private String id;

    private String author;

    private String badge;

    private String content;

    private Integer likes;

    private Long createdAt;

    private String replyTo;

    private List<CommunityCommentVo> replies = new ArrayList<>();
}
