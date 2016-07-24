package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：聚会方案投票（投票、取消）事件
 * 创建人：JanzLee
 * 日期：2016/7/13 16:10
 * 版本：V1.0.0
 */
public class PlanVoteEvent {

    public PlanVoteBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public PlanVoteEvent(Event event, PlanVoteBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public PlanVoteEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class PlanVoteBean extends ReplyBean {
        private String groupId;
        private String partyId;
        private String planId;
        private int vote;
        private String userNo;
        private String nickName;

        public static PlanVoteBean valueOf(String groupId, String planId, int vote,
                                           String userNo, String nickName) {
            PlanVoteBean bean = new PlanVoteBean();
            bean.groupId = groupId;
            bean.planId = planId;
            bean.vote = vote;
            bean.userNo = userNo;
            bean.nickName = nickName;
            return bean;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

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

        public String getUserNo() {
            return userNo;
        }

        public void setUserNo(String userNo) {
            this.userNo = userNo;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public String getPartyId() {
            return partyId;
        }

        public void setPartyId(String partyId) {
            this.partyId = partyId;
        }
    }

    public enum Event {
        PLAN_VOTE_OK,
        PLAN_VOTE_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public PlanVoteBean bean;

            public SendParam(Send send, PlanVoteBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {

                //投票发布（消息服务器）
                SEND_PLAN_VOTE_MSERVER_OK,
                SEND_PLAN_VOTE_MSERVER_FAILED,

            }
        }

        public static class RecvParam {
            public Recv recv;
            public PlanVoteBean bean;


            public RecvParam(Recv recv, PlanVoteBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {
                //处理本地方案投票数据
                PROCESS_LOCAL_CACHE_DATA_OK,
                PROCESS_LOCAL_CACHE_DATA_FAILED,

                //同步数据
                SYNCHRONIZE_PARTY_DATA_OK,
                SYNCHRONIZE_PARTY_DATA_FAILED
            }
        }
    }
}
