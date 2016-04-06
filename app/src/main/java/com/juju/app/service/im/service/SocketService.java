package com.juju.app.service.im.service;

import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.service.im.XMPPServiceCallback;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

/**
 * 项目名称：juju
 * 类描述：消息服务通信接口
 * 创建人：gm
 * 日期：2016/3/22 09:58
 * 版本：V1.0.0
 */
public interface SocketService {

    /**
     * 用户登陆消息服务器
     * @return 是否成功
     */
    public boolean login() throws JUJUXMPPException, IOException, XMPPException, SmackException;


    /**
     * 注销登陆
     *
     * @return 是否成功
     */
    public boolean logout();


    /**
     * 是否已经连接上服务器
     *
     * @return
     */
    public boolean isAuthenticated();

    /**
     * 向服务器发送心跳包，保持长连接 通过一个闹钟控制，定时发送，
     */
    public void sendServerPing();

    /**
     * 发送消息
     *
     * @param user
     * @param message
     */
    public void sendMessage(String user, String message) throws SmackException.NotConnectedException;


    /**
     * 注册所有监听
     */
    public void registerAllListener();


    /**
     *
     * @param newPassword 新密码
     * @return
     */
    public String changePassword(String newPassword);

    /**
     * 注册回调方法
     * @param callBack
     */
    public void registerCallback(XMPPServiceCallback callBack);

    /**
     * 注销回调方法
     */
    public void unRegisterCallback();

    /**
     * 加入聊天室
     */
    public void joinChatRoom() throws JUJUXMPPException, XMPPException,
            SmackException.NotConnectedException, SmackException.NoResponseException;
}
