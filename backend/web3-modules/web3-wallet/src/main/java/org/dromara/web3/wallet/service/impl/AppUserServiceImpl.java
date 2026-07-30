package org.dromara.web3.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.web3.wallet.domain.AppUser;
import org.dromara.web3.wallet.mapper.AppUserMapper;
import org.dromara.web3.wallet.service.IAppUserService;
import org.springframework.stereotype.Service;

/**
 * APP用户信息Service业务层处理
 *
 * @author web3
 */
@RequiredArgsConstructor
@Service
public class AppUserServiceImpl implements IAppUserService {

    private final AppUserMapper baseMapper;

    @Override
    public AppUser selectUserById(Long userId) {
        return baseMapper.selectById(userId);
    }

    @Override
    public AppUser selectUserByEmail(String email) {
        return baseMapper.selectOne(new LambdaQueryWrapper<AppUser>()
            .eq(AppUser::getEmail, email)
            .last("limit 1"));
    }

    @Override
    public boolean registerUser(AppUser user) {
        return baseMapper.insert(user) > 0;
    }

    @Override
    public boolean updateUser(AppUser user) {
        return baseMapper.updateById(user) > 0;
    }
}
