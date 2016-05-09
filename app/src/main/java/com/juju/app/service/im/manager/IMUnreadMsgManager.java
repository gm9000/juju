package com.juju.app.service.im.manager;

import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.MessageDao;
import com.juju.app.biz.impl.MessageDaoImpl;
import com.juju.app.biz.impl.UnreadDaoImpl;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.event.UnreadEvent;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.chat.EntityChangeEngine;
import com.juju.app.helper.chat.SequenceNumberMaker;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.iq.RedisResIQ;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.thread.MergeMessageThread;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.Logger;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.StringUtils;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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

    private volatile static IMUnreadMsgManager inst = null;

    private UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();

    private MessageDao messageDao;

    private DaoSupport unReadDao;

    List<String> chatRoomIds = new ArrayList<String>();

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMUnreadMsgManager instance() {
        if(inst == null) {
            synchronized (IMUnreadMsgManager.class) {
                if(inst == null) {
                    inst = new IMUnreadMsgManager();
                }
            }
        }
        return inst;
    }

    public ConcurrentHashMap<String, UnreadEntity> getUnreadMsgMap() {
        return unreadMsgMap;
    }


    @Override
    public void doOnStart() {
        messageDao = new MessageDaoImpl(ctx);
        unReadDao = new UnreadDaoImpl(ctx);
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        chatRoomIds.add(userInfoBean.getmRoomName() +
                "@" + userInfoBean.getmMucServiceName() + "." + userInfoBean.getmServiceName());
    }

    public void onNormalLoginOk(){
        unreadMsgMap.clear();
        //登录聊天室
        joinChatRooms(chatRoomIds);
        //获取未读消息
        reqUnreadMsgContactList(chatRoomIds);

//        reqMessageList(chatRoomIds);

    }

    public void onLocalNetOk(){
        unreadMsgMap.clear();
        reqUnreadMsgContactList(chatRoomIds);
//        reqMessageList(chatRoomIds);

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


    //IMService调用(群组列表界面显示未读消息)
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
            //存放更新时间
            unreadEntity.setCreated(msg.getUpdated());
            //持久化数据
            unReadDao.saveOrUpdate(unreadEntity);
        }
        unreadEntity.setLatestMsgData(msg.getMessageDisplay());
        unreadEntity.setLaststMsgId(msg.getMsgId());

        addIsForbidden(unreadEntity);

        /**放入manager 状态中*/
        unreadMsgMap.put(unreadEntity.getSessionKey(), unreadEntity);


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
     * 会话是否已经被设定为屏蔽,暂时不需要
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
     * 1. 清除会话内的未读计数
     * @param sessionKey
     */
    public void  readUnreadSession(String sessionKey){
        logger.d("unread#readUnreadSession# sessionKey:%s", sessionKey);
        if(unreadMsgMap.containsKey(sessionKey)){
            UnreadEntity entity = unreadMsgMap.remove(sessionKey);
            unReadDao.deleteById(sessionKey);

            triggerEvent(new UnreadEvent(UnreadEvent.Event.SESSION_READED_UNREAD_MSG));
        }
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


    /**
     * 加入聊天室，放在此处欠妥，需要调整
     * @param chatRoomIds
     */
    private void joinChatRooms(List<String> chatRoomIds) {
        logger.i("unread#joinChatRooms");
        try {
            for(String chatRoomId : chatRoomIds) {
                String chatRoom = DBConstant.SESSION_TYPE_GROUP + "_" + chatRoomId;
                long time = (long) SpfUtil.get(ctx, chatRoom, System.currentTimeMillis()-24*60*60*1000l);
                socketService.joinChatRoom(chatRoomId, time);
            }
        } catch (JUJUXMPPException e) {
            logger.error(e);
        } catch (XMPPException e) {
            logger.error(e);
        } catch (SmackException.NotConnectedException e) {
            logger.error(e);
        } catch (SmackException.NoResponseException e) {
            logger.error(e);
        }
    }


    /**
     * 初始化IMUnreadMsgManager后,请求未读消息列表
     */
    private void reqUnreadMsgContactList(List<String> chatRoomIds) {
        logger.i("unread#reqUnreadMsgContactList");
        //需要优化 (考虑多线程)
        for(final String chatRoomId : chatRoomIds) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String chatRoom = DBConstant.SESSION_TYPE_GROUP + "_" + chatRoomId;
                    final UnreadEntity dbUnreadEntity = (UnreadEntity) unReadDao.findById(chatRoom);

                    long time = 1l;
                    //未读消息
                    if(dbUnreadEntity != null && dbUnreadEntity.getCreated() >0) {
                        time = dbUnreadEntity.getCreated();
                    }
                    //消息已读，需要找APP退出前最后一条消息的时间
                    else {
                        time = messageDao.getSessionLastTime();
                        time++;
                    }
                    if(time == 1 || time == 2)
                        return;

                    final long created = time;
                    //更新最近会话
                    String uuid = UUID.randomUUID().toString();
                    socketService.countMessage(chatRoomId, String.valueOf(time), "", uuid,
                            new XMPPServiceCallbackImpl() {

                                @Override
                                public void onSuccess(Object t) {
                                    RedisResIQ redisResIQ = (RedisResIQ) t;
                                    UnreadEntity unreadEntity;
                                    try {
                                        unreadEntity = getUnreadEntity(redisResIQ);
                                        if (unreadEntity != null) {
                                            unreadEntity.setCreated(created);
                                            unreadMsgMap.put(chatRoom, unreadEntity);
                                            //保存或更新未读消息
                                            unReadDao.replaceInto(unreadEntity);
                                            //需进一步优化
                                            if(unreadEntity.getUnReadCnt() >0) {
                                                MergeMessageThread mergeMessageThread = new MergeMessageThread
                                                        (chatRoomId, created, unreadEntity.getUnReadCnt(), socketService);
                                                new Thread(mergeMessageThread, MergeMessageThread.class.getSimpleName()).start();
                                            }
                                        }
                                    } catch (JSONException e) {
                                        logger.error(e);
                                    }
                                    //通知刷新未读消息
                                    triggerEvent(new UnreadEvent(UnreadEvent.Event.UNREAD_MSG_LIST_OK));
                                }

                                @Override
                                public void onFailed() {
                                    logger.i("unread#reqUnreadMsgContactList is failed");
                                }

                                @Override
                                public void onTimeout() {
                                    logger.i("unread#reqUnreadMsgContactList is timeout");
                                }
                            });
                }
            }).start();


        }
    }

    /**
     * 初始化IMUnreadMsgManager后，merge本地数据库
     * @param chatRoomIds
     */
