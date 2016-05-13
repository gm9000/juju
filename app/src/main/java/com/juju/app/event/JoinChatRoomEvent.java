package com.juju.app.event;

import com.juju.app.entity.chat.GroupEntity;

/**
 * 项目名称：juju
 * 类描述：加入聊天室事件
 * 创建人：gm
 * 日期：2016/5/10 15:24
 * 版本：V1.0.0
 */
public class JoinChatRoomEvent {

    public GroupEntity groupEntity;

    public Event event;

    public enum Event {

        JOIN_REQ,

        JOIN_OK,

        JOIN_FAILED

    }

}


