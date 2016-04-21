package com.juju.app.bean.json;

import com.juju.app.entity.Plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/4/13 0013.
 */
public class PartyReqBean extends BaseReqBean{

    private String groupId;
    private PartyBean party;
    private List<PlanBean> plans;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public PartyBean getParty() {
        return party;
    }

    public void setParty(PartyBean party) {
        this.party = party;
    }

    public List<PlanBean> getPlans() {
        return plans;
    }

    public void setPlans(List<PlanBean> plans) {
        this.plans = plans;
    }
}
