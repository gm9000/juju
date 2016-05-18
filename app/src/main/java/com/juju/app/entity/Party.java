package com.juju.app.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.utils.json.JsonDateDeserializer;
import com.juju.app.utils.json.JsonDateSerializer;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.Date;


@Table(name = "party")
public class Party extends BaseEntity {

    @Column(name = "name")
    private String name;
    @Column(name = "desc")
    private String desc;
    @Column(name = "time")
    private Date time;

    //  -1：草稿箱  0：召集中   1：进行中   2：已结束
    @Column(name = "status")
    private int status;

    @Column(name = "follow_flag")
    private int followFlag;

    @Column(name = "attend_flag")
    private int attendFlag;

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    private boolean hidden;

//    @Foreign(column = "createUserNo",foreign = "userNo")
//    private User creator;

    @Column(name = "user_no")
    private String userNo;

//    @Foreign(column = "groupId",foreign = "id")
//    private GroupEntity group;

    @Column(name = "group_id")
    private String groupId;


    //临时属性(创建者)
    private User creator;



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @JsonSerialize(using=JsonDateSerializer.class)
    public Date getTime() {
        return time;
    }
    @JsonDeserialize(using=JsonDateDeserializer.class)
    public void setTime(Date time) {
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getFollowFlag() {
        return followFlag;
    }

    public void setFollowFlag(int followFlag) {
        this.followFlag = followFlag;
    }

    public int getAttendFlag() {
        return attendFlag;
    }

    public void setAttendFlag(int attendFlag) {
        this.attendFlag = attendFlag;
    }

//    public User getCreator() {
//        return creator;
//    }
//
//    public void setCreator(User creator) {
//        this.creator = creator;
//    }
//
//    public GroupEntity getGroup() {
//        return group;
//    }
//
//    public void setGroup(GroupEntity group) {
//        this.group = group;
//    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }
}
