package com.juju.app.exceptions;

/**
 * 项目名称：juju
 * 类描述：JUJU XMPP自定义异常
 * 创建人：gm
 * 日期：2016/3/22 10:16
 * 版本：V1.0.0
 */
public class JUJUXMPPException extends Exception {

    public JUJUXMPPException(String message) {
        super(message);
    }

    public JUJUXMPPException(String message, Throwable cause) {
        super(message, cause);
    }
}
