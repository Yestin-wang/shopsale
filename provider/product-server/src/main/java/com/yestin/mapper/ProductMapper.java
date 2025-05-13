package com.yestin.mapper;

import com.yestin.domain.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {
    /**
     * 根据id集合查询商品对象xinxi
     */
    List<Product> queryProductByIds(@Param("ids") List<Long> ids);
    
    /**
     * 查询所有商品
     * @return 所有商品列表
     */
    List<Product> queryAllProducts();
}
