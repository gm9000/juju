package com.juju.app.event.user;

/**
 * 项目名称：juju
 * 类描述：邀请/被邀请加入群聊事件
 * 创建人：gm
 * 日期：2016/6/21 12:14
 * 版本：V1.0.0
 */

import com.juju.app.entity.Invite;

public class InviteGroupEvent {


    public int status = -1;   // -1 未知，不处理 0:拒绝 1：加入
    public Invite invite;
    public Event event;

    public enum Event{
        //邀请基础事件
         //1：申请加入群聊
        INVITE_GROUP_NOTIFY_REQ_SUCCESS,
        INVITE_GROUP_NOTIFY_REQ_FAILED,
        //2：被申请人回复
        INVITE_GROUP_NOTIFY_RES_SUCCESS,
        INVITE_GROUP_NOTIFY_RES_FAILED,

        //基于邀请的业务相关事件
        //1:加入群组（业务层面）
        JOINGROUP_SUCCESS,
        JOINGROUP_FAILED,

        //2:获取群组详情
        GETGROUPINFO_SUCCESS,
        GETGROUPINFO_FAILED,

        //3：打开消息窗口
        OPEN_CHAT_SUCCESS,
        OPEN_CHAT_FAILED
    }


}
