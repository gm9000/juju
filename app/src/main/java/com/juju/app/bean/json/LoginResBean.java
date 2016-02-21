package com.juju.app.bean.json;

/**
 * 项目名称：juju
 * 类描述：登陆响应Bean
 * 创建人：gm
 * 日期：2016/2/17 18:34
 * 版本：V1.0.0
 */

public class LoginResBean extends BaseResBean {

    private String userNo;

    private String token;

    private int cycle;


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

    public int getCycle() {
        return cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
    }
}
