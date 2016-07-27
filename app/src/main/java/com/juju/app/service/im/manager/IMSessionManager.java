package com.juju.app.service.im.manager;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.MessageDao;
import com.juju.app.biz.impl.MessageDaoImpl;
import com.juju.app.biz.impl.SessionDaoIml;
import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.entity.chat.RecentInfo;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.event.SessionEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.helper.chat.SequenceNumberMaker;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.iq.RedisResIQ;
import com.juju.app.service.im.sp.ConfigurationSp;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.Logger;
import com.juju.app.utils.SpfUtil;

import org.apache.commons.lang.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目名称：juju
 * 类描述：最近联系人列表
 * 创建人：gm
 * 日期：2016/4/18 20:14
 * 版本：V1.0.0
 */
public class IMSessionManager extends IMManager {

    private Logger logger = Logger.getLogger(IMSessionManager.class);

    private volatile static IMSessionManager inst;

    // key = sessionKey -->  sessionType_peerId
    private Map<String, SessionEntity> sessionMap = new ConcurrentHashMap<>();

    //SessionManager 状态字段
    private boolean sessionListReady = false;

    private DaoSupport sessionDao;

    private DaoSupport messageDao;

    private UserInfoBean userInfoBean = AppContext.getUserInfoBean();

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMSessionManager instance() {
        if(inst == null) {
            synchronized (IMSessionManager.class) {
                if (inst == null) {
                    inst = new IMSessionManager();
                }
            }
        }
        return inst;
    }

    @Override
    public void doOnStart() {
//        sessionDao = new SessionDaoIml(ctx);
    }

    public void onNormalLoginOk() {
        onLocalLoginOk();
        onLocalNetOk();
    }

    public void onLocalLoginOk(){
        logger.i("session#loadFromDb");
        List<SessionEntity>  sessionInfoList = sessionDao.findAll4Order("updated:desc");
        for(SessionEntity sessionInfo:sessionInfoList){
            sessionMap.put(sessionInfo.getSessionKey(), sessionInfo);
        }

        //TODO 处理session和message表不同步问题(需要从MergeMessageThread处理)
        triggerEvent(SessionEvent.RECENT_SESSION_LIST_SUCCESS);
    }

