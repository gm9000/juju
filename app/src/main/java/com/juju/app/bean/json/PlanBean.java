package com.juju.app.bean.json;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.utils.json.JsonDateSerializer;

import java.util.Date;

/**
 * Created by Administrator on 2016/4/13 0013.
 */
public class PlanBean {
    private String address;
    private double latitude;
    private double longitude;
    private Date startTime;
    private String desc;

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

    @JsonSerialize(using= JsonDateSerializer.class)
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
