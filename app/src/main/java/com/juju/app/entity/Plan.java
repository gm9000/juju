package com.juju.app.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.utils.json.JsonDateDeserializer;
import com.juju.app.utils.json.JsonDateSerializer;
import com.lidroid.xutils.db.annotation.Column;
import com.lidroid.xutils.db.annotation.Table;

import java.util.Date;

@Table(name = "plan")
public class Plan extends BaseEntity {


    @Column(column="address")
    private String address;

    @Column(column="latitude")
    private double latitude;

    @Column(column="longitude")
    private double longitude;

    @Column(column="startTime")
    private Date startTime;

    @Column(column="desc")
    private String desc;

    @Column(column="partyId")
    private String partyId;

    @Column(column="status")
    private int status;

    @Column(column="attendNum")
    private int addtendNum;

    @Column(column = "signed")
    private int signed;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @JsonSerialize(using=JsonDateSerializer.class)
    public Date getStartTime() {
        return startTime;
    }

    @JsonDeserialize(using=JsonDateDeserializer.class)
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getAddtendNum() {
        return addtendNum;
    }

    public void setAddtendNum(int addtendNum) {
        this.addtendNum = addtendNum;
    }

    public int getSigned() {
        return signed;
    }

    public void setSigned(int signed) {
        this.signed = signed;
    }
}