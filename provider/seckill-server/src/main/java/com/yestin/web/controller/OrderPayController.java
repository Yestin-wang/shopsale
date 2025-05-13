package com.yestin.web.controller;

import com.yestin.common.domain.UserInfo;
import com.yestin.common.web.Result;
import com.yestin.common.web.anno.RequireLogin;
import com.yestin.common.web.resolver.RequestUser;
import com.yestin.domain.OrderInfo;
import com.yestin.domain.PayResult;
import com.yestin.service.IOrderInfoService;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orderPay")
@RefreshScope
public class OrderPayController {
    private final IOrderInfoService orderInfoService;

    public OrderPayController(IOrderInfoService orderInfoService) {
        this.orderInfoService = orderInfoService;
    }


    @GetMapping("/refund")
    public Result<String> refund(String orderNo) {
        orderInfoService.refund(orderNo);
        return Result.success("退款成功");
    }

    @RequireLogin
    @GetMapping("/pay")
    public Result<String> doPay(String orderNo, Integer type, @RequestUser UserInfo userInfo) {

        if (type == OrderInfo.PAY_TYPE_ONLINE) {
            return Result.success(orderInfoService.onlinePay(orderNo));
        }
        return Result.success(orderInfoService.integralPay(orderNo, userInfo.getPhone()));
    }

    @PostMapping("success")
    Result<?> updateOrderPaySuccess(@RequestBody PayResult result) {
        orderInfoService.alipaySuccess(result);
        return Result.success("支付成功");
    }
}
