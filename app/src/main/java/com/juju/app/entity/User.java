package com.juju.app.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.json.JsonDateDeserializer;
import com.juju.app.utils.json.JsonDateSerializer;
import com.lidroid.xutils.db.annotation.Column;
import com.lidroid.xutils.db.annotation.Table;
import com.lidroid.xutils.db.annotation.Transient;

import java.util.Date;

/**
 * 项目名称：juju
 * 类描述：用户 （测试使用）
 * 创建人：gm
 * 日期：2016/2/17 12:07
 * 版本：V1.0.0
 */
@Table(name = "user")
public class User extends BaseEntity {

    /**
     * 用户聚聚号
     */
    @Column(column = "userNo")
    private String userNo;

    /**
     * 用户手机号
     */
    @Column(column = "userPhone")
    private String userPhone;

    /**
     * 用户邮箱
     */
    @Column(column = "email")
    private String email;


    /**
     * 更新时间
     */
    @Column(column = "updateTime")
    private Date updateTime;

    @Column(column = "gender")
    private int gender;

    @Column(column = "nickName")
    private String nickName;

    @Transient
    private boolean update;

    @Column(column = "birthday")
    private Date birthday;

    @Column(column = "createTime")
    private Date createTime;

    @JsonSerialize(using=JsonDateSerializer.class)
    public Date getCreateTime() {
        return createTime;
    }

    @JsonDeserialize(using=JsonDateDeserializer.class)
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @JsonSerialize(using=JsonDateSerializer.class)
    public Date getBirthday() {
        return birthday;
    }

    @JsonDeserialize(using=JsonDateDeserializer.class)
    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonSerialize(using=JsonDateSerializer.class)
    public Date getUpdateTime() {
        return updateTime;
    }

    @JsonDeserialize(using=JsonDateDeserializer.class)
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String toString(){
        return JacksonUtil.turnObj2String(this);
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }
}
