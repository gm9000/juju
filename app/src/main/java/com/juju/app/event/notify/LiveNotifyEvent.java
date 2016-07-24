package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

public class LiveNotifyEvent {

    public LiveNotifyBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public LiveNotifyEvent(Event event, LiveNotifyBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public LiveNotifyEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class LiveNotifyBean extends ReplyBean {
        private String groupId;
        private String partyId;
        private String liveId;
        private String liveUrl; //直播rtmp地址”
        private String videoUrl; //直播rtmp地址”
        private String captureUrl;  //视频截图地址”
        private int width;
        private int height;
        private String userNo;
        private String nickName;

        // 0：进行中  1：结束  -1：抢播中
        private int status;

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

        public String getLiveId() {
            return liveId;
        }

        public void setLiveId(String liveId) {
            this.liveId = liveId;
        }

        public String getLiveUrl() {
            return liveUrl;
        }

        public void setLiveUrl(String liveUrl) {
            this.liveUrl = liveUrl;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getCaptureUrl() {
            return captureUrl;
        }

        public void setCaptureUrl(String captureUrl) {
            this.captureUrl = captureUrl;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
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

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }

    public enum Event {
        LIVE_START_OK,
        LIVE_START_FAILED,

        LIVE_STOP_OK,
        LIVE_STOP_FAILED,

    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public LiveNotifyBean bean;

            public SendParam(Send send, LiveNotifyBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {

                //直播开始（消息服务器）
                SEND_LIVE_START_MSERVER_OK,
                SEND_LIVE_START_MSERVER_FAILED,

                //直播结束（消息服务器）
                SEND_LIVE_STOP_MSERVER_OK,
                SEND_LIVE_STOP_MSERVER_FAILED,


                //成功
                OK,
                //失败
                FAILED
            }
        }

        public static class RecvParam {
            public Recv recv;
            public LiveNotifyBean bean;


            public RecvParam(Recv recv, LiveNotifyBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {

                PROCESS_LIVE_START_OK,
                PROCESS_LIVE_START_FAIL,

                PROCESS_LIVE_STOP_OK,
                PROCESS_LIVE_STOP_FAIL,

                PROCESS_LIVE_SEIZE_START_OK,
                PROCESS_LIVE_SEIZE_START_FAIL,

                PROCESS_LIVE_SEIZE_STOP_OK,
                PROCESS_LIVE_SEIZE_STOP_FAIL,

                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
