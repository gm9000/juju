package com.juju.app.service.im.manager;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.MessageDao;
import com.juju.app.biz.impl.MessageDaoImpl;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.AudioMessage;
import com.juju.app.entity.chat.ImageMessage;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.TextMessage;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.event.MessageEvent;
import com.juju.app.event.PriorityEvent;
import com.juju.app.event.RefreshHistoryMsgEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.MessageConstant;
import com.juju.app.helper.chat.SequenceNumberMaker;
import com.juju.app.service.im.LoadImageService;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.iq.RedisResIQ;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;

import org.apache.commons.lang.math.NumberUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 项目名称：juju
 * 类描述：消息管理服务
 * 创建人：gm
 * 日期：2016/3/22 09:45
 * 版本：V1.0.0
 */
public class IMMessageManager extends IMManager {

    private Logger logger = Logger.getLogger(IMMessageManager.class);

    private final String TAG = getClass().getSimpleName();

    private volatile static IMMessageManager inst;

    private IMSessionManager sessionManager = IMSessionManager.instance();

    private DaoSupport messageDao;

    private MessageDaoImpl publicMessageDao;


    //双重验证+volatile（禁止JMM重排序）保证线程安全
    public static IMMessageManager instance() {
        if(inst == null) {
            synchronized (IMMessageManager.class) {
                if (inst == null) {
                    inst = new IMMessageManager();
                }
            }
        }
        return inst;
    }

    public MessageDaoImpl getPublicMessageDao() {
        return publicMessageDao;
    }

    public DaoSupport getMessageDao() {
        return messageDao;
    }

    @Override
    public void doOnStart() {
//        messageDao = new MessageDaoImpl(ctx);
//        publicMessageDao = (MessageDaoImpl) messageDao;
    }

    public void onLoginSuccess() {
        if (!EventBus.getDefault().isRegistered(inst)) {
            EventBus.getDefault().register(inst);
        }
    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        EventBus.getDefault().unregister(inst);
        socketService = null;
        messageDao = null;
        publicMessageDao = null;
    }


