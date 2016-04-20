package com.juju.app.bean.json;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.juju.app.utils.json.JsonDateSerializer;

import java.util.Date;

/**
 * Created by Administrator on 2016/4/13 0013.
 */
public class PlanVoteBean {
    private String planId;
    private int vote;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public int getVote() {
        return vote;
    }

    public void setVote(int vote) {
        this.vote = vote;
    }
}
