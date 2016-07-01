package com.juju.app.service.notify;

import android.content.Context;
import android.os.Handler;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.SocketService;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/6/29 11:45
 * 版本：V1.0.0
 */
public abstract class BaseNotify<Req> {


    private Thread mUiThread = Thread.currentThread();

    private Handler uiHandler = new Handler();


    protected IMOtherManager imOtherManager;
    protected UserInfoBean userInfoBean;
    protected SocketService socketService;
    protected Context context;

    public void start(IMOtherManager imOtherManager) {
        this.imOtherManager = imOtherManager;
        this.userInfoBean = imOtherManager.getUserInfoBean();
        this.socketService = imOtherManager.getSocketService();
        this.context = imOtherManager.getContext();
    }

    public void stop() {
        this.userInfoBean = null;
        this.socketService = null;
        this.context = null;
        this.imOtherManager = null;
    }


    protected final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            uiHandler.post(action);
        } else {
            action.run();
        }
    }

    protected void triggerEvent(Object paramObject) {
        EventBus.getDefault().post(paramObject);
    }

    protected void triggerEvent4Sticky(Object paramObject) {
        EventBus.getDefault().postSticky(paramObject);
    }

    protected void notifyMessage4User(String peerId, String message, IMBaseDefine.NotifyType notifyType,
                                 String uuid, boolean isSaveMsg, XMPPServiceCallbackImpl listener,
                                 Object... reqEntity) {
            socketService.notifyMessage(peerId, message, Message.Type.normal, notifyType, uuid,
                    isSaveMsg, listener, reqEntity);
    }

    protected void notifyMessage4Group(String peerId, String message, IMBaseDefine.NotifyType notifyType,
                                       String uuid, boolean isSaveMsg, XMPPServiceCallbackImpl listener,
                                       Object... reqEntity) {
        socketService.notifyMessage(peerId, message, Message.Type.groupchat, notifyType, uuid,
                isSaveMsg, listener, reqEntity);
    }

    /**
     * 执行发送命令
     */
    public abstract void executeCommand4Send(Req req);

    public abstract void executeCommand4Recv(Req req);



}
