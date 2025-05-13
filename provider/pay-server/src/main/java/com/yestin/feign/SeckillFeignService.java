package com.yestin.feign;

import com.yestin.common.web.Result;
import com.yestin.domain.PayResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("seckill-service")
public interface SeckillFeignService {
    @PostMapping("/orderPay/success")
    Result<?> updateOrderPaySuccess(@RequestBody PayResult result);

}
