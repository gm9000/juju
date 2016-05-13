package com.juju.app.service.im.thread;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.JoinChatRoomEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * 项目名称：juju
 * 类描述：获取群组成员线程
 * 创建人：gm
 * 日期：2016/5/6 16:21
 * 版本：V1.0.0
 */
public class GetGroupUserThread implements Runnable {

    private Logger logger = Logger.getLogger(GetGroupUserThread.class);

    private final long TIME_WAIT = 10000l;

    //计数器
    private CountDownLatch countDownLatch;

    //group MAP(共享数据，原子操作可保证线程安全)
    private volatile Map<String, GroupEntity> groupMap;

    private volatile Map<String, User> userMap;

    private String id;
    private String name;
    private String desc;
    private String creatorId;

    private UserInfoBean userInfoBean;
    private DaoSupport groupDao;
    private DaoSupport userDao;

    private Object object = new Object();

    public GetGroupUserThread(CountDownLatch countDownLatch, String id, String name,
                              String desc, String creatorId, UserInfoBean userInfoBean,
                              DaoSupport groupDao, DaoSupport userDao,
                              Map<String, GroupEntity> groupMap, Map<String, User> userMap) {
        this.countDownLatch = countDownLatch;
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.creatorId = creatorId;
        this.userInfoBean = userInfoBean;
        this.groupDao = groupDao;
        this.userDao = userDao;
        this.groupMap = groupMap;
        this.userMap = userMap;
    }


    @Override
    public void run() {
        execute();
        //计数器-1
        System.out.println("执行id===================="+id);
        countDownLatch.countDown();
    }

    private void execute() {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("userNo", userInfoBean.getJujuNo());
        valueMap.put("token", userInfoBean.getToken());
        valueMap.put("groupId", id);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                0, HttpConstants.getUserUrl() + "/getGroupUsers",
                new HttpCallBack4OK() {

                    @Override
                    public void onSuccess4OK(Object obj, int accessId) {
                        synchronized (object) {
                            if(obj instanceof JSONObject) {
                                JSONObject jsonObject = (JSONObject)obj;
                                try {
                                    int status = jsonObject.getInt("status");
                                    if(status == 0) {
                                        JSONArray jsonArray = jsonObject.getJSONArray("users");
                                        if(jsonArray != null && jsonArray.length() >0) {
                                            String peerId = id+"@"+userInfoBean.getmMucServiceName()
                                                    +"."+userInfoBean.getmServiceName();
                                            StringBuilder userNoSbf = new StringBuilder();
                                            StringBuilder avatarSbf = new StringBuilder();
                                            for (int i = 0; i <jsonArray.length() ; i++) {
                                                JSONObject jsonUser = (JSONObject) jsonArray.get(i);
                                                String userNo = jsonUser.getString("userNo");
                                                String nickName = jsonUser.getString("nickName");
                                                String userPhone = jsonUser.getString("userPhone");
                                                String birthday = jsonUser.getString("birthday");
                                                int gender = jsonUser.getInt("gender");
                                                String createTime = jsonUser.getString("createTime");

                                                Date birthdayDate = null;
                                                Date createTimeDate = null;

                                                if(StringUtils.isNotBlank(birthday)) {
                                                    try {
                                                        birthdayDate = DateUtils.parseDate(birthday,
                                                                new String[] {"yyyy-MM-dd HH:mm:ss"});
                                                    } catch (ParseException e) {

                                                    }
                                                }
                                                if(StringUtils.isNotBlank(createTime)) {
                                                    try {
                                                        createTimeDate = DateUtils.parseDate(createTime,
                                                                new String[] {"yyyy-MM-dd HH:mm:ss"});
                                                    } catch (ParseException e) {

                                                    }
                                                }

                                                User userEntity = new User(userNo,  userPhone,  "",
                                                 gender,  nickName,  createTimeDate,  birthdayDate,
                                                        HttpConstants.getPortraitUrl()+userNo);

                                                saveUsers(userEntity);
                                                userMap.put(userNo, userEntity);
                                                userNoSbf.append(userNo);
//                                                avatarSbf.append(HttpConstants.getPortraitUrl()+userNo);
                                                if(i < jsonArray.length() - 1) {
                                                    userNoSbf.append(",");
//                                                    avatarSbf.append(",");
                                                }
                                            }
                                            GroupEntity groupEntity = new GroupEntity(0l, id,
                                                    peerId, 0, name, avatarSbf.toString(), creatorId,
                                                    jsonArray.length(), userNoSbf.toString(),
                                                    0, DBConstant.GROUP_STATUS_ONLINE, 0, 0, desc);
                                            groupDao.replaceInto(groupEntity);
                                            groupMap.put(groupEntity.getPeerId(), groupEntity);

                                            IMGroupManager.instance().joinChatRoom(groupEntity);
                                        }
                                    }
                                } catch (JSONException e) {
                                    logger.error(e);
                                }
                            }
                            object.notify();
                        }
                    }

                    @Override
                    public void onFailure4OK(Exception e, int accessId) {
                        synchronized (object) {
                            object.notify();
                        }
                    }
                }, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
            synchronized (object) {
                object.wait(TIME_WAIT);
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        } catch (JSONException e) {
            logger.error(e);
        } catch (InterruptedException e) {
            logger.error(e);
        }
    }

    private void saveUsers(User user) {
        User dbUser = (User) userDao.findUniByProperty("user_no", user.getUserNo());
        if(dbUser != null) {
            if(StringUtils.isNotBlank(user.getId())) {
                dbUser.setId(user.getId());
            }
            if(StringUtils.isNotBlank(user.getNickName())) {
                dbUser.setNickName(user.getNickName());
            }
            if(StringUtils.isNotBlank(user.getUserPhone())) {
                dbUser.setUserPhone(user.getUserPhone());
            }
            if(user.getBirthday() != null) {
                dbUser.setBirthday(user.getBirthday());
            }
            if(user.getUpdateTime() != null) {
                dbUser.setUpdateTime(user.getUpdateTime());
            }
            if(StringUtils.isNotBlank(user.getAvatar())) {
                dbUser.setAvatar(user.getAvatar());
            }
            dbUser.setGender(user.getGender());
            userDao.update(dbUser);
        } else {
            userDao.replaceInto(user);
        }
    }

    //发送消息，消息发布者，UI需监听
    private void triggerEvent(Object paramObject)
    {
        EventBus.getDefault().post(paramObject);
    }

}


