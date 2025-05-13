package com.yestin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.MD5Utils;
import com.yestin.common.domain.UserInfo;
import com.yestin.common.exception.BusinessException;
import com.yestin.domain.LoginLog;
import com.yestin.domain.UserLogin;
import com.yestin.domain.UserResponse;
import com.yestin.mapper.UserMapper;
import com.yestin.mq.LoginLogSender;
import com.yestin.mq.MQConstant;
import com.yestin.redis.CommonRedisKey;
import com.yestin.redis.UaaRedisKey;
import com.yestin.service.IUserService;
import com.yestin.util.MD5Util;
import com.yestin.web.msg.UAACodeMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    private LoginLogSender loginLogSender;

    private UserLogin getUser(Long phone) {
        UserLogin userLogin;
        String zSetKey = UaaRedisKey.USER_ZSET.getRealKey("");
        String hashKey = UaaRedisKey.USER_HASH.getRealKey("");
        String userKey = String.valueOf(phone);
        // 从redis缓存中查找
        String objStr = (String) redisTemplate.opsForHash().get(hashKey, String.valueOf(phone));
        if (StringUtils.isEmpty(objStr)) {
            // 缓存中没有从数据库中查询
            userLogin = userMapper.selectUserLoginByPhone(phone);
            // 将用户的登录信息存储到Hash结构中
            redisTemplate.opsForHash().put(hashKey, userKey, JSON.toJSONString(userLogin));
            // 使用zSet结构,value存用户手机号码，分数为登录时间，在定时器中找出7天前登录的用户，然后再缓存中删除.
        } else {
            userLogin = JSON.parseObject(objStr, UserLogin.class);
        }
        redisTemplate.opsForZSet().add(zSetKey, userKey, System.currentTimeMillis());

        return userLogin;
    }

    private UserInfo getByToken(String token) {
        String strobj = redisTemplate.opsForValue().get(CommonRedisKey.USER_TOKEN.getRealKey(token));
        if (StringUtils.isEmpty(strobj)) {
            return null;
        }
        return JSON.parseObject(strobj, UserInfo.class);
    }

    private String createToken(UserInfo userInfo) {
        // 创建token
        String token = UUID.randomUUID().toString().replace("-", "");
        // 将userInfo对象存入到redis中
        CommonRedisKey redisKey = CommonRedisKey.USER_TOKEN;
//        JSON.toJSONString(userInfo) 是使用 JSON 序列化工具（如 FastJSON）将 userInfo 对象转换为 JSON 字符串。
        // 这样做的目的是为了方便在 Redis 中存储复杂的对象，因为 Redis 只能存储字符串类型的数据。
        redisTemplate.opsForValue().set(redisKey.getRealKey(token), JSON.toJSONString(userInfo), redisKey.getExpireTime(), redisKey.getUnit());
        return token;
    }

    @Override
    public UserResponse login(Long phone, String password, String ip, String token) {
        LoginLog loginLog = new LoginLog(phone, ip, new Date());
        UserInfo userInfo = getByToken(token);
        // 如果token不在有效期之内进行登录操作
        if (userInfo == null) {
            // 根据用户手机号码查询用户对象
            UserLogin userLogin = this.getUser(phone);
            if (userLogin == null || !userLogin.getPassword().equals(MD5Util.encode(password, userLogin.getSalt()))) {
                // 登录失败
                loginLog.setState(LoginLog.LOGIN_FAIL);
                loginLogSender.sendLoginFailLog(loginLog);
                throw new BusinessException(UAACodeMsg.LOGIN_ERROR);
            }
            System.out.println("密码："+userLogin.getPassword());
            // 查询用户信息
            userInfo = userMapper.selectUserInfoByPhone(phone);

            // 设置登录IP
            if (userInfo != null && !StringUtils.isEmpty(ip)) {
                userInfo.setLoginIp(ip);
                System.out.println("已设置用户登录IP: " + ip);
            }

            token = createToken(userInfo);
            // 记录登录成功日志
            loginLogSender.sendLoginSuccessLog(loginLog);
        }
        return new UserResponse(token, userInfo);
    }
}
