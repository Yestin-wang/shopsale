package com.yestin.mq.listener;

import com.rabbitmq.client.Channel;
import com.yestin.mq.MQConstant;
import com.yestin.web.controller.OrderInfoController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CancelStockOverMsgListener {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MQConstant.CLEAR_STOCK_OVER_BROADCAST_QUEUE)
    public void onMessage(Message message, Long seckillId, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("[库存回滚] 接收清除售罄标识广播消息，商品ID: {}", seckillId);
            // 清除本地售罄标识缓存
            OrderInfoController.deleteKey(seckillId);
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[库存回滚] 处理清除售罄标识消息失败: {}", e.getMessage(), e);
            // 消息处理失败，拒绝消息并不重新入队
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
