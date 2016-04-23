package com.juju.app.service.im.manager;

import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.event.UnreadEvent;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目名称：juju
 * 类描述：未读消息相关的处理，归属于messageEvent中
 * 可以理解为MessageManager的又一次拆分
 * 为session提供未读支持
 * DB 中不保存
 * 创建人：gm
 * 日期：2016/4/20 19:43
 * 版本：V1.0.0
 */
public class IMUnreadMsgManager extends IMManager {

    private Logger logger = Logger.getLogger(IMUnreadMsgManager.class);


    /**key=> sessionKey*/
    private ConcurrentHashMap<String, UnreadEntity> unreadMsgMap = new ConcurrentHashMap<>();

    private int totalUnreadCount = 0;

    private boolean unreadListReady = false;

    private static IMUnreadMsgManager inst = new IMUnreadMsgManager();

    private UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();

    public static IMUnreadMsgManager instance() {
        return inst;
    }

    @Override
    public void doOnStart() {

    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        unreadListReady = false;
        unreadMsgMap.clear();
    }


    //IMService调用
    public void add(MessageEntity msg) {
        //更新session list中的msg信息
        //更新未读消息计数
        if(msg == null){
            logger.d("unread#unreadMgr#add msg is null!");
            return;
        }
        // isFirst场景:出现一条未读消息，出现小红点，需要触发 [免打扰的情况下]
        boolean isFirst = false;
        logger.d("unread#unreadMgr#add unread msg:%s", msg);
        UnreadEntity unreadEntity;
        String loginId = userInfoBean.getmAccount();
        String sessionKey = msg.getSessionKey();
        boolean isSend = msg.isSend(loginId);
        if(isSend){
//            IMNotificationManager.instance().cancelSessionNotifications(sessionKey);
            return;
        }

        if(unreadMsgMap.containsKey(sessionKey)){
            unreadEntity = unreadMsgMap.get(sessionKey);
            // 判断最后一条msgId是否相同
            if(unreadEntity.getLaststMsgId() == msg.getMsgId()){
                return;
            }
            unreadEntity.setUnReadCnt(unreadEntity.getUnReadCnt()+1);
        }else{
            isFirst = true;
            unreadEntity = new UnreadEntity();
            unreadEntity.setUnReadCnt(1);
            unreadEntity.setPeerId(msg.getPeerId(isSend));
            unreadEntity.setSessionType(msg.getSessionType());
            unreadEntity.buildSessionKey();
        }

        unreadEntity.setLatestMsgData(msg.getMessageDisplay());
        unreadEntity.setLaststMsgId(msg.getMsgId());
        addIsForbidden(unreadEntity);

        /**放入manager 状态中*/
        unreadMsgMap.put(unreadEntity.getSessionKey(),unreadEntity);

        /**没有被屏蔽才会发送广播*/
        //广播未读消息
        if(!unreadEntity.isForbidden() || isFirst) {
            UnreadEvent unreadEvent = new UnreadEvent();
            unreadEvent.event = UnreadEvent.Event.UNREAD_MSG_RECEIVED;
            unreadEvent.entity = unreadEntity;
            triggerEvent(unreadEvent);
        }

    }


    /**
     * 回话是否已经被设定为屏蔽,暂时不需要
     * @param unreadEntity
     */
    private void addIsForbidden(UnreadEntity unreadEntity){
//        if(unreadEntity.getSessionType() == DBConstant.SESSION_TYPE_GROUP){
//            GroupEntity groupEntity= IMGroupManager.instance().findGroup(unreadEntity.getPeerId());
//            if(groupEntity !=null && groupEntity.getStatus() == DBConstant.GROUP_STATUS_SHIELD){
//                unreadEntity.setForbidden(true);
//            }
//        }
    }

    /**
     * 备注: 先获取最后一条消息
     * 1. 清除回话内的未读计数
     * 2. 发送最后一条msgId的已读确认
     * @param sessionKey
     */
    public void  readUnreadSession(String sessionKey){
        logger.d("unread#readUnreadSession# sessionKey:%s", sessionKey);
        if(unreadMsgMap.containsKey(sessionKey)){
            UnreadEntity entity = unreadMsgMap.remove(sessionKey);
            //回复已读信息，暂不支持
//            ackReadMsg(entity);
            triggerEvent(new UnreadEvent(UnreadEvent.Event.SESSION_READED_UNREAD_MSG));
        }
    }

    public ConcurrentHashMap<String, UnreadEntity> getUnreadMsgMap() {
        return unreadMsgMap;
    }

    public int getTotalUnreadCount() {
        int count = 0;
        for(UnreadEntity entity : unreadMsgMap.values()){
            if(!entity.isForbidden()){
                count  = count +  entity.getUnReadCnt();
            }
        }
        return count;
    }

    /**
     * 服务端主动发送已读通知(暂时用服务器接收消息取代)
     */
    public void onNotifyRead(MessageEntity msg){
        logger.d("chat#onNotifyRead");
        //现在的逻辑是msgId之后的 全部都是已读的
        // 不做复杂判断了，简单处理
        int msgId = msg.getMsgId();
        int sessionType = msg.getSessionType();
        String loginId = BaseApplication.getInstance().getUserInfoBean().getmAccount();
        boolean isSend = msg.isSend(loginId);
        String peerId = msg.getPeerId(isSend);

        String sessionKey = EntityChangeEngine.getSessionKey(peerId, sessionType);

        // 通知栏也要去除掉
        NotificationManager notifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifyMgr == null) {
            return;
        }
//        int notificationId = IMNotificationManager.instance().getSessionNotificationId(sessionKey);
//        notifyMgr.cancel(notificationId);

        UnreadEntity unreadSession =  findUnread(sessionKey);
        if(unreadSession != null && unreadSession.getLaststMsgId() <= msgId){
            // 清空会话session
            logger.d("chat#onNotifyRead# unreadSession onLoginOut");
            readUnreadSession(sessionKey);
        }
    }

    public UnreadEntity findUnread(String sessionKey){
        logger.d("unread#findUnread# buddyId:%s", sessionKey);
        if(TextUtils.isEmpty(sessionKey) || unreadMsgMap.size()<=0){
            logger.i("unread#findUnread# no unread info");
            return null;
        }
        if(unreadMsgMap.containsKey(sessionKey)){
            return unreadMsgMap.get(sessionKey);
        }
        return null;
    }
}