    /**
     * 从消息服务请求最近会话 （采用未读消息处理）
     *
     */
    public void onLocalNetOk(){

    }



    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        socketService = null;
        sessionListReady = false;
        sessionMap.clear();
        sessionDao = null;
        messageDao = null;
    }

    /**
     * 1.自己发送消息
     * 2.收到消息
     * @param msg
     */
    public void updateSession(MessageEntity msg, @Nullable Boolean isTriggerEvent) {
        logger.d("recent#updateSession msg:%s", msg);
        if (msg == null) {
            logger.d("recent#updateSession is end,cause by msg is null")
            ;
            return;
        }
        String userNo = AppContext.getUserInfoBean().getUserNo();
        boolean isSend = msg.isSend(userNo);
        String peerId = msg.getPeerId(isSend);

        SessionEntity sessionEntity = sessionMap.get(msg.getSessionKey());
        if (sessionEntity == null) {
            logger.d("session#updateSession#not found msgSessionEntity");
            //将MessageEntity转换成SessionEntity
            sessionEntity = EntityChangeEngine.getSessionEntity(msg);
            sessionEntity.setPeerId(peerId);
            sessionEntity.buildSessionKey();
            // 判断群组的信息是否存在
//            if(sessionEntity.getPeerType() == DBConstant.SESSION_TYPE_GROUP){
//                GroupEntity groupEntity = groupManager.findGroup(peerId);
//                if(groupEntity == null){
//                    groupManager.reqGroupDetailInfo(peerId);
//                }
//            }
        }else{
            logger.d("session#updateSession#msgSessionEntity already in Map");
            sessionEntity.setUpdated(msg.getUpdated());
            sessionEntity.setLatestMsgData(msg.getMessageDisplay());
            sessionEntity.setTalkId(msg.getFromId());
            //todo check if msgid is null/0
            sessionEntity.setLatestMsgId(msg.getMsgId());
            sessionEntity.setLatestMsgType(msg.getMsgType());
        }

        /**DB 先更新*/
        ArrayList<SessionEntity> needDb = new ArrayList<>(1);
        needDb.add(sessionEntity);
        sessionDao.batchReplaceInto(needDb);
        sessionMap.put(sessionEntity.getSessionKey(), sessionEntity);
//        SpfUtil.put(ctx, sessionEntity.getSessionKey(),
//                sessionEntity.getUpdated());

        if(isTriggerEvent == null
                || isTriggerEvent == true) {
            //群组列表接收最新消息
            triggerEvent(SessionEvent.RECENT_SESSION_LIST_UPDATE);
        }
    }

    public SessionEntity findSession(String sessionKey){
        if(sessionMap.size()<=0 || TextUtils.isEmpty(sessionKey)){return null;}
        if(sessionMap.containsKey(sessionKey)){
            return sessionMap.get(sessionKey);
        }
        return null;
    }

    public PeerEntity findPeerEntity(String sessionKey){
        if(TextUtils.isEmpty(sessionKey)){
            return null;
        }
        // 拆分
        PeerEntity peerEntity;
        String[] sessionInfo = EntityChangeEngine.spiltSessionKey(sessionKey);
        int peerType = Integer.parseInt(sessionInfo[0]);
        String peerId = sessionInfo[1];
        switch (peerType){
            case DBConstant.SESSION_TYPE_GROUP:{
                peerEntity = IMGroupManager.instance().findGroup(peerId);
            }
            break;
            default:
                throw new IllegalArgumentException("findPeerEntity#peerType is illegal,cause by " +peerType);
        }
        return peerEntity;
    }



    public DaoSupport getSessionDao() {
        return sessionDao;
    }


    // 获取最近联系人列表，RecentInfo 是sessionEntity unreadEntity user/group 等等实体的封装
    public List<RecentInfo> getRecentListInfo(){
        /**整理topList*/
        List<RecentInfo> recentSessionList = new ArrayList<>();
//        String loginId = BaseApplication.getInstance().getUserInfoBean().getmAccount();
//        List<SessionEntity> sessionList = getRecentSessionList();

        //群组作为基础信息（以后有可能调整为会话）
        List<GroupEntity> groupList = IMGroupManager.instance().getGroupList();

        Map<String, User> userMap = IMContactManager.instance().getUserMap();
        Map<String, UnreadEntity> unreadMsgMap = IMUnreadMsgManager.instance().getUnreadMsgMap();
        Map<String, GroupEntity> groupEntityMap = IMGroupManager.instance().getGroupMap();
        HashSet<String> topList = ConfigurationSp.instance(ctx,
                userInfoBean.getUserNo()).getSessionTopList();

        //是否考虑每次都遍历群组
        for(GroupEntity groupEntity : groupList) {
            String sessionKey = groupEntity.getSessionKey();
            boolean isForbidden = groupEntity.getStatus()
                    == DBConstant.GROUP_STATUS_SHIELD ? true : false;
            UnreadEntity unreadEntity = unreadMsgMap.get(sessionKey);
            SessionEntity recentSession = sessionMap.get(sessionKey);
            RecentInfo recentInfo = new RecentInfo(recentSession, groupEntity, unreadEntity);
            if(topList !=null && topList.contains(sessionKey)){
                recentInfo.setTop(true);
            }
            recentInfo.setForbidden(isForbidden);
            recentSessionList.add(recentInfo);
        }

//        for(SessionEntity recentSession : sessionList) {
//            int sessionType = recentSession.getPeerType();
//            String peerId = recentSession.getPeerId();
//            String sessionKey = recentSession.getSessionKey();
//
//            UnreadEntity unreadEntity = unreadMsgMap.get(sessionKey);
//            if(sessionType == DBConstant.SESSION_TYPE_GROUP){
//                GroupEntity groupEntity = groupEntityMap.get(peerId);
//                RecentInfo recentInfo = new RecentInfo(recentSession, groupEntity, unreadEntity);
////                if(topList !=null && topList.contains(sessionKey)){
////                    recentInfo.setTop(true);
////                }
//
//                //谁说的这条信息，只有群组需要，例如 【XXX:您好】
//                //不做判断
////                String lastFromId = recentSession.getTalkId();
////                UserEntity talkUser = userMap.get(lastFromId);
////                // 用户已经不存在了
////                if(talkUser != null){
////                    String  oriContent =  recentInfo.getLatestMsgData();
////                    String  finalContent = talkUser.getMainName() + ": "+oriContent;
////                    recentInfo.setLatestMsgData(finalContent);
////                }
//                recentSessionList.add(recentInfo);
//            }
//        }

        sort(recentSessionList);
        return recentSessionList;
    }

    public List<SessionEntity> getRecentSessionList() {
        List<SessionEntity> recentInfoList = new ArrayList<>(sessionMap.values());
        return recentInfoList;
    }

    public Map<String, SessionEntity> getSessionMap() {
        return sessionMap;
    }


    /**
     * 请求最近回话
     */
