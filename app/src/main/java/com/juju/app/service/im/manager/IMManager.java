package com.juju.app.service.im.manager;

import android.app.Service;
import android.content.Context;

import com.juju.app.service.im.service.SocketService;

import org.greenrobot.eventbus.EventBus;

/**
 * 项目名称：juju
 * 类描述：聊天管理抽象类
 * 创建人：gm
 * 日期：2016/3/21 10:52
 * 版本：V1.0.0
 */
public abstract class IMManager {

    protected Context ctx;

    protected Service service;

    protected SocketService socketService;



    public IMManager() {
    }

    //启动设置参数
    public void onStartIMManager(Context paramContext, Service service)
    {
        setContext(paramContext);
        setService(service);
        doOnStart();
    }

    public void setContext(Context context) {
        if (context == null) {
            throw new RuntimeException("context is null");
        }
        ctx = context;
    }

    public void setService(Service service) {
        if (service == null) {
            throw new RuntimeException("service is null");
        }
        this.service = service;
    }


    public abstract void doOnStart();

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     * */
    public abstract void reset();


    //发送消息，消息发布者，UI需监听
    protected void triggerEvent(Object paramObject)
    {
        EventBus.getDefault().post(paramObject);
    }


    protected boolean isAuthenticated() {
        boolean isAuth = false;
        if(socketService != null
                && socketService.isAuthenticated()) {
            return true;
        }
        return isAuth;
    }

    protected void setSocketService(SocketService socketService) {
        this.socketService = socketService;
    }



}
