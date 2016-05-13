package com.juju.app.service.im;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.PriorityEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.service.im.manager.IMMessageManager;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.manager.IMRecentSessionManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.manager.IMUnreadMsgManager;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.SpfUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;


/**
 * 项目名称：juju
 * 类描述：IMService负责所有IMManager的初始化与reset
 * 创建人：gm   
 * 日期：2016/5/3 9:28
 * 版本：V1.0.0
 */
public class IMService extends Service {

    private final String TAG = getClass().getSimpleName();

    private Logger logger = Logger.getLogger(IMService.class);

    private IMServiceBinder binder = new IMServiceBinder();

    //验证业务管理器
    private IMLoginManager loginMgr = IMLoginManager.instance();
    //消息服务管理器
    private IMMessageManager messageMgr = IMMessageManager.instance();
    //会话服务管理器
    private IMSessionManager sessionMgr = IMSessionManager.instance();
    //未读服务管理器
    private IMUnreadMsgManager unReadMsgMgr = IMUnreadMsgManager.instance();
    //群组服务管理器
    private IMGroupManager groupMgr = IMGroupManager.instance();

    //其他服务管理器
    private IMOtherManager otherManager = IMOtherManager.instance();

    //联系人服务管理器
    private IMContactManager contactMgr = IMContactManager.instance();



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
        messageMgr.onStartIMManager(ctx, this);
        sessionMgr.onStartIMManager(ctx, this);
        unReadMsgMgr.onStartIMManager(ctx, this);
        groupMgr.onStartIMManager(ctx, this);
        contactMgr.onStartIMManager(ctx, this);


        otherManager.onStartIMManager(ctx, this);
        //服务kill掉后能重启
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy" + this.toString());
        EventBus.getDefault().unregister(this);

        //记录最后保存的 sessionKey
        List<SessionEntity> sessionList = sessionMgr.getRecentSessionList();
        if(sessionList != null) {
            for(SessionEntity sessionEntity : sessionList) {
                SpfUtil.put(getApplicationContext(), sessionEntity.getSessionKey(),
                        sessionEntity.getUpdated());
            }
        }
        handleLogout();
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



    public class IMServiceBinder extends Binder {
        public IMService getService() {
            return IMService.this;
        }
    }

    /**
     * 用户输入登陆流程
     * loginSuccess
     */
    private void onNormalLoginOk() {
        logger.d("imservice#onLogin Successful");
        groupMgr.onNormalLoginOk();
        unReadMsgMgr.onNormalLoginOk();
        sessionMgr.onNormalLoginOk();
        contactMgr.onNormalLoginOk();
    }

    /**
     * 自动登陆/离线登陆成功
     * autoLogin -> DB(loginInfo,loginId...) -> loginSucsess
     */
    private void onLocalLoginOk(){
//        Context ctx = getApplicationContext();
        groupMgr.onLocalLoginOk();
        sessionMgr.onLocalLoginOk();
        contactMgr.onLocalLoginOk();

        messageMgr.onLoginSuccess();


    }

    /**
     * 1.从本机加载成功之后，请求MessageService建立链接成功(loginMessageSuccess)
     * 2. 重练成功之后
     */
    private void onLocalNetOk(){
        sessionMgr.onLocalNetOk();
        unReadMsgMgr.onLocalNetOk();
        contactMgr.onLocalNetOk();
        groupMgr.onLocalNetOk();

    }

    private void handleLogout() {
        loginMgr.reset();
        messageMgr.reset();
        sessionMgr.reset();
        unReadMsgMgr.reset();
        contactMgr.reset();
    }


    /**收到消息需要上层的activity判断 {MessageActicity onEvent(PriorityEvent event)}，这个地方是特殊分支*/
    @Subscribe(threadMode = ThreadMode.POSTING, priority = Constants.SERVICE_EVENTBUS_PRIORITY)
    public void onEvent4PriorityEvent(PriorityEvent event){
        switch (event.event){
            case MSG_RECEIVED_MESSAGE:{
                MessageEntity entity = (MessageEntity) event.object;
                /**非当前的会话*/
                logger.d("messageactivity#not this session msg -> id:%s", entity.getFromId());
                unReadMsgMgr.add(entity);
            }break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4Login(LoginEvent event){
        switch (event){
            case LOGIN_OK:
                onNormalLoginOk();break;
            case LOCAL_LOGIN_SUCCESS:
                onLocalLoginOk();
                break;
            case  LOCAL_LOGIN_MSG_SERVICE:
                onLocalNetOk();
                break;
            case LOGIN_OUT:
                handleLogout();break;
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

    public IMGroupManager getGroupManager() {
        return groupMgr;
    }

    public IMContactManager getContactManager() {
        return contactMgr;
    }

}
