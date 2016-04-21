package com.juju.app.bean.json;

import java.util.List;

/**
 * Created by JanzLee on 2016/4/13 0013.
 */
public class PlanVoteReqBean extends BaseReqBean{

    private PlanVoteBean planVote;

    public PlanVoteBean getPlanVote() {
        return planVote;
    }

    public void setPlanVote(PlanVoteBean planVote) {
        this.planVote = planVote;
    }
}
