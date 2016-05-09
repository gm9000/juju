package com.juju.app.service.im.callback;


/**
 * 项目名称：juju
 * 类描述：XMPP协议回掉监听器
 * 创建人：gm
 * 日期：2016/4/23 17:39
 * 版本：V1.0.0
 */
public abstract class XMPPServiceCallbackImpl {

    //0:一问一答 1：一问多答
    private int type;

    private long createTime;
    private long timeOut;

    public XMPPServiceCallbackImpl(long timeOut){
        this.timeOut = timeOut;
        long now = System.currentTimeMillis();
        createTime = now;
    }

    public XMPPServiceCallbackImpl(){
        this.timeOut = 10*1000;
        long now = System.currentTimeMillis();
        createTime = now;
    }

    public XMPPServiceCallbackImpl(int type){
        this.timeOut = 10*1000;
        long now = System.currentTimeMillis();
        createTime = now;
        this.type = type;
    }

    public XMPPServiceCallbackImpl(long timeOut, int type){
        this.timeOut = timeOut;
        long now = System.currentTimeMillis();
        createTime = now;
        this.type = type;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /**
     * 新消息
     *
     */
    public abstract void onSuccess(Object t);

    /**
     * 消息异常
     *
     */
    public abstract void onFailed();


    /**
     * 消息超时
     *
     */
    public abstract void onTimeout();



}
