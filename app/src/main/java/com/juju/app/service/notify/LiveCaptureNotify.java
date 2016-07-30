package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.entity.VideoProgram;
import com.juju.app.event.notify.LiveNotifyEvent;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.ex.DbException;

import java.util.UUID;

public class LiveCaptureNotify extends BaseNotify<LiveNotifyEvent.LiveNotifyBean> {

    private Logger logger = Logger.getLogger(LiveCaptureNotify.class);

    private volatile static LiveCaptureNotify inst;

    public static LiveCaptureNotify instance() {
        if(inst == null) {
            synchronized (LiveCaptureNotify.class) {
                if (inst == null) {
                    inst = new LiveCaptureNotify();
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
    public void executeCommand4Send(LiveNotifyEvent.LiveNotifyBean LiveNotifyBean) {
        sendLiveCaptureToMServer(LiveNotifyBean);
    }

    @Override
    public void executeCommand4Recv(LiveNotifyEvent.LiveNotifyBean LiveNotifyBean) {
        synLocalLiveData(LiveNotifyBean);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(LiveNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_LIVE_CAPTURE_MSERVER_OK:
//                LiveNotifyEvent externalEvent = new LiveNotifyEvent(LiveNotifyEvent.Event
//                        .PARTY_RECRUIT_OK, sendParam.bean);
//                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.live_capture_send_success, 0);
                    }
                });
                break;
            case SEND_LIVE_CAPTURE_MSERVER_FAILED:
//                LiveNotifyEvent failEvent = new LiveNotifyEvent(LiveNotifyEvent.Event
//                        .PARTY_RECRUIT_FAILED, sendParam.bean);
//                triggerEvent(failEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.live_capture_send_failed, 0);
                    }
                });
                break;
        }
    }

    public void sendLiveCaptureToMServer(final LiveNotifyEvent.LiveNotifyBean liveNotifyBean) {
        String peerId = liveNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(liveNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.LIVE_CAPTURE, uuid, true,
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
                            liveNotifyBean.replyId = id;
                            liveNotifyBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            imOtherManager.updateGroupNotify(liveNotifyBean.getGroupId(),replyTime);
                            buildAndTriggerBusinessFlow4Send(LiveNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_LIVE_CAPTURE_MSERVER_OK, liveNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(LiveNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_LIVE_CAPTURE_MSERVER_FAILED, liveNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer failed");
                        buildAndTriggerBusinessFlow4Send(LiveNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_LIVE_CAPTURE_MSERVER_FAILED, liveNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(LiveNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_LIVE_CAPTURE_MSERVER_FAILED, liveNotifyBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            LiveNotifyEvent.BusinessFlow.SendParam.Send send,
            LiveNotifyEvent.LiveNotifyBean LiveNotifyBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        LiveNotifyEvent.BusinessFlow.SendParam sendParam = new LiveNotifyEvent.BusinessFlow
                .SendParam(send, LiveNotifyBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(LiveNotifyEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case PROCESS_LIVE_CAPTURE_OK:
                LiveNotifyEvent externalEvent = new LiveNotifyEvent(LiveNotifyEvent.Event.LIVE_CAPTURE_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case PROCESS_LIVE_CAPTURE_FAIL:
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
    public void synLocalLiveData(final LiveNotifyEvent.LiveNotifyBean liveNotifyBean) {

        VideoProgram videoProgram = null;
        try {
            videoProgram = JujuDbUtils.getInstance().selector(VideoProgram.class).where("id","=",liveNotifyBean.getLiveId()).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
        if(videoProgram != null){
            videoProgram.setCaptureUrl(liveNotifyBean.getCaptureUrl());
            JujuDbUtils.saveOrUpdate(videoProgram);
            buildAndTriggerBusinessFlow4Recv(LiveNotifyEvent.BusinessFlow.RecvParam
                    .Recv.PROCESS_LIVE_CAPTURE_OK, liveNotifyBean);
        }
    }

    private void buildAndTriggerBusinessFlow4Recv(LiveNotifyEvent.BusinessFlow.RecvParam.Recv recv,LiveNotifyEvent.LiveNotifyBean LiveNotifyBean) {
        LiveNotifyEvent.BusinessFlow.RecvParam recvParam
                = new LiveNotifyEvent.BusinessFlow.RecvParam(recv, LiveNotifyBean);
        triggerEvent(recvParam);
    }
}
