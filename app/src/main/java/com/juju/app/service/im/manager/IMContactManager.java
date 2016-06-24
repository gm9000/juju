package com.juju.app.service.im.manager;

import com.juju.app.R;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.event.UserInfoEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.utils.pinyin.PinYinUtil;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.util.NumberUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
public class IMContactManager extends IMManager implements HttpCallBack4OK {

    private Logger logger = Logger.getLogger(IMContactManager.class);

    private volatile static IMContactManager inst;

    // 自身状态字段
    private boolean  userDataReady = false;
    private Map<String, User> userMap = new ConcurrentHashMap<>();

    private DaoSupport userDao;

    private UserInfoBean userInfoBean;

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
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
//        userDao = new UserDaoImpl(ctx);
//        groupDao = new GroupDaoImpl(ctx);
    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        userDao = null;
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
//        userDataReady = true;
        //不需要更新用户列表 暂时不需要通知
        triggerEvent(UserInfoEvent.USER_INFO_OK);
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
        sendGetUserInfo2BServer(userInfoBean.getJujuNo());
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

    /**
     * 初始化DAO和服务(退出登录后或者第一次加载需要初始化)
     */
    public void initDaoAndService() {
        if(userDao == null) {
            userDao = new UserDaoImpl(ctx);
        }
    }

    //查询用户信息
    public void sendGetUserInfo2BServer(String targetNo) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("targetNo", targetNo);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETUSERINFO;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), this, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        } catch (JSONException e) {
            logger.error(e);
        }
    }

    @Override
    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.getInstance(accessId);
        switch (httpReqParam) {
            case GETUSERINFO :
                JSONObject jsonObject = (JSONObject)obj;
                handlerGetUserInfo4BServer(jsonObject);
                break;
        }
    }

    @Override
    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.getInstance(accessId);
        switch (httpReqParam) {
            case GETUSERINFO :
                logger.error(e);
                logger.e("GETUSERINFO is faild");
                break;
        }
    }

    //处理获取用户详情响应
    private void handlerGetUserInfo4BServer(JSONObject jsonObject) {
        int status = JSONUtils.getInt(jsonObject, "status", -1);
        if(status == 0) {
            //是否触发事件
            boolean needEvent = false;
            JSONObject jsonUser = null;
            try {
                jsonUser = jsonObject.getJSONObject("user");
                if(jsonUser != null) {
//                  String userNo = JSONUtils.getString(jsonUser, "userNo");
                    String nickName = JSONUtils.getString(jsonUser, "nickName");
                    String userPhone = JSONUtils.getString(jsonUser, "userPhone");
                    String birthday = JSONUtils.getString(jsonUser, "birthday");
                    int gender = JSONUtils.getInt(jsonUser, "gender", 1);
                    String createTime = JSONUtils.getString(jsonUser, "createTime");
                    userInfoBean.setPhone(userPhone);
                    userInfoBean.setUserName(nickName);
                    userInfoBean.setGender(gender);
//                    if(NumberUtils.isNumber(birthday)) {
//                        userInfoBean.setBirthday(Long.parseLong(birthday));
//                    }
                    Date birthdayDate = null;
                    if(StringUtils.isNotBlank(birthday)) {
                        try {
                            birthdayDate = DateUtils.parseDate(birthday,
                                    new String[] {"yyyy-MM-dd HH:mm:ss"});
                            userInfoBean.setBirthday(birthdayDate.getTime());
                        } catch (ParseException e) {

                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            String phone = JSONUtils.getString(jsonObject, "userPhone");
//            String nickName = JSONUtils.getString(jsonObject, "nickName");
//            int gender = JSONUtils.getInt(jsonObject, "status", 1);
//            String birthday = JSONUtils.getString(jsonObject, "birthday");
            //持久化数据库
            User user = User.build4UserInfoBean(userInfoBean);
            User dbUser = (User) userDao.findUniByProperty("user_no", user.getUserNo());
            if(dbUser == null) {
                user.setCreateTime(new Date());
            } else {
                user.setUpdateTime(new Date());
            }
            userDao.replaceInto(user);

            if(!userMap.containsKey(user.getUserNo())) {
                needEvent = true;
                userMap.put(user.getUserNo(), user);
                PinYinUtil.getPinYin(user.getNickName(), user.getPinyinElement());
            } else {
                User cacheUser = userMap.get(user.getUserNo());
                if(!user.equals(cacheUser)) {
                    needEvent = true;
                    userMap.put(user.getUserNo(), user);
                    PinYinUtil.getPinYin(user.getNickName(), user.getPinyinElement());
                }
            }
            // 判断有没有必要进行推送
            if(needEvent){
                EventBus.getDefault().postSticky(UserInfoEvent.USER_INFO_UPDATE);
            }
        } else {
            logger.e("GETUSERINFO is faild");
        }
    }


}
