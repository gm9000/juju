package com.juju.app.event;

/**
 *
 * 用户信息变化事件
 */
public class UserInfoChangeEvent {

    private String userNo;
    private Type changeType;

    public UserInfoChangeEvent(String userNo,Type changeType){
        this.userNo = userNo;
        this.changeType = changeType;
    }

    public String getUserNo() {
        return userNo;
    }

    public Type getChangeType() {
        return changeType;
    }

    public enum Type {
        BASIC_INFO_CHANGE,
        PORTRAIT_CHANGE,
        ALL_CHANGE,
    }

}
