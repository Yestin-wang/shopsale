package com.yestin.feign;

import com.yestin.common.web.Result;
import com.yestin.domain.Product;
import com.yestin.feign.fallback.ProductFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", fallback = ProductFeignFallback.class)
public interface ProductFeignApi {
    @RequestMapping("/product/selectByIdList")
    Result<List<Product>> selectByIdList(@RequestParam("ids") List<Long> idList);
}
