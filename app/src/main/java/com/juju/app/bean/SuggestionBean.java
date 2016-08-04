package com.juju.app.bean;

import com.baidu.mapapi.model.LatLng;

/**
 * Created by Administrator on 2016/8/3 0003.
 */
public class SuggestionBean {
    private String key;
    private LatLng location;
    private String address;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
