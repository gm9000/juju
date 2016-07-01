package com.juju.app.event;

import com.juju.app.entity.chat.OtherMessageEntity;

/**
 * 项目名称：juju
 * 类描述：系统通知栏事件 (由NotifyMessageEvent 过滤)
 * 创建人：gm
 * 日期：2016/6/24 11:29
 * 版本：V1.0.0
 */
public class NotificationMessageEvent {

    public OtherMessageEntity entity;
    public Event event;

    public NotificationMessageEvent() {

    }

    public NotificationMessageEvent(Event event) {
        this.event = event;
    }

    public enum Event {
        //加群邀请通知
        INVITE_USER_RECEIVED,

        //移除群组通知
        REMOVE_GROUP_RECEIVED,


        //打开窗口，事件信息、红点需要清除（对全局有效果）
        INVITE_GROUP_NOTIFY_OPEN_ACTIVITY

    }
}
