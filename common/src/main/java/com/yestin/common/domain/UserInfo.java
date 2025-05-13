package com.yestin.common.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo implements Serializable {
    private Long  phone;
    private String nickName;
    private String head;
    private String birthDay;
    private String info;
    private String loginIp;
}
