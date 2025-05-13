package com.yestin.mapper;

import org.apache.ibatis.annotations.Param;


public interface UsableIntegralMapper {
    /**
     * 冻结用户积分金额
     *
     * @param userId
     * @param amount
     * @return
     */
    int freezeIntegral(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 提交改变，冻结金额真实扣除
     *
     * @param userId
     * @param amount
     * @return
     */
    int commitChange(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 取消冻结金额
     *
     * @param userId
     * @param amount
     */
    void unFreezeIntegral(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 增加积分
     *
     * @param userId
     * @param amount
     */
    void addIntegral(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 扣除积分
     *
     * @param userId 用户 id
     * @param amount 扣除的积分
     * @return 是否扣除成功
     */
    int decrIntegral(@Param("userId") Long userId, @Param("amount") Long amount);
}
