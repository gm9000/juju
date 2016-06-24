package com.juju.app.event;

import com.juju.app.golobal.IMBaseDefine;

import org.jivesoftware.smack.packet.Message;

/**
 * 项目名称：juju
 * 类描述：消息通知事件
 * 创建人：gm
 * 日期：2016/6/17 14:48
 * 版本：V1.0.0
 */
public class NotifyMessageEvent {
    public IMBaseDefine.NotifyType msgType;
    public Message message;
}
