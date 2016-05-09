package com.juju.app.service.im.thread;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.golobal.DBConstant;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.utils.Logger;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
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

    private String id;
    private String name;
    private String desc;
    private String creatorId;

    private UserInfoBean userInfoBean;
    private DaoSupport groupDao;

    private Object object = new Object();

    public GetGroupUserThread(CountDownLatch countDownLatch, String id, String name,
                              String desc, String creatorId, UserInfoBean userInfoBean, DaoSupport groupDao) {
        this.countDownLatch = countDownLatch;
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.creatorId = creatorId;
        this.userInfoBean = userInfoBean;
        this.groupDao = groupDao;
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
                    public void onSuccess(Object obj, int accessId) {
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
//                                                String avatar = jsonUser.getString("portraitUrl");
                                                userNoSbf.append(userNo);
                                                avatarSbf.append("");
                                                if(i < jsonArray.length() - 1) {
                                                    userNoSbf.append(",");
                                                    avatarSbf.append(",");
                                                }
                                            }
                                            GroupEntity groupEntity = new GroupEntity(0l, id,  peerId, 0, name,
                                                    avatarSbf.toString(), creatorId, jsonArray.length(), userNoSbf.toString(),
                                                    0, DBConstant.GROUP_STATUS_ONLINE, 0, 0, desc);
                                            groupDao.replaceInto(groupEntity);
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
                    public void onFailure(Exception e, int accessId) {
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


//    @Override
//    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
//        synchronized (object) {
//            if(obj[0] instanceof JSONObject) {
//                JSONObject jsonObject = (JSONObject)obj[0];
//                try {
//                    int status = jsonObject.getInt("status");
//                    if(status == 0) {
//                        JSONArray jsonArray = jsonObject.getJSONArray("users");
//                        if(jsonArray != null && jsonArray.length() >0) {
//
//                            String peerId = id+"@"+userInfoBean.getmMucServiceName()
//                                    +"."+userInfoBean.getmServiceName();
//                            StringBuilder userNoSbf = new StringBuilder();
//                            StringBuilder avatarSbf = new StringBuilder();
//                            for (int i = 0; i <jsonArray.length() ; i++) {
//                                JSONObject jsonUser = (JSONObject) jsonArray.get(i);
//                                String userNo = jsonUser.getString("userNo");
//                                String avatar = jsonUser.getString("portraitUrl");
//                                userNoSbf.append(userNo);
//                                avatarSbf.append(avatar);
//                                if(i < jsonArray.length() - 1) {
//                                    userNoSbf.append(",");
//                                    avatarSbf.append(",");
//                                }
//                            }
//                            GroupEntity groupEntity = new GroupEntity(null, id,  peerId, 0, name,
//                                    avatarSbf.toString(), creatorId, jsonArray.length(), userNoSbf.toString(),
//                                    0, DBConstant.GROUP_STATUS_ONLINE, 0, 0, desc);
//                            groupDao.replaceInto(groupEntity);
//                        }
//                    }
//                } catch (JSONException e) {
//                    logger.error(e);
//                }
//            }
//            object.notify();
//        }
//    }
//
//    @Override
//    public void onFailure(HttpException error, String msg, int accessId) {
//        synchronized (object) {
//            object.notify();
//        }
//    }
}


