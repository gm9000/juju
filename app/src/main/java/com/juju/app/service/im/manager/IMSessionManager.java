package com.juju.app.service.im.manager;

import android.text.TextUtils;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.MessageDao;
import com.juju.app.biz.impl.MessageDaoImpl;
import com.juju.app.biz.impl.SessionDaoIml;
import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.RecentInfo;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.event.SessionEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.helper.chat.SequenceNumberMaker;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.iq.RedisResIQ;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.Logger;
import com.juju.app.utils.SpfUtil;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private final String TAG = getClass().getSimpleName();

    private volatile static IMSessionManager inst;

    // key = sessionKey -->  sessionType_peerId
    private Map<String, SessionEntity> sessionMap = new ConcurrentHashMap<>();

    //SessionManager 状态字段
    private boolean sessionListReady = false;

    private IMLoginManager imLoginManager = IMLoginManager.instance();

    private DaoSupport sessionDao;

    private MessageDao messageDao;

    private UserInfoBean userInfoBean;

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
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        sessionDao = new SessionDaoIml(ctx);
        messageDao = new MessageDaoImpl(ctx);
    }

    public void onNormalLoginOk() {
        onLocalLoginOk();
        onLocalNetOk();
    }

    public void onLocalLoginOk(){
        logger.i("session#loadFromDb");
        List<SessionEntity>  sessionInfoList = sessionDao.findAll4Order("updated:desc");
        if(sessionInfoList != null) {
            for(SessionEntity sessionInfo:sessionInfoList){
                sessionMap.put(sessionInfo.getSessionKey(), sessionInfo);
            }
        }
        sessionListReady = true;
        triggerEvent(SessionEvent.RECENT_SESSION_LIST_SUCCESS);
    }

    public void onLocalNetOk(){
        long latestUpdateTime = messageDao.getSessionLastTime();
        logger.d("session#更新时间:%d", latestUpdateTime);

        //群组需要另外启服务
        List<String> chatRoomIds = new ArrayList<String>();
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        chatRoomIds.add(userInfoBean.getmRoomName() +
                "@" + userInfoBean.getmMucServiceName() + "." + userInfoBean.getmServiceName());

        //latestUpdateTime>1 本地数据库存储了session数据
        if(latestUpdateTime > 1) {
//            reqGetRecentContacts(latestUpdateTime, chatRoomIds);
        }
    }



    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        sessionListReady = false;
        sessionMap.clear();
    }

    /**
     * 1.自己发送消息
     * 2.收到消息
     * @param msg
     */
    public void updateSession(MessageEntity msg) {
        logger.d("recent#updateSession msg:%s", msg);
        if (msg == null) {
            logger.d("recent#updateSession is end,cause by msg is null")
            ;
            return;
        }
        String loginId = BaseApplication.getInstance().getUserInfoBean().getmAccount();
        boolean isSend = msg.isSend(loginId);
        // 因为多端同步的问题
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
        SpfUtil.put(ctx, sessionEntity.getSessionKey(),
                sessionEntity.getUpdated());
        triggerEvent(SessionEvent.RECENT_SESSION_LIST_UPDATE);
    }

    public SessionEntity findSession(String sessionKey){
        if(sessionMap.size()<=0 || TextUtils.isEmpty(sessionKey)){return null;}
        if(sessionMap.containsKey(sessionKey)){
            return sessionMap.get(sessionKey);
        }
        return null;
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
//        HashSet<String> topList = ConfigurationSp.instance(ctx,loginId).getSessionTopList();

        //是否考虑每次都遍历群组
        for(GroupEntity groupEntity : groupList) {
            String sessionKey = groupEntity.getSessionKey();
            UnreadEntity unreadEntity = unreadMsgMap.get(sessionKey);
            SessionEntity recentSession = sessionMap.get(sessionKey);
            RecentInfo recentInfo = new RecentInfo(recentSession, groupEntity, unreadEntity);
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
    private void reqGetRecentContacts(long latestUpdateTime,  List<String> chatRoomIds) {
        logger.i("session#reqGetRecentContacts");

        for(String chatRoom : chatRoomIds) {
            String uuid = UUID.randomUUID().toString();

            socketService.findHisMessages("zrevrangebyscore", chatRoom, String.valueOf(latestUpdateTime+1), "",
                    uuid, 0, Constants.MSG_CNT_PER_PAGE, new XMPPServiceCallbackImpl(0) {

                /**
                 * 新消息
                 *
                 * @param t
                 */
                @Override
                public void onSuccess(Object t) {
                    if(t instanceof RedisResIQ) {
                        //需要放在循环体外(消息merge)
                        ArrayList<SessionEntity> needDb = new ArrayList<>();
                        RedisResIQ redisResIQ = (RedisResIQ)t;
                        SessionEntity sessionEntity = getSessionEntity(redisResIQ);
                        if(sessionEntity != null) {
                            sessionMap.put(sessionEntity.getSessionKey(), sessionEntity);
                            needDb.add(sessionEntity);
                            //将最新的session信息保存在DB中
                            sessionDao.batchReplaceInto(needDb);
                            triggerEvent(SessionEvent.RECENT_SESSION_LIST_UPDATE);
                        }
                    }
                }


                /**
                 * 消息异常
                 */
                @Override
                public void onFailed() {

                }

                /**
                 * 消息超时
                 */
                @Override
                public void onTimeout() {

                }
            });
        }

    }

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


    private static void sort(List<RecentInfo> data) {
        Collections.sort(data, new Comparator<RecentInfo>() {
            public int compare(RecentInfo o1, RecentInfo o2) {
                Long a = o1.getUpdateTime();
                Long b = o2.getUpdateTime();

                boolean isTopA = o1.isTop();
                boolean isTopB = o2.isTop();

                if (isTopA == isTopB) {
                    // 升序
                    //return a.compareTo(b);
                    // 降序
                    return b.compareTo(a);
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
    public void saveSession(SessionEntity  sessionEntity) {
        sessionMap.put(sessionEntity.getSessionKey(), sessionEntity);
        sessionDao.replaceInto(sessionEntity);
        triggerEvent(SessionEvent.RECENT_SESSION_LIST_UPDATE);
    }

    public List<SessionEntity> findAll(){
        return sessionDao.findAll();
    }

    public boolean isSessionListReady() {
        return sessionListReady;
    }
}
