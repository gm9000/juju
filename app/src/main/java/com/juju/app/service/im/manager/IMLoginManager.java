package com.juju.app.service.im.manager;

import android.util.Log;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.MessageDao;
import com.juju.app.biz.impl.MessageDaoImpl;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.entity.User;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.SmackSocketEvent;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ThreadPoolUtil;

import org.greenrobot.eventbus.EventBus;
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

    private Logger logger = Logger.getLogger(IMLoginManager.class);

    private volatile static IMLoginManager inst;

    private SocketService socketService;

    private DaoSupport messageDao;

    private DaoSupport userDao;

    private boolean isKickout = false;

    //自身状态是否有改变（譬如用户名、密码）
    private boolean  identityChanged = false;


    public IMLoginManager() {
        super();
    }


    //以前是否登陆过，用户重新登陆的判断
    private boolean everLogined = false;

    //本地包含登陆信息了[可以理解为支持离线登陆了]
    private boolean isLocalLogin = false;

    //是否自动登陆
    private boolean autoLogin = true;

    private String userNo;




    @Override
    public void doOnStart() {
//        messageDao = new MessageDaoImpl(ctx);
//        userDao = new UserDaoImpl(ctx);
//        socketService = new XMPPServiceImpl(ctx.getContentResolver(), service, messageDao);
//        logger.d("IMLoginManager#doOnStart#this -> this:%s", socketService.toString());
//        //登录成功后，为MessageManager设置XMPP服务， 暂时这样处理
//        IMMessageManager.instance().setSocketService(socketService);
//        IMUnreadMsgManager.instance().setSocketService(socketService);
//        IMSessionManager.instance().setSocketService(socketService);
//        IMOtherManager.instance().setSocketService(socketService);
//        IMGroupManager.instance().setSocketService(socketService);
    }



    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        identityChanged = false;
        isKickout=false;
        everLogined = false;
        isLocalLogin = false;
        //关闭消息服务连接
        socketService.logout();
        socketService = null;
        messageDao = null;
        userDao = null;
    }

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMLoginManager instance() {
        if(inst == null) {
            synchronized (IMLoginManager.class) {
                if (inst == null) {
                    inst = new IMLoginManager();
                }
            }
        }
        return inst;
    }

    public void login() {
        ThreadPoolUtil.instance().executeImTask(new Runnable() {
            @Override
            public void run() {
                try {
//                    handlerLoginEvent(LoginEvent.LOGINING);
                    logger.d("IMLoginManager#login -> socketService:%s",
                            socketService.toString());
                    boolean bool = socketService.login();
                    if(!bool) {
                        handlerLoginEvent(LoginEvent.LOGIN_MSG_FAILED);
                    }  else {
                        onLoginOk();
                    }
                } catch (JUJUXMPPException e) {
                    logger.error(e);
                    handlerSocketEvent(SmackSocketEvent.CONNECT_MSG_SERVER_FAILED);
                } catch (XMPPException e) {
                    logger.error(e);
                    handlerSocketEvent(SmackSocketEvent.CONNECT_MSG_SERVER_FAILED);
                } catch (SmackException e) {
                    logger.error(e);
                    handlerSocketEvent(SmackSocketEvent.CONNECT_MSG_SERVER_FAILED);
                } catch (IOException e) {
                    logger.error(e);
                    handlerSocketEvent(SmackSocketEvent.CONNECT_MSG_SERVER_FAILED);
                }
            }
        });
    }

    // 自动登陆流程
    public void autoLogin(String userNo, String pwd){
        // 初始化数据库
//        DBInterface.instance().initDbHelp(ctx, mLoginId);
        //用户需要添加密码、token，目前只验证聚聚号
        User loginEntity = (User) userDao.findUniByProperty("user_no", userNo);
        if(loginEntity == null) {
            loginEntity = (User) userDao.findUniByProperty("user_phone", userNo);
        }
        do{
            if(loginEntity == null){
                break;
            }
            this.userNo = userNo;
//            loginInfo = loginEntity;
//            loginId = loginEntity.getPeerId();
            // 这两个状态不要忘记掉
            isLocalLogin = true;
            everLogined = true;
            //TODO 本地登陆成功
            triggerEvent(LoginEvent.LOCAL_LOGIN_SUCCESS);
        } while(false);
        // 开始请求网络

    }

    //处理用户登陆事件
    public void handlerLoginEvent(LoginEvent event) {
        switch (event) {
            case LOGIN_OK:
                isKickout = false;
                isLocalLogin = true;
                break;
            case LOGIN_AUTH_FAILED:
            case LOGIN_INNER_FAILED:
                isLocalLogin = false;
                break;
            case LOGIN_MSG_FAILED:
                isKickout = true;
                break;
        }
        EventBus.getDefault().postSticky(event);
    }

    //处理Smack socket事件
    public void handlerSocketEvent(SmackSocketEvent event) {
        EventBus.getDefault().postSticky(event);
    }



    public void logout() {
        ThreadPoolUtil.instance().executeImTask(new Runnable() {
            @Override
            public void run() {
                logger.d("IMLoginManager#logout");
                socketService.logout();
            }
        });
    }


    public boolean isLocalLogin() {
        return isLocalLogin;
    }

    public boolean isKickout() {
        return isKickout;
    }

    public void setKickout(boolean kickout) {
        isKickout = kickout;
    }


    public boolean isAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }


    public void onLoginOk() {
        logger.i("login#onLoginOk");
        everLogined = true;
        isKickout = false;

        // 判断登陆的类型
        if(isLocalLogin){
            //触发ImService onLocalNetOk方法
            triggerEvent(LoginEvent.LOCAL_LOGIN_MSG_SERVICE);
        }else{
            isLocalLogin = true;
            triggerEvent(LoginEvent.LOGIN_OK);
        }

    }


    public boolean isAuthenticated() {
        return socketService.isAuthenticated();
    }

    //重连
    public void reConnect() {
        if(socketService != null)
            socketService.reConnect();
    }

//    public String getUserNo() {
//        return userNo;
//    }
//
//    public void setUserNo(String userNo) {
//        this.userNo = userNo;
//    }


    /**
     * 初始化DAO和服务(退出登录后或者第一次加载需要初始化)
     */
    public void initDaoAndService() {
        if(messageDao == null) {
            messageDao = new MessageDaoImpl(ctx);
        }
        if(userDao == null) {
            userDao = new UserDaoImpl(ctx);
        }
        if(socketService == null) {
            socketService = new XMPPServiceImpl(ctx.getContentResolver(), service, messageDao);
        }
        logger.d("IMLoginManager#initDaoAndService#this -> this:%s", socketService.toString());
        //登录成功后，为MessageManager设置XMPP服务， 暂时这样处理
        IMMessageManager.instance().setSocketService(socketService);
        IMUnreadMsgManager.instance().setSocketService(socketService);
        IMSessionManager.instance().setSocketService(socketService);
        IMOtherManager.instance().setSocketService(socketService);
        IMGroupManager.instance().setSocketService(socketService);
        IMOtherManager.instance().setSocketService(socketService);
    }

    public void createAccount(final String userNo, final String password) {
        if(socketService == null) {
            socketService = new XMPPServiceImpl(ctx.getContentResolver(), service, messageDao);
        }
        ThreadPoolUtil.instance().executeImTask(new Runnable() {
            @Override
            public void run() {
                socketService.createAccount(userNo, password);
            }
        });
    }
}