//    private void reqGetRecentContacts(long latestUpdateTime,  List<String> chatRoomIds) {
//        logger.i("session#reqGetRecentContacts");
//
//        for(String chatRoom : chatRoomIds) {
//            String uuid = UUID.randomUUID().toString();
//
//            socketService.findHisMessages("zrevrangebyscore", chatRoom, String.valueOf(latestUpdateTime+1), "",
//                    uuid, 0, Constants.MSG_CNT_PER_PAGE, new XMPPServiceCallbackImpl(0) {
//
//                /**
//                 * 新消息
//                 *
//                 * @param t
//                 */
//                @Override
//                public void onSuccess(Object t) {
//                    if(t instanceof RedisResIQ) {
//                        //需要放在循环体外(消息merge)
//                        ArrayList<SessionEntity> needDb = new ArrayList<>();
//                        RedisResIQ redisResIQ = (RedisResIQ)t;
//                        SessionEntity sessionEntity = getSessionEntity(redisResIQ);
//                        if(sessionEntity != null) {
//                            sessionMap.put(sessionEntity.getSessionKey(), sessionEntity);
//                            needDb.add(sessionEntity);
//                            //将最新的session信息保存在DB中
//                            sessionDao.batchReplaceInto(needDb);
//                            triggerEvent(SessionEvent.RECENT_SESSION_LIST_UPDATE);
//                        }
//                    }
//                }
//
//
//                /**
//                 * 消息异常
//                 */
//                @Override
//                public void onFailed() {
//
//                }
//
//                /**
//                 * 消息超时
//                 */
//                @Override
//                public void onTimeout() {
//
//                }
//            });
//        }
//
//    }

    public SessionEntity getSessionEntity(RedisResIQ redisResIQ){
        SessionEntity sessionEntity = null;
        if(StringUtils.isBlank(redisResIQ.getContent())) {
            return sessionEntity;
        }
        try {
            sessionEntity = new SessionEntity();
            //需要改进，不能写死
            int msgType = DBConstant.MSG_TYPE_GROUP_TEXT;
            sessionEntity.setLatestMsgType(msgType);
            //需要改进，不能写死
            sessionEntity.setPeerType(DBConstant.SESSION_TYPE_GROUP);
            JSONObject jsonBody = new JSONObject(redisResIQ.getContent());
            String to = jsonBody.getString("to");
            String from = jsonBody.getString("from");
            String thread = jsonBody.getString("thread");
            //以后需要考虑消息类型
            String body = jsonBody.getString("body");
            int laststMsgId = SequenceNumberMaker.getInstance().makelocalUniqueMsgId(Long.valueOf(thread));
            sessionEntity.setPeerId(to);
            sessionEntity.buildSessionKey();
            sessionEntity.setTalkId(from);
            sessionEntity.setLatestMsgId(laststMsgId);
            sessionEntity.setLatestMsgData(body);
            sessionEntity.setUpdated(Long.valueOf(thread));
            sessionEntity.setCreated(Long.valueOf(thread));
            return sessionEntity;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public SessionEntity getSessionEntity(String content){
        SessionEntity sessionEntity = null;
        if(StringUtils.isBlank(content)) {
            return sessionEntity;
        }
        try {
            sessionEntity = new SessionEntity();
            //需要改进，不能写死
            int msgType = DBConstant.MSG_TYPE_GROUP_TEXT;
            sessionEntity.setLatestMsgType(msgType);
            //需要改进，不能写死
            sessionEntity.setPeerType(DBConstant.SESSION_TYPE_GROUP);
            JSONObject jsonBody = new JSONObject(content);
            String to = jsonBody.getString("to");
            String from = jsonBody.getString("from");
            String thread = jsonBody.getString("thread");
            //以后需要考虑消息类型
            String body = jsonBody.getString("body");
            String code = jsonBody.getString("code");
            int laststMsgId = SequenceNumberMaker.getInstance().makelocalUniqueMsgId(Long.valueOf(thread));
            sessionEntity.setPeerId(to);
            sessionEntity.buildSessionKey();
            sessionEntity.setTalkId(from);
            sessionEntity.setLatestMsgId(laststMsgId);
            if(com.juju.app.utils.StringUtils.isBlank(code)
                    || IMBaseDefine.MsgType.MSG_TEXT.code().equals(code)) {
                sessionEntity.setLatestMsgData(body);
            }
            //短语音
            else if(IMBaseDefine.MsgType.MSG_AUDIO.code().equals(code)) {
                sessionEntity.setLatestMsgData(DBConstant.DISPLAY_FOR_AUDIO);
            }
            //图片
            else if(IMBaseDefine.MsgType.MSG_IMAGE.code().equals(code)) {
                sessionEntity.setLatestMsgData(DBConstant.DISPLAY_FOR_IMAGE);
            }
            sessionEntity.setUpdated(Long.valueOf(thread));
            sessionEntity.setCreated(Long.valueOf(thread));
            return sessionEntity;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static void sort(List<RecentInfo> data) {
        Collections.sort(data, new Comparator<RecentInfo>() {
            public int compare(RecentInfo o1, RecentInfo o2) {
                Long a = o1.getUpdateTime();
                Long b = o2.getUpdateTime();

                String aName = o1.getName();
                String bName = o2.getName();

                boolean isTopA = o1.isTop();
                boolean isTopB = o2.isTop();

                if (isTopA == isTopB) {
                    // 升序
                    //return a.compareTo(b);
                    // 降序
                    if(a.compareTo(b) == 0) {
                        return aName.compareTo(bName);
                    } else {
                        return b.compareTo(a);
                    }
                } else {
                    if (isTopA) {
                        return -1;
                    } else {
                        return 1;
                    }
                }

            }
        });
    }

    /**
     * （统计未读消息条目时，能够返回最新的未读消息）供IMUnreadMsgManager调用
     * @param sessionEntity
     */
    public void updateSession4RedisQuery(SessionEntity  sessionEntity) {
        sessionMap.put(sessionEntity.getSessionKey(), sessionEntity);
        sessionDao.replaceInto(sessionEntity);
        triggerEvent(SessionEvent.RECENT_SESSION_LIST_UPDATE);
    }

    /**
     * 更新会话信息（创建群组，被邀请加入群组调用）
     * @param peerId
     * @param peerType
     * @param latestMsgType
     * @param latestMsgData
     * @param talkId
     * @param created
     */
    public void updateSessionEntity(String peerId, int peerType, int latestMsgType,
                                                String latestMsgData, String talkId, Long created) {
        SessionEntity sessionEntity = SessionEntity.build4Update(peerId, peerType,
                    latestMsgType, latestMsgData, talkId, created);
        SessionEntity dbSession = sessionMap.get(sessionEntity.getSessionKey());
        if(dbSession != null) {
            sessionEntity.setCreated(dbSession.getCreated());
        } else {
            sessionEntity.setCreated(sessionEntity.getUpdated());
        }
        sessionDao.replaceInto(sessionEntity);
        sessionMap.put(sessionEntity.getSessionKey(), sessionEntity);
    }


    public List<SessionEntity> findAll(){
        return sessionDao.findAll();
    }

    public boolean isSessionListReady() {
        return sessionListReady;
    }


    public void setSessionTop(String sessionKey, boolean isTop) {
        if (TextUtils.isEmpty(sessionKey)) {
            return;
        }
        Set<String> topList = SpfUtil.getStringSet(ctx,
                CfgDimension.SESSIONTOP.name(), null);
        Set<String> newList = new HashSet<>();
        if (topList != null && topList.size() > 0) {
            newList.addAll(topList);
        }
        if (isTop) {
            newList.add(sessionKey);
        } else {
            if (newList.contains(sessionKey)) {
                newList.remove(sessionKey);
            }
        }
        SpfUtil.putStringSet(ctx, CfgDimension.SESSIONTOP.name(), newList);
        EventBus.getDefault().post(SessionEvent.SET_SESSION_TOP);
    }

    // 获取全部置顶的session
//    public HashSet<String> getSessionTopList() {
//        Set<String> topList = SpfUtil.getStringSet(ctx,
//                CfgDimension.SESSIONTOP.name(), null);
//        if (null == topList) {
//            return null;
//        }
//        return (HashSet<String>) topList;
//    }
//
//
//    public boolean isTopSession(String sessionKey) {
//        HashSet<String> list =  getSessionTopList();
//        if (list != null && list.size() > 0 && list.contains(sessionKey)) {
//            return true;
//        }
//        return false;
//    }

    /**
     * 1. 勿扰
     * 2. 声音
     * 3. 自动
     * 4. 通知的方式 one session/ one message
     */
    public enum CfgDimension {
        NOTIFICATION,
        SOUND,
        VIBRATION,

        //置顶session 设定
        SESSIONTOP,
    }

    @Override
    protected void triggerEvent(Object paramObject) {
        if(paramObject instanceof SessionEvent) {
            SessionEvent event = (SessionEvent)paramObject;
            switch (event){
                case RECENT_SESSION_LIST_SUCCESS:
                    sessionListReady = true;
                    break;
            }
            EventBus.getDefault().postSticky(event);
        }
    }

    /**
     * 初始化DAO和服务(退出登录后或者第一次加载需要初始化)
     */
    public void initDaoAndService() {
        if(sessionDao == null) {
            sessionDao = new SessionDaoIml(ctx);
        }
        if(messageDao == null) {
            messageDao = new MessageDaoImpl(ctx);
        }
    }
}
