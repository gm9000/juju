package com.juju.app.service.im.manager;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.JoinChatRoomEvent;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.thread.GetGroupUserThread;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ThreadPoolUtil;
import com.juju.app.utils.json.JSONUtils;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 项目名称：juju
 * 类描述：群组管理服务
 * 创建人：gm
 * 日期：2016/5/6 14:07
 * 版本：V1.0.0
 */
public class IMGroupManager extends IMManager {

    private Logger logger = Logger.getLogger(IMGroupManager.class);

    private volatile static IMGroupManager inst;

    //正式群,临时群都会有的，存在竞争 如果不同时请求的话
    private Map<String, GroupEntity> groupMap = new ConcurrentHashMap<String, GroupEntity>();

    private boolean isGroupReady = false;


    private DaoSupport groupDao;

    private DaoSupport userDao;

    private UserInfoBean userInfoBean;

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMGroupManager instance() {
        if(inst == null) {
            synchronized (IMGroupManager.class) {
                if (inst == null) {
                    inst = new IMGroupManager();
                }
            }
        }
        return inst;
    }

    @Override
    public void doOnStart() {
        groupDao = new GroupDaoImpl(ctx);
        userDao = new UserDaoImpl(ctx);
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        isGroupReady =false;
        groupMap.clear();
        EventBus.getDefault().unregister(inst);
    }

    public void onNormalLoginOk(){
        onLocalLoginOk();
        onLocalNetOk();
    }

    /**
     * 1. 加载本地信息
     * 2. 请求正规群信息 ， 与本地进行对比
     * 3. version groupId 请求
     * */
    public void onLocalLoginOk(){
        logger.i("group#loadFromDb");
//        if(!EventBus.getDefault().isRegistered(inst)){
//            EventBus.getDefault().register(inst);
//        }
        // 加载本地group
        List<GroupEntity> localGroupInfoList = groupDao.findAll();
        if(localGroupInfoList != null) {
            for(GroupEntity groupInfo: localGroupInfoList){
                if(StringUtils.isNotBlank(groupInfo.getPeerId())) {
                    groupMap.put(groupInfo.getPeerId(), groupInfo);
                }
            }
        }
        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_OK));
    }

    public void onLocalNetOk(){
        reqGetNormalGroupList();
    }

    /**
     * 创建群聊
     * @param groupId
     * @param mucServiceName
     * @param serviceName
     * @return
     */
    public boolean createChatRoom(String groupId, String mucServiceName, String serviceName) {
        return socketService.createChatRoom(groupId, mucServiceName, serviceName);
    }


    /**
     * 查询群组 (排序问题后期解决)
     * @return
     */
    public List<GroupEntity> findGroupList4DB() {
        return groupDao.findAll();
    }

    /**
     * 联系人页面正式群的请求
     * todo 正式群与临时群逻辑上的分开的，但是底层应该是相通的
     */
    private void reqGetNormalGroupList() {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("userNo", userInfoBean.getJujuNo());
        valueMap.put("token", userInfoBean.getToken());
        valueMap.put("index", 0);
        valueMap.put("size", Integer.MAX_VALUE);

        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                0, HttpConstants.getUserUrl() + "/getGroups",
                new HttpCallBack4OK() {

                    @Override
                    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                        if(obj instanceof JSONObject) {
                            JSONObject jsonObj = (JSONObject)obj;
                            try {
                                int status = jsonObj.getInt("status");
                                if(status == 0) {
                                    JSONArray jsonArray = jsonObj.getJSONArray("groups");
                                    if(jsonArray != null && jsonArray.length() >0) {
                                        CountDownLatch countDownLatch = new CountDownLatch
                                                (jsonArray.length());
                                        System.out.println("执行id length===================="+jsonArray.length());
                                        for (int i = 0; i <jsonArray.length(); i++) {
                                            JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                                            String id = jsonObject.getString("id");
                                            String name = jsonObject.getString("name");
                                            String desc = JSONUtils.getString(jsonObject, "desc", "");

                                            JSONObject jsonCreator = jsonObject
                                                    .getJSONObject("creator");
                                            String userNo = jsonCreator.getString("userNo");
                                            GetGroupUserThread thread = new
                                                    GetGroupUserThread(countDownLatch, id,  name,
                                                    desc,  userNo,  userInfoBean,  groupDao, userDao, groupMap,
                                                    IMContactManager.instance().getUserMap());
                                            Thread t = new Thread(thread, "GetGroupUserThread");
                                            ThreadPoolUtil.instance().executeImTask(t);
                                        }
                                        //最长等待时间为60秒
                                        countDownLatch.await(60, TimeUnit.SECONDS);
                                        //群组及群组下用户持久化完毕
                                        System.out.println("执行id完毕====================");

                                        //通知群组信息更新
                                        isGroupReady = true;
                                        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_UPDATED));
                                    }
                                }
                            } catch (JSONException e) {
                                logger.error(e);
                            } catch (InterruptedException e) {
                                logger.error(e);
                            }
                        }
                    }

                    @Override
                    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                        logger.error(e);
                    }
                }, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        } catch (JSONException e) {
            logger.error(e);
        }
        logger.i("group#send packet to server");
    }

    public void joinChatRoom(GroupEntity groupEntity) {
        JoinChatRoomEvent joinChatRoomEvent = new JoinChatRoomEvent();
        joinChatRoomEvent.groupEntity = groupEntity;
        joinChatRoomEvent.event = JoinChatRoomEvent.Event.JOIN_REQ;
        triggerEvent(joinChatRoomEvent);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void test(GroupEntity entity) {
//
//    }


    public List<GroupEntity> getGroupList() {
        List<GroupEntity> recentInfoList = new ArrayList<>(groupMap.values());
        return recentInfoList;
    }

    public boolean isGroupReady() {
        return isGroupReady;
    }

    public Map<String, GroupEntity> getGroupMap() {
        return groupMap;
    }

    public List<GroupEntity>  getSearchAllGroupList(String key){
        List<GroupEntity> searchList = new ArrayList<>();
        for(Map.Entry<String,GroupEntity> entry:groupMap.entrySet()){
            GroupEntity groupEntity = entry.getValue();
            if (IMUIHelper.handleGroupSearch(key, groupEntity)) {
                searchList.add(groupEntity);
            }
        }
        return searchList;
    }
}
