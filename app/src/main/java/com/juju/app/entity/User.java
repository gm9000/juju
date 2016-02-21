package com.juju.app.entity;

import com.juju.app.entity.base.BaseEntity;
import com.lidroid.xutils.db.annotation.Column;

/**
 * 项目名称：juju
 * 类描述：用户 （测试使用）
 * 创建人：gm
 * 日期：2016/2/17 12:07
 * 版本：V1.0.0
 */
public class User extends BaseEntity {

    /**
     * 用户聚聚号
     */
    @Column(column = "userNo")
    private String userNo;

    /**
     * 用户手机号
     */
    @Column(column = "phone")
    private String phone;

    /**
     * 用户邮箱
     */
    @Column(column = "email")
    private String email;


    /**
     * 更新时间
     */
    @Column(column = "updateTime")
    private long updateTime;


    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
