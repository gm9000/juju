package com.juju.app.event;

import com.juju.app.entity.chat.GroupEntity;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：加入聊天室事件
 * 创建人：gm
 * 日期：2016/5/10 15:24
 * 版本：V1.0.0
 */
public class JoinChatRoomEvent {

    public List<String> joinedGroupPeerIds;

    public Event event;

    public enum Event {

        JOIN_REQ,

        JOIN_OK_4_UNREAD_MSG_REQ,

        JOIN_OK_4_UNREAD_MSG_SUCCESS,

        JOIN_OK_4_UNREAD_NOTIFY_REQ,

        JOIN_OK_4_UNREAD_NOTIFY_SUCCESS,

        JOIN_FAILED

    }

}


