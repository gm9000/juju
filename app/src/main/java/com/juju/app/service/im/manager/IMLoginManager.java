package com.juju.app.service.im.manager;

import android.util.Log;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.bean.json.UserInfoResBean;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.service.im.XMPPServiceCallback;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.ui.base.BaseApplication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

/**
 * 项目名称：juju
 * 类描述：登陆管理服务
 * 创建人：gm
 * 日期：2016/3/21 10:57
 * 版本：V1.0.0
 */
public class IMLoginManager extends IMManager {

    private final String TAG = getClass().getName();

    private static IMLoginManager inst;

    private SocketService socketService;


    public IMLoginManager() {
        super();
    }

    @Override
    public void doOnStart() {
//        EventBus.getDefault().register(this);
        socketService = new XMPPServiceImpl(ctx.getContentResolver(), service);
        registerCallback();
    }

    public static IMLoginManager instance() {
        synchronized (IMLoginManager.class) {
            if (inst == null) {
                inst = new IMLoginManager();
            }
            return inst;
        }
    }

    public void login() {
        final UserInfoBean userBean = BaseApplication.getInstance().getUserInfoBean();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "socketService"+socketService.toString());
                    socketService.login();
                } catch (JUJUXMPPException e) {
                    Log.e(TAG, "login>>消息服务登录异常");
                } catch (XMPPException e) {
                    Log.e(TAG, "login>>消息服务登录异常");
                } catch (SmackException e) {
                    Log.e(TAG, "login>>消息服务登录异常");
                } catch (IOException e) {
                    Log.e(TAG, "login>>消息服务登录异常");
                }
            }
        }).start();
    }

    public void sendMessage(final String user, final String message) {
        final UserInfoBean userBean = BaseApplication.getInstance().getUserInfoBean();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "socketService"+socketService.toString());
                try {
                    socketService.sendMessage(user, message);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void joinChatRoom() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "socketService"+socketService.toString());
                try {
                    try {
                        socketService.joinChatRoom();
                    } catch (JUJUXMPPException e) {
                        e.printStackTrace();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    } catch (SmackException.NoResponseException e) {
                        e.printStackTrace();
                    }
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 注册回调方法
    */
    public void registerCallback() {
        socketService.registerCallback(new XMPPServiceCallbackImpl4Login());
    }

    /**
     * 注销回调方法
     */
    public void unRegisterCallback() {
        socketService.unRegisterCallback();
    }


    class XMPPServiceCallbackImpl4Login implements XMPPServiceCallback {

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
    }


//    @Subscribe(threadMode = ThreadMode.POSTING)
//    public void onMessageEvent(String event){
//        System.out.println("线程ID=" + Thread.currentThread().getId());
//    }


}
