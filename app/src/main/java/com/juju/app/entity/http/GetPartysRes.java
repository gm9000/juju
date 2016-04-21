package com.juju.app.entity.http;

import com.juju.app.entity.Party;

import java.util.List;

/**
 * Created by Administrator on 2016/4/14 0014.
 */
public class GetPartysRes {
    private int status;
    private List<Party> partys;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Party> getPartys() {
        return partys;
    }

    public void setPartys(List<Party> partys) {
        this.partys = partys;
    }
}
