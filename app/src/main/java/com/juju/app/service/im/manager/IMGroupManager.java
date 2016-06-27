package com.juju.app.service.im.manager;

import android.os.HandlerThread;
import android.util.Log;

import com.juju.app.R;
import com.juju.app.activity.MainActivity;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.biz.impl.InviteDaoImpl;
import com.juju.app.biz.impl.OtherMessageDaoImpl;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Invite;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.OtherMessageEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.GroupForbiddenEvent;
import com.juju.app.event.JoinChatRoomEvent;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.event.user.InviteGroupEvent;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.MessageConstant;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.service.im.thread.GetGroupUserThread;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ThreadPoolUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.utils.pinyin.PinYinUtil;


import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    //加入聊天室最长等待时间
    private final static int JOIN_GROUP_TIMEOUT = 5;


    //获取群组信息最长等待时间
    private final static int GET_GROUP_TIMEOUT = 15;

    //正式群,临时群都会有的，存在竞争 如果不同时请求的话
    private Map<String, GroupEntity> groupMap = new ConcurrentHashMap<String, GroupEntity>();

    private boolean isGroupReady = false;


    private DaoSupport groupDao;

    private DaoSupport userDao;

    private DaoSupport otherMessageDao;

    private DaoSupport inviteDao;

    private UserInfoBean userInfoBean;

    //已加入聊天室的群聊
    final List<String> joinedGroupPeerIds = new ArrayList<>();



    @Override
    public void doOnStart() {
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        socketService = null;
        isGroupReady =false;
        groupMap.clear();
        joinedGroupPeerIds.clear();
        EventBus.getDefault().unregister(inst);
        groupDao = null;
        userDao = null;
        otherMessageDao = null;
        inviteDao = null;
    }

    public void onNormalLoginOk(){
        logger.i("group#onNormalLoginOk");
        // 加载本地group
        List<GroupEntity> localGroupInfoList = groupDao.findAll();
        if(localGroupInfoList != null) {
            for(GroupEntity groupInfo: localGroupInfoList){
                if(StringUtils.isNotBlank(groupInfo.getPeerId())) {
                    //构造拼音属性
                    PinYinUtil.getPinYin(groupInfo.getMainName(), groupInfo.getPinyinElement());
                    groupMap.put(groupInfo.getPeerId(), groupInfo);
                    if(StringUtils.isBlank(groupInfo.getInviteCode())) {
                        getGroupInviteCode(groupInfo.getId());
                    }
                }
            }
        }
        //群组信息获取成功
        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_OK));
        onLocalNetOk();
        //加入聊天室
//        asynJoinChatRooms();
    }

    /**
     * 1. 加载本地信息
     *
     * */
    public void onLocalLoginOk(){
        logger.i("group#onLocalLoginOk");
        if(!EventBus.getDefault().isRegistered(inst)){
            EventBus.getDefault().register(inst);
        }
        // 加载本地group
        List<GroupEntity> localGroupInfoList = groupDao.findAll();
        if(localGroupInfoList != null) {
            for(GroupEntity groupInfo: localGroupInfoList){
                if(StringUtils.isNotBlank(groupInfo.getPeerId())) {
                    //构造拼音属性
                    PinYinUtil.getPinYin(groupInfo.getMainName(), groupInfo.getPinyinElement());
                    groupMap.put(groupInfo.getPeerId(), groupInfo);
                    if(StringUtils.isBlank(groupInfo.getInviteCode())) {
                        getGroupInviteCode(groupInfo.getId());
                    }
                }
            }
        }
        isGroupReady = true;
        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_OK));
        //加入聊天室
