package com.yestin.mapper;

import com.yestin.common.domain.UserInfo;
import com.yestin.domain.LoginLog;
import com.yestin.domain.UserLogin;

public interface UserMapper {
    UserLogin selectUserLoginByPhone(Long phone);

    UserInfo selectUserInfoByPhone(Long phone);

    int insertLoginLog(LoginLog loginLog);

}
