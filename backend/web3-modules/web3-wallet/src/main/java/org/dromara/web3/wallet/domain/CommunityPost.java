package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_post")
public class CommunityPost extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long postId;

    private Long userId;

    private Long topicId;

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
}
