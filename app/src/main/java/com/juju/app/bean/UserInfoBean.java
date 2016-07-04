package com.juju.app.bean;

/**
 * 项目名称：juju
 * 类描述：账号信息
 * 创建人：gm
 * 日期：2016/3/22 15:02
 * 版本：V1.0.0
 */
public class UserInfoBean {

    public UserInfoBean() {
        initTest();
    }

//    //消息服务器账号
//    private String mAccount;

    private String mServiceName;

    //消息服务服务器IP
    private String mHost;

    //消息服务服务器端口
    private int mPort;

    //消息服务器密码
    private String mPassword;

    //房间名
    private String mRoomName;

    //分组聊天服务名
    private String mMucServiceName;

    //手机号
    private String phone;

    //用户名
    private String nickName;

    //性别
    private int gender;

    //聚聚号
    private String userNo;

    private String token;

    //生日
    private long birthday;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }



//    public String getmAccount() {
//        return mAccount;
//    }
//
//    public void setmAccount(String mAccount) {
//        this.mAccount = mAccount;
//    }

    public String getmPassword() {
        return mPassword;
    }

    public void setmPassword(String mPassword) {
        this.mPassword = mPassword;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getmRoomName() {
        return mRoomName;
    }

    public void setmRoomName(String mRoomName) {
        this.mRoomName = mRoomName;
    }

    public String getmMucServiceName() {
        return mMucServiceName;
    }

    public void setmMucServiceName(String mMucServiceName) {
        this.mMucServiceName = mMucServiceName;
    }

    public String getmHost() {
        return mHost;
    }

    public void setmHost(String mHost) {
        this.mHost = mHost;
    }

    public int getmPort() {
        return mPort;
    }

    public void setmPort(int mPort) {
        this.mPort = mPort;
    }

    public String getmServiceName() {
        return mServiceName;
    }

    public void setmServiceName(String mServiceName) {
        this.mServiceName = mServiceName;
    }

    public long getBirthday() {
        return birthday;
    }

    public void setBirthday(long birthday) {
        this.birthday = birthday;
    }

    private void initTest() {
        this.mHost = "219.143.237.230";
        this.mPort = 5222;
//        this.mPassword = "123456";
        this.mRoomName = "ceshi";
        this.mMucServiceName = "conference";
        this.mServiceName = "juju";
        this.nickName = "100000001";
    }
}
