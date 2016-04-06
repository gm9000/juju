package com.juju.app.bean.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * 项目名称：sqb
 * 类描述：
 * 创建人：gm
 * 日期：2016/3/16 19:06
 * 版本：V1.0.0
 */
public class UserInfoResBean extends BaseResBean {

    @JsonUnwrapped
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static class User {
        private String phone;
        private String userName;
        private int gender;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public int getGender() {
            return gender;
        }

        public void setGender(int gender) {
            this.gender = gender;
        }
    }
}
