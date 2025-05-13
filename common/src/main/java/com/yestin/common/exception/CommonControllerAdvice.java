package com.yestin.common.exception;

import com.yestin.common.web.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
public class CommonControllerAdvice {
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result<?> handleBusinessException(BusinessException exception) {
        log.warn("[业务异常]: ", exception);
        return Result.error(exception.getCodeMsg());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<?> handleDefaultException(Exception exception) {
        log.error("[通用异常]", exception);
        return Result.defalutError();
    }
}
