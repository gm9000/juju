package com.juju.app.event.notify;

import com.juju.app.entity.Invite;

/**
 * 项目名称：juju
 * 类描述：加群邀请通知
 * 创建人：gm
 * 日期：2016/6/27 11:40
 * 版本：V1.0.0
 */
public class InviteUserEvent {

    public InviteUserBean bean;
    public Event event;

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

        public Send send;
        public Recv recv;

        public BusinessFlow(Send send) {
            this.send = send;
        }

        public BusinessFlow(Recv recv) {
            this.recv = recv;
        }

        public enum Send {
            //发送加入群组消息（业务服务器）
            SEND_INVITE_USER_BSERVER_OK,
            SEND_INVITE_USER_BSERVER_FAILED,

            //更新本地数据
            UPDATE_LOCAL_DATA_OK,
            UPDATE_LOCAL_DATA_FAILED,

            //成功
            OK,
            //失败
            FAILED
        }

        public enum Recv {

        }

    }
}
