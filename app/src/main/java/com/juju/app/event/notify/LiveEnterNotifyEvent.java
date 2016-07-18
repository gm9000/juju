package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：直播进入离开事件
 * 创建人：JanzLee
 * 版本：V1.0.0
 */
public class LiveEnterNotifyEvent {

    public LiveEnterNotifyBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public LiveEnterNotifyEvent(Event event, LiveEnterNotifyBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public LiveEnterNotifyEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class LiveEnterNotifyBean extends ReplyBean {
        private String groupId;
        private String liveId;
        private String userNo;
        private String nickName;
        private int type;   // 0：进入 1：离开

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getLiveId() {
            return liveId;
        }

        public void setLiveId(String liveId) {
            this.liveId = liveId;
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

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

    public enum Event {
        LIVE_ENTER_NOTIFY_OK,
        LIVE_ENTER_NOTIFY_FAILED,
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public LiveEnterNotifyBean bean;

            public SendParam(Send send, LiveEnterNotifyBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {

                //发布评论（消息服务器）
                LIVE_ENTER_NOTIFY_MSERVER_OK,
                LIVE_ENTER_NOTIFY_MSERVER_FAILED,

                //成功
                OK,
                //失败
                FAILED
            }
        }

        public static class RecvParam {
            public Recv recv;
            public LiveEnterNotifyBean bean;


            public RecvParam(Recv recv, LiveEnterNotifyBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {

                //处理接收评论
                PROCESS_LIVE_ENTER_NOTIFY_OK,
                PROCESS_LIVE_ENTER_NOTIFY_FAILED,

                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
