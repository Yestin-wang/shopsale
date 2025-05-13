package com.yestin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundVo implements Serializable {
    private String outTradeNo;
    private String refundAmount;
    private String refundReason;
}
