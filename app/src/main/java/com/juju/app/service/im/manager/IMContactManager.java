package com.juju.app.service.im.manager;

import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.event.UserInfoEvent;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.utils.Logger;
import com.juju.app.utils.pinyin.PinYinUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

//    private DaoSupport groupDao;


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
//        groupDao = new GroupDaoImpl(ctx);
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
        logger.d("contact#findAll");
        //获取本地用户信息
        List<User> userlist = userDao.findAll();
        logger.d("contact#findAll dbsuccess");
        for(User userInfo : userlist){
            PinYinUtil.getPinYin(userInfo.getNickName(), userInfo.getPinyinElement());
            userMap.put(userInfo.getUserNo(), userInfo);
        }
        userDataReady = true;
        //不需要更新用户列表 暂时不需要通知
//        triggerEvent(UserInfoEvent.USER_INFO_OK);
    }

    /**
     * 网络连接成功，登陆之后请求
     * 1：目前只有群组用户，在IMGroupManager已处理
     * 2：后期扩展单聊需要实现此方法
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

    // 确实要将对比的抽离出来 Collections
//    public  List<User> getSearchContactList(String key){
//        List<User> searchList = new ArrayList<>();
//        for(Map.Entry<String, User> entry : userMap.entrySet()){
//            User user = entry.getValue();
//            if (IMUIHelper.handleContactSearch(key, user)) {
//                searchList.add(user);
//            }
//        }
//        return searchList;
//    }

    //获取排序集合
    public  List<User> getContactSortedList() {
        List<User> contactList = new ArrayList<>(userMap.values());
        Collections.sort(contactList, new Comparator<User>(){
            @Override
            public int compare(User entity1, User entity2) {
                if (entity2.getPinyinElement().pinyin.startsWith("#")) {
                    return -1;
                } else if (entity1.getPinyinElement().pinyin.startsWith("#")) {
                    // todo eric guess: latter is > 0
                    return 1;
                } else {
                    if(entity1.getPinyinElement().pinyin==null)
                    {
                        PinYinUtil.getPinYin(entity1.getNickName(), entity1.getPinyinElement());
                    }
                    if(entity2.getPinyinElement().pinyin==null)
                    {
                        PinYinUtil.getPinYin(entity2.getNickName(), entity2.getPinyinElement());
                    }
                    return entity1.getPinyinElement().pinyin.compareToIgnoreCase(entity2.getPinyinElement().pinyin);
                }
            }
        });
        return contactList;
    }

    @Override
    protected void triggerEvent(Object paramObject) {
        if(paramObject instanceof UserInfoEvent) {
            UserInfoEvent event = (UserInfoEvent)paramObject;
            switch (event){
                case USER_INFO_OK:
                    userDataReady = true;
                    break;
            }
            EventBus.getDefault().postSticky(event);
        }
    }
}
