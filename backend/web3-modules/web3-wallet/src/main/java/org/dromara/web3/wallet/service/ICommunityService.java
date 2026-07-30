package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.bo.CommunityCommentCreateBo;
import org.dromara.web3.wallet.domain.bo.CommunityPostCreateBo;
import org.dromara.web3.wallet.domain.vo.CommunityCommentPageVo;
import org.dromara.web3.wallet.domain.vo.CommunityPostVo;
import org.dromara.web3.wallet.domain.vo.CommunityToggleVo;
import org.dromara.web3.wallet.domain.vo.CommunityTopicVo;

import java.util.List;

public interface ICommunityService {

    List<CommunityTopicVo> listTopics(boolean composeOnly, String keyword, Integer limit);

    List<CommunityPostVo> listPosts(Long currentUserId, String category, String keyword);

    CommunityPostVo getPostDetail(Long currentUserId, Long postId);

    CommunityCommentPageVo listComments(Long currentUserId, Long postId, String sortType, long pageNum, long pageSize);

    CommunityPostVo createPost(Long currentUserId, CommunityPostCreateBo bo);

    CommunityPostVo addComment(Long currentUserId, Long postId, CommunityCommentCreateBo bo);

    CommunityToggleVo togglePostLike(Long currentUserId, Long postId);

    CommunityToggleVo togglePostFavorite(Long currentUserId, Long postId);

    CommunityToggleVo toggleFollow(Long currentUserId, Long targetUserId);

    void increaseShareCount(Long postId);
}
