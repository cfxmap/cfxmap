package org.dromara.web3.wallet.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityPostVo {

    private String id;

    private String authorUserId;

    private String title;

    private String content;

    private String author;

    private String authorBadge;

    private String location;

    private String topicId;

    private String topicLabel;

    private Long publishedAt;

    private Boolean featured;

    private Boolean liked;

    private Boolean saved;

    private Boolean followingAuthor;

    private Boolean owner;

    private Integer viewCount;

    private List<String> imageList = new ArrayList<>();

    private PostStats stats = new PostStats();

    private List<CommunityCommentVo> comments = new ArrayList<>();

    @Data
    public static class PostStats {
        private Integer comments = 0;
        private Integer likes = 0;
        private Integer saves = 0;
        private Integer shares = 0;
        private Integer views = 0;
    }
}
