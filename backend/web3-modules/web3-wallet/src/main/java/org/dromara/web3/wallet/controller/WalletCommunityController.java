package org.dromara.web3.wallet.controller;

import cn.hutool.core.io.FileUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.MimeTypeUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.web3.wallet.domain.bo.CommunityCommentCreateBo;
import org.dromara.web3.wallet.domain.bo.CommunityPostCreateBo;
import org.dromara.web3.wallet.domain.vo.CommunityCommentPageVo;
import org.dromara.web3.wallet.domain.vo.CommunityImageUploadVo;
import org.dromara.web3.wallet.domain.vo.CommunityPostVo;
import org.dromara.web3.wallet.domain.vo.CommunityToggleVo;
import org.dromara.web3.wallet.domain.vo.CommunityTopicVo;
import org.dromara.web3.wallet.service.ICommunityService;
import org.dromara.web3.wallet.service.WalletCommunityImageStorage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class WalletCommunityController {

    private final ICommunityService communityService;
    private final WalletCommunityImageStorage communityImageStorage;

    @GetMapping("/topics")
    public R<List<CommunityTopicVo>> topics(
        @RequestParam(value = "composeOnly", required = false, defaultValue = "false") boolean composeOnly,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return R.ok(communityService.listTopics(composeOnly, keyword, limit));
    }

    @GetMapping("/posts")
    public R<List<CommunityPostVo>> posts(
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return R.ok(communityService.listPosts(currentUserId(), category, keyword));
    }

    @GetMapping("/posts/{postId}")
    public R<CommunityPostVo> detail(@PathVariable("postId") Long postId) {
        try {
            return R.ok(communityService.getPostDetail(currentUserId(), postId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/posts/{postId}/comments")
    public R<CommunityCommentPageVo> comments(
        @PathVariable("postId") Long postId,
        @RequestParam(value = "sortType", required = false, defaultValue = "time") String sortType,
        @RequestParam(value = "pageNum", required = false, defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", required = false, defaultValue = "8") long pageSize
    ) {
        try {
            return R.ok(communityService.listComments(currentUserId(), postId, sortType, pageNum, pageSize));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/posts")
    public R<CommunityPostVo> create(@RequestBody CommunityPostCreateBo bo) {
        try {
            return R.ok(communityService.createPost(currentUserId(), bo));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/posts/{postId}/comments")
    public R<CommunityPostVo> addComment(@PathVariable("postId") Long postId, @RequestBody CommunityCommentCreateBo bo) {
        try {
            return R.ok(communityService.addComment(currentUserId(), postId, bo));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/posts/{postId}/like")
    public R<CommunityToggleVo> like(@PathVariable("postId") Long postId) {
        try {
            return R.ok(communityService.togglePostLike(currentUserId(), postId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/posts/{postId}/favorite")
    public R<CommunityToggleVo> favorite(@PathVariable("postId") Long postId) {
        try {
            return R.ok(communityService.togglePostFavorite(currentUserId(), postId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/posts/{postId}/share")
    public R<Void> share(@PathVariable("postId") Long postId) {
        try {
            communityService.increaseShareCount(postId);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/users/{targetUserId}/follow")
    public R<CommunityToggleVo> follow(@PathVariable("targetUserId") Long targetUserId) {
        try {
            return R.ok(communityService.toggleFollow(currentUserId(), targetUserId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<CommunityImageUploadVo> upload(@RequestPart("file") MultipartFile file) {
        Long userId = currentUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }
        if (file == null || file.isEmpty()) {
            return R.fail("图片不能为空");
        }
        String extension = FileUtil.extName(file.getOriginalFilename());
        if (!StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION)) {
            return R.fail("文件格式不正确，请上传" + Arrays.toString(MimeTypeUtils.IMAGE_EXTENSION) + "格式");
        }
        try {
            return R.ok(new CommunityImageUploadVo(communityImageStorage.save(file, userId, extension)));
        } catch (IOException e) {
            return R.fail("上传图片失败");
        }
    }

    private Long currentUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
