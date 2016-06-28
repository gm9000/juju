package com.juju.app.event.notify;

import com.juju.app.entity.Invite;

import java.util.Date;

/**
 * 项目名称：juju
 * 类描述：加群邀请通知
 * 创建人：gm
 * 日期：2016/6/27 11:40
 * 版本：V1.0.0
 */
public class InviteUserEvent {

    public InviteUserBean bean;
    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;



    public InviteUserEvent(InviteUserBean bean, Event event) {
        this.bean = bean;
        this.event = event;
    }

    public InviteUserEvent(Event event) {
        this.event = event;
    }

    public InviteUserEvent(InviteUserBean bean, BusinessFlow businessFlow) {
        this.bean = bean;
        this.businessFlow = businessFlow;
    }

    public enum Event {
        INVITE_USER_OK,
        INVITE_USER_FAILED
    }

    //封装消息通知对象，防止多处定义json串
    //加群邀请通知Bean
    public static class InviteUserBean {
        public String groupId;
        public String groupName;
        public String userNo;
        //需要冗余此字段（系统通知使用）
        public String nickName;


        public static InviteUserBean valueOf(String groupId, String groupName,
                                             String userNo, String nickName) {
            InviteUserBean bean = new InviteUserBean();
            bean.groupId = groupId;
            bean.groupName = groupName;
            bean.userNo = userNo;
            bean.nickName = nickName;
            return bean;
        }
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public InviteUserBean bean;
            public String replayId;
            public long replayTime;

            public SendParam(Send send, InviteUserBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {
                //发送加入群组消息（业务服务器）
                SEND_INVITE_USER_BSERVER_OK,
                SEND_INVITE_USER_BSERVER_FAILED,

                //发送加入群组消息（消息服务器）
                SEND_INVITE_USER_MSERVER_OK,
                SEND_INVITE_USER_MSERVER_FAILED,

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
            public InviteUserBean bean;
            public String groupId;
            public String groupName;
            public String desc;
            public String creatorNo;
            public String masterNo;
            public Date createTimeDate;

            public RecvParam(Recv recv, InviteUserBean bean) {
                this.recv = recv;
                this.bean = bean;
            }

            //获取群详情

            //获取群组成员列表

            //加入聊天室


            //发送系统通知

            public enum Recv {
                //获取群组详情
                SEND_GET_GROUP_INFO_BSERVER_OK,
                SEND_GET_GROUP_INFO_BSERVER_FAILED,

                //获取群组成员列表
                SEND_GET_GROUP_USERS_BSERVER_OK,
                SEND_GET_GROUP_USERS_BSERVER_FAILED,

                //加入聊天室
                JOIN_CHAT_ROOM_MSERVER_OK,
                JOIN_CHAT_ROOM_MSERVER_FAILED,

                //系统通知
                NOTIFICATION_SYSTEM_OK,
                NOTIFICATION_SYSTEM_FAILED,

                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
