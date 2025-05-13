package com.yestin.web.msg;

import com.yestin.SeckillApplication;
import com.yestin.common.web.CodeMsg;

public class SeckillCodeMsg extends CodeMsg {
    private SeckillCodeMsg(Integer code, String msg) {
        super(code, msg);
    }

    public static final SeckillCodeMsg SECKILL_STOCK_OVER = new SeckillCodeMsg(500201, "您来晚了，商品已经被抢购完毕.");
    public static final SeckillCodeMsg REPEAT_SECKILL = new SeckillCodeMsg(500202, "您已经抢购到商品了，请不要重复抢购");
    public static final SeckillCodeMsg SECKILL_ERROR = new SeckillCodeMsg(500203, "秒杀失败");
    public static final SeckillCodeMsg CANCEL_ORDER_ERROR = new SeckillCodeMsg(500204, "超时取消失败");
    public static final SeckillCodeMsg PAY_SERVER_ERROR = new SeckillCodeMsg(500205, "支付服务繁忙，稍后再试");
    public static final SeckillCodeMsg REFUND_ERROR = new SeckillCodeMsg(500206, "退款失败，请联系管理员");
    public static final SeckillCodeMsg INTERGRAL_SERVER_ERROR = new SeckillCodeMsg(500207, "操作积分失败");
    public static final SeckillCodeMsg OP_ERROR = new SeckillCodeMsg(500208, "非法操作");
    public static final SeckillCodeMsg OVER_TIME_ERROR = new SeckillCodeMsg(500209, "活动还未开始或已结束");
    public static final SeckillCodeMsg RSA_CHECK_FAILED = new SeckillCodeMsg(500210, "支付服务验证签名失败");
    public static final SeckillCodeMsg PAY_FAILED_ERROR = new SeckillCodeMsg(500211, "发起支付失败");
    public static final SeckillCodeMsg INTERGRAL_PAY_FAILED_ERROR = new SeckillCodeMsg(500212, "积分支付失败");
    public static final SeckillCodeMsg SERVER_BUSY = new SeckillCodeMsg(5002123, "系统繁忙，请稍后再试");
}
