package com.yestin.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yestin.mq.callback.DefaultSendCallback;
import com.yestin.mq.MQConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig implements RabbitListenerConfigurer {

    // 消息转换器
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // RabbitListener处理器工厂
    @Bean
    public MessageHandlerMethodFactory messageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(objectMapper);
        factory.setMessageConverter(converter);
        return factory;
    }

    // 配置RabbitListener
    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
    }

    @Bean
    public DirectExchange orderPendingExchange() {
        return ExchangeBuilder.directExchange(MQConstant.ORDER_PEDDING_TOPIC)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderPendingQueue() {
        return QueueBuilder.durable(MQConstant.ORDER_PEDDING_TOPIC + "_QUEUE")
                .build();
    }

    @Bean
    public Binding bindingOrderPending() {
        return BindingBuilder.bind(orderPendingQueue())
                .to(orderPendingExchange())
                .with("");
    }

    @Bean
    public DirectExchange orderResultExchange() {
        return ExchangeBuilder.directExchange(MQConstant.ORDER_RESULT_TOPIC)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderResultQueue() {
        return QueueBuilder.durable(MQConstant.ORDER_RESULT_TOPIC + "_QUEUE")
                .build();
    }

    @Bean
    public Binding bindingOrderResult() {
        return BindingBuilder.bind(orderResultQueue())
                .to(orderResultExchange())
                .with("");
    }

    @Bean
    public DirectExchange orderPayTimeoutExchange() {
        return ExchangeBuilder.directExchange(MQConstant.ORDER_PAY_TIMEOUT_TOPIC)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderPayTimeoutDelayQueue() {
        return QueueBuilder.durable(MQConstant.ORDER_PAY_TIMEOUT_TOPIC + "_DELAY_QUEUE")
                .withArgument("x-dead-letter-exchange", MQConstant.ORDER_PAY_TIMEOUT_TOPIC) // 死信交换机
                .withArgument("x-dead-letter-routing-key", "") // 死信路由键
                .withArgument("x-message-ttl", MQConstant.ORDER_PAY_TIMEOUT_DELAY_SECONDS * 1000) // 消息TTL
                .build();
    }

    @Bean
    public DirectExchange orderPayTimeoutDelayExchange() {
        return ExchangeBuilder.directExchange(MQConstant.ORDER_PAY_TIMEOUT_TOPIC + "_DELAY")
                .durable(true)
                .build();
    }

    @Bean
    public Binding bindingOrderPayTimeoutDelay() {
        return BindingBuilder.bind(orderPayTimeoutDelayQueue())
                .to(orderPayTimeoutDelayExchange())
                .with("");
    }

    @Bean
    public Queue orderPayTimeoutQueue() {
        return QueueBuilder.durable(MQConstant.ORDER_PAY_TIMEOUT_TOPIC + "_QUEUE")
                .build();
    }

    @Bean
    public Binding bindingOrderPayTimeout() {
        return BindingBuilder.bind(orderPayTimeoutQueue())
                .to(orderPayTimeoutExchange())
                .with("");
    }

    @Bean
    public DirectExchange cancelSeckillOverSigeExchange() {
        return ExchangeBuilder.directExchange(MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC)
                .durable(true)
                .build();
    }

    @Bean
    public Queue cancelSeckillOverSigeQueue() {
        return QueueBuilder.durable(MQConstant.CANCEL_SECKILL_OVER_SIGE_TOPIC + "_QUEUE")
                .build();
    }

    @Bean
    public Binding bindingCancelSeckillOverSige() {
        return BindingBuilder.bind(cancelSeckillOverSigeQueue())
                .to(cancelSeckillOverSigeExchange())
                .with("");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setConfirmCallback(new DefaultSendCallback("消息队列"));
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    // 库存售罄广播交换机
    @Bean
    public FanoutExchange stockOverBroadcastExchange() {
        return ExchangeBuilder.fanoutExchange(MQConstant.STOCK_OVER_BROADCAST_EXCHANGE)
                .durable(true)
                .build();
    }

    // 库存售罄广播队列
    @Bean
    public Queue stockOverBroadcastQueue() {
        return QueueBuilder.durable(MQConstant.STOCK_OVER_BROADCAST_QUEUE)
                .build();
    }

    // 绑定广播队列到广播交换机
    @Bean
    public Binding bindingStockOverBroadcast() {
        return BindingBuilder.bind(stockOverBroadcastQueue())
                .to(stockOverBroadcastExchange());
    }

    // 清空库存售罄标识广播交换机
    @Bean
    public FanoutExchange clearStockOverBroadcastExchange() {
        return ExchangeBuilder.fanoutExchange(MQConstant.CLEAR_STOCK_OVER_BROADCAST_EXCHANGE)
                .durable(true)
                .build();
    }

    // 清空库存售罄标识广播队列
    @Bean
    public Queue clearStockOverBroadcastQueue() {
        return QueueBuilder.durable(MQConstant.CLEAR_STOCK_OVER_BROADCAST_QUEUE)
                .build();
    }

    // 绑定广播队列到广播交换机
    @Bean
    public Binding bindingClearStockOverBroadcast() {
        return BindingBuilder.bind(clearStockOverBroadcastQueue())
                .to(clearStockOverBroadcastExchange());
    }
}