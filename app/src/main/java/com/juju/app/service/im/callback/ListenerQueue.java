package com.juju.app.service.im.callback;

import android.os.Handler;


import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ListenerQueue {

    private static ListenerQueue listenerQueue = new ListenerQueue();
    private Logger logger = Logger.getLogger(ListenerQueue.class);
    public static ListenerQueue instance(){
        return listenerQueue;
    }

    private volatile  boolean stopFlag = false;
    private volatile  boolean hasTask = false;


    //callback 队列
    private Map<String, XMPPServiceCallbackImpl> callBackQueue = new ConcurrentHashMap<>();
    private Handler timerHandler = new Handler();


    public void onStart(){
        logger.d("ListenerQueue#onStart run");
        stopFlag = false;
        startTimer();
    }
    public void onDestory(){
        logger.d("ListenerQueue#onDestory ");
        callBackQueue.clear();
        stopTimer();
    }

    //以前是TimerTask处理方式
    private void startTimer() {
        if(!stopFlag && hasTask == false) {
            hasTask = true;
            timerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    timerImpl();
                    hasTask = false;
                    startTimer();
                }
            }, 5 * 1000);
        }
    }

    private void stopTimer(){
        stopFlag = true;
    }

    private void timerImpl() {
        long currentRealtime =   System.currentTimeMillis();//SystemClock.elapsedRealtime();
        for (Map.Entry<String, XMPPServiceCallbackImpl> entry : callBackQueue.entrySet()) {
            XMPPServiceCallbackImpl packetlistener = entry.getValue();
            String uuid = entry.getKey();
            long timeRange = currentRealtime - packetlistener.getCreateTime();

            try {
                if (timeRange >= packetlistener.getTimeOut()) {
                    logger.d("ListenerQueue#find timeout msg");
                    XMPPServiceCallbackImpl listener = pop(uuid);
                    if (listener != null) {
                        listener.onTimeout();
                    }
                }
            } catch (Exception e) {
                logger.d("ListenerQueue#timerImpl onTimeout is Error,exception is %s", e.getCause());
            }
        }
    }

    public void push(String uuid, XMPPServiceCallbackImpl packetlistener){
        if(StringUtils.isBlank(uuid) || null==packetlistener){
            logger.d("ListenerQueue#push error, cause by Illegal params");
            return;
        }
        callBackQueue.put(uuid, packetlistener);
    }


    public XMPPServiceCallbackImpl pop(String seqNo){
        synchronized (ListenerQueue.this) {
            if (callBackQueue.containsKey(seqNo)) {
                XMPPServiceCallbackImpl packetlistener = callBackQueue.remove(seqNo);
                return packetlistener;
            }
            return null;
        }
    }
}
