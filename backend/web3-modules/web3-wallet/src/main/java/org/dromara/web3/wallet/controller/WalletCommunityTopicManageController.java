package org.dromara.web3.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.web3.wallet.domain.CommunityTopic;
import org.dromara.web3.wallet.domain.bo.CommunityTopicAdminBo;
import org.dromara.web3.wallet.domain.bo.CommunityTopicQueryBo;
import org.dromara.web3.wallet.domain.vo.CommunityTopicAdminVo;
import org.dromara.web3.wallet.mapper.CommunityTopicMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/web3/community/topic")
public class WalletCommunityTopicManageController {

    private final CommunityTopicMapper topicMapper;

    @SaCheckPermission("web3:communityTopic:list")
    @GetMapping("/list")
    public TableDataInfo<CommunityTopicAdminVo> list(CommunityTopicQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CommunityTopic> wrapper = buildQueryWrapper(bo);
        Page<CommunityTopic> page = topicMapper.selectPage(pageQuery.build(), wrapper);
        List<CommunityTopicAdminVo> rows = page.getRecords().stream().map(this::toAdminVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @SaCheckPermission("web3:communityTopic:list")
    @GetMapping("/options")
    public R<List<CommunityTopicAdminVo>> options() {
        List<CommunityTopicAdminVo> list = topicMapper.selectList(
            new LambdaQueryWrapper<CommunityTopic>()
                .orderByAsc(CommunityTopic::getSortOrder, CommunityTopic::getTopicId)
        ).stream().map(this::toAdminVo).toList();
        return R.ok(list);
    }

    @SaCheckPermission("web3:communityTopic:query")
    @GetMapping("/{topicId}")
    public R<CommunityTopicAdminVo> getInfo(@PathVariable Long topicId) {
        CommunityTopic topic = topicMapper.selectById(topicId);
        return topic == null ? R.fail("话题不存在") : R.ok(toAdminVo(topic));
    }

    @SaCheckPermission("web3:communityTopic:add")
    @PostMapping
    public R<Void> add(@RequestBody CommunityTopicAdminBo bo) {
        String error = validateTopic(bo, true);
        if (error != null) {
            return R.fail(error);
        }
        CommunityTopic topic = new CommunityTopic();
        fillTopic(topic, bo);
        topicMapper.insert(topic);
        return R.ok();
    }

    @SaCheckPermission("web3:communityTopic:edit")
    @PutMapping
    public R<Void> edit(@RequestBody CommunityTopicAdminBo bo) {
        if (bo.getTopicId() == null) {
            return R.fail("话题ID不能为空");
        }
        CommunityTopic exists = topicMapper.selectById(bo.getTopicId());
        if (exists == null) {
            return R.fail("话题不存在");
        }
        String error = validateTopic(bo, false);
        if (error != null) {
            return R.fail(error);
        }
        fillTopic(exists, bo);
        topicMapper.updateById(exists);
        return R.ok();
    }

    @SaCheckPermission("web3:communityTopic:remove")
    @DeleteMapping("/{topicIds}")
    public R<Void> remove(@PathVariable Long[] topicIds) {
        if (topicIds == null || topicIds.length == 0) {
            return R.fail("请选择要删除的话题");
        }
        topicMapper.deleteBatchIds(Arrays.asList(topicIds));
        return R.ok();
    }

    private LambdaQueryWrapper<CommunityTopic> buildQueryWrapper(CommunityTopicQueryBo bo) {
        LambdaQueryWrapper<CommunityTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(bo.getTopicKey()), CommunityTopic::getTopicKey, bo.getTopicKey());
        wrapper.like(StringUtils.isNotBlank(bo.getTopicName()), CommunityTopic::getTopicName, bo.getTopicName());
        wrapper.eq(StringUtils.isNotBlank(bo.getStatus()), CommunityTopic::getStatus, bo.getStatus());
        wrapper.orderByAsc(CommunityTopic::getSortOrder, CommunityTopic::getTopicId);
        return wrapper;
    }

    private String validateTopic(CommunityTopicAdminBo bo, boolean create) {
        if (StringUtils.isAnyBlank(bo.getTopicKey(), bo.getTopicName())) {
            return "话题标识和名称不能为空";
        }
        CommunityTopic duplicate = topicMapper.selectOne(
            new LambdaQueryWrapper<CommunityTopic>()
                .eq(CommunityTopic::getTopicKey, bo.getTopicKey().trim())
                .ne(!create && bo.getTopicId() != null, CommunityTopic::getTopicId, bo.getTopicId())
                .last("limit 1")
        );
        if (duplicate != null) {
            return "话题标识已存在";
        }
        return null;
    }

    private void fillTopic(CommunityTopic topic, CommunityTopicAdminBo bo) {
        topic.setTopicKey(bo.getTopicKey().trim());
        topic.setTopicName(bo.getTopicName().trim());
        topic.setTone(StringUtils.defaultIfBlank(bo.getTone(), "soft"));
        topic.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        topic.setStatus(StringUtils.defaultIfBlank(bo.getStatus(), "0"));
    }

    private CommunityTopicAdminVo toAdminVo(CommunityTopic topic) {
        CommunityTopicAdminVo vo = new CommunityTopicAdminVo();
        vo.setTopicId(topic.getTopicId());
        vo.setTopicKey(topic.getTopicKey());
        vo.setTopicName(topic.getTopicName());
        vo.setTone(topic.getTone());
        vo.setSortOrder(topic.getSortOrder());
        vo.setStatus(topic.getStatus());
        vo.setCreateTime(topic.getCreateTime());
        return vo;
    }
}
