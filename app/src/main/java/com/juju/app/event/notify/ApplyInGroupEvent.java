package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：申请方式加入群组通知事件
 * 创建人：gm
 * 日期：2016/7/5 18:03
 * 版本：V1.0.0
 */
public class ApplyInGroupEvent {

    public ApplyInGroupBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public ApplyInGroupEvent(Event event, ApplyInGroupBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public ApplyInGroupEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime", "inviteCode"})
    public static class ApplyInGroupBean extends ReplyBean {
        public String groupId;
        public String userNo;
        public String nickname;

        public String inviteCode;
//        //消息时间
//        public long threadId;



        public static ApplyInGroupBean valueOf(String groupId, String userNo, String nickname,
                                               String inviteCode) {
            ApplyInGroupBean bean = new ApplyInGroupBean();
            bean.groupId = groupId;
            bean.userNo = userNo;
            bean.nickname = nickname;
            bean.inviteCode = inviteCode;
            return bean;
        }
    }

    public enum Event {
        SEND_APPLY_IN_GROUP_OK,
        SEND_APPLY_IN_GROUP_FAILED,

        RECV_APPLY_IN_GROUP_OK,
        RECV_APPLY_IN_GROUP_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public ApplyInGroupBean bean;

            public SendParam(Send send, ApplyInGroupBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {
                //加入群组（业务服务器）
                SEND_JOIN_IN_GROUP_BSERVER_OK,
                SEND_JOIN_IN_GROUP_BSERVER_FAILED,

                //获取群组详情
                SEND_GET_GROUP_INFO_BSERVER_OK,
                SEND_GET_GROUP_INFO_BSERVER_FAILED,

                //获取群组成员列表
                SEND_GET_GROUP_USERS_BSERVER_OK,
                SEND_GET_GROUP_USERS_BSERVER_FAILED,

                //加入聊天室
                SEND_JOIN_CHAT_ROOM_MSERVER_OK,
                SEND_JOIN_CHAT_ROOM_MSERVER_FAILED,


                //申请方式加入群组通知（消息服务器）
                SEND_APPLY_IN_GROUP_MSERVER_OK,
                SEND_APPLY_IN_GROUP_MSERVER_FAILED,

                //更新本地数据
                UPDATE_LOCAL_CACHE_DATA_OK,
                UPDATE_LOCAL_CACHE_DATA_FAILED,

                //成功
                OK,
                //失败
                FAILED
            }
        }

        public static class RecvParam {
            public Recv recv;
            public ApplyInGroupBean bean;


            public RecvParam(Recv recv, ApplyInGroupBean bean) {
                this.recv = recv;
                this.bean = bean;
            }

            public enum Recv {

                //更新本地数据
                UPDATE_LOCAL_CACHE_DATA_OK,
                UPDATE_LOCAL_CACHE_DATA_FAILED,

                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
