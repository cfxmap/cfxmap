package org.dromara.web3.wallet.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityCommentPageVo {

    private Long total = 0L;

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private Boolean hasMore = false;

    private List<CommunityCommentVo> list = new ArrayList<>();
}
