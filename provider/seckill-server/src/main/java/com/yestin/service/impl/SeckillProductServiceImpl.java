package com.yestin.service.impl;

import com.yestin.common.domain.UserInfo;
import com.yestin.common.exception.BusinessException;
import com.yestin.common.web.CodeMsg;
import com.yestin.common.web.Result;
import com.yestin.domain.OrderInfo;
import com.yestin.domain.Product;
import com.yestin.domain.SeckillProduct;
import com.yestin.domain.SeckillProductVo;
import com.yestin.feign.ProductFeignApi;
import com.yestin.mapper.OrderInfoMapper;
import com.yestin.mapper.SeckillProductMapper;
import com.yestin.service.IOrderInfoService;
import com.yestin.service.ISeckillProductService;
import com.yestin.util.IdGenerateUtil;
import com.yestin.web.msg.SeckillCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.cache.annotation.Cacheable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@CacheConfig(cacheNames = "SeckillProduct")
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignApi productFeignApi;
    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @Override
    public List<SeckillProductVo> selectTodayListByTime(Integer time) {
        // 查询当天的所有没秒杀商品数据
        List<SeckillProduct> todayList = seckillProductMapper.queryCurrentlySeckillProduct(time);
        if (todayList.size() == 0) {
            return Collections.emptyList();
        }
        // 遍历秒杀商品，得到商品id列表
        List<Long> productIdList = todayList.stream()
                .map(SeckillProduct::getProductId)
                .distinct()
                .collect(Collectors.toList());
        // 根据商品id列表，得到商品列表
        Result<List<Product>> response = productFeignApi.selectByIdList(productIdList);
        if (response.hasError() || response.getData() == null) {
            throw new BusinessException(new CodeMsg(response.getCode(), response.getMsg()));
        }
        List<Product> products = response.getData();
        // 将商品对象与秒杀商品对象聚合在一起 SeckillProduct->SeckillProductVo
        List<SeckillProductVo> productVoList = todayList.stream().map((sp) -> {
            SeckillProductVo vo = new SeckillProductVo();
            BeanUtils.copyProperties(sp, vo);
            // 将商品信息与秒杀商品信息聚合在一起 SeckillProductVo + = Product
            List<Product> sameProducts = products.stream().filter((p) -> {
                return sp.getProductId().equals(p.getId());
            }).collect(Collectors.toList());
            if (sameProducts.size() > 0) {
                Product product = sameProducts.get(0);
                BeanUtils.copyProperties(product, vo);
            }
            // product会将其id传递给vo中导致错误，重新传递秒杀商品的id
            vo.setId(sp.getId());
            return vo;
        }).collect(Collectors.toList());
        return productVoList;
    }


    @Override
    @Cacheable(key = "'selectByIdAndTime:'+ #time+':'+#seckillId")
    public SeckillProductVo selectByIdAndTime(Long seckillId, Integer time) {
        SeckillProduct seckillProduct = seckillProductMapper.selectByIdAndTime(seckillId, time);
        Result<List<Product>> result = productFeignApi.selectByIdList(Collections.singletonList(seckillProduct.getProductId()));
        if (result.hasError() || result.getData() == null || result.getData().size() == 0) {
            throw new BusinessException(new CodeMsg(result.getCode(), result.getMsg()));
        }
        Product product = result.getData().get(0);
        SeckillProductVo vo = new SeckillProductVo();
        BeanUtils.copyProperties(product, vo);
        // 覆盖id属性！！！
        BeanUtils.copyProperties(seckillProduct, vo);
        return vo;
    }

    @CacheEvict(key = "'selectByIdAndTime'+#id")
    @Override
    public void decrStockCount(Long id) {
        int row = seckillProductMapper.ol_decrStock(id);
        if (row <= 0) throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
    }

    @CacheEvict(key = "'selectByIdAndTime:' + #time + ':' + #id")
    @Override
    public void decrStockCount(Long id, Integer time) {
        String key = "seckill:product:stockcount:" + time + ":" + id;
        String threadId = "";
        // 定义过期时间
        long timeout = 10;
        ScheduledFuture<?> future = null;
        try {
            // 如果自旋五次，抛出异常
            int count = 0;
            boolean ret;
            do {
                // 雪花算法生成分布式唯一id
                threadId = IdGenerateUtil.get().nextId() + "";
                // 对秒杀商品加锁
                ret = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(threadId), timeout, TimeUnit.SECONDS));
                // 获取到锁继续往下执行
                if (ret) break;
                count++;
                if (count >= 5) throw new BusinessException(SeckillCodeMsg.SERVER_BUSY);
                // 一段时间后再重新请求锁，防止CPU过于频繁
                Thread.sleep(20);
            } while (true);

            // 加锁成功 创建watchdog监听业务是否执行完成，实现续期操作
            long delayTime = (long) (timeout * 0.8);
            final String finalThreadId = threadId;
            future = scheduledExecutorService.scheduleAtFixedRate(() -> {
                // 查询redis锁是否存在->业务执行未完成
                String value = redisTemplate.opsForValue().get(key);
                if (finalThreadId.equals(value)) {
                    // 将当前的redis锁续期
                    redisTemplate.expire(key, delayTime + 2, TimeUnit.SECONDS);
                    System.out.println("[WatchDog]-------------------执行Redis锁续期操作" + key);
                }
            }, delayTime, delayTime, TimeUnit.SECONDS);

            TimeUnit.SECONDS.sleep(11);
            // 先查询库存再扣库存
            Long stockCount = seckillProductMapper.getStockCount(id);
            if (stockCount <= 0) {
                throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
            }
            seckillProductMapper.decrStock(id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (future != null) {
                future.cancel(true);
            }
            // 先获取value，判断当前value是否与threadId相同
            String value = redisTemplate.opsForValue().get(key);
            if (threadId.equals(value))
                // 释放锁
                redisTemplate.delete(key);
        }
    }

    @Override
    public SeckillProduct findById(Long id) {
        return seckillProductMapper.selectById(id);
    }

    @Override
    public void incryStockCount(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }

    @Override
    public Long selectStockById(Long seckillId) {
        return seckillProductMapper.getStockCount(seckillId);
    }
}
