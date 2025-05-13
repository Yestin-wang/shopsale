package com.yestin.common.exception;

import com.yestin.common.web.CodeMsg;
import lombok.Data;

@Data
public class BusinessException extends RuntimeException {
    private CodeMsg codeMsg;

    public BusinessException(CodeMsg codeMsg) {
        super(codeMsg.getMsg());
        this.codeMsg = codeMsg;
    }
}
