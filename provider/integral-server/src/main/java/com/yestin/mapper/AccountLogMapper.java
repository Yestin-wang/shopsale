package com.yestin.mapper;

import com.yestin.domain.AccountLog;
import org.apache.ibatis.annotations.Param;

public interface AccountLogMapper {
    /**
     * 插入日志
     *
     * @param accountLog
     */
    void insert(AccountLog accountLog);

    AccountLog selectByPkAndType(@Param("orderNo") String orderNo, @Param("type") int type);

    AccountLog selectByPkAndStatus(@Param("orderNo") String orderNo, @Param("status") int status);

    AccountLog selectByTxId(String txId);

    void changeStatus(String tradeNo, int status);

    AccountLog selectByOutTradeNoAndType(@Param("outTradeNo") String outTradeNo, @Param("type") int type);
}
