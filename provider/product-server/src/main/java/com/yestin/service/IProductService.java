package com.yestin.service;

import com.yestin.domain.Product;

import java.util.List;

public interface IProductService {
    List<Product> selectByIdList(List<Long> idList);
    
    /**
     * 查询所有商品，支持分页
     * @param pageNum 页码
     * @param pageSize 每页记录数
     * @return 商品列表
     */
    List<Product> queryAllProducts(Integer pageNum, Integer pageSize);
}
