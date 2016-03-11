package com.juju.app.entity.http;

/**
 * 项目名称：juju
 * 类描述：群组实体
 * 创建人：gm
 * 日期：2016/2/22 11:14
 * 版本：V1.0.0
 */
public class Group {


    private long id;

    /**
     * 标示id,预留
     */
    private String markerId;

    private String name;

    /**
     * 群成员数
     */
    private int memberNum;

    public long getId() {
        return id;
    }

    public void setId(long id) {
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
