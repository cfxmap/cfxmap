package org.dromara.web3.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.CommunityPost;
import org.dromara.web3.wallet.domain.CommunityTopic;
import org.dromara.web3.wallet.domain.bo.CommunityPostAdminBo;
import org.dromara.web3.wallet.domain.bo.CommunityPostQueryBo;
import org.dromara.web3.wallet.domain.vo.CommunityPostAdminVo;
import org.dromara.web3.wallet.mapper.CommunityCommentMapper;
import org.dromara.web3.wallet.mapper.CommunityPostFavoriteMapper;
import org.dromara.web3.wallet.mapper.CommunityPostImageMapper;
import org.dromara.web3.wallet.mapper.CommunityPostLikeMapper;
import org.dromara.web3.wallet.mapper.CommunityPostMapper;
import org.dromara.web3.wallet.service.CommunityAdminSupport;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/web3/community/post")
public class WalletCommunityPostManageController {

    private final CommunityPostMapper postMapper;
    private final CommunityPostImageMapper postImageMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityPostLikeMapper likeMapper;
    private final CommunityPostFavoriteMapper favoriteMapper;
    private final CommunityAdminSupport adminSupport;

    @SaCheckPermission("web3:communityPost:list")
    @GetMapping("/list")
    public TableDataInfo<CommunityPostAdminVo> list(CommunityPostQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(bo.getTitle()), CommunityPost::getTitle, bo.getTitle());
        wrapper.eq(StringUtils.isNotBlank(bo.getStatus()), CommunityPost::getStatus, bo.getStatus());
        wrapper.eq(bo.getIsFeatured() != null, CommunityPost::getIsFeatured, bo.getIsFeatured());
        wrapper.orderByDesc(CommunityPost::getCreateTime, CommunityPost::getPostId);
        List<CommunityPost> records = postMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return TableDataInfo.build();
        }
        Map<Long, CommunityTopic> topicMap = adminSupport.topicMap(records.stream().map(CommunityPost::getTopicId).collect(Collectors.toSet()));
        Map<Long, AppUser> userMap = adminSupport.userMap(records.stream().map(CommunityPost::getUserId).collect(Collectors.toSet()));
        List<CommunityPostAdminVo> rows = records.stream()
            .map(item -> toPostVo(item, topicMap, userMap))
            .filter(item -> matchPostVo(item, bo))
            .toList();
        return TableDataInfo.build(rows, pageQuery.build());
    }

    @SaCheckPermission("web3:communityPost:query")
    @GetMapping("/{postId}")
    public R<CommunityPostAdminVo> getInfo(@PathVariable Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            return R.fail("帖子不存在");
        }
        Map<Long, CommunityTopic> topicMap = adminSupport.topicMap(Set.of(post.getTopicId()));
        Map<Long, AppUser> userMap = adminSupport.userMap(Set.of(post.getUserId()));
        return R.ok(toPostVo(post, topicMap, userMap));
    }

    @SaCheckPermission("web3:communityPost:edit")
    @PutMapping
    public R<Void> edit(@RequestBody CommunityPostAdminBo bo) {
        if (bo.getPostId() == null) {
            return R.fail("帖子ID不能为空");
        }
        CommunityPost post = postMapper.selectById(bo.getPostId());
        if (post == null) {
            return R.fail("帖子不存在");
        }
        if (StringUtils.isAnyBlank(bo.getTitle(), bo.getContent())) {
            return R.fail("标题和正文不能为空");
        }
        post.setTopicId(bo.getTopicId() == null ? post.getTopicId() : bo.getTopicId());
        post.setTitle(bo.getTitle().trim());
        post.setContent(bo.getContent().trim());
        post.setLocation(StringUtils.defaultIfBlank(bo.getLocation(), "社区里"));
        post.setIsFeatured(bo.getIsFeatured() == null ? 0 : bo.getIsFeatured());
        post.setStatus(StringUtils.defaultIfBlank(bo.getStatus(), "0"));
        postMapper.updateById(post);
        return R.ok();
    }

    @SaCheckPermission("web3:communityPost:remove")
    @DeleteMapping("/{postIds}")
    public R<Void> remove(@PathVariable Long[] postIds) {
        if (postIds == null || postIds.length == 0) {
            return R.fail("请选择要删除的帖子");
        }
        List<Long> idList = Arrays.asList(postIds);
        postImageMapper.delete(new LambdaQueryWrapper<org.dromara.web3.wallet.domain.CommunityPostImage>().in(org.dromara.web3.wallet.domain.CommunityPostImage::getPostId, idList));
        commentMapper.delete(new LambdaQueryWrapper<org.dromara.web3.wallet.domain.CommunityComment>().in(org.dromara.web3.wallet.domain.CommunityComment::getPostId, idList));
        likeMapper.delete(new LambdaQueryWrapper<org.dromara.web3.wallet.domain.CommunityPostLike>().in(org.dromara.web3.wallet.domain.CommunityPostLike::getPostId, idList));
        favoriteMapper.delete(new LambdaQueryWrapper<org.dromara.web3.wallet.domain.CommunityPostFavorite>().in(org.dromara.web3.wallet.domain.CommunityPostFavorite::getPostId, idList));
        postMapper.deleteBatchIds(idList);
        return R.ok();
    }

    private boolean matchPostVo(CommunityPostAdminVo vo, CommunityPostQueryBo bo) {
        if (StringUtils.isNotBlank(bo.getAuthorName()) && !StringUtils.containsIgnoreCase(vo.getAuthorName(), bo.getAuthorName())) {
            return false;
        }
        if (StringUtils.isNotBlank(bo.getTopicKey()) && !Objects.equals(vo.getTopicKey(), bo.getTopicKey())) {
            return false;
        }
        return true;
    }

    private CommunityPostAdminVo toPostVo(CommunityPost post, Map<Long, CommunityTopic> topicMap, Map<Long, AppUser> userMap) {
        CommunityTopic topic = topicMap.get(post.getTopicId());
        CommunityPostAdminVo vo = new CommunityPostAdminVo();
        vo.setPostId(post.getPostId());
        vo.setUserId(post.getUserId());
        vo.setAuthorName(adminSupport.displayName(userMap.get(post.getUserId())));
        vo.setTopicId(post.getTopicId());
        vo.setTopicName(topic == null ? "-" : topic.getTopicName());
        vo.setTopicKey(topic == null ? "" : topic.getTopicKey());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setLocation(post.getLocation());
        vo.setIsFeatured(post.getIsFeatured());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setSaveCount(post.getSaveCount());
        vo.setShareCount(post.getShareCount());
        vo.setViewCount(post.getViewCount());
        vo.setStatus(post.getStatus());
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }
}
