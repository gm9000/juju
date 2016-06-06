package com.juju.app.event;


/**
 * smack socket事件
 */
public enum SmackSocketEvent {

    /**请求登陆的过程*/
    CONNECTING_MSG_SERVER,
    CONNECT_MSG_SERVER_SUCCESS,
    CONNECT_MSG_SERVER_FAILED,
    MSG_SERVER_DISCONNECTED    //channel disconnect 会触发，再应用开启内，要重连【可能是服务端、客户端断掉】
}
