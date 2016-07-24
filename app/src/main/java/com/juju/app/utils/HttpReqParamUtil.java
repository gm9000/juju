package com.juju.app.utils;

import com.juju.app.bean.UserInfoBean;

import java.util.HashMap;
import java.util.Map;

/**
 * 项目名称：juju
 * 类描述：http请求参数
 * 创建人：gm
 * 日期：2016/6/18 12:50
 * 版本：V1.0.0
 */
public class HttpReqParamUtil {

    //此属性需要定一个全局变量 共享
    private UserInfoBean userInfoBean;


    private HttpReqParamUtil() {

    }

    private volatile static HttpReqParamUtil inst;


    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static HttpReqParamUtil instance() {
        if(inst == null) {
            synchronized (HttpReqParamUtil.class) {
                if (inst == null) {
                    inst = new HttpReqParamUtil();
                }
            }
        }
        return inst;
    }

    public void setUserInfoBean(UserInfoBean userInfoBean) {
        this.userInfoBean = userInfoBean;
    }

    public Map<String, Object> buildMap(String keys, Object... objs) {
        if(StringUtils.isBlank(keys) || objs == null || objs.length == 0)
            throw new IllegalArgumentException("buildMap#keys or objs is not null");

        String[] keyArr = keys.split(",");
        if(keyArr.length != objs.length)
            throw new IllegalArgumentException("buildMap#key/value pair does not match");

        Map<String, Object> map = new HashMap<String,Object>();
        if(userInfoBean != null) {
            map.put("userNo", userInfoBean.getUserNo());
            map.put("token", userInfoBean.getToken());
        }
        for (int i = 0; i < keyArr.length; i++) {
            map.put(keyArr[i].trim(), objs[i]);
        }
        return map;
    }

    public Map<String, Object> buildMap() {
        Map<String, Object> map = new HashMap<>();
        if(userInfoBean != null) {
            map.put("userNo", userInfoBean.getUserNo());
            map.put("token", userInfoBean.getToken());
        }
        return map;
    }

}
