package com.juju.app.service.im.callback;


import com.juju.app.helper.chat.FixLinkedHashMap;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;


/**
 * 项目名称：juju
 * 类描述：固定队列
 * 创建人：gm
 * 日期：2016/4/29 16:26
 * 版本：V1.0.0
 */
public class FixListenerQueue {

    private static FixListenerQueue listenerQueue = new FixListenerQueue();
    private Logger logger = Logger.getLogger(FixListenerQueue.class);

    public static FixListenerQueue instance(){
        return listenerQueue;
    }

    private volatile  boolean stopFlag = false;
    private volatile  boolean hasTask = false;


    //callback 队列，固定10个
    private FixLinkedHashMap<String, XMPPServiceCallbackImpl> callBackQueue = new FixLinkedHashMap<>(10);


    public void push(String uuid, XMPPServiceCallbackImpl packetlistener){
        if(StringUtils.isBlank(uuid) || null==packetlistener){
            logger.d("FixListenerQueue#push error, cause by Illegal params");
            return;
        }
        callBackQueue.put(uuid, packetlistener);
    }

    public XMPPServiceCallbackImpl get(String seqNo){
        synchronized (FixListenerQueue.this) {
            if (callBackQueue.containsKey(seqNo)) {
                XMPPServiceCallbackImpl packetlistener = callBackQueue.get(seqNo);
                return packetlistener;
            }
            return null;
        }
    }

    public void remove(String seqNo){
        synchronized (FixListenerQueue.this) {
            callBackQueue.remove(seqNo);
        }
    }

}
