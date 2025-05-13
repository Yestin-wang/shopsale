package com.yestin.web.msg;

import com.yestin.common.web.CodeMsg;

public class PayCodeMsg extends CodeMsg {

    private PayCodeMsg(Integer code, String msg){
        super(code,msg);
    }
    public static final PayCodeMsg PAY_ERROR = new PayCodeMsg(600101, "订单状态异常，无法发起支付");
    public static final PayCodeMsg ORDER_ERROR = new PayCodeMsg(600102, "订单信息有误");
    public static final PayCodeMsg UNPAID = new PayCodeMsg(600103, "订单未支付");
    public static final PayCodeMsg REFUND_ERROR = new PayCodeMsg(600104, "退款失败");
    public static final PayCodeMsg NOT_THIS_USER = new PayCodeMsg(600105, "非当前用户操作");
    public static final PayCodeMsg PAY_FAIL = new PayCodeMsg(600106, "支付失败");
}
