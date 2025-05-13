package com.yestin.web.controller;

import com.alibaba.fastjson.JSON;
import com.yestin.common.constants.CommonConstants;
import com.yestin.common.domain.UserInfo;
import com.yestin.common.exception.BusinessException;
import com.yestin.common.web.Result;
import com.yestin.common.web.anno.RequireLogin;
import com.yestin.common.web.resolver.RequestUser;
import com.yestin.domain.OrderInfo;
import com.yestin.domain.SeckillProductVo;
import com.yestin.mq.callback.DefaultSendCallback;
import com.yestin.mq.MQConstant;
import com.yestin.mq.domain.OrderMessage;
import com.yestin.redis.SeckillRedisKey;
import com.yestin.service.IOrderInfoService;
import com.yestin.service.ISeckillProductService;
import com.yestin.web.msg.SeckillCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public static final Map<Long, Boolean> STOCK_OVER_FLOW_MAP = new ConcurrentHashMap<>();

    public static void deleteKey(Long key) {
        STOCK_OVER_FLOW_MAP.remove(key);
    }

    //    @RequireLogin
//    @RequestMapping("/doSeckill")
//    public Result<String> doSeckill(Integer time, Long seckillId, @RequestUser UserInfo userInfo) {
//        // 判断库存是否已经卖完
//        Boolean flag = STOCK_OVER_FLOW_MAP.get(seckillId);
//        if (flag != null && flag) {
//            return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
//        }
//
//        // 基于秒杀id+场次查询秒杀商品对象
//        SeckillProductVo sp = seckillProductService.selectByIdAndTime(seckillId, time);
//        if (sp == null) {
//            return Result.error(SeckillCodeMsg.OP_ERROR);
//        }
//        // 判断当前时间是否在秒杀时间范围内
//        boolean isTime = betweenSeckillTime(sp);
//        if (!isTime) {
//            return Result.error(SeckillCodeMsg.OVER_TIME_ERROR);
//        }
//
//        // 判断用户是否已经下过订单
//        String userOrderFlag = SeckillRedisKey.SECKILL_ORDER_HASH.join(seckillId + "");
//        Boolean absent = redisTemplate.opsForHash().putIfAbsent(userOrderFlag, userInfo.getPhone() + "", "1");
//        if (!absent) return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
//
//        String orderNo = null;
//        try {
//            // 判断库存是否充足
//            String hashKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.join(time + "");
//            // 返回剩余库存
//            Long remain = redisTemplate.opsForHash().increment(hashKey, seckillId + "", -1);
//            if (remain < 0) throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
//            // 创建订单，扣除库存，返回订单id
//            orderNo = orderInfoService.doSeckill(userInfo, sp);
//        } catch (BusinessException e) {
//            // 当库存不够时，直接标记当前库存已经卖完
//            STOCK_OVER_FLOW_MAP.put(seckillId, true);
//            redisTemplate.opsForHash().delete(userOrderFlag, userInfo.getPhone() + "");
//            return Result.error(e.getCodeMsg());
//        }
//
//        return Result.success(orderNo);
//    }
//    @RequireLogin
//    @RequestMapping("/doSeckill")
//    public Callable<Result<String>> doSeckill(Integer time, Long seckillId, @RequestUser UserInfo userInfo) {
//        return () -> {
//            // 判断库存是否已经卖完
//            Boolean flag = STOCK_OVER_FLOW_MAP.get(seckillId);
//            if (flag != null && flag) {
//                return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
//            }
//
//            // 基于秒杀id+场次查询秒杀商品对象
//            SeckillProductVo sp = seckillProductService.selectByIdAndTime(seckillId, time);
//            if (sp == null) {
//                return Result.error(SeckillCodeMsg.OP_ERROR);
//            }
//            // 判断当前时间是否在秒杀时间范围内
//            boolean isTime = betweenSeckillTime(sp);
//            if (!isTime) {
//                return Result.error(SeckillCodeMsg.OVER_TIME_ERROR);
//            }
//
//            // 判断用户是否已经下过订单
//            String userOrderFlag = SeckillRedisKey.SECKILL_ORDER_HASH.join(seckillId + "");
//            Boolean absent = redisTemplate.opsForHash().putIfAbsent(userOrderFlag, userInfo.getPhone() + "", "1");
//            if (!absent) return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
//
//            String orderNo = null;
//            try {
//                // 判断库存是否充足
//                String hashKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.join(time + "");
//                // 返回剩余库存
//                Long remain = redisTemplate.opsForHash().increment(hashKey, seckillId + "", -1);
//                if (remain < 0) throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
//
//                // 异步创建订单，扣除库存，返回订单id
//                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> orderInfoService.doSeckill(userInfo, sp));
//                return Result.success(future.get());
//
//            } catch (BusinessException e) {
//                // 当库存不够时，直接标记当前库存已经卖完
//                STOCK_OVER_FLOW_MAP.put(seckillId, true);
//                redisTemplate.opsForHash().delete(userOrderFlag, userInfo.getPhone() + "");
//                return Result.error(e.getCodeMsg());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            return Result.defalutError();
//        };
//    }
    @RequireLogin
    @RequestMapping("/doSeckill")
    public Callable<Result<String>> doSeckill(Integer time, Long seckillId, @RequestUser UserInfo userInfo,@RequestHeader(CommonConstants.TOKEN_NAME) String token) {
        System.out.println("time" + time);
        System.out.println("secId" + seckillId);
        return () -> {
            // 判断库存是否已经卖完
            Boolean flag = STOCK_OVER_FLOW_MAP.get(seckillId);
            if (flag != null && flag) {
                return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
            }

            // 基于秒杀id+场次查询秒杀商品对象
            SeckillProductVo sp = seckillProductService.selectByIdAndTime(seckillId, time);
            if (sp == null) {
                return Result.error(SeckillCodeMsg.OP_ERROR);
            }
            // 判断当前时间是否在秒杀时间范围内
//            Integer anotherTime = 10;
//            if (time.equals(anotherTime))
            boolean isTime = betweenSeckillTime(sp);
            if (!isTime) {
                return Result.error(SeckillCodeMsg.OVER_TIME_ERROR);
            }

            // 判断用户是否已经下过订单
            String userOrderFlag = SeckillRedisKey.SECKILL_ORDER_HASH.join(seckillId + "");
            Boolean absent = redisTemplate.opsForHash().putIfAbsent(userOrderFlag, userInfo.getPhone() + "", "1");
            if (!absent) return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
            try {
                // 判断库存是否充足
                String hashKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.join(time + "");
                // 返回剩余库存
                Long remain = redisTemplate.opsForHash().increment(hashKey, seckillId + "", -1);
                if (remain < 0) throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);

                // 发送mq异步消息
                OrderMessage orderMessage = new OrderMessage(time, seckillId, token, userInfo.getPhone());
                log.info("[秒杀下单] 发送订单创建消息: {}", JSON.toJSONString(orderMessage));
                try {
                    rabbitTemplate.convertAndSend(
                            MQConstant.ORDER_PEDDING_TOPIC,
                            "",
                            orderMessage
                    );
                    log.info("[秒杀下单] 订单创建消息发送成功");
                    return Result.success("订单创建中，请稍后...");
                } catch (Exception e) {
                    log.error("[秒杀下单] 订单创建消息发送失败: {}", e.getMessage(), e);
                    return Result.error(SeckillCodeMsg.SECKILL_ERROR);
                }

            } catch (BusinessException e) {
                // 当库存不够时，直接标记当前库存已经卖完
                STOCK_OVER_FLOW_MAP.put(seckillId, true);
                redisTemplate.opsForHash().delete(userOrderFlag, userInfo.getPhone() + "");
                return Result.error(e.getCodeMsg());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return Result.defalutError();
        };
    }


    private boolean betweenSeckillTime(SeckillProductVo sp) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(sp.getStartDate());
        // 设置小时
        instance.set(Calendar.HOUR_OF_DAY, sp.getTime());
        // 秒杀开始时间
        Date startTime = instance.getTime();
        // 秒杀结束时间 = 秒杀开始时间 + 2小时
        instance.add(Calendar.HOUR_OF_DAY, 2);
        Date endTime = instance.getTime();
        // 当前时间
//        long now = System.currentTimeMillis();
        // 创建一个 LocalDateTime 对象，表示 2020-12-03 09:00:00
        LocalDateTime dateTime = LocalDateTime.of(2020, 12, 3, 11, 0, 0);
        // 将 LocalDateTime 对象转换为时间戳（毫秒数）
        long now = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();

        return startTime.getTime() <= now && now < endTime.getTime();
    }


}
