package com.juju.app.service.im.service;

import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;

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
     * @param to
     * @param message
     * @param uuid
     * @param listener
     * @throws SmackException.NotConnectedException
     */
    public void sendMessage(String to, String message, String uuid,
                            XMPPServiceCallbackImpl listener) throws SmackException.NotConnectedException;


    /**
     * 计算消息总数
     * @param minTime
     * @param maxTime
     * @param uuid
     * @param listener
     * @return
     */
    public void countMessage(String to, String minTime, String maxTime, String uuid,
                             XMPPServiceCallbackImpl listener);

    /**
     * 计算消息总数
     * @param minTime
     * @param maxTime
     * @param uuid
     * @param offset
     * @param length
     * @param listener
     * @return
     */
    public void findHisMessages(String command, String to, String minTime, String maxTime,
                                String uuid, int offset, int length, XMPPServiceCallbackImpl listener);

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

//    /**
//     * 注册回调方法
//     * @param callBack
//     */
//    public void registerCallback(XMPPServiceCallback callBack);
//
//    /**
//     * 注销回调方法
//     */
//    public void unRegisterCallback();

    /**
     * 加入聊天室
     */
    public void joinChatRoom(String chatRoom, long lastUpdateTime) throws JUJUXMPPException, XMPPException,
            SmackException.NotConnectedException, SmackException.NoResponseException;


    /**
     * 创建聊天室
     * @param groupId
     * @param groupName
     * @param mucServiceName
     * @param serviceName
     * @return
     */
    public boolean createChatRoom(String groupId, String groupName, String groupDesc,
                                  String mucServiceName, String serviceName);


    /**
     * 重连
     * @return
     */
    public void reConnect();

    //通知消息
    public void notifyMessage(String peerId, String message, IMBaseDefine.NotifyType notifyType,
                              String uuid, boolean isSaveMsg, XMPPServiceCallbackImpl listener,
                              Object... reqEntity);

    /**
     * 创建账号
     * @param userNo
     * @param password
     * @return
     */
    public boolean createAccount(String userNo, String password);
}
