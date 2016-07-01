package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

import java.util.Date;

/**
 * 项目名称：juju
 * 类描述：移除群组通知事件
 * 创建人：gm
 * 日期：2016/6/30 09:42
 * 版本：V1.0.0
 */
public class RemoveGroupEvent {


    public RemoveGroupBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public RemoveGroupEvent(Event event, RemoveGroupBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public RemoveGroupEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class RemoveGroupBean extends ReplyBean {
        public String groupId;
        public String groupName;
        public String userNo;


        public static RemoveGroupBean valueOf(String groupId, String groupName, String userNo) {
            RemoveGroupBean bean = new RemoveGroupBean();
            bean.groupId = groupId;
            bean.groupName = groupName;
            bean.userNo = userNo;
            return bean;
        }
    }

    public enum Event {
        SEND_REMOVE_GROUP_OK,
        SEND_REMOVE_GROUP_FAILED,

        RECV_REMOVE_GROUP_OK,
        RECV_REMOVE_GROUP_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public RemoveGroupBean bean;

            public SendParam(Send send, RemoveGroupBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {
                //踢出群组成员（业务服务器）
                SEND_DELETE_GROUP_MEMBER_BSERVER_OK,
                SEND_DELETE_GROUP_MEMBER_BSERVER_FAILED,

                //移除群组通知（消息服务器）
                SEND_REMOVE_GROUP_MSERVER_OK,
                SEND_REMOVE_GROUP_MSERVER_FAILED,

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
            public RemoveGroupBean bean;


            public RecvParam(Recv recv, RemoveGroupBean bean) {
                this.recv = recv;
                this.bean = bean;
            }


            public enum Recv {

                //退出聊天室
                EXIT_CHAT_ROOM_MSERVER_OK,
                EXIT_CHAT_ROOM_MSERVER_FAILED,

                //更新本地数据
                UPDATE_LOCAL_CACHE_DATA_OK,
                UPDATE_LOCAL_CACHE_DATA_FAILED,

//                //系统通知
//                NOTIFICATION_SYSTEM_OK,
//                NOTIFICATION_SYSTEM_FAILED,

                //成功
                OK,
                //失败
                FAILED

            }
        }
    }
}
