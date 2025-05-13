package com.yestin.mq;

public class MQConstant {
    //订单队列
    public static final String ORDER_PEDDING_TOPIC = "ORDER_PEDDING_TOPIC";
    public static final String ORDER_PENDING_CONSUMER_GROUP = "ORDER_PENDING_CONSUMER_GROUP";

    //订单结果
    public static final String ORDER_RESULT_TOPIC = "ORDER_RESULT_TOPIC";
    //订单超时取消
    public static final String ORDER_PAY_TIMEOUT_TOPIC = "ORDER_PAY_TIMEOUT_TOPIC";
    //取消的本地标识
    public static final String CANCEL_SECKILL_OVER_SIGE_TOPIC = "CANCEL_SECKILL_OVER_SIGE_TOPIC";
    //订单创建成功Tag
    public static final String ORDER_RESULT_SUCCESS_TAG = "SUCCESS";
    //订单创建失败Tag
    public static final String ORDER_RESULT_FAIL_TAG = "FAIL";
    //延迟消息秒数
    public static final int ORDER_PAY_TIMEOUT_DELAY_SECONDS = 600;

    // 库存售罄广播
    public static final String STOCK_OVER_BROADCAST_EXCHANGE = "STOCK_OVER_BROADCAST_EXCHANGE";
    public static final String STOCK_OVER_BROADCAST_QUEUE = "STOCK_OVER_BROADCAST_QUEUE";

    // 清空库存售罄标识广播
    public static final String CLEAR_STOCK_OVER_BROADCAST_EXCHANGE = "clearStockOverBroadcastExchange";
    public static final String CLEAR_STOCK_OVER_BROADCAST_QUEUE = "clearStockOverBroadcastQueue";
}
