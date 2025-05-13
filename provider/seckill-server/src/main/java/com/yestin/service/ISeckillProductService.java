package com.yestin.service;

import com.yestin.domain.SeckillProduct;
import com.yestin.domain.SeckillProductVo;
import com.yestin.service.impl.SeckillProductServiceImpl;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

public interface ISeckillProductService {
    List<SeckillProductVo> selectTodayListByTime(Integer time);


    SeckillProductVo selectByIdAndTime(Long seckillId, Integer time);

    void decrStockCount(Long id, Integer time);

    void decrStockCount(Long id);

    SeckillProduct findById(Long id);

    void incryStockCount(Long seckillId);

    Long selectStockById(Long seckillId);
}
