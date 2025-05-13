package com.yestin.mq.listener;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.yestin.mq.MQConstant;
import com.yestin.mq.domain.OrderMessage;
import com.yestin.service.IOrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OrderPayTimeoutMsgListener {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IOrderInfoService orderInfoService;

    @RabbitListener(queues = MQConstant.ORDER_PAY_TIMEOUT_TOPIC + "_QUEUE")
    public void onMessage(Message message, OrderMessage msg, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[订单超时未支付检查] 收到超时检查订单状态消息: {}", JSON.toJSONString(msg));
        try {
            orderInfoService.checkPayTimeout(msg);
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("[订单超时未支付检查] 处理成功，订单号: {}", msg.getOrderNo());
        } catch (Exception e) {
            log.error("[订单超时未支付检查] 处理失败: {}", e.getMessage(), e);
            // 消息处理失败，拒绝消息并不重新入队
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
