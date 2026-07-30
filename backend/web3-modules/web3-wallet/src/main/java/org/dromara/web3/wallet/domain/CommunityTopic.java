package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_topic")
public class CommunityTopic extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long topicId;

    private String topicKey;

    private String topicName;

    private String tone;

    private Integer sortOrder;

    private String status;
}
