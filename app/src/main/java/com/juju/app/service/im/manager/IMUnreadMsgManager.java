package com.juju.app.service.im.manager;

import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.MessageDao;
import com.juju.app.biz.impl.MessageDaoImpl;
import com.juju.app.biz.impl.SessionDaoIml;
import com.juju.app.biz.impl.UnreadDaoImpl;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.event.JoinChatRoomEvent;
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
import com.juju.app.utils.ThreadPoolUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 项目名称：juju
 * 类描述：未读消息相关的处理，归属于messageEvent中
 * 可以理解为MessageManager的又一次拆分
 * 为session提供未读支持
 * 创建人：gm
 * 日期：2016/4/20 19:43
 * 版本：V1.0.0
 */
public class IMUnreadMsgManager extends IMManager {


    private volatile static IMUnreadMsgManager inst = null;

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

    private Logger logger = Logger.getLogger(IMUnreadMsgManager.class);

    //未读消息查询超时时间
    private final static int UNREAD_REQ_TIMEOUT = 10;

    /**key=> sessionKey*/
    private ConcurrentHashMap<String, UnreadEntity> unreadMsgMap = new ConcurrentHashMap<>();

    private int totalUnreadCount = 0;

    private boolean unreadListReady = false;


    private UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();

    private MessageDao messageDao;

    private DaoSupport unReadDao;

    List<String> chatRoomIds = new ArrayList<>();


    public ConcurrentHashMap<String, UnreadEntity> getUnreadMsgMap() {
        return unreadMsgMap;
    }


    @Override
    public void doOnStart() {
//        messageDao = new MessageDaoImpl(ctx);
//        unReadDao = new UnreadDaoImpl(ctx);
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        chatRoomIds.add(userInfoBean.getmRoomName() +
                "@" + userInfoBean.getmMucServiceName() + "." + userInfoBean.getmServiceName());
    }

    public void onNormalLoginOk(){
        onLocalLoginOk();
        onLocalNetOk();
    }

    public void onLocalLoginOk() {
        if (!EventBus.getDefault().isRegistered(inst)) {
            EventBus.getDefault().register(inst);
        }
        unreadMsgMap.clear();
        //读取本地未读消息
        List<UnreadEntity> unreadEntityList = unReadDao.findAll();
        for(UnreadEntity entity : unreadEntityList) {
            unreadMsgMap.put(entity.getSessionKey(), entity);
        }
        unreadListReady = true;
        triggerEvent(new UnreadEvent(UnreadEvent.Event.UNREAD_MSG_LIST_OK));
    }

    /**
     * 未读消息由用户加入群组后 通知接收,初始化需要清理
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
        EventBus.getDefault().unregister(inst);
        unreadListReady = false;
        unreadMsgMap.clear();
        messageDao = null;
        unReadDao = null;
    }


    /**
     * 未读消息添加入口
     * IMService调用(群组列表界面显示未读消息)
     */
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
            IMNotificationManager.instance().cancelSessionNotifications(sessionKey);
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
        }
        unreadEntity.setFromId(msg.getFromId());
        unreadEntity.setLatestMsgData(msg.getMessageDisplay());
        unreadEntity.setLaststMsgId(msg.getMsgId());
        unreadEntity.setUpdated(msg.getUpdated());
        unReadDao.replaceInto(unreadEntity);
