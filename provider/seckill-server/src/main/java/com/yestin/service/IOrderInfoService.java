package com.yestin.service;

import com.yestin.common.domain.UserInfo;
import com.yestin.domain.OrderInfo;
import com.yestin.domain.PayResult;
import com.yestin.domain.SeckillProductVo;
import com.yestin.mq.domain.OrderMessage;


public interface IOrderInfoService {
    OrderInfo selectByUserIdAndSeckillId(Long userId, Long seckillId, Integer time);

    String doSeckill(Long phone, SeckillProductVo vo);

    String doSeckill(Long phone, Long seckillId, Integer time);

    void failedRollback(OrderMessage msg);

    void checkPayTimeout(OrderMessage msg);

    String onlinePay(String orderNo);

    void alipaySuccess(PayResult result);

    OrderInfo selectByOrderInfo(String orderNo);

    void refund(String orderNo);

    void alipayRefund(String orderNo);

    String integralPay(String orderNo, Long phone);


}
