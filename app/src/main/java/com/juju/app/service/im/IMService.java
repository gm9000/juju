package com.juju.app.service.im;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.juju.app.entity.base.MessageEntity;
import com.juju.app.event.PriorityEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.service.im.manager.IMMessageManager;
import com.juju.app.service.im.manager.IMRecentSessionManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.manager.IMUnreadMsgManager;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class IMService extends Service implements XMPPServiceCallback {

    private final String TAG = getClass().getSimpleName();

    private Logger logger = Logger.getLogger(IMService.class);


    private IMServiceBinder binder = new IMServiceBinder();

    private IMLoginManager loginMgr = IMLoginManager.instance();
    private IMMessageManager messageMgr = IMMessageManager.instance();
    private IMSessionManager sessionMgr = IMSessionManager.instance();
    private IMUnreadMsgManager unReadMsgMgr = IMUnreadMsgManager.instance();



    public IMService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind" + this.toString());
        return binder;
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate" + this.toString());
        Log.d(TAG, "threadId" + Thread.currentThread().getId());
        super.onCreate();
        EventBus.getDefault().register(this);

//        startForeground((int) System.currentTimeMillis(), new Notification());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand" + this.toString());
        Context ctx = getApplicationContext();
        loginMgr.onStartIMManager(ctx, this);
        //注册消息回调事件
        loginMgr.registerCallback(this);
        messageMgr.onStartIMManager(ctx, this);
        sessionMgr.onStartIMManager(ctx, this);
        unReadMsgMgr.onStartIMManager(ctx, this);
        Log.d(TAG, "XMPP建立连接完成");
        //服务kill掉后能重启
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy" + this.toString());
        IMLoginManager.instance().unRegisterCallback();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind" + this.toString());
        return super.onUnbind(intent);
    }


    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind" + this.toString());
        super.onRebind(intent);
    }


    /**
     * 新消息
     *
     * @param from
     * @param messageBody
     * @param silent_notification
     */
    @Override
    public void newMessage(String from, String messageBody, boolean silent_notification) {

    }

    /**
     * 消息异常
     *
     * @param from
     * @param errorBody
     * @param silent_notification
     */
    @Override
    public void messageError(String from, String errorBody, boolean silent_notification) {

    }

    /**
     * 连接状态变更
     */
    @Override
    public void connectionStateChanged() {

    }

    public class IMServiceBinder extends Binder {
        public IMService getService() {
            return IMService.this;
        }
    }

    /**收到消息需要上层的activity判断 {MessageActicity onEvent(PriorityEvent event)}，这个地方是特殊分支*/
    @Subscribe(threadMode = ThreadMode.POSTING, priority = Constants.SERVICE_EVENTBUS_PRIORITY)
    public void onEvent4PriorityEvent(PriorityEvent event){
        switch (event.event){
            case MSG_RECEIVED_MESSAGE:{
                MessageEntity entity = (MessageEntity) event.object;
                /**非当前的会话*/
                logger.d("messageactivity#not this session msg -> id:%s", entity.getFromId());
                //发送确认消息，暂不需要
//                messageMgr.ackReceiveMsg(entity);
                unReadMsgMgr.add(entity);
            }break;
        }
    }

    public IMLoginManager getLoginManager() {
        return loginMgr;
    }

    public IMUnreadMsgManager getUnReadMsgManager() {
        return unReadMsgMgr;
    }

    public IMMessageManager getMessageManager() {
        return messageMgr;
    }

    public IMSessionManager getSessionManager() {
        return sessionMgr;
    }


}
