package com.yestin.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLogin implements Serializable {
    private Long phone;
    private String password;
    private String salt;
}
