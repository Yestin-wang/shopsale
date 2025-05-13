package com.yestin.feign;

import com.yestin.common.web.Result;
import com.yestin.domain.OperateIntergralVo;
import com.yestin.domain.RefundVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("integral-service")
public interface IntegralFeignApi {
    @PostMapping("/integral/prepay")
    Result<String> prepay(@RequestBody OperateIntergralVo vo);

    @PostMapping("/integral/refund")
    Result<Boolean> refund(@RequestBody RefundVo refundVo);
}
