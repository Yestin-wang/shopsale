package com.yestin.web.controller;

import com.yestin.common.web.CodeMsg;
import com.yestin.common.web.Result;
import com.yestin.domain.Product;
import com.yestin.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/product")
@Slf4j
public class ProductController {
    @Autowired
    private IProductService productService;

    @RequestMapping("/selectByIdList")
    public Result<List<Product>> selectByIdList(@RequestParam("ids") List<Long> idList) {
        return Result.success(productService.selectByIdList(idList));
    }
    
    /**
     * 查询所有商品
     * @param pageNum 页码，默认为1
     * @param pageSize 每页数量，默认为10
     * @return 商品列表
     */
    @RequestMapping("/queryAll")
    public Result<List<Product>> queryAll(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        log.info("查询所有商品: pageNum={}, pageSize={}", pageNum, pageSize);
        return Result.success(productService.queryAllProducts(pageNum, pageSize));
    }
    
    /**
     * 根据ID查询单个商品
     * @param id 商品ID
     * @return 商品详情
     */
    @RequestMapping("/findById")
    public Result<Product> findById(@RequestParam("id") Long id) {
        log.info("根据ID查询商品: id={}", id);
        List<Long> idList = new ArrayList<>();
        idList.add(id);
        List<Product> products = productService.selectByIdList(idList);
        if (products != null && !products.isEmpty()) {
            return Result.success(products.get(0));
        } else {
            return Result.error(new CodeMsg(11404,"未找到商品"));
        }
    }
}

