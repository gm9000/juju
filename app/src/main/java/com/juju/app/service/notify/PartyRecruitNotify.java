package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

public class PartyRecruitNotify extends BaseNotify<PartyNotifyEvent.PartyNotifyBean> {

    private Logger logger = Logger.getLogger(PartyRecruitNotify.class);

    private volatile static PartyRecruitNotify inst;

    public static PartyRecruitNotify instance() {
        if(inst == null) {
            synchronized (PartyRecruitNotify.class) {
                if (inst == null) {
                    inst = new PartyRecruitNotify();
                }
            }
        }
        return inst;
    }

    private IMGroupManager imGroupManager;

    public void start(IMOtherManager imOtherManager, IMGroupManager imGroupManager) {
        super.start(imOtherManager);
        this.imGroupManager = imGroupManager;
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void executeCommand4Send(PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        sendPartyRecruitToMServer(partyNotifyBean);
    }

    @Override
    public void executeCommand4Recv(PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
    }


    public void stop() {
        super.stop();
        imGroupManager = null;
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(PartyNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_PARTY_RECRUIT_MSERVER_OK:
                PartyNotifyEvent externalEvent = new PartyNotifyEvent(PartyNotifyEvent.Event
                        .PARTY_RECRUIT_OK, sendParam.bean);
                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.master_transfer_send_success, 0);
                    }
                });
                break;
            case SEND_PARTY_RECRUIT_MSERVER_FAILED:
                PartyNotifyEvent failEvent = new PartyNotifyEvent(PartyNotifyEvent.Event
                        .PARTY_RECRUIT_FAILED, sendParam.bean);
                triggerEvent(failEvent);
                break;
        }
    }

    public void sendPartyRecruitToMServer(final PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        String peerId = partyNotifyBean.groupId+"@"+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(partyNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4User(peerId, message,
                IMBaseDefine.NotifyType.PARTY_RECRUIT, uuid, true,
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
                            partyNotifyBean.replyId = id;
                            partyNotifyBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_PARTY_RECRUIT_MSERVER_OK, partyNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_PARTY_RECRUIT_MSERVER_FAILED, partyNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer failed");
                        buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_PARTY_RECRUIT_MSERVER_FAILED, partyNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_PARTY_RECRUIT_MSERVER_FAILED, partyNotifyBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            PartyNotifyEvent.BusinessFlow.SendParam.Send send,
            PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        PartyNotifyEvent.BusinessFlow.SendParam sendParam = new PartyNotifyEvent.BusinessFlow
                .SendParam(send, partyNotifyBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(PartyNotifyEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case UPDATE_LOCAL_CACHE_DATA_OK:
                break;
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
                break;
        }
    }

    /**
     * 构建接收业务流
     * @param recv
     */
    private void buildAndTriggerBusinessFlow4Recv(
            PartyNotifyEvent.BusinessFlow.RecvParam.Recv recv,
            PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        if(recv == null)
            throw new IllegalArgumentException("MasterTransferNotify#recv is null");

        PartyNotifyEvent.BusinessFlow.RecvParam recvParam = new PartyNotifyEvent.BusinessFlow
                .RecvParam(recv, partyNotifyBean);
        triggerEvent(recvParam);
    }
}
