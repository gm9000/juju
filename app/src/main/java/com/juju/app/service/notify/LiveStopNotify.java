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

import java.util.Date;
import java.util.UUID;

public class LiveStopNotify extends BaseNotify<LiveNotifyEvent.LiveNotifyBean> {

    private Logger logger = Logger.getLogger(LiveStopNotify.class);

    private volatile static LiveStopNotify inst;

    public static LiveStopNotify instance() {
        if(inst == null) {
            synchronized (LiveStopNotify.class) {
                if (inst == null) {
                    inst = new LiveStopNotify();
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
        sendLiveStopToMServer(LiveNotifyBean);
    }

    @Override
    public void executeCommand4Recv(LiveNotifyEvent.LiveNotifyBean liveNotifyBean) {

    }

    public void executeCommand4Recv(LiveNotifyEvent.LiveNotifyBean LiveNotifyBean,long stopTime) {
        endLocalLiveData(LiveNotifyBean,stopTime);
    }

    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(LiveNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_LIVE_STOP_MSERVER_OK:
                LiveNotifyEvent.LiveNotifyBean liveNotifyBean = sendParam.bean;

                VideoProgram videoProgram = null;
                try {
                    videoProgram = JujuDbUtils.getInstance().selector(VideoProgram.class).where("id","=",liveNotifyBean.getLiveId()).findFirst();
                    videoProgram.setEndTime(new Date(liveNotifyBean.replyTime));
                    videoProgram.setStatus(1);
                    videoProgram.setVideoUrl(liveNotifyBean.getVideoUrl());
                    JujuDbUtils.saveOrUpdate(videoProgram);
                } catch (DbException e) {
                    e.printStackTrace();
                }

                triggerEvent(new LiveNotifyEvent(LiveNotifyEvent.Event.LIVE_STOP_OK,liveNotifyBean));
                break;
            case SEND_LIVE_STOP_MSERVER_FAILED:
//                LiveNotifyEvent failEvent = new LiveNotifyEvent(LiveNotifyEvent.Event
//                        .PARTY_RECRUIT_FAILED, sendParam.bean);
//                triggerEvent(failEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.live_stop_send_failed, 0);
                    }
                });
                break;
        }
    }

    public void sendLiveStopToMServer(final LiveNotifyEvent.LiveNotifyBean liveNotifyBean) {
        String peerId = liveNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(liveNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.LIVE_STOP, uuid, true,
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
                                    .Send.SEND_LIVE_STOP_MSERVER_OK, liveNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(LiveNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_LIVE_STOP_MSERVER_FAILED, liveNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer failed");
                        buildAndTriggerBusinessFlow4Send(LiveNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_LIVE_STOP_MSERVER_FAILED, liveNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify#sendPartyRecruitToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(LiveNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_LIVE_STOP_MSERVER_FAILED, liveNotifyBean);
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
            case PROCESS_LIVE_STOP_OK:
                LiveNotifyEvent externalEvent = new LiveNotifyEvent(LiveNotifyEvent.Event.LIVE_STOP_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case PROCESS_LIVE_STOP_FAIL:
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
    public void endLocalLiveData(final LiveNotifyEvent.LiveNotifyBean liveNotifyBean,final long stopTime) {

        VideoProgram videoProgram = null;
        try {
            videoProgram = JujuDbUtils.getInstance().selector(VideoProgram.class).where("id", "=", liveNotifyBean.getLiveId()).findFirst();
            if(videoProgram == null){
                if(videoProgram==null){
                    LiveStartNotify.instance().addLocalLiveData(liveNotifyBean, false, new LiveStartNotify.CallBack() {
                        @Override
                        public void afterProcessSuccess() {
                            endLocalLiveData(liveNotifyBean,stopTime);
                        }

                        @Override
                        public void afterProcessFail() {
                            buildAndTriggerBusinessFlow4Recv(LiveNotifyEvent.BusinessFlow.RecvParam
                                    .Recv.PROCESS_LIVE_STOP_FAIL, liveNotifyBean);
                        }
                    });
                }
            }else {
                videoProgram.setEndTime(new Date(stopTime));
                videoProgram.setStatus(1);
                videoProgram.setCaptureUrl(liveNotifyBean.getCaptureUrl());
                videoProgram.setVideoUrl(liveNotifyBean.getVideoUrl());
                JujuDbUtils.saveOrUpdate(videoProgram);
            }
        } catch (DbException e) {
            e.printStackTrace();
            buildAndTriggerBusinessFlow4Recv(LiveNotifyEvent.BusinessFlow.RecvParam
                    .Recv.PROCESS_LIVE_STOP_FAIL, liveNotifyBean);
        }

        buildAndTriggerBusinessFlow4Recv(LiveNotifyEvent.BusinessFlow.RecvParam
                .Recv.PROCESS_LIVE_STOP_OK, liveNotifyBean);
    }

    private void buildAndTriggerBusinessFlow4Recv(LiveNotifyEvent.BusinessFlow.RecvParam.Recv recv,LiveNotifyEvent.LiveNotifyBean LiveNotifyBean) {
        LiveNotifyEvent.BusinessFlow.RecvParam recvParam
                = new LiveNotifyEvent.BusinessFlow.RecvParam(recv, LiveNotifyBean);
        triggerEvent(recvParam);
    }
}
