package org.dromara.web3.wallet.service;

import org.dromara.web3.wallet.domain.AppUser;

/**
 * APP用户信息Service接口
 *
 * @author web3
 */
public interface IAppUserService {

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    AppUser selectUserById(Long userId);

    /**
     * 通过邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户对象信息
     */
    AppUser selectUserByEmail(String email);

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean registerUser(AppUser user);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean updateUser(AppUser user);

}
