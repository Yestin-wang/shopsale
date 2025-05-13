package com.yestin.job;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.yestin.redis.JobRedisKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Set;

@Component
@Setter
@Getter
@RefreshScope
@Slf4j
public class UserCacheJob implements SimpleJob {
    @Value("${job.userCache.cron}")
    private String cron;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public void execute(ShardingContext shardingContext) {

    }

    private void doWork() {
        // 获取日历对象
        Calendar calendar = Calendar.getInstance();
        // 获取七天前的日期
        calendar.add(Calendar.DATE, -7);
        Long max = calendar.getTime().getTime();
        String userZSetKey = JobRedisKey.USER_ZSET.join("");
        String userHashKey = JobRedisKey.USER_HASH.join("");
        Set<String> ids = redisTemplate.opsForZSet().rangeByScore(userZSetKey, 0, max);
        // 删除7天前的用户缓存数据
        if (ids.size() > 0) {
            redisTemplate.opsForHash().delete(userHashKey, ids.toArray());
            redisTemplate.opsForZSet().removeRangeByScore(userZSetKey, 0, max);
        }
    }
}