    public void sendText(final MessageEntity msgEntity) {
        if (isAuthenticated()) {
            String uuid = UUID.randomUUID().toString();
            msgEntity.setStatus(MessageConstant.MSG_SENDING);
            msgEntity.setId(uuid);
            //插入本地数据库
            final MessageEntity dbMessage = msgEntity.clone();
            messageDao.saveOrUpdate(dbMessage);
            try {
                socketService.sendMessage(msgEntity.getFromId(), msgEntity.getToId(),
                        msgEntity.getContent(), uuid, null, new XMPPServiceCallbackImpl() {
                            @Override
                            public void onSuccess(Object t) {
                                //回复时间
                                if (t instanceof XMPPServiceImpl.ReplayMessageTime) {
                                    XMPPServiceImpl.ReplayMessageTime messageTime =
                                            (XMPPServiceImpl.ReplayMessageTime) t;
                                    String id = messageTime.getId();
                                    String time = messageTime.getTime();
                                    if (dbMessage != null) {
                                        dbMessage.setStatus(MessageConstant.MSG_SUCCESS);
                                        //通知消息
                                        msgEntity.setStatus(MessageConstant.MSG_SUCCESS);
                                        if (NumberUtils.isNumber(time)) {
                                            int msgId = SequenceNumberMaker.getInstance()
                                                    .makelocalUniqueMsgId(Long.parseLong(time));
                                            //更新msgId
                                            dbMessage.setMsgId(msgId);
                                            dbMessage.setCreated(Long.parseLong(time));
                                            dbMessage.setUpdated(Long.parseLong(time));
                                        }
                                        //更新会话
                                        sessionManager.updateSession(dbMessage, true);
                                        triggerEvent(new MessageEvent(MessageEvent.Event
                                                .ACK_SEND_MESSAGE_OK, msgEntity));
                                        messageDao.saveOrUpdate(dbMessage);
                                    }
                                }
                            }

                            @Override
                            public void onFailed() {
                                //消息发送失败
                                dbMessage.setStatus(MessageConstant.MSG_FAILURE);
                                messageDao.saveOrUpdate(dbMessage);
                                msgEntity.setStatus(MessageConstant.MSG_FAILURE);
                                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, msgEntity));
                            }

                            @Override
                            public void onTimeout() {
                                //消息发送超时
                                dbMessage.setStatus(MessageConstant.MSG_FAILURE);
                                messageDao.saveOrUpdate(dbMessage);
                                msgEntity.setStatus(MessageConstant.MSG_FAILURE);
                                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, msgEntity));
                            }
                        });
            } catch (SmackException.NotConnectedException e) {
                Log.e(TAG, "sendText:", e);
                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, msgEntity));
            }
        } else {
            //TODO ChatActivity提示未验证通过

        }
    }

    // 拉取历史消息 {from ChatActivity}
    public List<MessageEntity> loadHistoryMsg(int pullTimes, String sessionKey,
                                              PeerEntity peerEntity) {
        int lastMsgId = 99999999;
        long lastCreateTime = 1761055138000l;
        int count = Constants.MSG_CNT_PER_PAGE;
        SessionEntity sessionEntity = IMSessionManager.instance().findSession(sessionKey);
        if (sessionEntity != null) {
            lastMsgId = sessionEntity.getLatestMsgId();
            logger.i("#loadHistoryMsg# sessionEntity -> lastMsgId:%d", lastMsgId);
            // 这个地方设定有问题，先使用最大的时间,session的update设定存在问题
//            lastCreateTime = sessionEntity.getUpdated();
        }

        if(lastMsgId <1 || TextUtils.isEmpty(sessionKey)){
            return Collections.emptyList();
        }

        if (count > lastMsgId) {
            count = lastMsgId;
        }

        List<MessageEntity> msgList = doLoadHistoryMsg(
                pullTimes, peerEntity.getPeerId(),
                peerEntity.getType(),
                sessionKey, lastMsgId, lastCreateTime, count);

        return msgList;
    }

    // 根据次数有点粗暴
    public List<MessageEntity> loadHistoryMsg(MessageEntity entity, int pullTimes) {
        logger.d("IMMessageActivity#LoadHistoryMsg");
        // 在滑动的过程中请求，msgId请求下一条的
        int reqLastMsgId = entity.getMsgId() - 1;
//        int loginId = IMLoginManager.instance().getLoginId();
        long reqLastCreateTime = entity.getCreated();
        String chatKey = entity.getSessionKey();
        int cnt = Constants.MSG_CNT_PER_PAGE;
        List<MessageEntity> msgList = doLoadHistoryMsg(pullTimes,
                "0", entity.getSessionType(), chatKey, reqLastMsgId, reqLastCreateTime, cnt);
        return msgList;
    }

    /**
     * 从DB中请求信息
     * 1. 从最近会话点击进入，拉取消息
     * 2. 在消息页面下拉刷新
     *
     * @param pullTimes
     * @param peerId
     * @param peerType
     * @param sessionKey
     * @param lastMsgId
     * @param lastCreateTime
     * @param count
     * @return
     */
    private List<MessageEntity> doLoadHistoryMsg(int pullTimes, final String peerId,
                                                 final int peerType, final String sessionKey,
                                                 int lastMsgId, long lastCreateTime, int count) {
        if(lastMsgId <1 || TextUtils.isEmpty(sessionKey)){
            return Collections.emptyList();
        }
        if (count > lastMsgId) {
            count = lastMsgId;
        }
        // 降序结果输出desc
        List<MessageEntity> listMsg = publicMessageDao.findHistoryMsgs(sessionKey, lastMsgId, lastCreateTime, count);
        // asyn task refresh
        int resSize = listMsg == null ? 0 : listMsg.size();
        logger.d("LoadHistoryMsg return size is %d", resSize);
        if (resSize == 0 || pullTimes == 1 || pullTimes % 3 == 0) {
            //刷新历史记录
            RefreshHistoryMsgEvent historyMsgEvent = new RefreshHistoryMsgEvent();
            historyMsgEvent.pullTimes = pullTimes;
            historyMsgEvent.count = count;
            historyMsgEvent.lastMsgId = lastMsgId;
            historyMsgEvent.listMsg = listMsg;
            historyMsgEvent.peerId = peerId;
            historyMsgEvent.peerType = peerType;
            historyMsgEvent.sessionKey = sessionKey;
            triggerEvent(historyMsgEvent);
        }
        return listMsg;
    }

    /**
     * 事件的处理会在一个后台线程中执行，对应的函数名是onEventBackgroundThread，
     * 虽然名字是BackgroundThread，事件处理是在后台线程，
     * 但事件处理时间还是不应该太长
     * 因为如果发送事件的线程是后台线程，会直接执行事件，
     * 如果当前线程是UI线程，事件会被加到一个队列中，由一个线程依次处理这些事件，
     * 如果某个事件处理时间太长，会阻塞后面的事件的派发或处理
     */
    public void onEventBackgroundThread(RefreshHistoryMsgEvent historyMsgEvent) {
        doRefreshLocalMsg(historyMsgEvent);
    }

    /**
     * asyn task
     * 因为是多端同步，本地信息并不一定完成，拉取时提前异步检测
     */
    private void doRefreshLocalMsg(RefreshHistoryMsgEvent hisEvent) {
        /**check DB数据的一致性*/
        int lastSuccessMsgId = hisEvent.lastMsgId;
        List<MessageEntity> listMsg = hisEvent.listMsg;

        int resSize = listMsg.size();
        if (hisEvent.pullTimes > 1) {
            for (int index = resSize - 1; index >= 0; index--) {
                MessageEntity entity = listMsg.get(index);
                if (!SequenceNumberMaker.getInstance().isFailure(entity.getMsgId())) {
                    lastSuccessMsgId = entity.getMsgId();
                    break;
                }
            }
        } else {
            /**是第一次拉取*/
            if (SequenceNumberMaker.getInstance().isFailure(lastSuccessMsgId))
            /**正序第一个*/
                for (MessageEntity entity : listMsg) {
                    if (!SequenceNumberMaker.getInstance().isFailure(entity.getMsgId())) {
                        lastSuccessMsgId = entity.getMsgId();
                        break;
                    }
                }
        }

        final int refreshCnt = hisEvent.count * 3;
        String peerId = hisEvent.peerId;
        int peerType = hisEvent.peerType;
        String sessionKey = hisEvent.sessionKey;
        boolean localFailure = SequenceNumberMaker.getInstance().isFailure(lastSuccessMsgId);
        if (localFailure) {
            logger.e("LoadHistoryMsg# all msg is failure!");
            if (hisEvent.pullTimes == 1) {
                //走网络
//                reqHistoryMsgNet(peerId, peerType, lastSuccessMsgId, refreshCnt);
            }
        } else {
            /**正常*/
            refreshDBMsg(peerId, peerType, sessionKey, lastSuccessMsgId, refreshCnt);
        }
    }

    /**
     * 历史消息直接从DB中获取。
     * 所以要保证DB数据没有问题
     */
    public void refreshDBMsg(String peerId, int peedType, String chatKey, int lastMsgId, int refreshCnt) {
        if (lastMsgId < 1) {
            return;
        }
        int beginMsgId = lastMsgId - refreshCnt;
        if (beginMsgId < 1) {
            beginMsgId = 1;
        }

        // 返回的结果是升序
//        List<Integer> msgIdList =  dbInterface.refreshHistoryMsgId(chatKey, beginMsgId, lastMsgId);
//        if(msgIdList.size() == (lastMsgId-beginMsgId+1)){
//            logger.d("refreshDBMsg#do need refresh Message!,cause sizeOfList is right");
//            return;
//        }
        // 查找缺失的msgid
        List<Integer> needReqList = new ArrayList<>();
//        for(int startIndex = beginMsgId, endIndex = lastMsgId; startIndex <= endIndex; startIndex++){
//            if(!msgIdList.contains(startIndex)){
//                needReqList.add(startIndex);
//            }
//        }
        // 请求缺失的消息
        if (needReqList.size() > 0) {
            reqMsgById(peerId, peedType, needReqList);
        }


        //需要和服务器进行比对同步
        /**事件驱动通知*/
        MessageEvent event = new MessageEvent();
        event.setEvent(MessageEvent.Event.HISTORY_MSG_OBTAIN);
        triggerEvent(event);

    }

    private void reqMsgById(String peerId, int sessionType, List<Integer> msgIds) {

    }

    public List<MessageEntity> findAll4Order(String orders) {
        List<MessageEntity> list = new ArrayList<>();
        list = messageDao.findAll4Order(orders);
        return list;
    }

    public MessageEntity getMessageEntity(RedisResIQ redisResIQ) throws JSONException {
        MessageEntity messageEntity = null;
        if(StringUtils.isNotBlank(redisResIQ.getContent())) {
            messageEntity = new MessageEntity();
            JSONObject jsonBody = new JSONObject(redisResIQ.getContent());
            String to = jsonBody.getString("to");
            String from = jsonBody.getString("from");
            String thread = jsonBody.getString("thread");
            String body = jsonBody.getString("body");

            messageEntity.setCreated(Long.parseLong(thread));
            messageEntity.setUpdated(Long.parseLong(thread));
            int msgId = SequenceNumberMaker.getInstance()
                    .makelocalUniqueMsgId(Long.valueOf(thread));


            String[] fromArr = from.split("@");
            if(fromArr.length >=2) {
                messageEntity.setFromId(fromArr[0]);
            } else {
                messageEntity.setFromId(from);
            }
            messageEntity.setMsgId(msgId);
            messageEntity.setToId(to);
            messageEntity.setMsgType(DBConstant.MSG_TYPE_GROUP_TEXT);
            messageEntity.setStatus(MessageConstant.MSG_SUCCESS);
            messageEntity.setContent(body);
            messageEntity.setDisplayType(DBConstant.SHOW_ORIGIN_TEXT_TYPE);
            messageEntity.buildSessionKey(false);
        }
        return messageEntity;
    }


    /**
     * 供IMUnreadMsgManager调用
     * @param messageEntity
     */
    public void saveMessage(MessageEntity  messageEntity) {
        if(messageEntity != null) {
            messageDao.replaceInto(messageEntity);
        }
    }

    /**
     * 初始化DAO和服务(退出登录后或者第一次加载需要初始化)
     */
    public void initDaoAndService() {
        if(messageDao == null) {
            messageDao = new MessageDaoImpl(ctx);
        }
        if(publicMessageDao == null) {
            publicMessageDao = (MessageDaoImpl) messageDao;
        }
    }


    public void sendMsgAudio(final AudioMessage audioMessage) {
        if (isAuthenticated()) {
            String uuid = UUID.randomUUID().toString();
            audioMessage.setStatus(MessageConstant.MSG_SENDING);
            audioMessage.setId(uuid);
            //插入本地数据库
            final MessageEntity dbMessage = audioMessage.clone();
            messageDao.saveOrUpdate(dbMessage);
            try {
                socketService.sendMessage(audioMessage.getFromId(), audioMessage.getToId(),
                        audioMessage.getSendContent(), uuid, IMBaseDefine.MsgType.MSG_AUDIO,
                        new XMPPServiceCallbackImpl() {
                            @Override
                            public void onSuccess(Object t) {
                                //回复时间
                                if (t instanceof XMPPServiceImpl.ReplayMessageTime) {
                                    XMPPServiceImpl.ReplayMessageTime messageTime =
                                            (XMPPServiceImpl.ReplayMessageTime) t;
                                    String id = messageTime.getId();
                                    String time = messageTime.getTime();
                                    if (dbMessage != null) {
                                        dbMessage.setStatus(MessageConstant.MSG_SUCCESS);
                                        //通知消息
                                        audioMessage.setStatus(MessageConstant.MSG_SUCCESS);
                                        if (NumberUtils.isNumber(time)) {
                                            int msgId = SequenceNumberMaker.getInstance()
                                                    .makelocalUniqueMsgId(Long.parseLong(time));
                                            //更新msgId
                                            dbMessage.setMsgId(msgId);
                                            dbMessage.setCreated(Long.parseLong(time));
                                            dbMessage.setUpdated(Long.parseLong(time));
                                        }
                                        //更新会话
                                        sessionManager.updateSession(dbMessage, true);
                                        triggerEvent(new MessageEvent(MessageEvent.Event
                                                .ACK_SEND_MESSAGE_OK, audioMessage));
                                        messageDao.saveOrUpdate(dbMessage);
                                    }
                                }
                            }

                            @Override
                            public void onFailed() {
                                //消息发送失败
                                dbMessage.setStatus(MessageConstant.MSG_FAILURE);
                                messageDao.saveOrUpdate(dbMessage);
                                audioMessage.setStatus(MessageConstant.MSG_FAILURE);
                                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, audioMessage));
                            }

                            @Override
                            public void onTimeout() {
                                //消息发送超时
                                dbMessage.setStatus(MessageConstant.MSG_FAILURE);
                                messageDao.saveOrUpdate(dbMessage);
                                audioMessage.setStatus(MessageConstant.MSG_FAILURE);
                                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, audioMessage));
                            }
                        });
            } catch (SmackException.NotConnectedException e) {
                Log.e(TAG, "sendText:", e);
                triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, audioMessage));
            }
        } else {
            //TODO ChatActivity提示未验证通过

        }
    }

