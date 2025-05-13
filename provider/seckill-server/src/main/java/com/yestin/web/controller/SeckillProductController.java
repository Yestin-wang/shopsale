package com.yestin.web.controller;

import com.yestin.common.web.Result;
import com.yestin.domain.SeckillProductVo;
import com.yestin.service.ISeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seckillProduct")
@Slf4j
public class SeckillProductController {
    @Autowired
    private ISeckillProductService seckillProductService;

    @RequestMapping("/selectTodayListByTime")
    public Result<List<SeckillProductVo>> selectTodayListByTime(@RequestParam("time") Integer time) {
        return Result.success(seckillProductService.selectTodayListByTime(time));
    }

    @RequestMapping("/find")
    public Result<SeckillProductVo> findById(Integer time, Long seckillId) {
        return Result.success(seckillProductService.selectByIdAndTime(seckillId, time));
    }
}
