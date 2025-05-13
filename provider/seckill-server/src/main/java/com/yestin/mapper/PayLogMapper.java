package com.yestin.mapper;

import com.yestin.domain.PayLog;

import java.sql.SQLException;

public interface PayLogMapper {
    /**
     * 插入支付日志，用于幂等性控制
     *
     * @param payLog
     * @return
     */
    int insert(PayLog payLog);
}
