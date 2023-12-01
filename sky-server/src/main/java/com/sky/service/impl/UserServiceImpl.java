package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.constant.UserConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    //微信服务接口地址
    public static final String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private HttpClientUtil httpClientUtil;
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String code = userLoginDTO.getCode();
        //向微信接口发送请求获得openid
        String openId = getOpenId(code);
        //判断openId是否有效
        if(openId==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断当前用户是否为外卖服务的新用户
        User user=userMapper.getByOpenid(openId);
        //如果是新用户则自动注册
        if(user==null){
            user = User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .sex(UserConstant.SEX)
                    .phone(UserConstant.PHONE)
                    .avatar(UserConstant.AVATAR)
                    .idNumber(UserConstant.ID_NUMBER)
                    .name(UserConstant.NAME)
                    .build();
            userMapper.insert(user);
        }
        return user;
    }

    private String getOpenId(String code) {
        Map<String,String> map=new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = httpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject=JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
