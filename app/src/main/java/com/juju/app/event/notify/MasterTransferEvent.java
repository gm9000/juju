package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：转让群主事件
 * 创建人：gm
 * 日期：2016/7/1 11:22
 * 版本：V1.0.0
 */
public class MasterTransferEvent {

    public MasterTransferBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public MasterTransferEvent(Event event, MasterTransferBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public MasterTransferEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime"})
    public static class MasterTransferBean extends ReplyBean {
        public String groupId;
        public String userNo;
        public String nickname;
        public String masterNo;
        public String masterNickName;



        public static MasterTransferBean valueOf(String groupId, String userNo, String nickname,
                                                 String masterNo, String masterNickName) {
            MasterTransferBean bean = new MasterTransferBean();
            bean.groupId = groupId;
            bean.userNo = userNo;
            bean.nickname = nickname;
            bean.masterNo = masterNo;
            bean.masterNickName = masterNickName;
            return bean;
        }
    }

    public enum Event {
        SEND_MASTER_TRANSFER_OK,
        SEND_MASTER_TRANSFER_FAILED,

        RECV_MASTER_TRANSFER_OK,
        RECV_MASTER_TRANSFER_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public MasterTransferBean bean;

            public SendParam(Send send, MasterTransferBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {
                //群主转让（业务服务器）
                SEND_UPDATE_GROUP_MASTER_BSERVER_OK,
                SEND_UPDATE_GROUP_MASTER_BSERVER_FAILED,

                //群主转让（消息服务器）
                SEND_MASTER_TRANSFER_MSERVER_OK,
                SEND_MASTER_TRANSFER_MSERVER_FAILED,

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
            public MasterTransferBean bean;


            public RecvParam(Recv recv, MasterTransferBean bean) {
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
