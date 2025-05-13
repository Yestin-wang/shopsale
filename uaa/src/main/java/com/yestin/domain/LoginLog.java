package com.yestin.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
public class LoginLog implements Serializable {
    public static Boolean LOGIN_SUCCESS = Boolean.TRUE;
    public static Boolean LOGIN_FAIL = Boolean.FALSE;

    private Long id; //自增id
    private Long phone;
    private String loginIp;
    private Date loginTime;
    private Boolean state = LOGIN_SUCCESS;

    public LoginLog(Long phone, String loginIp, Date loginTime) {
        this.phone = phone;
        this.loginIp = loginIp;
        this.loginTime = loginTime;
    }

}