//        asynJoinChatRooms();
    }

    public void onLocalNetOk(){
        reqGetServerGroupList();
    }


    /**
     * 获取远程服务群组列表 ？ TODO 是否需要考虑同步本地群组，目前只是累加
     */
    private void reqGetServerGroupList() {
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
                                        logger.d("GetGroups success-> num:%d", jsonArray.length());
                                        CountDownLatch countDownLatch = new CountDownLatch
                                                (jsonArray.length());
                                        logger.d("begin GetGroupUsers-> beginTime:%d", System.currentTimeMillis());
                                        for (int i = 0; i <jsonArray.length(); i++) {
                                            JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                                            String id = JSONUtils.getString(jsonObject, "id");
                                            String name = JSONUtils.getString(jsonObject, "name");
                                            String desc = JSONUtils.getString(jsonObject, "desc");
                                            String creatorNo = JSONUtils.getString(jsonObject, "creatorNo");
                                            String createTime = JSONUtils.getString(jsonObject, "createTime");
                                            Date createTimeDate = null;
                                            if(StringUtils.isNotBlank(createTime)) {
                                                createTimeDate = DateUtils.parseDate(createTime, new String[]{"yyyy-MM-dd HH:mm:ss"});
                                            }
                                            sendGetGroupUsersToBServer(countDownLatch, id, name, desc, creatorNo, createTimeDate);
                                        }
                                        countDownLatch.await(GET_GROUP_TIMEOUT, TimeUnit.SECONDS);
                                        //群组及群组下用户持久化完毕
                                        logger.d("end GetGroupUsers -> endTime:%d", System.currentTimeMillis());
                                        //重新构造拼音(需要改进)
                                        for(GroupEntity groupInfo: groupMap.values()){
                                            if(StringUtils.isNotBlank(groupInfo.getPeerId())) {
                                                //构造拼音属性
                                                PinYinUtil.getPinYin(groupInfo.getMainName(), groupInfo.getPinyinElement());
                                                groupMap.put(groupInfo.getPeerId(), groupInfo);
                                            }
                                        }
                                        //更新群组
                                        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_OK));
                                        //加入群聊
                                        joinChatRooms(groupMap.values());
                                    }
                                }
                            } catch (JSONException e) {
                                logger.error(e);
                            } catch (InterruptedException e) {
                                logger.error(e);
                            } catch (ParseException e) {
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

    public void sendGetGroupUsersToBServer(CountDownLatch countDownLatch, String id, String name,
                                            String desc, String userNo, Date createTime) {
        GetGroupUserThread thread = new
                GetGroupUserThread(countDownLatch, id,  name,
                desc,  userNo, createTime,  userInfoBean,  groupDao, userDao, groupMap,
                IMContactManager.instance().getUserMap());
        Thread t = new Thread(thread, "GetGroupUserThread");
        ThreadPoolUtil.instance().executeImTask(t);
    }

    /**
     * 创建群聊
     * @param groupId
     * @param mucServiceName
     * @param serviceName
     * @return
     */
    public boolean createChatRoom(String groupId, String groupName, String groupDesc,
                                  String mucServiceName, String serviceName) {
        return socketService.createChatRoom(groupId, groupName, groupDesc,
                mucServiceName, serviceName);
    }


    /**
     * 查询群组列表
     * @return
     */
    public List<GroupEntity> findGroupList4DB() {
        return groupDao.findAll();
    }




    //加入聊天室(对外接口)
//    public void joinChatRoom(GroupEntity groupEntity) {
//        JoinChatRoomEvent joinChatRoomEvent = new JoinChatRoomEvent();
//        joinChatRoomEvent.groupEntity = groupEntity;
//        joinChatRoomEvent.event = JoinChatRoomEvent.Event.JOIN_REQ; //跳转到未读消息管理模块
//        triggerEvent(joinChatRoomEvent);
//    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void test(GroupEntity entity) {
//
//    }


    //获取缓存群组信息（对外接口）
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

    public GroupEntity findGroup(String groupId) {
        logger.d("group#findGroup groupId:%s", groupId);
        if(groupMap.containsKey(groupId)){
            return groupMap.get(groupId);
        }
        return null;
    }


    public void getGroupInviteCode(String groupId) {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("userNo", userInfoBean.getJujuNo());
        valueMap.put("token", userInfoBean.getToken());
        valueMap.put("groupId", groupId);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient(
                0, HttpConstants.getUserUrl() + "/getGroupInviteCode",
                new HttpCallBack4OK() {
                    @Override
                    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                        if(obj instanceof JSONObject) {
                            JSONObject result = (JSONObject)obj;
                            int status = JSONUtils.getInt(result, "status");
                            if(status == 0) {
                                Map<String, Object> parameter = (Map<String, Object>)inputParameter;
                                String groupId = (String)parameter.get("groupId");
                                String inviteCode = JSONUtils.getString(result, "inviteCode");
                                String peerId = groupId+"@"+userInfoBean.getmMucServiceName()
                                        +"."+userInfoBean.getmServiceName();
                                GroupEntity groupEntity = findGroup(peerId);
                                if(groupEntity != null) {
                                    //保存二维码
                                    String qrCode =
                                            HttpConstants.getUserUrl() + "/joinInGroup?inviteCode="+inviteCode;
                                    groupEntity.setQrCode(qrCode);
                                    //保存邀请码
                                    groupEntity.setInviteCode(inviteCode);
                                    groupDao.replaceInto(groupEntity);
                                }
                            }
                        }
                    }
                    @Override
                    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                        logger.e("IMGroupManager#getGroupInviteCode#获取群组邀请码失败", e);
                    }
                }, valueMap, JSONObject.class);

        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {
            logger.e("IMGroupManager#getGroupInviteCode#获取群组邀请码失败", e);
        } catch (JSONException e) {
            logger.e("IMGroupManager#getGroupInviteCode#json解析失败", e);
        }
    }