//        addIsForbidden(unreadEntity);

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
     * 会话是否已经被设定为屏蔽
     * @param unreadEntity
     */
    private void addIsForbidden(UnreadEntity unreadEntity){
        if(unreadEntity.getSessionType() == DBConstant.SESSION_TYPE_GROUP){
            GroupEntity groupEntity= IMGroupManager.instance().findGroup(unreadEntity.getPeerId());
            if(groupEntity !=null && groupEntity.getStatus() == DBConstant.GROUP_STATUS_SHIELD){
                unreadEntity.setForbidden(true);
            }
        }
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
     * IMGroupManager登陆成功后，需要获取未读消息
     * @param joinChatRoomEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent4JoinChatRoom(final JoinChatRoomEvent joinChatRoomEvent) {
        switch (joinChatRoomEvent.event){
            case JOIN_OK_4_UNREAD_MSG_REQ:
                ThreadPoolUtil.instance().executeImTask(new Runnable() {
                    @Override
                    public void run() {
                        reqUnreadMsgContactList(joinChatRoomEvent.joinedGroupPeerIds, UNREAD_REQ_TIMEOUT);
                    }
                });
                break;
        }
    }








//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void reqUnreadMsgContactList(GroupEntity groupEntity) {
//        final List<String> chatRoomIds = new ArrayList<String>();
////        chatRoomIds.add(groupEntity.getPeerId());
//        reqUnreadMsgContactList(groupEntity.getPeerId());
//    }


    /**
     * 初始化IMUnreadMsgManager后,请求未读消息列表
     */
    private void reqUnreadMsgContactList(List<String> groupPeerIds, int timeOut) {
        logger.i("unread#reqUnreadMsgContactList");

        if(groupPeerIds != null && groupPeerIds.size() >0) {
            final List<String> newGroupPeerIds = new ArrayList<>(groupPeerIds);
            final CountDownLatch countDownLatch = new CountDownLatch(groupPeerIds.size());
            for(final String peerId : newGroupPeerIds) {
                ThreadPoolUtil.instance().executeImTask(new Runnable() {
                    @Override
                    public void run() {
                        reqUnreadMsgContact(peerId, countDownLatch);
                    }
                });
            }
//            ThreadPoolUtil.instance().executeImTask(new Runnable() {
//                @Override
//                public void run() {
//                    for(final String peerId : newGroupPeerIds) {
//                        reqUnreadMsgContact(peerId, countDownLatch);
//                    }
//                }
//            });
            try {
                //消息收取时间不能超过1分钟 (不能在MAIN线程中执行)
                countDownLatch.await(timeOut, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //通知刷新未读消息(GroupChatFragment)
        unreadListReady = true;
        triggerEvent(new UnreadEvent(UnreadEvent.Event.UNREAD_MSG_LIST_OK));
    }

    //获取群组未读消息
    private void reqUnreadMsgContact(final String chatRoomId, final CountDownLatch countDownLatch) {
        final String chatRoom = DBConstant.SESSION_TYPE_GROUP + "_" + chatRoomId;
        final UnreadEntity dbUnreadEntity = unreadMsgMap.get(chatRoom);
        if(dbUnreadEntity == null) {

        }
        long time = 1l;

        //未读消息
        if(dbUnreadEntity != null
                && dbUnreadEntity.getUpdated() >0) {
            time = dbUnreadEntity.getUpdated();
            time++;
        }
        //消息已读，需要找APP退出前最后一条消息的时间
        else {
            time = messageDao.getSessionLastTime();
            time++;
        }
        //没有查询条件，返回
        if(time == 1 || time == 2) {
            countDownLatch.countDown();
            return;
        }

        final long updated = time;
        //更新最近会话
        String uuid = UUID.randomUUID().toString();
        socketService.countMessage(chatRoomId, String.valueOf(time), "", uuid,
                new XMPPServiceCallbackImpl() {

                    @Override
                    public void onSuccess(Object t) {
                        RedisResIQ redisResIQ = (RedisResIQ) t;
                        UnreadEntity unreadEntity;
                        try {
                            unreadEntity = getUnreadEntity(redisResIQ, dbUnreadEntity);
                            if(unreadEntity != null) {
                                unreadMsgMap.put(chatRoom, unreadEntity);
                                //保存或更新未读消息
                                unReadDao.replaceInto(unreadEntity);
                                //需进一步优化
                                if(unreadEntity.getUnReadCnt() >0) {
                                    MergeMessageThread mergeMessageThread = new MergeMessageThread
                                            (chatRoomId, updated, unreadEntity.getUnReadCnt(), socketService);
                                    Thread thread = new Thread(mergeMessageThread,
                                            MergeMessageThread.class.getSimpleName());
                                    ThreadPoolUtil.instance().executeImTask(thread);
                                }
                            }
                        } catch (JSONException e) {
                            logger.error(e);
                        }
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFailed() {
                        logger.i("unread#reqUnreadMsgContactList is failed");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onTimeout() {
                        logger.i("unread#reqUnreadMsgContactList is timeout");
                        countDownLatch.countDown();
                    }
                });
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
     * @param dbUnreadEntity
     * @return
     */
    private UnreadEntity getUnreadEntity(RedisResIQ redisResIQ, UnreadEntity dbUnreadEntity) throws JSONException {
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
                //消息服务+本地数据库
                int unReadCnt = Integer.parseInt(count)
                        + (dbUnreadEntity == null ? 0 : dbUnreadEntity.getUnReadCnt());

                unreadEntity.setUnReadCnt(unReadCnt);
                unreadEntity.setUpdated(Long.valueOf(thread));
                unreadEntity.buildSessionKey();

                //更新session (IMMessageManager不需要重复查询)
                SessionEntity sessionEntity = IMSessionManager.instance().getSessionEntity(bodyStr);
                IMSessionManager.instance().saveSession(sessionEntity);
            }
        }
        if(unreadEntity == null && dbUnreadEntity != null) {
            unreadEntity = dbUnreadEntity;
        }
        return unreadEntity;
    }



    public List<UnreadEntity> findAll() {
        return unReadDao.findAll();
    }


    public boolean isUnreadListReady() {
        return unreadListReady;
    }



    /**设定未读会话为屏蔽会话 仅限于群组 todo*/
    public void setForbidden(String sessionKey,boolean isFor){
        UnreadEntity unreadEntity =  unreadMsgMap.get(sessionKey);
        if(unreadEntity !=null){
            unreadEntity.setForbidden(isFor);
        }
    }

    /**
     * 初始化DAO和服务(退出登录后或者第一次加载需要初始化)
     */
    public void initDaoAndService() {
        if(messageDao == null) {
            messageDao = new MessageDaoImpl(ctx);
        }
        if(unReadDao == null) {
            unReadDao = new UnreadDaoImpl(ctx);
        }
    }
}