//    /**
//     * 发送图片消息
//     * @param msgList
//     */
//    public void sendImages(List<ImageMessage> msgList) {
//        logger.i("chat#image#sendImages size:%d",msgList.size());
//        if(null == msgList || msgList.size() <=0){
//            return ;
//        }
//
//        int len = msgList.size();
//        ArrayList<MessageEntity> needDbList = new ArrayList<>();
//        for (ImageMessage msg : msgList) {
//            needDbList.add(msg);
//        }
////        DBInterface.instance().batchInsertOrUpdateMessage(needDbList);
//
//        for (ImageMessage msg : msgList) {
//            logger.d("chat#pic#sendImage  msg:%s",msg);
//            // image message would wrapped as a text message after uploading
//            int loadStatus = msg.getLoadStatus();
//
//            switch (loadStatus){
//                case MessageConstant.IMAGE_LOADED_FAILURE:
//                case MessageConstant.IMAGE_UNLOAD:
//                case MessageConstant.IMAGE_LOADING:
//                    msg.setLoadStatus(MessageConstant.IMAGE_LOADING);
//                    Intent loadImageIntent = new Intent(ctx, LoadImageService.class);
//                    loadImageIntent.putExtra(Constants.UPLOAD_IMAGE_INTENT_PARAMS, msg);
//                    ctx.startService(loadImageIntent);
//                    break;
//                case MessageConstant.IMAGE_LOADED_SUCCESS:
////                    sendMessage(msg);
//                    break;
//                default:
//                    throw new RuntimeException("sendImages#status不可能出现的状态");
//            }
//        }
//        /**将最后一条更新到Session上面*/
//        sessionManager.updateSession(msgList.get(len-1), true);
//    }


    public void sendMsgImages(List<ImageMessage> msgList) {
        if (isAuthenticated()) {
            for (final ImageMessage imageMessage : msgList) {
                String uuid = UUID.randomUUID().toString();
                logger.d("chat#pic#sendImage  msg:%s",imageMessage);
                // image message would wrapped as a text message after uploading
                int loadStatus = imageMessage.getLoadStatus();
                imageMessage.setStatus(MessageConstant.MSG_SENDING);
                imageMessage.setId(uuid);
                //插入本地数据库
                final MessageEntity dbMessage = imageMessage.clone();
                messageDao.saveOrUpdate(dbMessage);

                switch (loadStatus){
                    case MessageConstant.IMAGE_LOADED_FAILURE:
                    case MessageConstant.IMAGE_UNLOAD:
                    case MessageConstant.IMAGE_LOADING:
                        imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADING);
                        Intent loadImageIntent = new Intent(ctx, LoadImageService.class);
                        loadImageIntent.putExtra(Constants.UPLOAD_IMAGE_INTENT_PARAMS, imageMessage);
                        ctx.startService(loadImageIntent);
                        break;
//                    case MessageConstant.IMAGE_LOADED_SUCCESS:
//                        try {
//                            socketService.sendMessage(imageMessage.getFromId(), imageMessage.getToId(),
//                                    imageMessage.getSendContent(), uuid, IMBaseDefine.MsgType.MSG_IMAGE,
//                                    new XMPPServiceCallbackImpl() {
//                                        @Override
//                                        public void onSuccess(Object t) {
//                                            //回复时间
//                                            if (t instanceof XMPPServiceImpl.ReplayMessageTime) {
//                                                XMPPServiceImpl.ReplayMessageTime messageTime =
//                                                        (XMPPServiceImpl.ReplayMessageTime) t;
//                                                String id = messageTime.getId();
//                                                String time = messageTime.getTime();
//                                                if (dbMessage != null) {
//                                                    dbMessage.setStatus(MessageConstant.MSG_SUCCESS);
//                                                    //通知消息
//                                                    imageMessage.setStatus(MessageConstant.MSG_SUCCESS);
//                                                    if (NumberUtils.isNumber(time)) {
//                                                        int msgId = SequenceNumberMaker.getInstance()
//                                                                .makelocalUniqueMsgId(Long.parseLong(time));
//                                                        //更新msgId
//                                                        dbMessage.setMsgId(msgId);
//                                                        dbMessage.setCreated(Long.parseLong(time));
//                                                        dbMessage.setUpdated(Long.parseLong(time));
//                                                    }
//
//                                                    //更新会话
//                                                    sessionManager.updateSession(dbMessage, true);
//                                                    triggerEvent(new MessageEvent(MessageEvent.Event
//                                                            .ACK_SEND_MESSAGE_OK, imageMessage));
//                                                    messageDao.saveOrUpdate(dbMessage);
//                                                }
//                                            }
//                                        }
//
//                                        @Override
//                                        public void onFailed() {
//                                            //消息发送失败
//                                            dbMessage.setStatus(MessageConstant.MSG_FAILURE);
//                                            messageDao.saveOrUpdate(dbMessage);
//                                            imageMessage.setStatus(MessageConstant.MSG_FAILURE);
//                                            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, imageMessage));
//                                        }
//
//                                        @Override
//                                        public void onTimeout() {
//                                            //消息发送超时
//                                            dbMessage.setStatus(MessageConstant.MSG_FAILURE);
//                                            messageDao.saveOrUpdate(dbMessage);
//                                            imageMessage.setStatus(MessageConstant.MSG_FAILURE);
//                                            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, imageMessage));
//                                        }
//                                    });
//                        } catch (SmackException.NotConnectedException e) {
//                            Log.e(TAG, "sendText:", e);
//                            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, imageMessage));
//                        }
//                        break;
                    default:
                        throw new RuntimeException("sendImages#status不可能出现的状态");
                }
            }
        } else {
            //TODO ChatActivity提示未验证通过

        }
    }



    public void onRecvMsg(Message message, IMBaseDefine.MsgType msgType) {
        if(msgType != null) {
            switch (msgType) {
                case MSG_AUDIO:
                    executeMsgAudio(message);
                    break;
                case MSG_IMAGE:
                    executeMsgImage(message);
                    break;
            }
        }
    }

    public void executeMsgAudio(Message message) {
        String[] fromArr = message.getFrom().split("/");
        if(fromArr != null && fromArr.length >= 2) {
            String toId = fromArr[0];
            String fromId = fromArr[1];
            if(fromId.indexOf("@") >= 0) {
                fromId = fromId.substring(0, fromId.indexOf("@"));
            }
            try {
                AudioMessage audioMessage = AudioMessage.buildForReceive(message, fromId, toId);
                //将消息保存到本地数据库
                MessageEntity dbMessage = audioMessage.clone();
                messageDao.saveOrUpdate(dbMessage);
                SessionEntity cacheSessionEntity = sessionManager.getSessionMap()
                        .get(audioMessage.getSessionKey());
                /**
                 * 1:最新接收的消息时间不能小于缓存消息的创建时间
                 * 2:时间由服务器生成
                 */
                if(cacheSessionEntity == null
                        || cacheSessionEntity.getCreated() < audioMessage.getCreated()) {
                    //更新缓存，触发通知（更新群组列表UI）
                    sessionManager.updateSession(dbMessage, true);
                }

                /**
                 *  发送已读确认由上层的activity处理 特殊处理
                 *  1. 未读计数、 通知、session页面
                 *  2. 当前会话
                 * */
                PriorityEvent notifyEvent = new PriorityEvent();
                notifyEvent.event = PriorityEvent.Event.MSG_RECEIVED_MESSAGE;
                notifyEvent.object = audioMessage;
                triggerEvent(notifyEvent);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }


    public void executeMsgImage(Message message) {
        String[] fromArr = message.getFrom().split("/");
        if(fromArr != null && fromArr.length >= 2) {
            String toId = fromArr[0];
            String fromId = fromArr[1];
            if(fromId.indexOf("@") >= 0) {
                fromId = fromId.substring(0, fromId.indexOf("@"));
            }
            try {
                ImageMessage imageMessage = ImageMessage.buildForReceive(message, fromId, toId);
                //将消息保存到本地数据库
                MessageEntity dbMessage = imageMessage.clone();
                messageDao.saveOrUpdate(dbMessage);
                SessionEntity cacheSessionEntity = sessionManager.getSessionMap()
                        .get(imageMessage.getSessionKey());
                /**
                 * 1:最新接收的消息时间不能小于缓存消息的创建时间
                 * 2:时间由服务器生成
                 */
                if(cacheSessionEntity == null
                        || cacheSessionEntity.getCreated() < imageMessage.getCreated()) {
                    //更新缓存，触发通知（更新群组列表UI）
                    sessionManager.updateSession(dbMessage, true);
                }

                /**
                 *  发送已读确认由上层的activity处理 特殊处理
                 *  1. 未读计数、 通知、session页面
                 *  2. 当前会话
                 * */
                PriorityEvent notifyEvent = new PriorityEvent();
                notifyEvent.event = PriorityEvent.Event.MSG_RECEIVED_MESSAGE;
                notifyEvent.object = imageMessage;
                triggerEvent(notifyEvent);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void replaceInto(MessageEntity messageEntity) {
        messageDao.replaceInto(messageEntity);
    }


    /**图片的处理放在这里，因为在发送图片的过程中，很可能chatActivity已经关闭掉*/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4Message(MessageEvent event){
        MessageEvent.Event  type = event.getEvent();
        switch (type){
            case IMAGE_UPLOAD_FAILD:
                logger.d("pic#onUploadImageFaild");
                ImageMessage imageMessage = (ImageMessage)event.getMessageEntity();
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_FAILURE);
                imageMessage.setStatus(MessageConstant.MSG_FAILURE);

                MessageEntity messageEntity = imageMessage.clone();
                messageDao.replaceInto(messageEntity);

                /**通知Activity层 失败*/
                event.setEvent(MessageEvent.Event.HANDLER_IMAGE_UPLOAD_FAILD);
                event.setMessageEntity(imageMessage);
                triggerEvent(event);
                break;
            case IMAGE_UPLOAD_SUCCESS:
                onImageUploadFinish(event);
                break;
            case IMAGE_UPLOAD_PROGRESSING:
                onImageLoadProgress(event);
                break;
        }
    }


    //上传下载进度
    private void onImageLoadProgress(MessageEvent imageEvent) {
        logger.d("pic#onImageLoadProgress");
        final ImageMessage imageMessage = (ImageMessage)imageEvent.getMessageEntity();
        imageMessage.setProgress(imageEvent.getProgress());
        triggerEvent(new MessageEvent(MessageEvent.Event
                .ACK_SEND_MESSAGE_OK, imageMessage));
    }

    private void onImageUploadFinish(MessageEvent imageEvent){
        final ImageMessage imageMessage = (ImageMessage)imageEvent.getMessageEntity();
        logger.d("pic#onImageUploadFinish");
        String imageUrl = imageMessage.getUrl();
        logger.d("pic#imageUrl:%s", imageUrl);
        String realImageURL = "";
        try {
            realImageURL = URLDecoder.decode(imageUrl, "utf-8");
            logger.d("pic#realImageUrl:%s", realImageURL);
        } catch (UnsupportedEncodingException e) {
            logger.e(e.toString());
        }

        imageMessage.setUrl(realImageURL);
        imageMessage.setStatus(MessageConstant.MSG_SUCCESS);
        imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_SUCCESS);
        final MessageEntity dbMessage = imageMessage.clone();
//        dbInterface.insertOrUpdateMessage(imageMessage);
        messageDao.replaceInto(dbMessage);

        /**通知Activity层 成功 ， 事件通知*/
        imageEvent.setEvent(MessageEvent.Event.HANDLER_IMAGE_UPLOAD_SUCCESS);
        imageEvent.setMessageEntity(imageMessage);
        triggerEvent(imageEvent);

        imageMessage.setContent(MessageConstant.IMAGE_MSG_START
                + realImageURL + MessageConstant.IMAGE_MSG_END);
//        sendMessage(imageMessage);

        String uuid = UUID.randomUUID().toString();
        try {
            socketService.sendMessage(imageMessage.getFromId(), imageMessage.getToId(),
                    imageMessage.getSendContent(), uuid, IMBaseDefine.MsgType.MSG_IMAGE,
                    new XMPPServiceCallbackImpl() {
                        @Override
                        public void onSuccess(Object t) {
                            //回复时间
                            if (t instanceof XMPPServiceImpl.ReplayMessageTime) {
                                XMPPServiceImpl.ReplayMessageTime messageTime =
                                        (XMPPServiceImpl.ReplayMessageTime) t;
                                String id = messageTime.getId();
                                String time = messageTime.getTime();
                                if (dbMessage != null) {
                                    dbMessage.setStatus(MessageConstant.MSG_SUCCESS);
                                    //通知消息
                                    imageMessage.setStatus(MessageConstant.MSG_SUCCESS);
                                    if (NumberUtils.isNumber(time)) {
                                        int msgId = SequenceNumberMaker.getInstance()
                                                .makelocalUniqueMsgId(Long.parseLong(time));
                                        //更新msgId
                                        dbMessage.setMsgId(msgId);
                                        dbMessage.setCreated(Long.parseLong(time));
                                        dbMessage.setUpdated(Long.parseLong(time));
                                    }

                                    //更新会话
                                    sessionManager.updateSession(dbMessage, true);
                                    triggerEvent(new MessageEvent(MessageEvent.Event
                                            .ACK_SEND_MESSAGE_OK, imageMessage));
                                    messageDao.replaceInto(dbMessage);
                                }
                            }
                        }

                        @Override
                        public void onFailed() {
                            //消息发送失败
                            dbMessage.setStatus(MessageConstant.MSG_FAILURE);
                            messageDao.replaceInto(dbMessage);
                            imageMessage.setStatus(MessageConstant.MSG_FAILURE);
                            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, imageMessage));
                        }

                        @Override
                        public void onTimeout() {
                            //消息发送超时
                            dbMessage.setStatus(MessageConstant.MSG_FAILURE);
                            messageDao.replaceInto(dbMessage);
                            imageMessage.setStatus(MessageConstant.MSG_FAILURE);
                            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, imageMessage));
                        }
                    });
        } catch (SmackException.NotConnectedException e) {
            Log.e(TAG, "sendText:", e);
            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_FAILURE, imageMessage));
        }
    }

    /**
     * 重新发送 message数据包
     * 1.检测DB状态
     * 2.删除DB状态 [不用删除]
     * 3.调用对应的发送
     * 判断消息的类型、判断是否是重发的状态
     *
     * */
    public void resendMessage(MessageEntity msgInfo) {
        if (msgInfo == null) {
            logger.d("chat#resendMessage msgInfo is null or already send success!");
            return;
        }
        /**check 历史原因处理*/
        if(!SequenceNumberMaker.getInstance().isFailure(msgInfo.getMsgId())){
            // 之前的状态处理有问题
            msgInfo.setStatus(MessageConstant.MSG_SUCCESS);
            messageDao.replaceInto(msgInfo);
            triggerEvent(new MessageEvent(MessageEvent.Event.ACK_SEND_MESSAGE_OK,msgInfo));
            return;
        }

        logger.d("chat#resendMessage msgInfo %s",msgInfo);
        /**重新设定message 的时间,已经从DB中删除*/
        long nowTime = System.currentTimeMillis();
        msgInfo.setUpdated(nowTime);
        msgInfo.setCreated(nowTime);

        /**判断信息的类型*/
        int msgType = msgInfo.getDisplayType();
        switch (msgType){
            case DBConstant.SHOW_ORIGIN_TEXT_TYPE:
                sendText((TextMessage)msgInfo);
                break;
            case DBConstant.SHOW_IMAGE_TYPE:
                sendMsgImage((ImageMessage) msgInfo);
                break;
            case DBConstant.SHOW_AUDIO_TYPE:
                sendMsgAudio((AudioMessage)msgInfo); break;
            default:
                throw new IllegalArgumentException("#resendMessage#enum type is wrong!!,cause by displayType"+msgType);
        }
    }


    public void sendMsgImage(ImageMessage msg){
        logger.d("ImMessageManager#sendImage ");
        ArrayList<ImageMessage> msgList = new ArrayList<>();
        msgList.add(msg);
        sendMsgImages(msgList);
    }
}
