package com.juju.app.event;


import com.juju.app.entity.chat.GroupEntity;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：群事件
 * 创建人：gm   
 * 日期：2016/5/6 14:13
 * 版本：V1.0.0
 */
public class GroupEvent {

    private GroupEntity groupEntity;
    private Event event;

    /**很多的场景只是关心改变的类型以及change的Ids*/
    private int changeType;
    private List<Integer> changeList;

    public GroupEvent(Event event){
        this.event = event;
    }

    public GroupEvent(Event event, GroupEntity groupEntity){
        this.groupEntity = groupEntity;
        this.event = event;
    }

    public enum Event{
        NONE,

        GROUP_INFO_OK,
        GROUP_INFO_UPDATED,

        CHANGE_GROUP_MEMBER_SUCCESS,
        CHANGE_GROUP_MEMBER_FAIL,
        CHANGE_GROUP_MEMBER_TIMEOUT,

        CREATE_GROUP_OK,
        CREATE_GROUP_FAIL,
        CREATE_GROUP_TIMEOUT,

        SHIELD_GROUP_OK,
        SHIELD_GROUP_TIMEOUT,
        SHIELD_GROUP_FAIL


    }

    public int getChangeType() {
        return changeType;
    }

    public void setChangeType(int changeType) {
        this.changeType = changeType;
    }

    public List<Integer> getChangeList() {
        return changeList;
    }

    public void setChangeList(List<Integer> changeList) {
        this.changeList = changeList;
    }

    public GroupEntity getGroupEntity() {
        return groupEntity;
    }
    public void setGroupEntity(GroupEntity groupEntity) {
        this.groupEntity = groupEntity;
    }

    public Event getEvent() {
        return event;
    }
    public void setEvent(Event event) {
        this.event = event;
    }
}
