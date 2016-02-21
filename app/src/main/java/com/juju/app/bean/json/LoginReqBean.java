package com.juju.app.bean.json;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/2/18 10:20
 * 版本：V1.0.0
 */
public class LoginReqBean extends BaseReqBean {

    private String userNo;

    private String password;

    public LoginReqBean(String userNo, String password) {
        this.userNo = userNo;
        this.password = password;
    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
