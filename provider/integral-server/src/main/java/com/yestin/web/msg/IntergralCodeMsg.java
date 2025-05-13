package com.yestin.web.msg;


import com.yestin.common.web.CodeMsg;

/**
 * Created by wolfcode
 */
public class IntergralCodeMsg extends CodeMsg {
    private IntergralCodeMsg(Integer code, String msg){
        super(code,msg);
    }
    public static final IntergralCodeMsg OP_INTERGRAL_ERROR = new IntergralCodeMsg(500601,"操作积分失败");
    public static final IntergralCodeMsg INTERGRAL_NOT_ENOUGH = new IntergralCodeMsg(500602,"积分余额不足");
    public static final IntergralCodeMsg INTERGRAL_PAY_REPEATE = new IntergralCodeMsg(500603,"请不要重复支付");
    public static final IntergralCodeMsg AMOUNT_ERROR = new IntergralCodeMsg(500604,"退款金额不能大于支付金额");
    public static final IntergralCodeMsg INTERGRAL_REFUND_REPEATE = new IntergralCodeMsg(500605,"请不要重复退款");
    public static final IntergralCodeMsg INTERGRAL_UNPAID = new IntergralCodeMsg(500606,"订单未支付");

}
