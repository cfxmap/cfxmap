package org.dromara.web3.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.CommunityComment;
import org.dromara.web3.wallet.domain.CommunityPost;
import org.dromara.web3.wallet.domain.bo.CommunityCommentAdminBo;
import org.dromara.web3.wallet.domain.bo.CommunityCommentQueryBo;
import org.dromara.web3.wallet.domain.vo.CommunityCommentAdminVo;
import org.dromara.web3.wallet.mapper.CommunityCommentMapper;
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
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/web3/community/comment")
public class WalletCommunityCommentManageController {

    private final CommunityCommentMapper commentMapper;
    private final CommunityPostMapper postMapper;
    private final CommunityAdminSupport adminSupport;

    @SaCheckPermission("web3:communityComment:list")
    @GetMapping("/list")
    public TableDataInfo<CommunityCommentAdminVo> list(CommunityCommentQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(bo.getPostId() != null, CommunityComment::getPostId, bo.getPostId());
        wrapper.like(StringUtils.isNotBlank(bo.getContent()), CommunityComment::getContent, bo.getContent());
        wrapper.eq(StringUtils.isNotBlank(bo.getStatus()), CommunityComment::getStatus, bo.getStatus());
        wrapper.orderByDesc(CommunityComment::getCreateTime, CommunityComment::getCommentId);
        List<CommunityComment> records = commentMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return TableDataInfo.build();
        }
        Set<Long> userIds = records.stream().map(CommunityComment::getUserId).collect(Collectors.toSet());
        userIds.addAll(records.stream().map(CommunityComment::getReplyToUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, AppUser> userMap = adminSupport.userMap(userIds);
        Map<Long, CommunityPost> postMap = adminSupport.postMap(records.stream().map(CommunityComment::getPostId).collect(Collectors.toSet()));
        List<CommunityCommentAdminVo> rows = records.stream()
            .map(item -> toCommentVo(item, postMap, userMap))
            .filter(item -> matchCommentVo(item, bo))
            .toList();
        return TableDataInfo.build(rows, pageQuery.build());
    }

    @SaCheckPermission("web3:communityComment:query")
    @GetMapping("/{commentId}")
    public R<CommunityCommentAdminVo> getInfo(@PathVariable Long commentId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return R.fail("评论不存在");
        }
        Map<Long, AppUser> userMap = adminSupport.userMap(Stream.of(comment.getUserId(), comment.getReplyToUserId()).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, CommunityPost> postMap = adminSupport.postMap(Set.of(comment.getPostId()));
        return R.ok(toCommentVo(comment, postMap, userMap));
    }

    @SaCheckPermission("web3:communityComment:edit")
    @PutMapping
    public R<Void> edit(@RequestBody CommunityCommentAdminBo bo) {
        if (bo.getCommentId() == null) {
            return R.fail("评论ID不能为空");
        }
        CommunityComment comment = commentMapper.selectById(bo.getCommentId());
        if (comment == null) {
            return R.fail("评论不存在");
        }
        if (StringUtils.isBlank(bo.getContent())) {
            return R.fail("评论内容不能为空");
        }
        comment.setContent(bo.getContent().trim());
        comment.setStatus(StringUtils.defaultIfBlank(bo.getStatus(), "0"));
        commentMapper.updateById(comment);
        return R.ok();
    }

    @SaCheckPermission("web3:communityComment:remove")
    @DeleteMapping("/{commentIds}")
    public R<Void> remove(@PathVariable Long[] commentIds) {
        if (commentIds == null || commentIds.length == 0) {
            return R.fail("请选择要删除的评论");
        }
        List<Long> idList = Arrays.asList(commentIds);
        List<CommunityComment> deleting = commentMapper.selectBatchIds(idList);
        if (!deleting.isEmpty()) {
            Map<Long, Long> countMap = deleting.stream().collect(Collectors.groupingBy(CommunityComment::getPostId, Collectors.counting()));
            countMap.forEach((postId, count) -> {
                CommunityPost post = postMapper.selectById(postId);
                if (post != null) {
                    int next = Math.max(0, (post.getCommentCount() == null ? 0 : post.getCommentCount()) - count.intValue());
                    post.setCommentCount(next);
                    postMapper.updateById(post);
                }
            });
        }
        commentMapper.deleteBatchIds(idList);
        return R.ok();
    }

    private boolean matchCommentVo(CommunityCommentAdminVo vo, CommunityCommentQueryBo bo) {
        if (StringUtils.isNotBlank(bo.getAuthorName()) && !StringUtils.containsIgnoreCase(vo.getAuthorName(), bo.getAuthorName())) {
            return false;
        }
        return true;
    }

    private CommunityCommentAdminVo toCommentVo(CommunityComment comment, Map<Long, CommunityPost> postMap, Map<Long, AppUser> userMap) {
        CommunityCommentAdminVo vo = new CommunityCommentAdminVo();
        CommunityPost post = postMap.get(comment.getPostId());
        vo.setCommentId(comment.getCommentId());
        vo.setPostId(comment.getPostId());
        vo.setPostTitle(post == null ? "-" : post.getTitle());
        vo.setUserId(comment.getUserId());
        vo.setAuthorName(adminSupport.displayName(userMap.get(comment.getUserId())));
        vo.setParentCommentId(comment.getParentCommentId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setReplyToUserName(adminSupport.displayName(userMap.get(comment.getReplyToUserId())));
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
