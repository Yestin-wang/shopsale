package com.yestin.service.impl;

import com.yestin.domain.Product;
import com.yestin.mapper.ProductMapper;
import com.yestin.service.IProductService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements IProductService {
    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Product> selectByIdList(List<Long> idList) {
        return productMapper.queryProductByIds(idList);
    }
    
    @Override
    public List<Product> queryAllProducts(Integer pageNum, Integer pageSize) {
        // 不再使用分页参数，直接返回所有商品
        return productMapper.queryAllProducts();
    }
}
