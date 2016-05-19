package com.juju.app.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.entity.chat.SearchElement;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.json.JsonDateDeserializer;
import com.juju.app.utils.json.JsonDateSerializer;
import com.juju.app.utils.pinyin.PinYinUtil;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.Date;

/**
 * 项目名称：juju
 * 类描述：用户 （测试使用）
 * 创建人：gm
 * 日期：2016/2/17 12:07
 * 版本：V1.0.0
 */
@Table(name = "user",
        onCreated = "CREATE UNIQUE INDEX index_user_user_no_key ON user(user_no)")
public class User extends BaseEntity {


    public User() {
    }

    private User(String userNo, String userPhone, String email, int gender, String nickName,
                Date createTime, Date birthday, String avatar) {
        this.userNo = userNo;
        this.userPhone = userPhone;
        this.email = email;
        this.gender = gender;
        this.nickName = nickName;
        this.createTime = createTime;
        this.birthday = birthday;
        this.avatar = avatar;
    }

    /**
     * 用户聚聚号
     */
    @Column(name = "user_no")
    private String userNo;

    /**
     * 用户手机号
     */
    @Column(name = "userPhone")
    private String userPhone;

    /**
     * 用户邮箱
     */
    @Column(name = "email")
    private String email;


    /**
     * 更新时间
     */
    @Column(name = "updateTime")
    private Date updateTime;

    @Column(name = "gender")
    private int gender;

    @Column(name = "nickName")
    private String nickName;

    private boolean update;

    @Column(name = "birthday")
    private Date birthday;

    @Column(name = "createTime")
    private Date createTime;

    /**
     * 头像地址
     */
    @Column(name = "avatar")
    private String avatar;

    private PinYinUtil.PinYinElement pinyinElement = new PinYinUtil.PinYinElement();
    private SearchElement searchElement = new SearchElement();

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

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public SearchElement getSearchElement() {
        return searchElement;
    }

    public PinYinUtil.PinYinElement getPinyinElement() {
        return pinyinElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (gender != user.gender) return false;
        if (!userNo.equals(user.userNo)) return false;
        if (userPhone != null ? !userPhone.equals(user.userPhone) : user.userPhone != null)
            return false;
        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (updateTime != null ? !updateTime.equals(user.updateTime) : user.updateTime != null)
            return false;
        if (nickName != null ? !nickName.equals(user.nickName) : user.nickName != null)
            return false;
        if (birthday != null ? !birthday.equals(user.birthday) : user.birthday != null)
            return false;
        return !(createTime != null ? !createTime.equals(user.createTime) : user.createTime != null);

    }

    @Override
    public int hashCode() {
        int result = userNo.hashCode();
        result = 31 * result + (userPhone != null ? userPhone.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (updateTime != null ? updateTime.hashCode() : 0);
        result = 31 * result + gender;
        result = 31 * result + (nickName != null ? nickName.hashCode() : 0);
        result = 31 * result + (birthday != null ? birthday.hashCode() : 0);
        result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "userNo='" + userNo + '\'' +
                ", userPhone='" + userPhone + '\'' +
                ", email='" + email + '\'' +
                ", updateTime=" + updateTime +
                ", gender=" + gender +
                ", nickName='" + nickName + '\'' +
                ", update=" + update +
                ", birthday=" + birthday +
                ", createTime=" + createTime +
                '}';
    }



    public static User buildForReceive(String userNo, String userPhone,  String email, int gender,
                                       String nickName, Date createTime, Date birthday) {
        if(StringUtils.isBlank(userNo)
                || StringUtils.isBlank(nickName))
            throw new IllegalArgumentException("user#buildForReceive is error");
        User userEntity = new User(userNo,  userPhone,  email,
            gender,  nickName,  createTime,  birthday,
            HttpConstants.getPortraitUrl()+userNo);
        return userEntity;
    }
}
