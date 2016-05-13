package com.juju.app.entity;

import com.juju.app.entity.base.BaseEntity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;


@Table(name = "plan_vote")
public class PlanVote extends BaseEntity {

    @Column(name="planId")
    private String planId;

//    @Foreign(column = "attenderNo",foreign = "userNo")


    //关联User(user_no字段)
    @Column(name="attender_no")
    private String attenderNo;


    //临时属性
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

    public String getAttenderNo() {
        return attenderNo;
    }

    public void setAttenderNo(String attenderNo) {
        this.attenderNo = attenderNo;
    }
}
