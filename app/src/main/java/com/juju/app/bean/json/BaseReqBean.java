package com.juju.app.bean.json;

/**
 * 项目名称：juju
 * 类描述：Json请求公共Bean
 * 创建人：gm
 * 日期：2016/2/18 09:48
 * 版本：V1.0.0
 */
public class BaseReqBean {
    private String userNo;
    private String token;

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
