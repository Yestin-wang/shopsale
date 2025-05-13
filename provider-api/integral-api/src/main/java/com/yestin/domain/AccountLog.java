package com.yestin.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分变更日志表
 */
@Setter
@Getter
public class AccountLog implements Serializable {
    public static final int TYPE_DECR = 0;
    public static final int TYPE_INCR = 1;

    public static final int ACCOUNT_LOG_STATUS_TRY = 0;
    public static final int ACCOUNT_LOG_STATUS_CONFIRM = 1;
    public static final int ACCOUNT_LOG_STATUS_CANCEL = 2;

    private String tradeNo;//支付流水号
    private String outTradeNo;//订单编号
    private int type;//积分变更类型. 0是减少 1是增加
    private Long amount;//此次变化金额
    private Date gmtTime;//日志插入时间
    private String info;//备注信息
    private Long userId; // 用户id
    private String txId;
    private String actionId;
    private Integer status;
    private Long timestamp;
}
