package com.yestin.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.yestin.common.exception.BusinessException;
import com.yestin.common.web.CodeMsg;
import com.yestin.common.web.Result;
import com.yestin.config.AlipayConfig;
import com.yestin.config.AlipayProperties;
import com.yestin.domain.PayResult;
import com.yestin.domain.PayVo;
import com.yestin.domain.RefundVo;
import com.yestin.feign.SeckillFeignService;
import com.yestin.web.msg.PayCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alipay")
public class AlipayController {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AlipayProperties alipayProperties;
    @Autowired
    private SeckillFeignService seckillFeignService;

    @PostMapping("/refund")
    public Result<Boolean> refund(@RequestBody RefundVo refund) {
        log.info("[支付宝退款] 准备退款请求: {}", JSON.toJSONString(refund));
        AlipayTradeRefundRequest alipay_request = new AlipayTradeRefundRequest();

        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(refund.getOutTradeNo());
        model.setRefundAmount(refund.getRefundAmount());
        model.setRefundReason(refund.getRefundReason());
        alipay_request.setBizModel(model);

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(alipay_request);
            log.info("[支付宝退款] 收到支付宝退款响应结果: {}", JSON.toJSONString(response));
            if (response.isSuccess()) {
                // 判断是否支付成功
                if ("Y".equalsIgnoreCase(response.getFundChange())) {
                    return Result.success(true);
                }
                // 如果 fund_change = N 表示退款没有成功/需要等待结果确认
                return Result.success(false);
            } else {
                // 处理响应不成功的情况
                log.error("[支付宝退款] 退款请求失败: {}", response.getSubMsg());
                return Result.error(new CodeMsg(500001, response.getSubMsg()));
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return Result.error(new CodeMsg(500001, e.getMessage()));
        }
    }
    @GetMapping("/return_url")
    public String returnUrl(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            try {
                valueStr = new String(valueStr.getBytes("ISO-8859-1"), alipayProperties.getCharset());
            } catch (UnsupportedEncodingException e) {
                log.error("字符转换异常", e);
            }
            params.put(name, valueStr);
        }

        try {
            log.info("[同步回调] 收到交易消息：{}", params);
            log.info("[同步回调] 签名类型：{}，支付宝公钥：{}", alipayProperties.getSignType(), alipayProperties.getAlipayPublicKey());
            boolean verify_result = AlipaySignature.rsaCheckV1(params, alipayProperties.getAlipayPublicKey(), alipayProperties.getCharset(), alipayProperties.getSignType());
            if (!verify_result) {
                log.error("[同步回调] 验签失败");
                throw new BusinessException(PayCodeMsg.PAY_ERROR);
            }
            // 商户订单号
            String outTradeNo = request.getParameter("out_trade_no");
            log.info("[同步回调] 验签成功，订单号：{}", outTradeNo);
            return outTradeNo;
        } catch (AlipayApiException e) {
            log.error("[同步回调] 验签异常", e);
            e.printStackTrace();
        }
        return "签名验证失败";
    }

    @PostMapping("/notify_url")
    public String notifyUrl(HttpServletRequest request) {
        // 接收支付宝参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决。这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            try {
                valueStr = new String(valueStr.getBytes("ISO-8859-1"), alipayProperties.getCharset());
            } catch (UnsupportedEncodingException e) {
                log.error("字符转换异常", e);
            }
            params.put(name, valueStr);
        }
        try {
            log.info("[异步回调] 收到交易已完成消息: {}", params);
            log.info("[异步回调] 签名类型：{}，支付宝公钥：{}", alipayProperties.getSignType(), alipayProperties.getAlipayPublicKey());
            boolean verify_result = AlipaySignature.rsaCheckV1(params, alipayProperties.getAlipayPublicKey(), alipayProperties.getCharset(), alipayProperties.getSignType());
            if (verify_result) {
                // 商户订单号
                String out_trade_no = request.getParameter("out_trade_no");
                // 支付宝交易号
                String trade_no = request.getParameter("trade_no");
                // 交易状态
                String trade_status = request.getParameter("trade_status");
                // 支付金额
                String totalAmount = request.getParameter("total_amount");

                log.info("[异步回调] 验签成功，订单号：{}，交易号：{}，交易状态：{}", out_trade_no, trade_no, trade_status);

                if (trade_status.equals("TRADE_FINISHED")) {
                    log.info("[异步回调] {}订单已完成", out_trade_no);
                } else if (trade_status.equals("TRADE_SUCCESS")) {
                    log.info("[异步回调] {}订单已支付", out_trade_no);
                    // 远程调用秒杀服务更新订单状态
                    PayResult payResult = new PayResult(out_trade_no, trade_no, totalAmount);
                    Result<?> result = seckillFeignService.updateOrderPaySuccess(payResult);
                    if (result.hasError()) {
                        log.error("[异步回调] 更新订单状态失败：{}", result.getMsg());
                        throw new BusinessException(PayCodeMsg.PAY_ERROR);
                    }
                    log.info("[异步回调] 更新订单状态成功");
                }
                return "success";
            } else {
                log.error("[异步回调] 验签失败");
            }
        } catch (AlipayApiException e) {
            log.error("[异步回调] 验签异常", e);
            e.printStackTrace();
        }
        return "fail";
    }

    @PostMapping("/prepay")
    public Result<String> prepay(@RequestBody PayVo payVo) {
        // 支付宝SDK API向支付宝发起支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 必传参数
        JSONObject bizContent = new JSONObject();
        // 商户订单号
        bizContent.put("out_trade_no", payVo.getOutTradeNo());
        // 支付金额
        bizContent.put("total_amount", payVo.getTotalAmount());
        // 订单标题
        bizContent.put("subject", payVo.getSubject());
        bizContent.put("body", payVo.getBody());
        // 电脑网站支付场景固定传值
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        request.setBizContent(bizContent.toString());
        // 异步接收地址 仅支持http/https
        request.setNotifyUrl(payVo.getNotifyUrl() != null ? payVo.getNotifyUrl() : alipayProperties.getNotifyUrl());
        // 同步跳转地址 仅支持http/https
        request.setReturnUrl(payVo.getReturnUrl() != null ? payVo.getReturnUrl() : alipayProperties.getReturnUrl());
        
        // 记录请求参数，用于调试
        log.info("支付宝支付请求参数: bizContent={}, notifyUrl={}, returnUrl={}", 
                bizContent.toString(), request.getNotifyUrl(), request.getReturnUrl());
        log.info("支付宝配置信息: appId={}, signType={}, charset={}, gatewayUrl={}", 
                alipayProperties.getAppId(), alipayProperties.getSignType(), 
                alipayProperties.getCharset(), alipayProperties.getGatewayUrl());
            
        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            log.info("支付宝响应完整信息: {}", JSON.toJSONString(response));
            
            if (response.isSuccess()) {
                log.info("支付宝预支付请求成功: {}", payVo.getOutTradeNo());
                return Result.success(response.getBody());
            } else {
                log.error("支付宝预支付请求失败: {}, 错误码: {}, 错误信息: {}",
                        payVo.getOutTradeNo(), response.getCode(), response.getMsg());
                return Result.defalutError();
            }
        } catch (AlipayApiException e) {
            log.error("支付宝预支付请求异常", e);
            return Result.defalutError();
        }
    }
}
