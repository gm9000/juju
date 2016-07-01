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
        public String groupId;
        public String partyId;
        public String partyName;
        public String userNo;
        public String nickName;



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
    }

    public enum Event {
        PARTY_RECRUIT_OK,
        PARTY_RECRUIT_FAILED,

        PARTY_CANCEL_OK,
        PARTY_CANCEL_FAILED
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

                //群主转让（消息服务器）
                SEND_PARTY_RECRUIT_MSERVER_OK,
                SEND_PARTY_RECRUIT_MSERVER_FAILED,

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
