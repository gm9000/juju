package com.juju.app.event.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juju.app.event.notify.baseBean.ReplyBean;

import java.util.Date;

/**
 * 项目名称：juju
 * 类描述：邀请方式加入群组通知
 * 创建人：gm
 * 日期：2016/7/2 18:09
 * 版本：V1.0.0
 */
public class InviteInGroupEvent {

    public InviteInGroupBean bean;
    //对外事件
    public Event event;


    public InviteInGroupEvent(InviteInGroupBean bean, Event event) {
        this.bean = bean;
        this.event = event;
    }

    public InviteInGroupEvent(Event event) {
        this.event = event;
    }



    public enum Event {
        SEND_INVITE_IN_GROUP_OK,
        SEND_INVITE_IN_GROUP_FAILED,
        RECV_INVITE_IN_GROUP_OK,
        RECV_INVITE_IN_GROUP_FAILED
    }

//    “groupId”:”群ID”,
//            ”userNo”:”加入者聚聚号”,
//            “nckname”:” 加入者昵称”,
//            ”invitorNo”:”邀请者聚聚号”,
//            “invitorNickName”:” 邀请者昵称”


    //封装消息通知对象，防止多处定义json串
    //加群邀请通知Bean
    @JsonIgnoreProperties(value = {"replayId", "replayTime"})
    public static class InviteInGroupBean extends ReplyBean {
        public String groupId;
        public String userNo;
        public String nickName;
        public String invitorNo;
        public String invitorNickName;


        public static InviteInGroupBean valueOf(String groupId, String userNo, String nickName,
                                                String invitorNo, String invitorNickName) {
            InviteInGroupBean bean = new InviteInGroupBean();
            bean.groupId = groupId;
            bean.userNo = userNo;
            bean.nickName = nickName;
            bean.invitorNo = invitorNo;
            bean.invitorNickName = invitorNickName;
            return bean;
        }

    }



}
