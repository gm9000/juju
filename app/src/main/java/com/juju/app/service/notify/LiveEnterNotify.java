package com.juju.app.service.notify;

import com.juju.app.event.notify.LiveEnterNotifyEvent;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

public class LiveEnterNotify extends BaseNotify<LiveEnterNotifyEvent.LiveEnterNotifyBean> {

    private Logger logger = Logger.getLogger(LiveEnterNotify.class);

    private volatile static LiveEnterNotify inst;

    public static LiveEnterNotify instance() {
        if(inst == null) {
            synchronized (LiveEnterNotify.class) {
                if (inst == null) {
                    inst = new LiveEnterNotify();
                }
            }
        }
        return inst;
    }


    public void start(IMOtherManager imOtherManager) {
        super.start(imOtherManager);
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void executeCommand4Send(LiveEnterNotifyEvent.LiveEnterNotifyBean liveEnterNotifyBean) {
        sendLiveEnterToMServer(liveEnterNotifyBean);
    }

    @Override
    public void executeCommand4Recv(LiveEnterNotifyEvent.LiveEnterNotifyBean liveEnterNotifyBean) {
        LiveEnterNotifyEvent failEvent = new LiveEnterNotifyEvent(LiveEnterNotifyEvent.Event.LIVE_ENTER_NOTIFY_OK, liveEnterNotifyBean);
        triggerEvent(failEvent);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(LiveEnterNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case LIVE_ENTER_NOTIFY_MSERVER_OK:
                LiveEnterNotifyEvent externalEvent = new LiveEnterNotifyEvent(LiveEnterNotifyEvent.Event.LIVE_ENTER_NOTIFY_OK, sendParam.bean);
                triggerEvent(externalEvent);
                break;
            case LIVE_ENTER_NOTIFY_MSERVER_FAILED:
                LiveEnterNotifyEvent failEvent = new LiveEnterNotifyEvent(LiveEnterNotifyEvent.Event.LIVE_ENTER_NOTIFY_FAILED, sendParam.bean);
                triggerEvent(failEvent);
                break;
        }
    }

    public void sendLiveEnterToMServer(final LiveEnterNotifyEvent.LiveEnterNotifyBean liveEnterNotifyBean) {
        String peerId = liveEnterNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(liveEnterNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.LIVE_ENTER, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            liveEnterNotifyBean.replyId = id;
                            liveEnterNotifyBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(LiveEnterNotifyEvent.BusinessFlow.SendParam
                                    .Send.LIVE_ENTER_NOTIFY_MSERVER_OK, liveEnterNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(LiveEnterNotifyEvent.BusinessFlow.SendParam
                                    .Send.LIVE_ENTER_NOTIFY_MSERVER_FAILED, liveEnterNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer failed");
                        buildAndTriggerBusinessFlow4Send(LiveEnterNotifyEvent.BusinessFlow.SendParam
                                .Send.LIVE_ENTER_NOTIFY_MSERVER_FAILED, liveEnterNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(LiveEnterNotifyEvent.BusinessFlow.SendParam
                                .Send.LIVE_ENTER_NOTIFY_MSERVER_FAILED, liveEnterNotifyBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            LiveEnterNotifyEvent.BusinessFlow.SendParam.Send send,
            LiveEnterNotifyEvent.LiveEnterNotifyBean LiveEnterNotifyBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        LiveEnterNotifyEvent.BusinessFlow.SendParam sendParam = new LiveEnterNotifyEvent.BusinessFlow
                .SendParam(send, LiveEnterNotifyBean);
        triggerEvent(sendParam);
    }
}
