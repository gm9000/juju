package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.event.notify.SeizeNotifyEvent;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

public class LiveSeizeStopNotify extends BaseNotify<SeizeNotifyEvent.SeizeNotifyBean> {

    private Logger logger = Logger.getLogger(LiveSeizeStopNotify.class);

    private volatile static LiveSeizeStopNotify inst;

    public static LiveSeizeStopNotify instance() {
        if(inst == null) {
            synchronized (LiveSeizeStopNotify.class) {
                if (inst == null) {
                    inst = new LiveSeizeStopNotify();
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
    public void executeCommand4Send(SeizeNotifyEvent.SeizeNotifyBean seizeNotifyBean) {
        sendLiveSeizeStopToMServer(seizeNotifyBean);
    }

    @Override
    public void executeCommand4Recv(SeizeNotifyEvent.SeizeNotifyBean seizeNotifyBean) {
        synLocalLiveData(seizeNotifyBean);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(SeizeNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_LIVE_SEIZE_STOP_MSERVER_OK:
                SeizeNotifyEvent externalEvent = new SeizeNotifyEvent(SeizeNotifyEvent.Event
                        .LIVE_SEIZE_STOP_OK, sendParam.bean);
                triggerEvent(externalEvent);
                break;
            case SEND_LIVE_SEIZE_STOP_MSERVER_FAILED:
//                SeizeNotifyEvent failEvent = new SeizeNotifyEvent(SeizeNotifyEvent.Event
//                        .PARTY_RECRUIT_FAILED, sendParam.bean);
//                triggerEvent(failEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.live_seize_stop_send_failed, 0);
                    }
                });
                break;
        }
    }

    public void sendLiveSeizeStopToMServer(final SeizeNotifyEvent.SeizeNotifyBean seizeNotifyBean) {
        String peerId = seizeNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(seizeNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.LIVE_RELAY, uuid, true,
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
                            seizeNotifyBean.replyId = id;
                            seizeNotifyBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(SeizeNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_LIVE_SEIZE_STOP_MSERVER_OK, seizeNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(SeizeNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_LIVE_SEIZE_STOP_MSERVER_FAILED, seizeNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer failed");
                        buildAndTriggerBusinessFlow4Send(SeizeNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_LIVE_SEIZE_STOP_MSERVER_FAILED, seizeNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(SeizeNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_LIVE_SEIZE_STOP_MSERVER_FAILED, seizeNotifyBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            SeizeNotifyEvent.BusinessFlow.SendParam.Send send,
            SeizeNotifyEvent.SeizeNotifyBean SeizeNotifyBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        SeizeNotifyEvent.BusinessFlow.SendParam sendParam = new SeizeNotifyEvent.BusinessFlow
                .SendParam(send, SeizeNotifyBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(SeizeNotifyEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case PROCESS_LIVE_SEIZE_STOP_OK:
                SeizeNotifyEvent externalEvent = new SeizeNotifyEvent(SeizeNotifyEvent.Event.LIVE_SEIZE_STOP_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case PROCESS_LIVE_SEIZE_STOP_FAIL:
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ToastUtil.TextStringToast(context,"receive failue",0);
//                    }
//                });
                break;
        }
    }



    /**
     * 同步本地聚会方案数据
     */
    public void synLocalLiveData(final SeizeNotifyEvent.SeizeNotifyBean seizeNotifyBean) {

        buildAndTriggerBusinessFlow4Recv(SeizeNotifyEvent.BusinessFlow.RecvParam
                .Recv.PROCESS_LIVE_SEIZE_STOP_OK, seizeNotifyBean);
    }

    private void buildAndTriggerBusinessFlow4Recv(SeizeNotifyEvent.BusinessFlow.RecvParam.Recv recv,SeizeNotifyEvent.SeizeNotifyBean SeizeNotifyBean) {
        SeizeNotifyEvent.BusinessFlow.RecvParam recvParam
                = new SeizeNotifyEvent.BusinessFlow.RecvParam(recv, SeizeNotifyBean);
        triggerEvent(recvParam);
    }
}
