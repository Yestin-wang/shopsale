package com.yestin.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yestin.common.exception.BusinessException;
import com.yestin.domain.*;
import com.yestin.mapper.AccountLogMapper;
import com.yestin.mapper.AccountTransactionMapper;
import com.yestin.mapper.UsableIntegralMapper;
import com.yestin.service.IUsableIntegralService;
import com.yestin.util.IdGenerateUtil;
import com.yestin.web.msg.IntergralCodeMsg;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;


@Slf4j
@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;
    @Autowired
    private AccountLogMapper accountLogMapper;

    @Override
    public String tryIncrIntegral(OperateIntergralVo vo, BusinessActionContext context) {
        log.info("[积分支付] 执行一阶段 TRY 方法，准备冻结金额：xid={}, branchId={}, params={}", context.getXid(), context.getBranchId(), JSON.toJSONString(vo));
        // 1. 先检查是否已经回滚过
        AccountLog accountLog = accountLogMapper.selectByTxId(context.getXid());
        if (accountLog != null) {
            // 之前已经回滚过，就不能再继续执行 TRY 操作
            throw new BusinessException(IntergralCodeMsg.ILLEGAL_OPERATION);
        }
        // 2. 查询金额（totalAmount-freezedAmount），判断是否足够
        // 3. 增加冻结金额
        int row = usableIntegralMapper.freezeIntegral(vo.getUserId(), vo.getValue());
        if (row == 0) {
            // 余额不足
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }

        // 4. 插入事务记录
        accountLog = insertAccountLog(vo, context, AccountLog.ACCOUNT_LOG_STATUS_TRY);
        return accountLog.getTradeNo();
    }

    private AccountLog insertAccountLog(OperateIntergralVo vo, BusinessActionContext context, Integer status) {
        AccountLog accountLog;
        accountLog = this.buildAccountLog(vo.getOutTradeVo(), vo.getValue(), vo.getInfo(), AccountLog.TYPE_DECR);
        accountLog.setTxId(context.getXid());
        accountLog.setActionId(context.getBranchId() + "");
        accountLog.setStatus(status);
        if (status.equals(AccountLog.ACCOUNT_LOG_STATUS_CANCEL)) {
            accountLog.setTimestamp(System.currentTimeMillis());
        } else {
            accountLog.setTimestamp(status.longValue());
        }
        accountLogMapper.insert(accountLog);
        return accountLog;
    }

    @Override
    public void commitIncrIntegral(BusinessActionContext context) {
        JSONObject json = (JSONObject) context.getActionContext("integralVo");
        log.info("[积分支付] 执行二阶段 CONFIRM 方法，提交积分变动操作：xid={}, branchId={}, params={}", context.getXid(), context.getBranchId(), json);
        // 1. 先按照事务 id 查询日志记录对象
        AccountLog accountLog = accountLogMapper.selectByTxId(context.getXid());
        if (accountLog == null) {
            log.warn("[积分支付] 操作流程异常，未查询到一阶段 TRY 方法执行记录...");
            throw new BusinessException(IntergralCodeMsg.ILLEGAL_OPERATION);
        }
        // 2. 判断状态是否为以回滚
        if (accountLog.getStatus().equals(AccountLog.ACCOUNT_LOG_STATUS_CANCEL)) {
            log.warn("[积分支付] 操作流程异常，已执行过回滚操作...");
            throw new BusinessException(IntergralCodeMsg.ILLEGAL_OPERATION);
        } else if (accountLog.getStatus().equals(AccountLog.ACCOUNT_LOG_STATUS_CONFIRM)) {
            // 3. 判断状态是否为以提交
            log.warn("[积分支付] 重复执行 COMMIT 方法，执行幂等操作...");
            return;
        }

        // 4. 执行扣除总金额、冻结金额
        usableIntegralMapper.commitChange(json.getLong("userId"), json.getLong("value"));

        // 5. 更新账户日志变动的状态为 CONFIRM
        accountLogMapper.changeStatus(accountLog.getTradeNo(), AccountLog.ACCOUNT_LOG_STATUS_CONFIRM);
        return;
    }

    @Override
    public void rollbackIncrIntegral(BusinessActionContext context) {
        JSONObject json = (JSONObject) context.getActionContext("integralVo");
        log.info("[积分支付] 执行二阶段 ROLLBACK 方法，提交积分变动操作：xid={}, branchId={}, params={}", context.getXid(), context.getBranchId(), json);
        // 1. 先查询之前 TRY 阶段执行的记录是否存在
        AccountLog accountLog = accountLogMapper.selectByTxId(context.getXid());
        if (accountLog == null) {
            // 说明执行没有执行过 TRY 操作，执行空回滚，插入空回滚记录
            this.insertAccountLog(json.toJavaObject(OperateIntergralVo.class), context, AccountLog.ACCOUNT_LOG_STATUS_CANCEL);
            return;
        }

        if (accountLog.getStatus().equals(AccountLog.ACCOUNT_LOG_STATUS_CONFIRM)) {
            log.warn("[积分支付] 当前事务已经 COMMIT，无法执行 CANCEL，流程异常...");
            // 如果当前状态为已经 COMMIT，那说明流程异常，直接抛出异常
            throw new BusinessException(IntergralCodeMsg.ILLEGAL_OPERATION);
        } else if (accountLog.getStatus().equals(AccountLog.ACCOUNT_LOG_STATUS_CANCEL)) {
            log.warn("[积分支付] 重复执行 CANCEL 方法，执行幂等操作...");
            return;
        }

        // 2. 取消冻结
        usableIntegralMapper.unFreezeIntegral(json.getLong("userId"),
                json.getLong("value"));
        // 3. 将操作日志状态更新为回滚
        accountLogMapper.changeStatus(accountLog.getTradeNo(), AccountLog.ACCOUNT_LOG_STATUS_CANCEL);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String doPay(OperateIntergralVo vo) {
        // 1. 获取当前用户的积分账户，判断账户余额是否足够
        // 2. 扣除积分，更新时间
        int row = usableIntegralMapper.decrIntegral(vo.getUserId(), vo.getValue());
        if (row <= 0) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
        // 3. 记录账户变动日志
        AccountLog log = new AccountLog();
        log.setUserId(vo.getUserId());
        log.setAmount(vo.getValue());
        log.setInfo(vo.getInfo());
        log.setGmtTime(new Date());
        log.setOutTradeNo(vo.getOutTradeVo());
        // 生成交易流水号
        log.setTradeNo(IdGenerateUtil.get().nextId() + "");
        // 幂等性问题 将数据库中的(out_trade_no,type)设置成复合唯一索引
        // ALTER TABLE `shop_intergral`.`t_account_log` ADD UNIQUE INDEX `uk_out_trade_no_type` (`out_trade_no`, `type`);
        // type用来标识积分增/减 这样就能保证每个订单都有一次增减操作，分别代表支付/退款
        // 这样第二次保存就会重复，解决了feign请求重传等之类的操作的重复扣积分等问题
        accountLogMapper.insert(log);
        return log.getTradeNo();

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean refund(RefundVo vo) {
        // 1.基于订单编号查询支付流水
        AccountLog decrLog = accountLogMapper.selectByOutTradeNoAndType(vo.getOutTradeNo(), AccountLog.TYPE_DECR);
        if (decrLog == null) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_UNPAID);
        }
        // 2.通过支付流水中的支付金额，退回到用户积分账户
        BigDecimal refundAmount = new BigDecimal(vo.getRefundAmount());
        if (refundAmount.compareTo(new BigDecimal(decrLog.getAmount()))>0) {
            // 退款金额大于支付金额
            throw new BusinessException(IntergralCodeMsg.AMOUNT_ERROR);
        }
        // 注意数值转换
        usableIntegralMapper.addIntegral(decrLog.getUserId(), refundAmount.longValue());
        // 3. 记录新的退款流水记录
        AccountLog incrLog = new AccountLog();
        incrLog.setInfo(vo.getRefundReason());
        // 幂等性问题 => type与OutTradeNo保证了唯一索引, 用户再次退款会报SQL错误
        incrLog.setType(AccountLog.TYPE_INCR);
        incrLog.setOutTradeNo(vo.getOutTradeNo());
        incrLog.setAmount(refundAmount.longValue());
        incrLog.setTradeNo(IdGenerateUtil.get().nextId() + "");
        incrLog.setUserId(decrLog.getUserId());
        incrLog.setGmtTime(new Date());
        accountLogMapper.insert(incrLog);
        return true;
    }

    private AccountLog buildAccountLog(String orderNo, Long amount, String info, Integer type) {
        AccountLog log = new AccountLog();
        log.setAmount(amount);
        log.setInfo(info);
        log.setOutTradeNo(orderNo);

        log.setGmtTime(new Date());
        log.setType(type);
        log.setTradeNo(IdGenerateUtil.get().nextId() + "");
        return log;
    }
}
