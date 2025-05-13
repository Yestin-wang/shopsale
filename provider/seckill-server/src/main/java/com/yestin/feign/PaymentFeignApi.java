package com.yestin.feign;

import com.yestin.common.web.Result;
import com.yestin.domain.PayVo;
import com.yestin.domain.RefundVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("pay-service")
public interface PaymentFeignApi {
    @PostMapping("/alipay/prepay")
    Result<String> prepay(@RequestBody PayVo vo);

    @PostMapping("/alipay/refund")
    Result<Boolean> refund(@RequestBody RefundVo refundVo);
}
