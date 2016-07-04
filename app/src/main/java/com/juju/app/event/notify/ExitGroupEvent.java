package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

/**
 * 项目名称：juju
 * 类描述：退出群组
 * 创建人：gm
 * 日期：2016/7/1 14:49
 * 版本：V1.0.0
 */
public class ExitGroupEvent {

    public ExitGroupBean bean;

    //对外事件
    public Event event;

    //业务流事件
    public BusinessFlow businessFlow;


    public ExitGroupEvent(Event event, ExitGroupBean bean) {
        this.bean = bean;
        this.event = event;
    }

    public ExitGroupEvent(Event event) {
        this.event = event;
    }



    @JsonIgnoreProperties(value = {"replyId", "replyTime", "errorDesc"})
    public static class ExitGroupBean extends ReplyBean {
        public String groupId;
        public String userNo;
        public String nickname;
        //防止不赋值，采用封装类型
        public Integer flag;
        //错误描述
        public String errorDesc;

        private ExitGroupBean() {

        }

        public static ExitGroupBean valueOf(String groupId, String userNo, String nickname,
                                            Integer flag) {
            ExitGroupBean bean = new ExitGroupBean();
            bean.groupId = groupId;
            bean.userNo = userNo;
            bean.nickname = nickname;
            bean.flag = flag;
            return bean;
        }
    }

    public enum Event {
        SEND_EXIT_GROUP_OK,
        SEND_EXIT_GROUP_FAILED,

        RECV_EXIT_GROUP_OK,
        RECV_EXIT_GROUP_FAILED
    }

    /**
     * 业务流(方便梳理并处理业务流程)
     */
    public static class BusinessFlow {

        public static class SendParam {
            public Send send;
            public ExitGroupBean bean;

            public SendParam(Send send, ExitGroupBean bean) {
                this.send = send;
                this.bean = bean;
            }

            public enum Send {
                //退出群组（业务服务器）
                SEND_QUIT_GROUP_BSERVER_OK,
                SEND_QUIT_GROUP_BSERVER_FAILED,

                //退出群组（消息服务器）
                SEND_EXIT_GROUP_MSERVER_OK,
                SEND_EXIT_GROUP_MSERVER_FAILED,

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
            public ExitGroupBean bean;


            public RecvParam(Recv recv, ExitGroupBean bean) {
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
