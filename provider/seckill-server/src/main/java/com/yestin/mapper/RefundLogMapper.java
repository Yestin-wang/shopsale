package com.yestin.mapper;

import com.yestin.domain.RefundLog;

public interface RefundLogMapper {
    /**
     * 插入退款日志，用于幂等性控制
     * @param refundLog
     * @return
     */
    int insert(RefundLog refundLog);
}
