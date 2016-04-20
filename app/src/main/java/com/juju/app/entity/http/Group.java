package com.juju.app.entity.http;

import com.juju.app.entity.User;

/**
 * 项目名称：juju
 * 类描述：群组实体
 * 创建人：gm
 * 日期：2016/2/22 11:14
 * 版本：V1.0.0
 */
public class Group {


    private String id;

    /**
     * 标示id,预留
     */
    private String markerId;

    private String name;

    private String desc;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    private User creator;

    /**
     * 群成员数
     */
    private int memberNum;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMemberNum() {
        return memberNum;
    }

    public void setMemberNum(int memberNum) {
        this.memberNum = memberNum;
    }

    public String getMarkerId() {
        return markerId;
    }

    public void setMarkerId(String markerId) {
        this.markerId = markerId;
    }
}
