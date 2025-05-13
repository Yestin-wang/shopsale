package com.yestin.domain;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PayLog {

    public static final int PAY_TYPE_ONLINE = 0;//在线支付
    public static final int PAY_TYPE_INTEGRAL = 1;//积分支付

    /**
     * 支付状态：支付中
     */
    public static final int PAY_STATUS_PAYING = 0;
    /**
     * 支付状态：支付成功
     */
    public static final int PAY_STATUS_SUCCESS = 1;
    /**
     * 支付状态：支付失败
     */
    public static final int PAY_STATUS_FAILED = 2;

    //支付流水号
    private String tradeNo;
    // 订单编号
    private String outTradeNo;
    // 更新时间
    private String notifyTime;
    // 交易金额
    private String totalAmount;
    // 支付类型
    private int payType;
    private int status;
}