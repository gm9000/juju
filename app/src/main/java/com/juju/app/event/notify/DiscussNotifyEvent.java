package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：直播评论事件
 * 创建人：JanzLee
 * 版本：V1.0.0
 */
public class DiscussNotifyEvent {

    public DiscussNotifyBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public DiscussNotifyEvent(Event event, DiscussNotifyBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public DiscussNotifyEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class DiscussNotifyBean extends ReplyBean {
        private String groupId;
        private String liveId;
        private String content;
        private String userNo;
        private String nickName;

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

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
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
    }

    public enum Event {
        DISCUSS_NOTIFY_OK,
        DISCUSS_NOTIFY_FAILED,
        RECEIVE_DISCUSS_NOTIFY_OK,
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public DiscussNotifyBean bean;

            public SendParam(Send send, DiscussNotifyBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {

                //发布评论（消息服务器）
                DISCUSS_NOTIFY_MSERVER_OK,
                DISCUSS_NOTIFY_MSERVER_FAILED,

                //成功
                OK,
                //失败
                FAILED
            }
        }

        public static class RecvParam {
            public Recv recv;
            public DiscussNotifyBean bean;


            public RecvParam(Recv recv, DiscussNotifyBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {

                //处理接收评论
                PROCESS_DISCUSS_NOTIFY_OK,
                PROCESS_DISCUSS_NOTIFY_FAILED,

                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
