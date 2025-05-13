package com.yestin.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeckillProduct {
    private Long id;//秒杀商品ID
    private Long productId;//商品ID
    private BigDecimal seckillPrice;//秒杀价格
    private Long integral;//积分
    private Integer stockCount;//库存总数
    private Date startDate;//秒杀日期
    private Integer time;//秒杀场次
}
