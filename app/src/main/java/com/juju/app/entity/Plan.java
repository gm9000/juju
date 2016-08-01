package com.juju.app.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.entity.base.BaseEntity;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.json.JsonDateDeserializer;
import com.juju.app.utils.json.JsonDateSerializer;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.Date;

@Table(name = "plan")
public class Plan extends BaseEntity {


    @Column(name="address")
    private String address;

    @Column(name="latitude")
    private double latitude;

    @Column(name="longitude")
    private double longitude;

    @Column(name="start_time")
    private Date startTime;

    @Column(name="desc")
    private String desc;

    @Column(name="party_id")
    private String partyId;

    @Column(name="status")
    private int status;

    @Column(name="attend_num")
    private int addtendNum;

    @Column(name = "signed")
    private int signed;

    private String distance;

    @Column(name = "type")
    private String type;

    @Column(name = "coverUrl")
    private String coverUrl;

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

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getCoverUrl() {
        if(StringUtils.empty(coverUrl)){
            return this.type;
        }else{
            return coverUrl;
        }
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public enum Type{
        JUCAN,
        KTV,
        QIPAI,
        JIAOYOU,
        DIANYING,
        GUANGJIE,
        YUNDONG,
        JIANSHEN,
        PAOBA,
        TUANJIAN,
        SHALONG,
        QITA
    }
}
