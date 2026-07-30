package org.dromara.web3.wallet.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.domain.CommunityPost;
import org.dromara.web3.wallet.domain.CommunityTopic;
import org.dromara.web3.wallet.mapper.AppUserMapper;
import org.dromara.web3.wallet.mapper.CommunityPostMapper;
import org.dromara.web3.wallet.mapper.CommunityTopicMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityAdminSupport {

    private final AppUserMapper appUserMapper;
    private final CommunityTopicMapper topicMapper;
    private final CommunityPostMapper postMapper;

    public Map<Long, AppUser> userMap(Collection<Long> userIds) {
        Set<Long> safeIds = userIds == null ? Collections.emptySet() : userIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return appUserMapper.selectBatchIds(safeIds).stream()
            .collect(Collectors.toMap(AppUser::getUserId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    public Map<Long, CommunityTopic> topicMap(Collection<Long> topicIds) {
        Set<Long> safeIds = topicIds == null ? Collections.emptySet() : topicIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return topicMapper.selectBatchIds(safeIds).stream()
            .collect(Collectors.toMap(CommunityTopic::getTopicId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    public Map<Long, CommunityPost> postMap(Collection<Long> postIds) {
        Set<Long> safeIds = postIds == null ? Collections.emptySet() : postIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return postMapper.selectBatchIds(safeIds).stream()
            .collect(Collectors.toMap(CommunityPost::getPostId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    public String displayName(AppUser user) {
        if (user == null) {
            return "-";
        }
        if (StringUtils.isNotBlank(user.getNickName())) {
            return user.getNickName().trim();
        }
        if (StringUtils.isNotBlank(user.getUserName())) {
            return user.getUserName().trim();
        }
        return "-";
    }
}
