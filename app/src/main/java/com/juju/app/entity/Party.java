package com.juju.app.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.utils.json.JsonDateDeserializer;
import com.juju.app.utils.json.JsonDateSerializer;
import com.lidroid.xutils.db.annotation.Column;
import com.lidroid.xutils.db.annotation.Foreign;
import com.lidroid.xutils.db.annotation.Table;
import com.lidroid.xutils.db.annotation.Transient;

import java.util.Date;


@Table(name = "party")
public class Party extends BaseEntity {

    @Column(column = "name")
    private String name;
    @Column(column = "desc")
    private String desc;
    @Column(column = "time")
    private Date time;

    //  -1：草稿箱  0：召集中   1：进行中   2：已结束
    @Column(column = "status")
    private int status;

    @Column(column = "followFlag")
    private int followFlag;

    @Column(column = "attendFlag")
    private int attendFlag;

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Transient
    private boolean hidden;

    @Foreign(column = "createUserNo",foreign = "userNo")
    private User creator;

    @Foreign(column = "groupId",foreign = "id")
    private GroupEntity group;

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

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }
}
