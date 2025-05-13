package com.yestin.service;

import com.yestin.domain.UserResponse;

public interface IUserService {
    UserResponse login(Long phone, String pwd, String ip, String token);
}
