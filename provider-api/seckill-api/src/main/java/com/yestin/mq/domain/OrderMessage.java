package com.yestin.mq.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@NoArgsConstructor
public class OrderMessage implements Serializable {
    public OrderMessage(Integer time, Long seckillId, String token, Long userPhone) {
        this.time = time;
        this.seckillId = seckillId;
        this.token = token;
        this.userPhone = userPhone;
    }

    private Integer time; // 秒杀场次
    private Long seckillId; // 秒杀商品ID
    private String token; // 用户的token信息
    private Long userPhone; // 用户手机号码
    private String orderNo;
}