//    private void reqMessageList(List<String> chatRoomIds) {
//        logger.i("unread#reqMessageList");
//
//        //需要优化 (考虑多线程)
//        for(String chatRoomId : chatRoomIds) {
//            MergeMessageThread mergeMessageThread = new MergeMessageThread(chatRoomId,
//                    messageDao,  unReadDao, socketService);
//            new Thread(mergeMessageThread).start();
//        }
//    }




    /**
     * 转换类
     * @param redisResIQ
     * @return
     */
    private UnreadEntity getUnreadEntity(RedisResIQ redisResIQ) throws JSONException {
        UnreadEntity unreadEntity = null;
        if(StringUtils.isNotBlank(redisResIQ.getContent())) {
            JSONObject jsonRoot = new JSONObject(redisResIQ.getContent());
            String count = jsonRoot.getString("count");
            String bodyStr = jsonRoot.getString("body");
            if(Integer.parseInt(count) > 0 && StringUtils.isNotBlank(bodyStr)) {
                unreadEntity = new UnreadEntity();
                unreadEntity.setSessionType(DBConstant.SESSION_TYPE_GROUP);
                JSONObject jsonBody = new JSONObject(bodyStr);
                String to = jsonBody.getString("to");
                String from = jsonBody.getString("from");
                String thread = jsonBody.getString("thread");
                String body = jsonBody.getString("body");
                int laststMsgId = SequenceNumberMaker.getInstance()
                        .makelocalUniqueMsgId(Long.valueOf(thread));
                unreadEntity.setLatestMsgData(body);
                unreadEntity.setPeerId(to);
                unreadEntity.setLaststMsgId(laststMsgId);
                unreadEntity.setUnReadCnt(Integer.parseInt(count));
                unreadEntity.buildSessionKey();

                //更新session (IMMessageManager不需要重复查询)
                SessionEntity sessionEntity = IMSessionManager.instance().getSessionEntity(bodyStr);
                IMSessionManager.instance().saveSession(sessionEntity);
            }
        }
        return unreadEntity;
    }



    public List<UnreadEntity> findAll() {
        return unReadDao.findAll();
    }



}
