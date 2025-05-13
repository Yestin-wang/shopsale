package com.yestin.mq.listener;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.yestin.common.web.Result;
import com.yestin.mq.MQConstant;
import com.yestin.mq.OrderMQResult;
import com.yestin.mq.domain.OrderMessage;
import com.yestin.mq.callback.DefaultSendCallback;
import com.yestin.service.IOrderInfoService;
import com.yestin.web.msg.SeckillCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OrderPendingMessageListener {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MQConstant.ORDER_PEDDING_TOPIC + "_QUEUE")
    public void onMessage(Message message, OrderMessage orderMessage, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[创建订单] 收到创建订单消息，准备开始创建订单： {}", JSON.toJSONString(orderMessage));
        OrderMQResult result = new OrderMQResult();
        try {
            result.setTime(orderMessage.getTime());
            result.setSeckillId(orderMessage.getSeckillId());
            result.setToken(orderMessage.getToken());
            String orderNo = orderInfoService.doSeckill(orderMessage.getUserPhone(), orderMessage.getSeckillId(), orderMessage.getTime());
            result.setOrderNo(orderNo);
            result.setCode(Result.SUCCESS_CODE);
            result.setMsg("订单创建成功");
            // 当下单成功后 发送延迟消息 检查订单支付状态 超时未支付就直接取消订单
            orderMessage.setOrderNo(orderNo);
            log.info("[创建订单] 开始发送订单支付超时检查延迟消息，订单号: {}, 延迟时间: {}秒", orderNo, MQConstant.ORDER_PAY_TIMEOUT_DELAY_SECONDS);
            try {
                // 发送到延迟队列，通过TTL和死信队列实现延迟
                rabbitTemplate.convertAndSend(
                        MQConstant.ORDER_PAY_TIMEOUT_TOPIC + "_DELAY",
                        "",
                        orderMessage
                );
                log.info("[创建订单] 订单支付超时检查延迟消息发送成功，订单号: {}", orderNo);
            } catch (Exception e) {
                log.error("[创建订单] 订单支付超时检查延迟消息发送失败: {}", e.getMessage(), e);
            }
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[创建订单] 处理订单消息失败: {}", e.getMessage(), e);
            // 订单创建失败
            SeckillCodeMsg codeMsg = SeckillCodeMsg.SECKILL_ERROR;
            result.setCode(codeMsg.getCode());
            result.setMsg(codeMsg.getMsg());
            orderInfoService.failedRollback(orderMessage);
            // 消息处理失败
            channel.basicNack(deliveryTag, false, false);
        }
        // 发送订单创建结果消息
        rabbitTemplate.convertAndSend(
                MQConstant.ORDER_RESULT_TOPIC,
                "",
                result
        );
    }
}
