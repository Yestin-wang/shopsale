package com.yestin.web.controller;


import com.yestin.common.web.Result;
import com.yestin.domain.IntegralRefundVo;
import com.yestin.domain.OperateIntergralVo;
import com.yestin.domain.RefundVo;
import com.yestin.service.IUsableIntegralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/integral")
public class IntegralController {
    @Autowired
    private IUsableIntegralService usableIntegralService;

    @PostMapping("/prepay")
    public Result<String> prepay(@RequestBody OperateIntergralVo vo) {
        String tradeNo = usableIntegralService.doPay(vo);
        return Result.success(tradeNo);
    }

    @PostMapping("/refund")
    public Result<Boolean> refund(@RequestBody RefundVo vo) {
        boolean ret = usableIntegralService.refund(vo);
        return Result.success(ret);
    }
}
