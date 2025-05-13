package com.yestin.feign.fallback;

import com.yestin.common.web.Result;
import com.yestin.domain.Product;
import com.yestin.feign.ProductFeignApi;

import java.util.List;

public class ProductFeignFallback implements ProductFeignApi {

    @Override
    public Result<List<Product>> selectByIdList(List<Long> idList) {
        return null;
    }
}
