package com.juju.app.event;


import com.juju.app.entity.chat.UnreadEntity;

public class UnreadEvent {

    public UnreadEntity entity;
    public Event event;

    public UnreadEvent(){}
    public UnreadEvent(Event e){
        this.event = e;
    }

    public enum Event {
        UNREAD_MSG_LIST_OK,

        UNREAD_NOTIFY_LIST_OK,

        UNREAD_MSG_RECEIVED,

        SESSION_READED_UNREAD_MSG
    }
}
