package com.yestin.job;

import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.yestin.common.web.Result;
import com.yestin.domain.SeckillProductVo;
import com.yestin.feign.SeckillProductFeignApi;
import com.yestin.redis.SeckillRedisKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.annotation.Reference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
@Data
@RefreshScope
@Slf4j
public class SeckillProductInitJob implements SimpleJob {
    @Value("${job.seckillProduct.cron}")
    private String cron;
    @Value("${job.seckillProduct.shardingCount}")
    private Integer shardingCount;
    @Value("${job.seckillProduct.shardingParameters}")
    private String shardingParameters;
    @Value("${job.seckillProduct.dataFlow}")
    private boolean dataFlow;

    @Autowired
    private SeckillProductFeignApi seckillProductFeignApi;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void execute(ShardingContext shardingContext) {
        // 分片参数利用场次进行分片
        String time = shardingContext.getShardingParameter();
        // 清空之前的数据
        String key = SeckillRedisKey.SECKILL_PRODUCT_LIST.join(time);
        stringRedisTemplate.delete(key);
        // 调用秒杀服务接口，查询秒杀商品数据
        Result<List<SeckillProductVo>> result = seckillProductFeignApi.selectTodayListByTime(Integer.valueOf(time));
        if (result.hasError() || result.getData() == null) {
            log.warn("[秒杀商品数据预热] 查询秒杀商品数据失败, 远程服务异常. res={}", JSON.toJSONString(result));
            return;
        }
        
        List<SeckillProductVo> productVoList = result.getData();
        log.info("[秒杀商品数据预热] 准备开始预热秒杀商品数据, 当前场次:{}, 本次缓存的数据:{}", time, productVoList.size());
        
        // 获取当前日期
        Date today = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = dateFormat.format(today);
        
        for (SeckillProductVo vo : productVoList) {
            // 将秒杀商品的日期修改为当前日期
            try {
                vo.setStartDate(dateFormat.parse(todayStr));
            } catch (ParseException e) {
                log.error("[秒杀商品数据预热] 日期格式转换错误", e);
            }
            
            String json = JSON.toJSONString(vo);
            stringRedisTemplate.opsForList().rightPush(key, json);
            // 保存库存  外部 key = seckillStockCount:{time}
            // hash key = {seckillId} hash value = 库存
            String stockCountKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.join(time + "");
            stringRedisTemplate.opsForHash().put(stockCountKey, vo.getId() + "", vo.getStockCount() + "");
        }
        
        log.info("[秒杀商品数据预热] 数据预热完成，所有商品日期已更新为当前日期: {}", todayStr);
    }
}
