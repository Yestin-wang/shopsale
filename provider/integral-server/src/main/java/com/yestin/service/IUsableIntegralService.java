package com.yestin.service;


import com.yestin.domain.IntegralRefundVo;
import com.yestin.domain.OperateIntergralVo;
import com.yestin.domain.RefundVo;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface IUsableIntegralService {
    /**
     * 积分支付接口
     *
     * @param vo 积分支付 vo
     * @return 支付流水号
     */
    String doPay(OperateIntergralVo vo);

    /**
     * 积分退款
     *
     * @param vo 积分退款 vo
     * @return 是否退款成功
     */
    boolean refund(RefundVo vo);

    /**
     * 一阶段
     * TCC 的 try 方法，执行资源预留操作
     * 先检查余额是否足够，并且冻结积分
     *
     * @param vo      操作积分 vo 对象
     * @param context 事务上下文对象，该对象在 try 阶段传入 null 即可，RM 会自动创建并注入该参数
     * @return 账户流水 id
     */
    @TwoPhaseBusinessAction(name = "tryIncrIntegral", commitMethod = "commitIncrIntegral", rollbackMethod = "rollbackIncrIntegral")
    String tryIncrIntegral(@BusinessActionContextParameter(paramName = "integralVo") OperateIntergralVo vo, BusinessActionContext context);

    /**
     * 二阶段
     * TCC 的 confirm 方法，执行确认资源扣除操作
     * 扣除真实积分 & 扣除冻结积分
     * 增加账户变动流水记录
     *
     * @param context 事务上下文对象
     * @return 账户流水 id
     */
    void commitIncrIntegral(BusinessActionContext context);

    /**
     * 二阶段
     * TCC 的 rollback 方法，执行回滚 try 预留资源
     * 扣除 try 阶段冻结的金额
     *
     * @param context 事务上下文对象
     * @return 账户流水 id
     */
    void rollbackIncrIntegral(BusinessActionContext context);
}
