package com.yestin.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SeckillProductVo extends SeckillProduct implements Serializable {
    private String productName;
    private String productTitle;
    private String productImg;
    private String productDetail;
    private BigDecimal productPrice;
    private Integer currentCount;
}