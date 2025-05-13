package com.yestin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayResult {
    private String outTradeNo;
    private String TradeNo;
    private String totalAmount;
}
