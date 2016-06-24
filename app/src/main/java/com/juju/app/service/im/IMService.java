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
import com.juju.app.service.im.manager.IMNotificationManager;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.manager.IMRecentSessionManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.manager.IMUnreadMsgManager;
import com.juju.app.service.im.sp.ConfigurationSp;
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

    //通知管理服务
    private IMNotificationManager notificationMgr = IMNotificationManager.instance();

    private ConfigurationSp configSp;



    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
//        Context ctx = getApplicationContext();
//        loginMgr.onStartIMManager(ctx, this);
//        messageMgr.onStartIMManager(ctx, this);
//        sessionMgr.onStartIMManager(ctx, this);
//        unReadMsgMgr.onStartIMManager(ctx, this);
//        groupMgr.onStartIMManager(ctx, this);
//        contactMgr.onStartIMManager(ctx, this);
//        notificationMgr.onStartIMManager(ctx, this);
//        otherManager.onStartIMManager(ctx, this);

        startForeground((int) System.currentTimeMillis(), new Notification());
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.d("onStartCommand" + this.toString());
        Context ctx = getApplicationContext();
        loginMgr.onStartIMManager(ctx, this);
        messageMgr.onStartIMManager(ctx, this);
        sessionMgr.onStartIMManager(ctx, this);
        unReadMsgMgr.onStartIMManager(ctx, this);
        groupMgr.onStartIMManager(ctx, this);
        contactMgr.onStartIMManager(ctx, this);
        notificationMgr.onStartIMManager(ctx, this);
        otherManager.onStartIMManager(ctx, this);
        //服务kill掉后能重启
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        //记录最后保存的 sessionKey
//        List<SessionEntity> sessionList = sessionMgr.getRecentSessionList();
//        if(sessionList != null) {
//            for(SessionEntity sessionEntity : sessionList) {
//                SpfUtil.put(getApplicationContext(), sessionEntity.getSessionKey(),
//                        sessionEntity.getUpdated());
//            }
//        }
        handleLogout();
        super.onDestroy();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    @Override
    public void onRebind(Intent intent) {
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
        logger.d("imservice#onNormalLoginOk Successful");
        //获取群组或联系人构造群聊列表

        Context ctx = getApplicationContext();
        String userNo = loginMgr.getUserNo();
        configSp = ConfigurationSp.instance(ctx, userNo);
        //先获取群组
        groupMgr.onNormalLoginOk();
        //再获取联系人
        contactMgr.onNormalLoginOk();
        sessionMgr.onNormalLoginOk();
        unReadMsgMgr.onNormalLoginOk();
        //消息通知栏状态
        notificationMgr.onLoginSuccess();
        otherManager.onNormalLoginOk();
    }

    /**
     * 自动登陆/离线登陆成功
     * autoLogin -> DB(loginInfo,loginId...) -> loginSucsess
     */
    private void onLocalLoginOk(){
//        Context ctx = getApplicationContext();
        Context ctx = getApplicationContext();
        String userNo = loginMgr.getUserNo();
        configSp = ConfigurationSp.instance(ctx, userNo);
        groupMgr.onLocalLoginOk();
        contactMgr.onLocalLoginOk();
        sessionMgr.onLocalLoginOk();
        unReadMsgMgr.onLocalLoginOk();
        otherManager.onLocalLoginOk();

        messageMgr.onLoginSuccess();

        //消息通知栏状态
        notificationMgr.onLoginSuccess();
    }

    /**
     * 1.从本机加载成功之后，请求MessageService建立连接成功(loginMessageSuccess)
     * 2. 重连成功之后
     */
    private void onLocalNetOk(){
        Context ctx = getApplicationContext();
        String userNo = loginMgr.getUserNo();
        configSp = ConfigurationSp.instance(ctx, userNo);

        groupMgr.onLocalNetOk();
        contactMgr.onLocalNetOk();
        sessionMgr.onLocalNetOk();
        unReadMsgMgr.onLocalNetOk();
        otherManager.onLocalNetOk();
    }

    //更加用户ID初始化数据仓库和聊天服务
    public void initDaoAndService() {
//        loginMgr.initDaoAndService();
        groupMgr.initDaoAndService();
        contactMgr.initDaoAndService();
        sessionMgr.initDaoAndService();
        unReadMsgMgr.initDaoAndService();
        messageMgr.initDaoAndService();
        otherManager.initDaoAndService();
    }

    private void handleLogout() {
        loginMgr.reset();
        groupMgr.reset();
        messageMgr.reset();
        sessionMgr.reset();
        unReadMsgMgr.reset();
        contactMgr.reset();
        notificationMgr.reset();
        otherManager.reset();
    }



    /**收到消息需要上层的activity判断 {ChatActicity onEvent(PriorityEvent event)}，这个地方是特殊分支*/
    @Subscribe(threadMode = ThreadMode.POSTING, priority = Constants.SERVICE_EVENTBUS_PRIORITY)
    public void onEvent4PriorityEvent(PriorityEvent event){
        switch (event.event){
            case MSG_RECEIVED_MESSAGE:
                MessageEntity entity = (MessageEntity) event.object;
                /**非当前的会话*/
                logger.d("chatactivity#not this session msg -> id:%s", entity.getFromId());
                unReadMsgMgr.add(entity);
                break;
        }
    }

//    @Subscribe(threadMode = ThreadMode.POSTING, priority = Constants.SERVICE_EVENTBUS_PRIORITY)
//    public void onEvent4PriorityEvent(PriorityEvent event){
//        switch (event.event){
//            case MSG_RECEIVED_MESSAGE:
//                MessageEntity entity = (MessageEntity) event.object;
//                /**非当前的会话*/
//                logger.d("chatactivity#not this session msg -> id:%s", entity.getFromId());
//                unReadMsgMgr.add(entity);
//                break;
//        }
//    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4Login(LoginEvent event){
        switch (event){
            //网络登陆回调
            case LOGIN_OK:
                initDaoAndService();
                onNormalLoginOk();
                break;
            //本地登陆回调
            case LOCAL_LOGIN_SUCCESS:
                initDaoAndService();
                onLocalLoginOk();
                break;
            case  LOCAL_LOGIN_MSG_SERVICE:
//                initDaoAndService();
                onLocalNetOk();
                break;
            case LOGIN_OUT:
                handleLogout();
                break;
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

    public IMNotificationManager getNotificationManager() {
        return notificationMgr;
    }

    public IMOtherManager getOtherManager() {
        return otherManager;
    }

    public ConfigurationSp getConfigSp() {
        return configSp;
    }
}
