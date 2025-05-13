package com.yestin.common.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeMsg implements Serializable {
    private Integer code;
    private String msg;
    public static final CodeMsg ILLEGAL_OPERATION = new CodeMsg(500403, "非法操作");
}
