package com.juju.app.entity;

import com.juju.app.entity.base.BaseEntity;
import com.lidroid.xutils.db.annotation.Column;

public class Party extends BaseEntity {

    @Column(column = "creatorId")
    private String creatorId;

    @Column(column = "creatorName")
    private String creatorName;



    @Column(column = "name")
    private String name;

    @Column(column = "description")
    private String description;


    @Column(column = "startTime")
    private String startTime;

    @Column(column = "status")
    private int status;

    @Column(column = "followFlag")
    private int followFlag;

    @Column(column = "attendFlag")
    private int attendFlag;


    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
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
}
