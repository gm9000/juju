package com.juju.app.service.im.manager;

import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.event.UserInfoEvent;
import com.juju.app.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目名称：juju
 * 类描述：负责用户信息的请求,为会话页面以及联系人页面提供服务（联系人信息管理）
 * 创建人：gm
 * 日期：2016/5/10 10:34
 * 版本：V1.0.0
 */
public class IMContactManager extends IMManager {

    private Logger logger = Logger.getLogger(IMContactManager.class);


    private volatile static IMContactManager inst;

    // 自身状态字段
    private boolean  userDataReady = false;
    private Map<String, User> userMap = new ConcurrentHashMap<String, User>();

    private DaoSupport userDao;


    //双重验证+volatile（禁止JMM重排序）保证线程安全
    public static IMContactManager instance() {
        if(inst == null) {
            synchronized (IMContactManager.class) {
                if (inst == null) {
                    inst = new IMContactManager();
                }
            }
        }
        return inst;
    }

    @Override
    public void doOnStart() {
        userDao = new UserDaoImpl(ctx);
    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        userDataReady = false;
        userMap.clear();
    }

    /**
     * 登陆成功触发
     * auto自动登陆
     * */
    public void onNormalLoginOk(){
        onLocalLoginOk();
        onLocalNetOk();
    }


    /**
     * 加载本地DB的状态
     * 不管是离线还是在线登陆，loadFromDb 要运行的
     */
    public void onLocalLoginOk() {
        logger.d("contact#loadAllUserInfo");
        List<User> userlist = userDao.findAll();
        logger.d("contact#loadAllUserInfo dbsuccess");
        for(User userInfo:userlist){
            userMap.put(userInfo.getUserNo(), userInfo);
        }
        userDataReady = true;
        triggerEvent(UserInfoEvent.USER_INFO_OK);
    }

    /**
     * 网络连接成功，登陆之后请求 (根据修改时间)
     */
    public void onLocalNetOk(){

//        // 用户信息
//        int updateTime = dbInterface.getUserInfoLastTime();
//        logger.d("contact#loadAllUserInfo req-updateTime:%d", updateTime);
//        reqGetAllUsers(updateTime);
    }

    public User findContact(String userNo){
        if(userMap.containsKey(userNo)){
            return userMap.get(userNo);
        }
        return null;
    }


    public Map<String, User> getUserMap() {
        return userMap;
    }


    public boolean isUserDataReady() {
        return userDataReady;
    }
}
