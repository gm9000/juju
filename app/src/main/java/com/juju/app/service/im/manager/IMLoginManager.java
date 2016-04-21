package com.juju.app.service.im.manager;

import android.util.Log;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.MessageDao;
import com.juju.app.biz.impl.MessageDaoImpl;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.service.im.XMPPServiceCallback;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.ui.base.BaseApplication;

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

    private DaoSupport messageDao;

    public IMLoginManager() {
        super();
    }

    private UserInfoBean userInfoBean;



    @Override
    public void doOnStart() {
        messageDao = new MessageDaoImpl(ctx);
        socketService = new XMPPServiceImpl(ctx.getContentResolver(), service, messageDao);
        //登录成功后，为MessageManager设置XMPP服务， 暂时这样处理
        IMMessageManager.instance().setSocketService(socketService);

    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "socketService" + socketService.toString());
                    socketService.login();
                    IMLoginManager.instance().joinChatRoom();
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

    public void logout() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "app logout");
                socketService.logout();
            }
        }).start();
    }


//    public void sendMessage(final String user, final String message) {
//        final UserInfoBean userBean = BaseApplication.getInstance().getUserInfoBean();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "socketService"+socketService.toString());
//                try {
//                    socketService.sendMessage(user, message);
//                } catch (SmackException.NotConnectedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

    public void joinChatRoom() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "socketService"+socketService.toString());
                try {
                    //加入聊天室
                    socketService.joinChatRoom();
                    //为用户信息赋值
                    userInfoBean = BaseApplication.getInstance().getUserInfoBean();
                } catch (JUJUXMPPException e) {
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NoResponseException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 注册回调方法
    */
    public void registerCallback(XMPPServiceCallback callBack) {
        socketService.registerCallback(callBack);
    }

    /**
     * 注销回调方法
     */
    public void unRegisterCallback() {
        socketService.unRegisterCallback();
    }


    public UserInfoBean getUserInfoBean() {
        return userInfoBean;
    }
}
