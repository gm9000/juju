package com.juju.app.service.im.thread;

import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.event.UnreadEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.iq.RedisResIQ;
import com.juju.app.service.im.manager.IMMessageManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.utils.Logger;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * 项目名称：juju
 * 类描述：合并消息线程
 * 创建人：gm
 * 日期：2016/5/4 11:49
 * 版本：V1.0.0
 */
public class MergeMessageThread implements Runnable{

    private final String COMMAND = "zrevrangebyscore";

    private Logger logger = Logger.getLogger(MergeMessageThread.class);

    //计数器
    private CountDownLatch countDownLatch;

    //聊天室ID
    private String chatRoomId;

    private SocketService socketService;

    private long time;

    private int length = Constants.MSG_CNT_PER_PAGE;

    private Object obj = new Object();

    public MergeMessageThread(String chatRoomId, long time, int length, SocketService socketService) {
        this.chatRoomId = chatRoomId;
        this.time = time;
        this.length = length;
        this.socketService = socketService;
    }

    public MergeMessageThread(CountDownLatch countDownLatch, String chatRoomId,
                              long time, int length, SocketService socketService) {
        this.countDownLatch = countDownLatch;
        this.chatRoomId = chatRoomId;
        this.time = time;
        this.length = length;
        this.socketService = socketService;
    }

    @Override
    public void run() {
       execute();
    }

    /**
     * 执行查询
     */
    private void execute() {
        if(time > 2) {
            //更新最近会话
            String uuid = UUID.randomUUID().toString();
            socketService.findHisMessages(COMMAND, chatRoomId, String.valueOf(time), "",
                    uuid, 0, length, new XMPPServiceCallbackImpl(1) {
                        /**
                         * 新消息
                         *
                         * @param t
                         */
                        @Override
                        public void onSuccess(Object t) {
                            RedisResIQ redisResIQ = (RedisResIQ) t;
                            MessageEntity messageEntity = saveMessage(redisResIQ);
                            if(messageEntity != null) {
                                updateSession(messageEntity);
                            }
                            synchronized (obj) {
                                obj.notify();
                            }

                        }


                        /**
                         * 消息异常
                         */
                        @Override
                        public void onFailed() {
                            logger.i("MergeMessageThread#run is failed");
                            synchronized (obj) {
                                obj.notify();
                            }
                        }

                        /**
                         * 消息超时
                         */
                        @Override
                        public void onTimeout() {
                            logger.i("MergeMessageThread#run is timeout");
                            synchronized (obj) {
                                obj.notify();
                            }
                        }
                    });

            try {
                System.out.println("before wait...............");
                //最长等待时间12秒
                synchronized (obj) {
                    obj.wait(12000l);
                }
                System.out.println("end wait...............");
            } catch (InterruptedException e) {
                logger.error(e);
            }


        }
    }

    //发送消息，消息发布者，UI需监听
//    private void triggerEvent(Object paramObject)
//    {
//        EventBus.getDefault().post(paramObject);
//    }

    private MessageEntity saveMessage(RedisResIQ redisResIQ) {
        MessageEntity messageEntity = null;
        try {
            messageEntity = IMMessageManager.instance().getMessageEntity(redisResIQ);
            IMMessageManager.instance().saveMessage(messageEntity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return messageEntity;
    }

    private void updateSession(MessageEntity messageEntity) {
        long updated = messageEntity.getUpdated();
        String sessionKey = messageEntity.getSessionKey();
        SessionEntity cacheSessionEntity = IMSessionManager.instance()
                .getSessionMap().get(sessionKey);
        //从服务器查询消息比本地session要新时，需要更新session
        if(cacheSessionEntity != null
                && cacheSessionEntity.getUpdated() < updated) {
            //更新session(是否调用此函数，函数包含刷新通知)
            IMSessionManager.instance().updateSession(messageEntity, true);
        }
    }
}
