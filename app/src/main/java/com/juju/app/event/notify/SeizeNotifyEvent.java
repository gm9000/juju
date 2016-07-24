package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

public class SeizeNotifyEvent {

    public SeizeNotifyBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public SeizeNotifyEvent(Event event, SeizeNotifyBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public SeizeNotifyEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class SeizeNotifyBean extends ReplyBean {
        private String groupId;
        private int seconds;
        private String liveId;
        private int type; //竞技类型，0：点击计数
        private int count;
        private String userNo;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public int getSeconds() {
            return seconds;
        }

        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }

        public String getLiveId() {
            return liveId;
        }

        public void setLiveId(String liveId) {
            this.liveId = liveId;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getUserNo() {
            return userNo;
        }

        public void setUserNo(String userNo) {
            this.userNo = userNo;
        }
    }

    public enum Event {

        LIVE_SEIZE_START_OK,
        LIVE_SEIZE_START_FAILED,

        LIVE_SEIZE_REPORT_OK,
        LIVE_SEIZE_REPORT_FAILED,

        LIVE_SEIZE_STOP_OK,
        LIVE_SEIZE_STOP_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public SeizeNotifyBean bean;

            public SendParam(Send send, SeizeNotifyBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {

                //抢播开始（消息服务器）
                SEND_LIVE_SEIZE_START_MSERVER_OK,
                SEND_LIVE_SEIZE_START_MSERVER_FAILED,

                //抢播计数（消息服务器）
                SEND_LIVE_SEIZE_REPORT_MSERVER_OK,
                SEND_LIVE_SEIZE_REPORT_MSERVER_FAILED,

                //抢播结束（消息服务器）
                SEND_LIVE_SEIZE_STOP_MSERVER_OK,
                SEND_LIVE_SEIZE_STOP_MSERVER_FAILED,

                //成功
                OK,
                //失败
                FAILED
            }
        }

        public static class RecvParam {
            public Recv recv;
            public SeizeNotifyBean bean;


            public RecvParam(Recv recv, SeizeNotifyBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {

                PROCESS_LIVE_SEIZE_START_OK,
                PROCESS_LIVE_SEIZE_START_FAIL,

                PROCESS_LIVE_SEIZE_STOP_OK,
                PROCESS_LIVE_SEIZE_STOP_FAIL,

                PROCESS_LIVE_SEIZE_REPORT_OK,
                PROCESS_LIVE_SEIZE_REPORT_FAIL,

                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
