package com.yestin.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderInfo implements Serializable {
    public static final Integer STATUS_ARREARAGE = 0;//未付款
    public static final Integer STATUS_ACCOUNT_PAID = 1;//已付款
    public static final Integer STATUS_CANCEL = 0;//手动取消订单
    public static final Integer STATUS_TIMEOUT = 0;//超时取消订单
    public static final Integer STATUS_REFUND = 4;//已退款
    public static final int PAY_TYPE_ONLINE = 0;//在线支付
    public static final int PAY_TYPE_INTEGRAL = 1;//积分支付

    @Id
    @Column(name = "order_no")
    private String orderNo;//订单编号
    @Column(name = "user_id")
    private Long userId;//用户ID
    @Column(name = "product_id")
    private Long productId;//商品ID
    @Column(name = "delivery_addr_id")
    private Long deliveryAddrId;//收货地址
    @Column(name = "product_name")
    private String productName;//商品名称
    @Column(name = "product_img")
    private String productImg;//商品图片
    @Column(name = "product_count")
    private Integer productCount;//商品总数
    @Column(name = "product_price")
    private BigDecimal productPrice;//商品原价
    @Column(name = "seckill_price")
    private BigDecimal seckillPrice;//秒杀价格
    private Long integral;//消耗积分
    private Integer status;//订单状态
    @Column(name = "create_date")
    private Date createDate;//订单创建时间
    @Column(name = "pay_date")
    private Date payDate;//订单支付时间
    @Column(name = "pay_type")
    private Integer payType;//支付方式 1-在线支付 2-积分支付
    @Column(name = "seckill_date")
    private Date seckillDate;//秒杀的日期
    @Column(name = "seckill_time")
    private Integer seckillTime;// 秒杀场次
    @Column(name = "seckill_id")
    private Long seckillId;//秒杀商品ID
}
