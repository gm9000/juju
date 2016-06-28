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
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.utils.pinyin.PinYinUtil;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 项目名称：juju
 * 类描述：获取群组成员线程
 * 创建人：gm
 * 日期：2016/5/6 16:21
 * 版本：V1.0.0
 */
public class GetGroupUserThread implements Runnable {

    private Logger logger = Logger.getLogger(GetGroupUserThread.class);


    //是否执行成功
    private boolean isExecuteSuccess = true;

    //回调超时时间为10秒
    private final int TIME_WAIT = 10;

//    ReentrantLock lock = new ReentrantLock();
//
//    Condition condition = null;

    //计数器
    private CountDownLatch countDownLatch;

    //采用synchronized或CountDownLatch
    private CountDownLatch myCountDownLatch = new CountDownLatch(1);


    //group MAP(共享数据，原子操作可保证线程安全)
    private volatile Map<String, GroupEntity> groupMap;

    private volatile Map<String, User> userMap;

    private String id;
    private String name;
    private String desc;
    private String creatorId;
    private Date createTime;

    private UserInfoBean userInfoBean;
    private DaoSupport groupDao;
    private DaoSupport userDao;


    public GetGroupUserThread(CountDownLatch countDownLatch, String id, String name,
                              String desc, String creatorId, Date createTime, UserInfoBean userInfoBean,
                              DaoSupport groupDao, DaoSupport userDao,
                              Map<String, GroupEntity> groupMap, Map<String, User> userMap) {
        this.countDownLatch = countDownLatch;
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.creatorId = creatorId;
        this.createTime = createTime;
        this.userInfoBean = userInfoBean;
        this.groupDao = groupDao;
        this.userDao = userDao;
        this.groupMap = groupMap;
        this.userMap = userMap;
    }


    @Override
    public void run() {
//        condition = lock.newCondition();
        logger.d("Begin GetGroupUserThread -> groupId:%s", id);
        execute();
        //计数器-1
//        System.out.println("执行id====================" + id);

        countDownLatch.countDown();
    }


    private void execute() {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("userNo", userInfoBean.getJujuNo());
        valueMap.put("token", userInfoBean.getToken());
        valueMap.put("groupId", id);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                0, HttpConstants.getUserUrl() + "/getGroupUsers",
                new HttpCallBack() {
                    @Override
                    public void onSuccess(Object obj, int accessId, Object inputParameter) {
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
                                            String userNo = JSONUtils.getString(jsonUser, "userNo");
                                            String nickName = JSONUtils.getString(jsonUser, "nickName");
                                            String userPhone = JSONUtils.getString(jsonUser, "userPhone");
                                            String birthday = JSONUtils.getString(jsonUser, "birthday");
                                            int gender = JSONUtils.getInt(jsonUser, "gender");
                                            String createTime = JSONUtils.getString(jsonUser, "createTime");

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
                                            User userEntity = User.buildForReceive(userNo,
                                                    userPhone,  null,  gender, nickName,
                                                    createTimeDate,  birthdayDate);
                                            saveUsers(userEntity);
                                            //暂时这样处理
                                            PinYinUtil.getPinYin(userEntity.getNickName(),
                                                    userEntity.getPinyinElement());
                                            userMap.put(userNo, userEntity);
                                            userNoSbf.append(userNo);
                                            if(i < jsonArray.length() - 1) {
                                                userNoSbf.append(",");
                                            }
                                        }
                                        GroupEntity groupEntity = GroupEntity.buildForReceive(id,
                                                peerId, DBConstant.GROUP_TYPE_NORMAL,  name,
                                                userNoSbf.toString(), creatorId, desc, createTime, null);

                                        //暂时这样处理
                                        GroupEntity cacheGroup = groupMap.get(groupEntity.getPeerId());
                                        if(cacheGroup != null) {
                                            //本地数据
                                            groupEntity.setInviteCode(cacheGroup.getInviteCode());
                                            groupEntity.setQrCode(cacheGroup.getQrCode());
                                        }
                                        groupDao.replaceInto(groupEntity);
                                        groupMap.put(groupEntity.getPeerId(), groupEntity);
                                    }
                                }
                            } catch (JSONException e) {
                                logger.error(e);
                            }

                        }
//                        System.out.println("返回结果id:" + Thread.currentThread().getName());
                        myCountDownLatch.countDown();
                        logger.d("End GetGroupUserThread -> groupId:%s", id);
                    }

                    @Override
                    public void onFailure(Throwable ex, boolean isOnCallback, int accessId, Object inputParameter) {
                        myCountDownLatch.countDown();
                        isExecuteSuccess = false;
                        logger.d("End GetGroupUserThread onFailure -> groupId:%s", id);
                    }

                    @Override
                    public void onCancelled(Callback.CancelledException cex) {
//                        myCountDownLatch.countDown();
                    }

                    @Override
                    public void onFinished() {
//                        myCountDownLatch.countDown();
                    }
                }, valueMap, JSONObject.class);
        try {
            System.out.println("请求前id:"+Thread.currentThread().getName());
            client.sendGet();
            myCountDownLatch.await(TIME_WAIT, TimeUnit.SECONDS);
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            isExecuteSuccess = false;
        } catch (JSONException e) {
            logger.error(e);
            isExecuteSuccess = false;
        } catch (InterruptedException e) {
            logger.error(e);
            isExecuteSuccess = false;
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

    public boolean isExecuteSuccess() {
        return isExecuteSuccess;
    }
}