//    public void replaceGroup(GroupEntity groupEntity) {
//        groupDao.replaceInto(groupEntity);
//    }

    /**
     * 新增群成员
     * ADD_CHANGE_MEMBER_TYPE
     * 可能会触发头像的修改
     */
    public void reqAddGroupMember(String peerId, String groupId, Set<String> addMemberList){
        reqChangeGroupMemberToBServer(peerId, groupId,
                IMBaseDefine.GroupModifyType.GROUP_MODIFY_TYPE_ADD, addMemberList);
    }


    //加入群聊
    public void joinChatRooms(final Collection<GroupEntity> groupEntityList) {
        joinedGroupPeerIds.clear();
        if(groupEntityList != null && groupEntityList.size() >0) {
            final CountDownLatch countDownLatch = new CountDownLatch(groupEntityList.size());
            for (final GroupEntity groupEntity : groupEntityList) {
                final String chatRoomId = groupEntity.getPeerId();
//                ThreadPoolUtil.instance().executeImTask(new Runnable() {
//                    @Override
//                    public void run() {
                        try {
                            String chatRoom = DBConstant.SESSION_TYPE_GROUP + "_" + chatRoomId;
                            long time = (long) SpfUtil.get(ctx, chatRoom, System.currentTimeMillis()
                                    -24*60*60*1000l);
                            socketService.joinChatRoom(chatRoomId, time);
                            logger.d("joinChatRooms -> chatRoomId:%s", chatRoomId);
                            joinedGroupPeerIds.add(chatRoomId);
                            countDownLatch.countDown();
                        } catch (JUJUXMPPException e) {
                            logger.error(e);
                        } catch (XMPPException e) {
                            logger.error(e);
                        } catch (SmackException.NotConnectedException e) {
                            logger.error(e);
                            e.printStackTrace();
                        } catch (SmackException.NoResponseException e) {
                            logger.error(e);
                        }
//                    }
//                });
            }
            try {
                //控制群组登陆时间
                countDownLatch.await(JOIN_GROUP_TIMEOUT, TimeUnit.SECONDS);
                //通知群组信息更新
                triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_UPDATED));
            } catch (InterruptedException e) {
               logger.error(e);
            }

            //通知IMUnreadMsgManager 获取未读消息
            JoinChatRoomEvent joinChatRoomEvent = new JoinChatRoomEvent();
            joinChatRoomEvent.joinedGroupPeerIds = joinedGroupPeerIds;
            joinChatRoomEvent.event = JoinChatRoomEvent.Event.JOIN_OK_4_UNREAD_MSG_REQ;
            triggerEvent(joinChatRoomEvent);
        }
    }

    public void updateGroup4Members(String groupId, String userNo, long updated) {
        //TODO 后期需要调整
        String peerId = groupId+"@"+userInfoBean.getmMucServiceName()
                +"."+userInfoBean.getmServiceName();
        GroupEntity cacheGroup = groupMap.get(peerId);
        if(cacheGroup != null) {
            cacheGroup.setUpdated(updated);
            cacheGroup.setUserList(cacheGroup.getUserList()+","+userNo);
            triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_UPDATED));
            groupDao.replaceInto(cacheGroup);
        }
    }

    //TODO 未读消息和加入群聊没关系 （本地登陆成功也需要加入群聊）
