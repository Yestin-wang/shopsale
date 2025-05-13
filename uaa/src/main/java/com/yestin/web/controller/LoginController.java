package com.yestin.web.controller;

import com.yestin.common.constants.CommonConstants;
import com.yestin.common.web.Result;
import com.yestin.domain.UserLogin;
import com.yestin.domain.UserResponse;
import com.yestin.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class LoginController {
    @Autowired
    private IUserService userService;
    
    @PostMapping({"/login", "/uaa/login"})
    public Result<UserResponse> login(Long phone, String pwd,
                                      @RequestHeader(value = CommonConstants.REAL_IP, required = false) String ip,
                                      @RequestHeader(value = CommonConstants.TOKEN_NAME, required = false) String token) {
        System.out.println("phone: " + phone + " pwd: " + pwd);
        System.out.println("收到登录请求，客户端IP: " + ip);
        //进行登录，并将这个token返回给前台
        UserResponse userResponse = userService.login(phone, pwd, ip, token);
        return Result.success(userResponse);
    }
}
