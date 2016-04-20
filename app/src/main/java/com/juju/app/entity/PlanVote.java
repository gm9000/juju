package com.juju.app.entity;

import com.juju.app.entity.base.BaseEntity;
import com.lidroid.xutils.db.annotation.Column;
import com.lidroid.xutils.db.annotation.Foreign;
import com.lidroid.xutils.db.annotation.Table;

@Table(name = "plan_vote")
public class PlanVote extends BaseEntity {

    @Column(column="planId")
    private String planId;

    @Foreign(column = "attenderNo",foreign = "userNo")
    private User attender;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public User getAttender() {
        return attender;
    }

    public void setAttender(User attender) {
        this.attender = attender;
    }
}
