package com.juju.app.service.im.manager;

import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.SessionDaoIml;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.RecentInfo;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.event.SessionEvent;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private static IMSessionManager inst;

    // key = sessionKey -->  sessionType_peerId
    private Map<String, SessionEntity> sessionMap = new ConcurrentHashMap<>();

    //SessionManager 状态字段
    private boolean sessionListReady = false;

    private IMLoginManager imLoginManager = IMLoginManager.instance();

    private DaoSupport sessionDao;



    public static IMSessionManager instance() {
        synchronized (IMSessionManager.class) {
            if (inst == null) {
                inst = new IMSessionManager();
            }
            return inst;
        }
    }

    @Override
    public void doOnStart() {
        sessionDao = new SessionDaoIml(ctx);
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
            logger.d("recent#updateSession is end,cause by msg is null");
            return;
        }
        String loginId = imLoginManager.getUserInfoBean().getmAccount();
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
        sessionDao.batchSaveOrUpdate(needDb);
        sessionMap.put(sessionEntity.getSessionKey(), sessionEntity);
        triggerEvent(SessionEvent.RECENT_SESSION_LIST_UPDATE);
    }

    public DaoSupport getSessionDao() {
        return sessionDao;
    }


    // 获取最近联系人列表，RecentInfo 是sessionEntity unreadEntity user/group 等等实体的封装
    // todo every time it has to sort, kind of inefficient, change it
    public List<RecentInfo> getRecentListInfo(){
        /**整理topList*/
        List<RecentInfo> recentSessionList = new ArrayList<>();
        String loginId = BaseApplication.getInstance().getUserInfoBean().getmAccount();

        List<SessionEntity> sessionList = getRecentSessionList();
//        Map<Integer, UserEntity> userMap = IMContactManager.instance().getUserMap();
        Map<String, UnreadEntity> unreadMsgMap = IMUnreadMsgManager.instance().getUnreadMsgMap();
//        Map<Integer, GroupEntity> groupEntityMap = IMGroupManager.instance().getGroupMap();
//        HashSet<String> topList = ConfigurationSp.instance(ctx,loginId).getSessionTopList();


        for(SessionEntity recentSession:sessionList){

            int sessionType = recentSession.getPeerType();
            String peerId = recentSession.getPeerId();
            String sessionKey = recentSession.getSessionKey();

            UnreadEntity unreadEntity = unreadMsgMap.get(sessionKey);
            if(sessionType == DBConstant.SESSION_TYPE_GROUP){
//                GroupEntity groupEntity = groupEntityMap.get(peerId);
                RecentInfo recentInfo = new RecentInfo(recentSession, null, unreadEntity);
//                if(topList !=null && topList.contains(sessionKey)){
//                    recentInfo.setTop(true);
//                }

                //谁说的这条信息，只有群组需要，例如 【XXX:您好】
                //不做判断
//                String lastFromId = recentSession.getTalkId();
//                UserEntity talkUser = userMap.get(lastFromId);
//                // 用户已经不存在了
//                if(talkUser != null){
//                    String  oriContent =  recentInfo.getLatestMsgData();
//                    String  finalContent = talkUser.getMainName() + ": "+oriContent;
//                    recentInfo.setLatestMsgData(finalContent);
//                }
                recentSessionList.add(recentInfo);
            }
        }
        sort(recentSessionList);
        return recentSessionList;
    }

    public List<SessionEntity> getRecentSessionList() {
        List<SessionEntity> recentInfoList = new ArrayList<>(sessionMap.values());
        return recentInfoList;
    }

    private static void sort(List<RecentInfo> data) {
        Collections.sort(data, new Comparator<RecentInfo>() {
            public int compare(RecentInfo o1, RecentInfo o2) {
                Integer a = o1.getUpdateTime();
                Integer b = o2.getUpdateTime();

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
}
