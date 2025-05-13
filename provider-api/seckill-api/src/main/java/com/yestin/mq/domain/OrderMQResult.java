package com.yestin.mq;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderMQResult implements Serializable {
    private Integer time;//秒杀场次
    private Long seckillId;//秒杀商品id
    private String orderNo;//订单编号
    private String msg = "订单创建成功";//提示消息
    private Integer code = 200;//状态码
    private String token;//用户token
}
