package com.yestin.mq.callback;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
public class DefaultSendCallback implements RabbitTemplate.ConfirmCallback {
    
    private String operationType; // 操作类型，用于区分不同的操作
    
    public DefaultSendCallback() {
        this.operationType = "默认操作";
    }
    
    public DefaultSendCallback(String operationType) {
        this.operationType = operationType;
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.info("[{}] 消息发送成功，消息id={}", operationType, correlationData != null ? correlationData.getId() : "");
        } else {
            log.info("[{}] 消息发送失败:{}", operationType, cause);
        }
    }
} 