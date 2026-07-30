package org.dromara.web3.wallet.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.CommunityComment;
import org.dromara.web3.wallet.domain.CommunityPost;
import org.dromara.web3.wallet.domain.CommunityPostFavorite;
import org.dromara.web3.wallet.domain.CommunityPostImage;
import org.dromara.web3.wallet.domain.CommunityPostLike;
import org.dromara.web3.wallet.domain.CommunityTopic;
import org.dromara.web3.wallet.domain.CommunityUserFollow;
import org.dromara.web3.wallet.domain.bo.CommunityCommentCreateBo;
import org.dromara.web3.wallet.domain.bo.CommunityPostCreateBo;
import org.dromara.web3.wallet.domain.vo.CommunityCommentPageVo;
import org.dromara.web3.wallet.domain.vo.CommunityCommentVo;
import org.dromara.web3.wallet.domain.vo.CommunityPostVo;
import org.dromara.web3.wallet.domain.vo.CommunityToggleVo;
import org.dromara.web3.wallet.domain.vo.CommunityTopicVo;
import org.dromara.web3.wallet.mapper.AppUserMapper;
import org.dromara.web3.wallet.mapper.CommunityCommentMapper;
import org.dromara.web3.wallet.mapper.CommunityPostFavoriteMapper;
import org.dromara.web3.wallet.mapper.CommunityPostImageMapper;
import org.dromara.web3.wallet.mapper.CommunityPostLikeMapper;
import org.dromara.web3.wallet.mapper.CommunityPostMapper;
import org.dromara.web3.wallet.mapper.CommunityTopicMapper;
import org.dromara.web3.wallet.mapper.CommunityUserFollowMapper;
import org.dromara.web3.wallet.service.ICommunityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements ICommunityService {

    private static final String TOPIC_RECOMMENDED = "recommended";
    private static final Set<String> SYSTEM_TABS = Set.of("following", "saved");
    private static final Pattern CONTENT_TOPIC_PATTERN = Pattern.compile("(^|\\n)#([^\\s#\\n]{1,20})(?=\\s|$)");

    private final CommunityTopicMapper topicMapper;
    private final CommunityPostMapper postMapper;
    private final CommunityPostImageMapper postImageMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityPostFavoriteMapper favoriteMapper;
    private final CommunityPostLikeMapper likeMapper;
    private final CommunityUserFollowMapper followMapper;
    private final AppUserMapper appUserMapper;

    @Override
    public List<CommunityTopicVo> listTopics(boolean composeOnly, String keyword, Integer limit) {
        List<CommunityTopic> topics = topicMapper.selectList(
            Wrappers.<CommunityTopic>lambdaQuery()
                .eq(CommunityTopic::getStatus, "0")
                .orderByDesc(CommunityTopic::getUpdateTime, CommunityTopic::getTopicId)
        );
        String safeKeyword = StringUtils.trimToEmpty(keyword).toLowerCase();
        int safeLimit = limit == null || limit <= 0 ? 12 : Math.min(limit, 20);
        return topics.stream()
            .filter(item -> !composeOnly || !SYSTEM_TABS.contains(item.getTopicKey()))
            .filter(item -> StringUtils.isBlank(safeKeyword)
                || StringUtils.containsIgnoreCase(item.getTopicName(), safeKeyword)
                || StringUtils.containsIgnoreCase(item.getTopicKey(), safeKeyword))
            .map(this::toTopicVo)
            .limit(safeLimit)
            .toList();
    }

    @Override
    public List<CommunityPostVo> listPosts(Long currentUserId, String category, String keyword) {
        String safeCategory = StringUtils.isBlank(category) ? TOPIC_RECOMMENDED : category.trim();
        Map<Long, CommunityTopic> topicMap = listTopicMap();
        List<CommunityPost> posts = postMapper.selectList(
            Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, "0")
                .orderByDesc(CommunityPost::getIsFeatured, CommunityPost::getCreateTime, CommunityPost::getPostId)
        );
        if (CollUtil.isEmpty(posts)) {
            return Collections.emptyList();
        }
        Set<Long> followingUserIds = currentUserId == null ? Collections.emptySet() : listFollowedUserIds(currentUserId);
        Set<Long> favoritePostIds = currentUserId == null ? Collections.emptySet() : listFavoritePostIds(currentUserId);
        Set<Long> likePostIds = currentUserId == null ? Collections.emptySet() : listLikePostIds(currentUserId);
        Map<Long, AppUser> userMap = listUserMap(posts.stream().map(CommunityPost::getUserId).collect(Collectors.toSet()));
        Map<Long, List<String>> imageMap = listPostImageMap(posts.stream().map(CommunityPost::getPostId).collect(Collectors.toSet()));

        return posts.stream()
            .filter(post -> matchCategory(post, safeCategory, followingUserIds, favoritePostIds, topicMap))
            .filter(post -> matchKeyword(post, keyword, userMap, topicMap))
            .map(post -> toPostVo(post, currentUserId, userMap, topicMap, imageMap, likePostIds, favoritePostIds, followingUserIds, false))
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityPostVo getPostDetail(Long currentUserId, Long postId) {
        CommunityPost post = requirePost(postId);
        return buildPostDetail(currentUserId, post);
    }

    @Override
    public CommunityCommentPageVo listComments(Long currentUserId, Long postId, String sortType, long pageNum, long pageSize) {
        CommunityPost post = requirePost(postId);
        List<CommunityCommentVo> comments = buildCommentTree(post.getPostId());
        comments.sort(resolveCommentComparator(sortType));
        long safePageNum = Math.max(1L, pageNum);
        long safePageSize = Math.max(1L, pageSize);
        int fromIndex = (int) Math.min((safePageNum - 1) * safePageSize, comments.size());
        int toIndex = (int) Math.min(fromIndex + safePageSize, comments.size());
        CommunityCommentPageVo vo = new CommunityCommentPageVo();
        vo.setTotal((long) comments.size());
        vo.setPageNum(safePageNum);
        vo.setPageSize(safePageSize);
        vo.setHasMore(toIndex < comments.size());
        vo.setList(new ArrayList<>(comments.subList(fromIndex, toIndex)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityPostVo createPost(Long currentUserId, CommunityPostCreateBo bo) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        String title = StringUtils.trim(bo.getTitle());
        String content = StringUtils.trim(bo.getContent());
        if (StringUtils.isAnyBlank(title, content)) {
            throw new IllegalArgumentException("标题和正文不能为空");
        }
        CommunityTopic topic = resolveTopicFromContent(content);
        CommunityPost post = new CommunityPost();
        post.setUserId(currentUserId);
        post.setTopicId(topic.getTopicId());
        post.setTitle(StringUtils.substring(title, 0, 24));
        post.setContent(StringUtils.substring(content, 0, 500));
        post.setLocation("刚刚发出");
        post.setIsFeatured(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setSaveCount(0);
        post.setShareCount(0);
        post.setViewCount(1);
        post.setStatus("0");
        postMapper.insert(post);
        savePostImages(post.getPostId(), bo.getImageList());
        return buildPostDetail(currentUserId, postMapper.selectById(post.getPostId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityPostVo addComment(Long currentUserId, Long postId, CommunityCommentCreateBo bo) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        CommunityPost post = requirePost(postId);
        String content = StringUtils.trim(bo.getContent());
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(currentUserId);
        comment.setContent(StringUtils.substring(content, 0, 240));
        comment.setLikeCount(0);
        comment.setStatus("0");
        if (StringUtils.isNotBlank(bo.getParentId())) {
            Long parentId = Long.valueOf(bo.getParentId());
            CommunityComment parent = requireComment(parentId);
            comment.setParentCommentId(parent.getCommentId());
            comment.setReplyToUserId(parent.getUserId());
        } else {
            comment.setParentCommentId(0L);
            comment.setReplyToUserId(0L);
        }
        commentMapper.insert(comment);
        post.setCommentCount(safeInt(post.getCommentCount()) + 1);
        postMapper.updateById(post);
        return buildPostDetail(currentUserId, postMapper.selectById(postId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityToggleVo togglePostLike(Long currentUserId, Long postId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        CommunityPost post = requirePost(postId);
        CommunityPostLike exists = likeMapper.selectOne(
            Wrappers.<CommunityPostLike>lambdaQuery()
                .eq(CommunityPostLike::getUserId, currentUserId)
                .eq(CommunityPostLike::getPostId, postId)
                .last("limit 1")
        );
        boolean active;
        if (exists == null) {
            CommunityPostLike like = new CommunityPostLike();
            like.setUserId(currentUserId);
            like.setPostId(postId);
            likeMapper.insert(like);
            post.setLikeCount(safeInt(post.getLikeCount()) + 1);
            active = true;
        } else {
            likeMapper.deleteById(exists.getLikeId());
            post.setLikeCount(Math.max(0, safeInt(post.getLikeCount()) - 1));
            active = false;
        }
        postMapper.updateById(post);
        CommunityToggleVo vo = new CommunityToggleVo();
        vo.setActive(active);
        vo.setCount(safeInt(post.getLikeCount()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityToggleVo togglePostFavorite(Long currentUserId, Long postId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        CommunityPost post = requirePost(postId);
        CommunityPostFavorite exists = favoriteMapper.selectOne(
            Wrappers.<CommunityPostFavorite>lambdaQuery()
                .eq(CommunityPostFavorite::getUserId, currentUserId)
                .eq(CommunityPostFavorite::getPostId, postId)
                .last("limit 1")
        );
        boolean active;
        if (exists == null) {
            CommunityPostFavorite favorite = new CommunityPostFavorite();
            favorite.setUserId(currentUserId);
            favorite.setPostId(postId);
            favoriteMapper.insert(favorite);
            post.setSaveCount(safeInt(post.getSaveCount()) + 1);
            active = true;
        } else {
            favoriteMapper.deleteById(exists.getFavoriteId());
            post.setSaveCount(Math.max(0, safeInt(post.getSaveCount()) - 1));
            active = false;
        }
        postMapper.updateById(post);
        CommunityToggleVo vo = new CommunityToggleVo();
        vo.setActive(active);
        vo.setCount(safeInt(post.getSaveCount()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityToggleVo toggleFollow(Long currentUserId, Long targetUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        if (targetUserId == null || Objects.equals(currentUserId, targetUserId)) {
            throw new IllegalArgumentException("关注对象不合法");
        }
        CommunityUserFollow exists = followMapper.selectOne(
            Wrappers.<CommunityUserFollow>lambdaQuery()
                .eq(CommunityUserFollow::getUserId, currentUserId)
                .eq(CommunityUserFollow::getFollowUserId, targetUserId)
                .last("limit 1")
        );
        boolean active;
        if (exists == null) {
            CommunityUserFollow follow = new CommunityUserFollow();
            follow.setUserId(currentUserId);
            follow.setFollowUserId(targetUserId);
            followMapper.insert(follow);
            active = true;
        } else {
            followMapper.deleteById(exists.getFollowId());
            active = false;
        }
        long count = followMapper.selectCount(
            Wrappers.<CommunityUserFollow>lambdaQuery()
                .eq(CommunityUserFollow::getFollowUserId, targetUserId)
        );
        CommunityToggleVo vo = new CommunityToggleVo();
        vo.setActive(active);
        vo.setCount((int) count);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseShareCount(Long postId) {
        CommunityPost post = requirePost(postId);
        post.setShareCount(safeInt(post.getShareCount()) + 1);
        postMapper.updateById(post);
    }

    private CommunityPostVo buildPostDetail(Long currentUserId, CommunityPost post) {
        Map<Long, CommunityTopic> topicMap = listTopicMap();
        Map<Long, AppUser> userMap = listUserMap(Set.of(post.getUserId()));
        Map<Long, List<String>> imageMap = listPostImageMap(Set.of(post.getPostId()));
        Set<Long> followingUserIds = currentUserId == null ? Collections.emptySet() : listFollowedUserIds(currentUserId);
        Set<Long> favoritePostIds = currentUserId == null ? Collections.emptySet() : listFavoritePostIds(currentUserId);
        Set<Long> likePostIds = currentUserId == null ? Collections.emptySet() : listLikePostIds(currentUserId);
        CommunityPostVo vo = toPostVo(post, currentUserId, userMap, topicMap, imageMap, likePostIds, favoritePostIds, followingUserIds, true);
        vo.setComments(buildCommentTree(post.getPostId()));
        return vo;
    }

    private List<CommunityCommentVo> buildCommentTree(Long postId) {
        List<CommunityComment> comments = commentMapper.selectList(
            Wrappers.<CommunityComment>lambdaQuery()
                .eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getStatus, "0")
                .orderByDesc(CommunityComment::getCreateTime, CommunityComment::getCommentId)
        );
        if (CollUtil.isEmpty(comments)) {
            return new ArrayList<>();
        }
        Set<Long> userIds = comments.stream().map(CommunityComment::getUserId).collect(Collectors.toSet());
        userIds.addAll(
            comments.stream()
                .map(CommunityComment::getReplyToUserId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toSet())
        );
        Map<Long, AppUser> userMap = listUserMap(userIds);
        Map<Long, List<CommunityComment>> grouped = comments.stream()
            .collect(Collectors.groupingBy(item -> safeLong(item.getParentCommentId())));
        List<CommunityComment> roots = new ArrayList<>(grouped.getOrDefault(0L, Collections.emptyList()));
        List<CommunityCommentVo> result = new ArrayList<>();
        for (CommunityComment root : roots) {
            CommunityCommentVo vo = toCommentVo(root, userMap);
            List<CommunityComment> replies = new ArrayList<>(grouped.getOrDefault(root.getCommentId(), Collections.emptyList()));
            replies.sort(Comparator.comparing(CommunityComment::getCreateTime, Comparator.nullsLast(Date::compareTo)));
            vo.setReplies(replies.stream().map(reply -> toCommentVo(reply, userMap)).toList());
            result.add(vo);
        }
        return result;
    }

    private CommunityCommentVo toCommentVo(CommunityComment comment, Map<Long, AppUser> userMap) {
        CommunityCommentVo vo = new CommunityCommentVo();
        vo.setId(String.valueOf(comment.getCommentId()));
        AppUser author = userMap.get(comment.getUserId());
        vo.setAuthor(displayName(author));
        vo.setBadge(safeLong(comment.getParentCommentId()) > 0 ? "" : (comment.getUserId() != null && comment.getUserId() > 0 ? "刚刚发言" : ""));
        vo.setContent(comment.getContent());
        vo.setLikes(safeInt(comment.getLikeCount()));
        vo.setCreatedAt(comment.getCreateTime() == null ? System.currentTimeMillis() : comment.getCreateTime().getTime());
        if (safeLong(comment.getReplyToUserId()) > 0) {
            vo.setReplyTo(displayName(userMap.get(comment.getReplyToUserId())));
        }
        return vo;
    }

    private CommunityPostVo toPostVo(
        CommunityPost post,
        Long currentUserId,
        Map<Long, AppUser> userMap,
        Map<Long, CommunityTopic> topicMap,
        Map<Long, List<String>> imageMap,
        Set<Long> likePostIds,
        Set<Long> favoritePostIds,
        Set<Long> followingUserIds,
        boolean includeComments
    ) {
        CommunityPostVo vo = new CommunityPostVo();
        AppUser user = userMap.get(post.getUserId());
        CommunityTopic topic = topicMap.get(post.getTopicId());
        vo.setId(String.valueOf(post.getPostId()));
        vo.setAuthorUserId(post.getUserId() == null ? "" : String.valueOf(post.getUserId()));
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setAuthor(displayName(user));
        vo.setAuthorBadge("");
        vo.setLocation(StringUtils.defaultIfBlank(post.getLocation(), "社区里"));
        vo.setTopicId(topic == null ? TOPIC_RECOMMENDED : topic.getTopicKey());
        vo.setTopicLabel(topic == null ? "推荐" : topic.getTopicName());
        vo.setPublishedAt(post.getCreateTime() == null ? System.currentTimeMillis() : post.getCreateTime().getTime());
        vo.setFeatured(safeInt(post.getIsFeatured()) == 1);
        vo.setLiked(likePostIds.contains(post.getPostId()));
        vo.setSaved(favoritePostIds.contains(post.getPostId()));
        vo.setFollowingAuthor(followingUserIds.contains(post.getUserId()));
        vo.setOwner(currentUserId != null && Objects.equals(currentUserId, post.getUserId()));
        vo.setViewCount(safeInt(post.getViewCount()));
        vo.setImageList(new ArrayList<>(imageMap.getOrDefault(post.getPostId(), Collections.emptyList())));
        CommunityPostVo.PostStats stats = new CommunityPostVo.PostStats();
        stats.setComments(safeInt(post.getCommentCount()));
        stats.setLikes(safeInt(post.getLikeCount()));
        stats.setSaves(safeInt(post.getSaveCount()));
        stats.setShares(safeInt(post.getShareCount()));
        stats.setViews(safeInt(post.getViewCount()));
        vo.setStats(stats);
        if (includeComments) {
            vo.setComments(buildCommentTree(post.getPostId()));
        }
        return vo;
    }

    private CommunityTopicVo toTopicVo(CommunityTopic topic) {
        CommunityTopicVo vo = new CommunityTopicVo();
        vo.setTopicId(topic.getTopicId());
        vo.setId(topic.getTopicKey());
        vo.setLabel(topic.getTopicName());
        vo.setTone(StringUtils.defaultIfBlank(topic.getTone(), "soft"));
        return vo;
    }

    private Map<Long, CommunityTopic> listTopicMap() {
        return topicMapper.selectList(
            Wrappers.<CommunityTopic>lambdaQuery().eq(CommunityTopic::getStatus, "0")
        ).stream().collect(Collectors.toMap(CommunityTopic::getTopicId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, AppUser> listUserMap(Collection<Long> userIds) {
        Set<Long> safeIds = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return appUserMapper.selectByIds(safeIds).stream()
            .collect(Collectors.toMap(AppUser::getUserId, item -> item, (a, b) -> a, HashMap::new));
    }

    private Map<Long, List<String>> listPostImageMap(Collection<Long> postIds) {
        Set<Long> safeIds = postIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CommunityPostImage> images = postImageMapper.selectList(
            Wrappers.<CommunityPostImage>lambdaQuery()
                .in(CommunityPostImage::getPostId, safeIds)
                .orderByAsc(CommunityPostImage::getSortOrder, CommunityPostImage::getImageId)
        );
        Map<Long, List<String>> imageMap = new HashMap<>();
        for (CommunityPostImage image : images) {
            imageMap.computeIfAbsent(image.getPostId(), key -> new ArrayList<>()).add(image.getImageUrl());
        }
        return imageMap;
    }

    private Set<Long> listFollowedUserIds(Long currentUserId) {
        List<CommunityUserFollow> follows = followMapper.selectList(
            Wrappers.<CommunityUserFollow>lambdaQuery()
                .eq(CommunityUserFollow::getUserId, currentUserId)
        );
        return follows.stream()
            .map(CommunityUserFollow::getFollowUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Set<Long> listFavoritePostIds(Long currentUserId) {
        List<CommunityPostFavorite> favorites = favoriteMapper.selectList(
            Wrappers.<CommunityPostFavorite>lambdaQuery()
                .eq(CommunityPostFavorite::getUserId, currentUserId)
        );
        return favorites.stream()
            .map(CommunityPostFavorite::getPostId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Set<Long> listLikePostIds(Long currentUserId) {
        List<CommunityPostLike> likes = likeMapper.selectList(
            Wrappers.<CommunityPostLike>lambdaQuery()
                .eq(CommunityPostLike::getUserId, currentUserId)
        );
        return likes.stream()
            .map(CommunityPostLike::getPostId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private boolean matchCategory(
        CommunityPost post,
        String category,
        Set<Long> followingUserIds,
        Set<Long> favoritePostIds,
        Map<Long, CommunityTopic> topicMap
    ) {
        if ("following".equals(category)) {
            return followingUserIds.contains(post.getUserId());
        }
        if ("saved".equals(category)) {
            return favoritePostIds.contains(post.getPostId());
        }
        if (TOPIC_RECOMMENDED.equals(category)) {
            return true;
        }
        CommunityTopic topic = topicMap.get(post.getTopicId());
        return topic != null && Objects.equals(topic.getTopicKey(), category);
    }

    private boolean matchKeyword(CommunityPost post, String keyword, Map<Long, AppUser> userMap, Map<Long, CommunityTopic> topicMap) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        CommunityTopic topic = topicMap.get(post.getTopicId());
        AppUser author = userMap.get(post.getUserId());
        String joined = String.join(
            " ",
            StringUtils.defaultString(post.getTitle()),
            StringUtils.defaultString(post.getContent()),
            displayName(author),
            StringUtils.defaultString(post.getLocation()),
            topic == null ? "" : StringUtils.defaultString(topic.getTopicName())
        ).toLowerCase();
        return joined.contains(keyword.trim().toLowerCase());
    }

    private CommunityTopic resolveTopicByKey(String topicKey) {
        String safeKey = StringUtils.isBlank(topicKey) ? TOPIC_RECOMMENDED : topicKey.trim();
        if (SYSTEM_TABS.contains(safeKey)) {
            safeKey = TOPIC_RECOMMENDED;
        }
        CommunityTopic topic = topicMapper.selectOne(
            Wrappers.<CommunityTopic>lambdaQuery()
                .eq(CommunityTopic::getTopicKey, safeKey)
                .eq(CommunityTopic::getStatus, "0")
                .last("limit 1")
        );
        if (topic == null) {
            throw new IllegalArgumentException("话题不存在");
        }
        return topic;
    }

    private CommunityTopic resolveTopicFromContent(String content) {
        String topicName = extractTopicName(content);
        if (StringUtils.isBlank(topicName)) {
            return resolveTopicByKey(TOPIC_RECOMMENDED);
        }
        CommunityTopic existed = topicMapper.selectOne(
            Wrappers.<CommunityTopic>lambdaQuery()
                .eq(CommunityTopic::getTopicName, topicName)
                .eq(CommunityTopic::getStatus, "0")
                .last("limit 1")
        );
        if (existed != null) {
            return existed;
        }
        CommunityTopic topic = new CommunityTopic();
        topic.setTopicKey(buildTopicKey(topicName));
        topic.setTopicName(topicName);
        topic.setTone("soft");
        topic.setSortOrder(nextTopicSortOrder());
        topic.setStatus("0");
        topicMapper.insert(topic);
        return topic;
    }

    private String extractTopicName(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        Matcher matcher = CONTENT_TOPIC_PATTERN.matcher(content);
        if (!matcher.find()) {
            return "";
        }
        return StringUtils.substring(StringUtils.trim(matcher.group(2)), 0, 20);
    }

    private String buildTopicKey(String topicName) {
        String normalized = topicName.toLowerCase()
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
            .replaceAll("(^-+|-+$)", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = "topic";
        }
        String key = "topic-" + normalized;
        CommunityTopic duplicate = topicMapper.selectOne(
            Wrappers.<CommunityTopic>lambdaQuery()
                .eq(CommunityTopic::getTopicKey, key)
                .last("limit 1")
        );
        if (duplicate == null) {
            return key;
        }
        return key + "-" + System.currentTimeMillis();
    }

    private int nextTopicSortOrder() {
        CommunityTopic lastTopic = topicMapper.selectOne(
            Wrappers.<CommunityTopic>lambdaQuery()
                .orderByDesc(CommunityTopic::getSortOrder, CommunityTopic::getTopicId)
                .last("limit 1")
        );
        return lastTopic == null || lastTopic.getSortOrder() == null ? 100 : lastTopic.getSortOrder() + 1;
    }

    private void savePostImages(Long postId, List<String> imageList) {
        if (CollUtil.isEmpty(imageList)) {
            return;
        }
        int sort = 1;
        for (String imageUrl : imageList) {
            if (StringUtils.isBlank(imageUrl)) {
                continue;
            }
            CommunityPostImage image = new CommunityPostImage();
            image.setPostId(postId);
            image.setImageUrl(imageUrl.trim());
            image.setSortOrder(sort++);
            postImageMapper.insert(image);
        }
    }

    private CommunityPost requirePost(Long postId) {
        CommunityPost post = postMapper.selectOne(
            Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getPostId, postId)
                .eq(CommunityPost::getStatus, "0")
                .last("limit 1")
        );
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        return post;
    }

    private CommunityComment requireComment(Long commentId) {
        CommunityComment comment = commentMapper.selectOne(
            Wrappers.<CommunityComment>lambdaQuery()
                .eq(CommunityComment::getCommentId, commentId)
                .eq(CommunityComment::getStatus, "0")
                .last("limit 1")
        );
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        return comment;
    }

    private Comparator<CommunityCommentVo> resolveCommentComparator(String sortType) {
        if ("oldest".equalsIgnoreCase(StringUtils.trim(sortType))) {
            return Comparator.comparing(CommunityCommentVo::getCreatedAt, Comparator.nullsLast(Long::compareTo));
        }
        return Comparator.comparing(CommunityCommentVo::getCreatedAt, Comparator.nullsLast(Long::compareTo)).reversed();
    }

    private String displayName(AppUser user) {
        if (user == null) {
            return "邻居";
        }
        if (StringUtils.isNotBlank(user.getNickName())) {
            return user.getNickName().trim();
        }
        if (StringUtils.isNotBlank(user.getUserName())) {
            return user.getUserName().trim();
        }
        return "邻居";
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
