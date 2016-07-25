package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：聚会通知（发起、取消）事件
 * 创建人：JanzLee
 * 日期：2016/7/1 16:10
 * 版本：V1.0.0
 */
public class PartyNotifyEvent {

    public PartyNotifyBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public PartyNotifyEvent(Event event, PartyNotifyBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public PartyNotifyEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class PartyNotifyBean extends ReplyBean {
        private String groupId;
        private String partyId;
        private String partyName;
        private String userNo;
        private String nickName;
        private String coverUrl;
        //  -1：取消  0：召集中   1：进行中   2：已结束
        private int status;
        private String planId;

        public static PartyNotifyBean valueOf(String groupId, String partyId, String partyName,
                                              String userNo, String nickName) {
            PartyNotifyBean bean = new PartyNotifyBean();
            bean.groupId = groupId;
            bean.partyId = partyId;
            bean.partyName = partyName;
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

        public String getPartyId() {
            return partyId;
        }

        public void setPartyId(String partyId) {
            this.partyId = partyId;
        }

        public String getPartyName() {
            return partyName;
        }

        public void setPartyName(String partyName) {
            this.partyName = partyName;
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

        public String getCoverUrl() {
            return coverUrl;
        }

        public void setCoverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }
    }

    public enum Event {
        PARTY_RECRUIT_OK,
        PARTY_RECRUIT_FAILED,

        PARTY_CANCEL_OK,
        PARTY_CANCEL_FAILED,

        PARTY_CONFIRM_OK,
        PARTY_CONFIRM_FAILED,

        PARTY_END_OK,
        PARTY_END_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public PartyNotifyBean bean;

            public SendParam(Send send, PartyNotifyBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {

                //聚会发布（消息服务器）
                SEND_PARTY_RECRUIT_MSERVER_OK,
                SEND_PARTY_RECRUIT_MSERVER_FAILED,

                //聚会取消（消息服务器）
                SEND_PARTY_CANCEL_MSERVER_OK,
                SEND_PARTY_CANCEL_MSERVER_FAILED,

                //聚会启动（消息服务器）
                SEND_PARTY_CONFIRM_MSERVER_OK,
                SEND_PARTY_CONFIRM_MSERVER_FAILED,


                //聚会启动（消息服务器）
                SEND_PARTY_END_MSERVER_OK,
                SEND_PARTY_END_MSERVER_FAILED,

                //成功
                OK,
                //失败
                FAILED
            }
        }

        public static class RecvParam {
            public Recv recv;
            public PartyNotifyBean bean;


            public RecvParam(Recv recv, PartyNotifyBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {
                //获取聚会详情失败
                GET_PARTY_INFO_FAILED,
                //解析聚会详情失败
                ANALYZE_PARTY_INFO_FAILED,

                //添加本地数据
                ADD_LOCAL_CACHE_DATA_OK,
                ADD_LOCAL_CACHE_DATA_FAILED,

                //更新本地数据
                UPDATE_LOCAL_CACHE_DATA_OK,
                UPDATE_LOCAL_CACHE_DATA_FAILED,

                //删除本地数据
                CLEAR_LOCAL_CACHE_DATA_OK,
                CLEAR_LOCAL_CACHE_DATA_FAILED,


                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
