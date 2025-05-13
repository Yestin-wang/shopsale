package com.yestin.service.impl;

import com.yestin.common.exception.BusinessException;
import com.yestin.common.web.Result;
import com.yestin.domain.*;
import com.yestin.feign.IntegralFeignApi;
import com.yestin.feign.PaymentFeignApi;
import com.yestin.mapper.OrderInfoMapper;
import com.yestin.mapper.PayLogMapper;
import com.yestin.mapper.RefundLogMapper;
import com.yestin.mq.MQConstant;
import com.yestin.mq.domain.OrderMessage;
import com.yestin.redis.SeckillRedisKey;
import com.yestin.service.IOrderInfoService;
import com.yestin.service.ISeckillProductService;
import com.yestin.util.IdGenerateUtil;
import com.yestin.web.msg.PayCodeMsg;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.Date;

@Slf4j
@RefreshScope
@Service
public class OrderInfoServiceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private RefundLogMapper refundLogMapper;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PaymentFeignApi paymentFeignApi;
    @Autowired
    private IntegralFeignApi integralFeignApi;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public OrderInfo selectByUserIdAndSeckillId(Long userId, Long seckillId, Integer time) {
        return orderInfoMapper.selectByUserIdAndSeckillId(userId, seckillId, time);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String doSeckill(Long phone, SeckillProductVo vo) {
        // 扣除秒杀商品库存
        // 分布式锁
//        seckillProductService.decrStockCount(vo.getId(), vo.getTime());
        // 乐观锁
        seckillProductService.decrStockCount(vo.getId());
        // 创建秒杀订单并保存
        OrderInfo orderInfo = this.buildOrderInfo(phone, vo);
        orderInfoMapper.insert(orderInfo);
        // 返回订单编号
        return orderInfo.getOrderNo();
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED)
    @Override
    public String doSeckill(Long phone, Long seckillId, Integer time) {
        SeckillProductVo sp = seckillProductService.selectByIdAndTime(seckillId, time);
        return this.doSeckill(phone, sp);
    }

    @Override
    public void failedRollback(OrderMessage msg) {
        // 回补redis库存
        rollbackRedis(msg.getSeckillId(), msg.getTime());

        // 删除用户下单标识
        String userOrderFlag = SeckillRedisKey.SECKILL_ORDER_HASH.join(msg.getSeckillId() + "");
        redisTemplate.opsForHash().delete(userOrderFlag, msg.getUserPhone() + "");

        // 使用广播交换机进行广播，清除所有节点的售罄标识
        log.info("[订单回滚] 发送清除售罄标识广播消息，商品ID: {}", msg.getSeckillId());
        rabbitTemplate.convertAndSend(
                MQConstant.CLEAR_STOCK_OVER_BROADCAST_EXCHANGE,
                "",
                msg.getSeckillId()
        );
    }

    private void rollbackRedis(Long seckillId, Integer time) {
        Long stock = seckillProductService.selectStockById(seckillId);
        String hashKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.join(time + "");
        redisTemplate.opsForHash().put(hashKey, seckillId + "", stock + "");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void checkPayTimeout(OrderMessage msg) {
        // 查询订单对象
        // 判断状态 未支付-> 取消订单
        int row = orderInfoMapper.changePayStatus(msg.getOrderNo(), OrderInfo.STATUS_CANCEL, OrderInfo.PAY_TYPE_ONLINE);
        if (row > 0) {
            // MySQL秒杀商品库存数量+1
            seckillProductService.incryStockCount(msg.getSeckillId());
        }
        // 失败订单信息回滚
        this.failedRollback(msg);
    }

    @Override
    public String onlinePay(String orderNo) {
        // 基于订单号查询订单对象
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        // 判断订单状态
        if (!OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            throw new BusinessException(PayCodeMsg.PAY_ERROR);
        }
        // 封装支付参数
        PayVo vo = new PayVo();
        vo.setBody("seckill:" + orderInfo.getProductName());
        vo.setSubject(orderInfo.getProductName());
        vo.setOutTradeNo(orderNo);
        vo.setTotalAmount(orderInfo.getSeckillPrice().toString());
        // 远程调用支付服务发起支付
        Result<String> result = paymentFeignApi.prepay(vo);
        if (result.hasError()) {
            throw new BusinessException(PayCodeMsg.PAY_ERROR);
        }
        return result.getData();
    }

    @Override
    public void alipaySuccess(PayResult result) {
        // 查询订单信息
        OrderInfo orderInfo = orderInfoMapper.find(result.getOutTradeNo());
        if (orderInfo == null) throw new BusinessException(PayCodeMsg.ORDER_ERROR);
        // 判断订单信息是否正确
        if (!orderInfo.getSeckillPrice().toString().equals(result.getTradeNo())) {
            throw new BusinessException(PayCodeMsg.ORDER_ERROR);
        }
        // 更新订单状态
        int row = orderInfoMapper.changePayStatus(result.getOutTradeNo(), OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAY_TYPE_ONLINE);
        if (row <= 0) throw new BusinessException(PayCodeMsg.PAY_ERROR);
        // 记录支付流水
        PayLog paylog = new PayLog();
        paylog.setOutTradeNo(result.getOutTradeNo());
        paylog.setPayType(PayLog.PAY_TYPE_ONLINE);
        paylog.setTotalAmount(result.getTotalAmount());
        paylog.setTradeNo(result.getTradeNo());
        paylog.setNotifyTime(System.currentTimeMillis() + "");
        payLogMapper.insert(paylog);
    }

    @Override
    public OrderInfo selectByOrderInfo(String orderNo) {
        return orderInfoMapper.find(orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo) {
        // 查询订单对象
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        // 判断订单状态是否为已支付
        if (!OrderInfo.STATUS_ACCOUNT_PAID.equals(orderInfo.getStatus())) {
            throw new BusinessException(PayCodeMsg.UNPAID);
        }
        // 判断订单支付类型
        Result<Boolean> result = null;
        RefundVo refundVo = new RefundVo(orderNo, orderInfo.getSeckillPrice().toString(), "不想要了");
        if (orderInfo.getPayType() == OrderInfo.PAY_TYPE_ONLINE) {
            //支付宝退款
            result = paymentFeignApi.refund(refundVo);
        } else {
            //积分退款
            result = integralFeignApi.refund(refundVo);
        }
        // 判断是否退款成功
        if (result == null || result.hasError() || !result.getData()) {
            throw new BusinessException(PayCodeMsg.REFUND_ERROR);
        }
        // 退款成功 更新订单状态为已退款
        int row = orderInfoMapper.changeRefundStatus(orderNo, OrderInfo.STATUS_REFUND);
        if (row <= 0) throw new BusinessException(PayCodeMsg.REFUND_ERROR);
        // 库存回补
        seckillProductService.incryStockCount(orderInfo.getSeckillId());
        this.rollbackRedis(orderInfo.getSeckillId(), orderInfo.getSeckillTime());
        // 使用广播交换机进行广播，清除所有节点的售罄标识
        log.info("[订单回滚] 发送清除售罄标识广播消息，商品ID: {}", orderInfo.getSeckillId());
        rabbitTemplate.convertAndSend(
                MQConstant.CLEAR_STOCK_OVER_BROADCAST_EXCHANGE,
                "",
                orderInfo.getSeckillId()
        );
        // 创建退款日志对象
        RefundLog refundLog = new RefundLog();
        refundLog.setRefundReason("用户申请退款" + orderInfo.getProductName());
        refundLog.setRefundTime(new Date());
        refundLog.setRefundType(orderInfo.getPayType());
        refundLog.setRefundAmount(orderInfo.getSeckillPrice().toString());
        refundLog.setOutTradeNo(orderNo);
        refundLogMapper.insert(refundLog);
    }

    @Override
    public void alipayRefund(String orderNo) {
        this.refund(orderNo);
    }

    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String integralPay(String orderNo, Long phone) {
        // 基于订单号查询订单对象
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        // 判断订单状态
        if (!OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            throw new BusinessException(PayCodeMsg.PAY_ERROR);
        }
        // 判断当前用户是否是创建但钱订单的用户 =》 确保自己订单用自己的积分消费
        if (!orderInfo.getUserId().equals(phone)) throw new BusinessException(PayCodeMsg.NOT_THIS_USER);
        // 封装支付参数
        OperateIntergralVo operateIntergralVo = new OperateIntergralVo();
        operateIntergralVo.setInfo("积分秒杀:" + orderInfo.getProductName());
        operateIntergralVo.setValue(orderInfo.getIntegral());
        operateIntergralVo.setUserId(phone);
        operateIntergralVo.setOutTradeVo(orderNo);
        // 远程调用支付服务发起支付
        Result<String> result = integralFeignApi.prepay(operateIntergralVo);
        String tradeNo = result.getData();
        // 更新订单支付状态和记录支付流水日志
        int row = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAY_TYPE_INTEGRAL);
        if(row<0) throw new BusinessException(PayCodeMsg.PAY_FAIL);

        PayLog payLog = new PayLog();
        payLog.setPayType(PayLog.PAY_TYPE_INTEGRAL);
        payLog.setTotalAmount(operateIntergralVo.getValue() + "");
        payLog.setOutTradeNo(orderNo);
        payLog.setTradeNo(tradeNo);
        payLog.setNotifyTime(System.currentTimeMillis() + "");
        payLogMapper.insert(payLog);

        int i = 1 / 0;
        return "积分支付成功!\n" + result.toString();
    }


    private OrderInfo buildOrderInfo(Long phone, SeckillProductVo vo) {
        Date now = new Date();
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(now);
        orderInfo.setDeliveryAddrId(1L);
        orderInfo.setIntegral(vo.getIntegral());
        // 雪花算法
        orderInfo.setOrderNo(IdGenerateUtil.get().nextId() + "");
        orderInfo.setPayType(OrderInfo.PAY_TYPE_ONLINE);
        orderInfo.setProductCount(1);
        orderInfo.setProductId(vo.getProductId());
        orderInfo.setProductImg(vo.getProductImg());
        orderInfo.setProductName(vo.getProductName());
        orderInfo.setProductPrice(vo.getProductPrice());
        orderInfo.setSeckillDate(now);
        orderInfo.setSeckillId(vo.getId());
        orderInfo.setSeckillPrice(vo.getSeckillPrice());
        orderInfo.setSeckillTime(vo.getTime());
        orderInfo.setStatus(OrderInfo.STATUS_ARREARAGE);
        orderInfo.setUserId(phone);
        return orderInfo;
    }
}