//    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
//    public void onEvent4AsynJoinChatRooms(LoginEvent event) {
//        switch (event) {
//            //网络登陆
//            case LOGIN_OK:
//                //本地登陆
//            case LOCAL_LOGIN_MSG_SERVICE:
//                ThreadPoolUtil.instance().executeImTask(new Runnable() {
//                    @Override
//                    public void run() {
//                        joinChatRooms(groupMap.values());
//                    }
//                });
//                break;
//        }
//
//    }

    @Override
    protected void triggerEvent(Object paramObject) {
       if(paramObject instanceof GroupEvent) {
           GroupEvent event = (GroupEvent)paramObject;
           switch (event.getEvent()){
               case GROUP_INFO_OK:
                   isGroupReady = true;
                   break;
               case GROUP_INFO_UPDATED:
                   isGroupReady = true;
                   break;
           }
       }
        //粘性通知
        EventBus.getDefault().postSticky(paramObject);
    }

    public List<String> getJoinedGroupPeerIds() {
        return joinedGroupPeerIds;
    }


    public void updateGroup4Forbidden(String groupId, boolean isForbidden) {
        GroupEntity groupEntity = groupMap.get(groupId);
        if(groupEntity == null) {
            logger.i("GroupEntity do not exist!");
            return;
        }
        int status = DBConstant.GROUP_STATUS_ONLINE;
        if(isForbidden) {
            status = DBConstant.GROUP_STATUS_SHIELD;
        }
        groupEntity.setStatus(status);
        groupDao.replaceInto(groupEntity);
        IMUnreadMsgManager.instance().setForbidden(
                EntityChangeEngine.getSessionKey(groupId, DBConstant.SESSION_TYPE_GROUP), isForbidden);
        triggerEvent(new GroupEvent(GroupEvent.Event.SHIELD_GROUP_OK, groupEntity));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateGroup4Forbidden(GroupForbiddenEvent event) {
        updateGroup4Forbidden(event.groupId, event.isForbidden);
    }

    /**
     * 初始化DAO和服务(退出登录后或者第一次加载需要初始化)
     */
    public void initDaoAndService() {
        if(groupDao == null) {
            groupDao = new GroupDaoImpl(ctx);
        }
        if(userDao == null) {
            userDao = new UserDaoImpl(ctx);
        }
        if(otherMessageDao == null) {
            otherMessageDao = new OtherMessageDaoImpl(ctx);
        }
        if(inviteDao == null) {
            inviteDao = new InviteDaoImpl(ctx);
        }

    }

    private void reqChangeGroupMemberToBServer(final String peerId, final String groupId,
                                               IMBaseDefine.GroupModifyType groupModifyType,
                                               Set<String> changeMemberList) {

        String userNo = userInfoBean.getJujuNo();
        String token = userInfoBean.getToken();
        for(final String memberNo : changeMemberList) {
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("userNo", userNo);
            valueMap.put("token", token);
            valueMap.put("memberNo", memberNo);
            valueMap.put("groupId", groupId);
            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                    groupModifyType.ordinal(), HttpConstants.getUserUrl() + "/inviteUser",
                    new HttpCallBack4OK() {
                @Override
                public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                    if(obj != null && obj instanceof  JSONObject) {
                        JSONObject jsonRoot = (JSONObject)obj;
                        int status = JSONUtils.getInt(jsonRoot, "status", -1);
                        if(status == 0) {
//                            String inviteCode = JSONUtils.getString(jsonRoot, "inviteCode");
//                            if(StringUtils.isNotBlank(inviteCode)) {
//                                //通知用户（需支持离线消息）
//                                User userInfo = IMContactManager.instance().findContact(memberNo);
//                                if(userInfo != null) {
//
//                                }
//                            }
                            User userInfo = IMContactManager.instance().findContact(memberNo);
                            String uuid = UUID.randomUUID().toString();
                            //发送通知消息
//                            sendInviteGroupNotifyReqToMServer(userInfo,  uuid,  peerId, groupId);
                        } else {
                            logger.d("status is not 0");
                            InviteUserEvent event = new InviteUserEvent(InviteUserEvent.Event
                                    .INVITE_USER_FAILED);
                            triggerEvent(event);
                        }
                    }
                }
                @Override
                public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                    logger.d("inviteUser Failed");
                    logger.error(e);
                    InviteUserEvent event = new InviteUserEvent(InviteUserEvent.Event
                            .INVITE_USER_FAILED);
                    triggerEvent(event);                }
            }, valueMap, JSONObject.class);
            try {
                client.sendPost4OK();
            } catch (UnsupportedEncodingException e) {
                logger.error(e);
            } catch (JSONException e) {
                logger.error(e);
            }
        }
    }





    //邀请加入群聊
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEvent4InviteGroup(InviteGroupEvent event) {
//        switch (event.event) {
//            case INVITE_GROUP_NOTIFY_JOININGROUP_SUCCESS:
//                sendGetGroupInfoToBServer(event.invite);
//                break;
//        }
//    }


    //暂时将消息发送到主线程消息队列 （若消息处理耗时，需要使用HandlerThread或EVENTBUS（子线程）解决）
    static class InviteUserTask {
        private Logger logger = Logger.getLogger(InviteUserTask.class);

        private IMGroupManager imGroupManager;
        private UserInfoBean userInfoBean;
        private SocketService socketService;
        private DaoSupport inviteDao;

        public InviteUserTask(IMGroupManager imGroupManager, UserInfoBean userInfoBean,
                              SocketService socketService, DaoSupport inviteDao) {
            this.imGroupManager = imGroupManager;
            this.userInfoBean = userInfoBean;
            this.socketService = socketService;
            this.inviteDao = inviteDao;
        }

        @Subscribe(threadMode = ThreadMode.POSTING)
        public void onEvent4BusinessFlowSendEvent(InviteUserEvent.BusinessFlow.Send businessFlow) {
            switch (businessFlow) {
                case SEND_INVITE_USER_BSERVER_OK:

                    break;
            }

        }

        //发送加入群组请求——业务服务器
        public void sendInviteUserToBServer(final InviteUserEvent.InviteUserBean inviteUserBean) {
            CommandActionConstant.HttpReqParam JOININGROUP =
                    CommandActionConstant.HttpReqParam.JOININGROUP;
            Map<String, Object> valueMap = HttpReqParamUtil.instance()
                    .buildMap("memberNo, groupId", inviteUserBean.userNo, inviteUserBean.groupId);
            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                    JOININGROUP.code(), JOININGROUP.url(),
                    new HttpCallBack4OK() {
                        @Override
                        public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                            if(obj != null && obj instanceof  JSONObject) {
                                JSONObject jsonRoot = (JSONObject)obj;
                                int status = JSONUtils.getInt(jsonRoot, "status", -1);
                                if(status == 0) {
                                    InviteUserEvent inviteUserEvent = buildBusinessSendFlow(inviteUserBean,
                                            InviteUserEvent.BusinessFlow.Send.SEND_INVITE_USER_BSERVER_OK);
                                    triggerEvent4Sticky(inviteUserEvent);
                                } else {
                                    logger.d("status is not 0");
                                    InviteUserEvent inviteUserEvent = buildBusinessSendFlow(inviteUserBean,
                                            InviteUserEvent.BusinessFlow.Send.SEND_INVITE_USER_BSERVER_FAILED);
                                    triggerEvent4Sticky(inviteUserEvent);
                                }
                            }
                        }
                        @Override
                        public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                            InviteUserEvent inviteUserEvent = buildBusinessSendFlow(inviteUserBean,
                            InviteUserEvent.BusinessFlow.Send.SEND_INVITE_USER_BSERVER_FAILED);
                            triggerEvent4Sticky(inviteUserEvent);
                        }
                    }, valueMap, JSONObject.class);
            try {
                client.sendPost4OK();
            } catch (UnsupportedEncodingException e) {
                logger.error(e);
            } catch (JSONException e) {
                logger.error(e);
            }
        }


        //发送加入群组请求——消息服务器
        public void sendInviteUserToMServer(InviteUserEvent.InviteUserBean inviteUserBean) {
            String peerId = inviteUserBean.userNo+"@"+userInfoBean.getmServiceName();
            GroupEntity groupEntity = imGroupManager.getGroupMap().get(peerId);
            if(groupEntity == null) {
                logger.e("groupEntity is not exist -->peerId:%s", peerId);
                return;
            }
            String message = JacksonUtil.turnObj2String(inviteUserBean);
            String  uuid = UUID.randomUUID().toString();

            //通知群组
            socketService.notifyMessage(peerId, message,
                    IMBaseDefine.NotifyType.INVITE_USER, uuid, true,
                    new XMPPServiceCallbackImpl() {
                        @Override
                        public void onSuccess(Object t) {
                            logger.d("sendInviteGroupNotifyToMServer success");
                            if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                                XMPPServiceImpl.ReplayMessageTime messageTime =
                                        (XMPPServiceImpl.ReplayMessageTime) t;
                                String id = messageTime.getId();
                                String time = messageTime.getTime();

                                Invite dbEntity = (Invite)
                                        inviteDao.findUniByProperty("id", id);

                                if(dbEntity != null) {
                                    Invite.buildInviteReq4SendOnAck(dbEntity,
                                            Long.parseLong(time));
                                    //更新时间
                                    inviteDao.saveOrUpdate(dbEntity);
//                                    InviteUserEvent.InviteUserBean inviteUserBean = InviteUserEvent
//                                            .InviteUserBean.valueOf(dbEntity.getGroupId(), dbEntity.getGroupName(),
//                                                    dbEntity.getUserNo(), dbEntity.getNickName());
//                                    updateGroup4Members(dbEntity.getGroupId(), dbEntity.getUserNo(),
//                                            Long.parseLong(time));
//                                    InviteUserEvent event = new InviteUserEvent(inviteUserBean,
//                                            InviteUserEvent.Event.INVITE_USER_OK);
//                                    triggerEvent4Sticky(event);
                                } else {
                                    InviteUserEvent event = new InviteUserEvent(InviteUserEvent.Event
                                            .INVITE_USER_FAILED);
                                    triggerEvent4Sticky(event);
                                }
                            }
                        }

                        @Override
                        public void onFailed() {
                            logger.d("sendInviteGroupNotifyToMServer failed");
                            InviteUserEvent event = new InviteUserEvent(InviteUserEvent.Event
                                    .INVITE_USER_FAILED);
                            triggerEvent4Sticky(event);
                        }

                        @Override
                        public void onTimeout() {
                            logger.d("sendInviteGroupNotifyToMServer timeout");
                            InviteUserEvent event = new InviteUserEvent(InviteUserEvent.Event
                                    .INVITE_USER_FAILED);
                            triggerEvent4Sticky(event);                      }
                    });
        }


        private void triggerEvent4Sticky(Object paramObject) {
            //粘性通知
            EventBus.getDefault().postSticky(paramObject);
        }

        /**
         * 构建业务流
         * @param inviteUserBean
         * @param send
         */
        private InviteUserEvent buildBusinessSendFlow(InviteUserEvent.InviteUserBean inviteUserBean,
                                       InviteUserEvent.BusinessFlow.Send send) {
            if(send == null)
                throw new IllegalArgumentException("InviteUserTask#inviteUserBean or send is null");

            InviteUserEvent.BusinessFlow businessFlow = new InviteUserEvent.BusinessFlow(send);
            InviteUserEvent event = new InviteUserEvent(inviteUserBean, businessFlow);
            return event;
        }
    }



}
