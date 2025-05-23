package com.yestin.redis;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

@Getter
public enum SeckillRedisKey {
    SECKILL_PRODUCT_LIST("today:seckill:products:"),
    SECKILL_ORDER_HASH("seckillOrderHash:"),
    SECKILL_STOCK_COUNT_HASH("seckillStockCount:"),
    SECKILL_REAL_COUNT_HASH("seckillRealCount:");

    private String prefix;
    private TimeUnit unit;
    private int expireTime;
    SeckillRedisKey(String prefix, TimeUnit unit, int expireTime) {
        this.prefix = prefix;
        this.unit = unit;
        this.expireTime = expireTime;
    }

    SeckillRedisKey(String prefix) {
        this.prefix = prefix;
    }

    public String join(String key) {
        return this.prefix + key;
    }
}
