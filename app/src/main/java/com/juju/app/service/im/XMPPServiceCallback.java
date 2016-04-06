package com.juju.app.service.im;

/**
 * 项目名称：juju
 * 类描述：XMPP服务回调接口
 * 创建人：gm
 * 日期：2016/3/22 10:38
 * 版本：V1.0.0
 */
public interface XMPPServiceCallback {

    /**
     * 新消息
     * @param from
     * @param messageBody
     * @param silent_notification
     */
    void newMessage(String from, String messageBody, boolean silent_notification);

    /**
     * 消息异常
     * @param from
     * @param errorBody
     * @param silent_notification
     */
    void messageError(String from, String errorBody, boolean silent_notification);

    /**
     * 连接状态变更
     */
    void connectionStateChanged();

}
