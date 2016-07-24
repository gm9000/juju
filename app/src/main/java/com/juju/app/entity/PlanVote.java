package com.juju.app.entity;

import com.juju.app.entity.base.BaseEntity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;


@Table(name = "plan_vote")
public class PlanVote extends BaseEntity {

    @Column(name="plan_id")
    private String planId;


    //关联User(user_no字段)
    @Column(name="attender_no")
    private String attenderNo;




    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getAttenderNo() {
        return attenderNo;
    }

    public void setAttenderNo(String attenderNo) {
        this.attenderNo = attenderNo;
    }
}
