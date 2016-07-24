package com.juju.app.service.notify;

import com.juju.app.event.notify.DiscussNotifyEvent;
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

public class LiveDiscussNotify extends BaseNotify<DiscussNotifyEvent.DiscussNotifyBean> {

    private Logger logger = Logger.getLogger(LiveDiscussNotify.class);

    private volatile static LiveDiscussNotify inst;

    public static LiveDiscussNotify instance() {
        if(inst == null) {
            synchronized (LiveDiscussNotify.class) {
                if (inst == null) {
                    inst = new LiveDiscussNotify();
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
    public void executeCommand4Send(DiscussNotifyEvent.DiscussNotifyBean discussNotifyBean) {
        sendDiscussToMServer(discussNotifyBean);
    }

    @Override
    public void executeCommand4Recv(DiscussNotifyEvent.DiscussNotifyBean discussNotifyBean) {
        DiscussNotifyEvent failEvent = new DiscussNotifyEvent(DiscussNotifyEvent.Event.RECEIVE_DISCUSS_NOTIFY_OK, discussNotifyBean);
        triggerEvent(failEvent);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(DiscussNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case DISCUSS_NOTIFY_MSERVER_OK:
                DiscussNotifyEvent externalEvent = new DiscussNotifyEvent(DiscussNotifyEvent.Event.DISCUSS_NOTIFY_OK, sendParam.bean);
                triggerEvent(externalEvent);
                break;
            case DISCUSS_NOTIFY_MSERVER_FAILED:
                DiscussNotifyEvent failEvent = new DiscussNotifyEvent(DiscussNotifyEvent.Event.DISCUSS_NOTIFY_FAILED, sendParam.bean);
                triggerEvent(failEvent);
                break;
        }
    }

    public void sendDiscussToMServer(final DiscussNotifyEvent.DiscussNotifyBean discussNotifyBean) {
        String peerId = discussNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(discussNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.LIVE_DISCUSS, uuid, true,
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
                            discussNotifyBean.replyId = id;
                            discussNotifyBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(DiscussNotifyEvent.BusinessFlow.SendParam
                                    .Send.DISCUSS_NOTIFY_MSERVER_OK, discussNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(DiscussNotifyEvent.BusinessFlow.SendParam
                                    .Send.DISCUSS_NOTIFY_MSERVER_FAILED, discussNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer failed");
                        buildAndTriggerBusinessFlow4Send(DiscussNotifyEvent.BusinessFlow.SendParam
                                .Send.DISCUSS_NOTIFY_MSERVER_FAILED, discussNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(DiscussNotifyEvent.BusinessFlow.SendParam
                                .Send.DISCUSS_NOTIFY_MSERVER_FAILED, discussNotifyBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            DiscussNotifyEvent.BusinessFlow.SendParam.Send send,
            DiscussNotifyEvent.DiscussNotifyBean DiscussNotifyBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        DiscussNotifyEvent.BusinessFlow.SendParam sendParam = new DiscussNotifyEvent.BusinessFlow
                .SendParam(send, DiscussNotifyBean);
        triggerEvent(sendParam);
    }

}
