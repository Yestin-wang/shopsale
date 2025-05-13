package com.yestin.config;

import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.yestin.job.SeckillProductInitJob;
import com.yestin.job.UserCacheJob;
import com.yestin.util.ElasticJobUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessJobConfig {

    @Bean(initMethod = "init")
    public SpringJobScheduler initUserCacheJob(CoordinatorRegistryCenter registryCenter, UserCacheJob userCacheJob) {
        LiteJobConfiguration jobConfiguration = ElasticJobUtil.createDefaultSimpleJobConfiguration(userCacheJob.getClass(), userCacheJob.getCron());
        return new SpringJobScheduler(userCacheJob, registryCenter, jobConfiguration);
    }

    @Bean(initMethod = "init")
    public SpringJobScheduler initSeckillProductListJob(CoordinatorRegistryCenter registryCenter, SeckillProductInitJob job) {
        LiteJobConfiguration jobConfiguration =
                ElasticJobUtil.createJobConfiguration(
                        job.getClass(),
                        job.getCron(),
                        job.getShardingCount(),
                        job.getShardingParameters(),
                        job.isDataFlow());
        return new SpringJobScheduler(job, registryCenter, jobConfiguration);
    }
}
