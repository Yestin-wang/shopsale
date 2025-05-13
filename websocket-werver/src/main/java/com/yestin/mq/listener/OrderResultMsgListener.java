package com.yestin.mq.listener;

import com.alibaba.fastjson.JSON;
import com.yestin.core.WebsocketServer;
import com.yestin.mq.MQConstant;
import com.yestin.mq.OrderMQResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.websocket.Session;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OrderResultMsgListener {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MQConstant.ORDER_RESULT_TOPIC + "_QUEUE")
    public void onMessage(Message message, OrderMQResult result, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[订单结果] 收到创建订单消息： {}", JSON.toJSONString(result));
        log.info("[订单结果] 收到订单消息，token={}", result.getToken());
        
        try {
            String token = result.getToken();
            if (token == null || token.isEmpty()) {
                log.error("[订单结果] 消息token为空，无法发送消息");
                channel.basicAck(deliveryTag, false); // 确认消息已处理
                return;
            }
            
            // 记录当前所有活跃会话
            log.info("[订单结果] 当前活跃会话数量: {}", WebsocketServer.SESSION_MAP.size());
            if (!WebsocketServer.SESSION_MAP.isEmpty()) {
                log.info("[订单结果] 当前活跃会话token列表: {}", WebsocketServer.SESSION_MAP.keySet());
            }
            
            // 使用指数退避策略进行重试
            int maxRetries = 3; // 减少重试次数，避免过多重试导致消息队列堆积
            int retryCount = 0;
            long retryInterval = 500; // 初始重试间隔500ms
            boolean messageSent = false;
            
            while (retryCount < maxRetries && !messageSent) {
                // 使用WebsocketServer的静态方法发送消息，而不是直接操作Session
                messageSent = WebsocketServer.sendMessage(token, JSON.toJSONString(result));
                
                if (messageSent) {
                    log.info("[订单结果] 消息发送成功: token={}", token);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
                
                retryCount++;
                if (retryCount < maxRetries) {
                    log.info("[订单结果] 第{}次发送失败，等待{}ms后重试...", retryCount, retryInterval);
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                    retryInterval *= 2; // 指数退避
                }
            }
            
            // 如果客户端确实没有连接，那么确认消息并记录日志，不要将消息重新放回队列
            // 这避免了消息队列中积累大量无法处理的消息
            log.warn("[订单结果] 经过{}次重试后无法发送消息: token={}", maxRetries, token);
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("[订单结果] 处理订单结果消息异常: {}", e.getMessage(), e);
            // 只有在处理过程中出现异常时才将消息重新放回队列
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
