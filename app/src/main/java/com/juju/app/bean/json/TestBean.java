package com.juju.app.bean.json;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/5/16 09:44
 * 版本：V1.0.0
 */
public class TestBean implements Parcelable {


    /**
     * status : 0
     * user : {"userNo":"100000003","nickName":"爱上一个人","userPhone":"13800000003","birthday":"2016-01-04 14:42:32","gender":0,"createTime":"2016-01-04 14:42:32"}
     */

    private int status;
    /**
     * userNo : 100000003
     * nickName : 爱上一个人
     * userPhone : 13800000003
     * birthday : 2016-01-04 14:42:32
     * gender : 0
     * createTime : 2016-01-04 14:42:32
     */

    private UserEntity user;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public static class UserEntity implements Parcelable {
        private String userNo;
        private String nickName;
        private String userPhone;
        private String birthday;
        private int gender;
        private String createTime;

        public String getUserNo() {
            return userNo;
        }

        public void setUserNo(String userNo) {
            this.userNo = userNo;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public String getUserPhone() {
            return userPhone;
        }

        public void setUserPhone(String userPhone) {
            this.userPhone = userPhone;
        }

        public String getBirthday() {
            return birthday;
        }

        public void setBirthday(String birthday) {
            this.birthday = birthday;
        }

        public int getGender() {
            return gender;
        }

        public void setGender(int gender) {
            this.gender = gender;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.userNo);
            dest.writeString(this.nickName);
            dest.writeString(this.userPhone);
            dest.writeString(this.birthday);
            dest.writeInt(this.gender);
            dest.writeString(this.createTime);
        }

        public UserEntity() {
        }

        protected UserEntity(Parcel in) {
            this.userNo = in.readString();
            this.nickName = in.readString();
            this.userPhone = in.readString();
            this.birthday = in.readString();
            this.gender = in.readInt();
            this.createTime = in.readString();
        }

        public static final Parcelable.Creator<UserEntity> CREATOR = new Parcelable.Creator<UserEntity>() {
            @Override
            public UserEntity createFromParcel(Parcel source) {
                return new UserEntity(source);
            }

            @Override
            public UserEntity[] newArray(int size) {
                return new UserEntity[size];
            }
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.status);
        dest.writeParcelable(this.user, flags);
    }

    public TestBean() {
    }

    protected TestBean(Parcel in) {
        this.status = in.readInt();
        this.user = in.readParcelable(UserEntity.class.getClassLoader());
    }

    public static final Parcelable.Creator<TestBean> CREATOR = new Parcelable.Creator<TestBean>() {
        @Override
        public TestBean createFromParcel(Parcel source) {
            return new TestBean(source);
        }

        @Override
        public TestBean[] newArray(int size) {
            return new TestBean[size];
        }
    };
}
