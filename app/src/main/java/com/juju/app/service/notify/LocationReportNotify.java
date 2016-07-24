package com.juju.app.service.notify;

import com.juju.app.event.notify.LocationReportEvent;
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

public class LocationReportNotify extends BaseNotify<LocationReportEvent.LocationReportBean> {

    private Logger logger = Logger.getLogger(LocationReportNotify.class);

    private volatile static LocationReportNotify inst;

    public static LocationReportNotify instance() {
        if(inst == null) {
            synchronized (LocationReportNotify.class) {
                if (inst == null) {
                    inst = new LocationReportNotify();
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
    public void executeCommand4Send(LocationReportEvent.LocationReportBean LocationReportBean) {
        sendLocationReportToMServer(LocationReportBean);
    }

    @Override
    public void executeCommand4Recv(LocationReportEvent.LocationReportBean LocationReportBean) {
        processLocalLocationData(LocationReportBean);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(LocationReportEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_LOCATION_REPORT_MSERVER_OK:
                break;
            case SEND_LOCATION_REPORT_MSERVER_FAILED:
                break;
        }
    }

    public void sendLocationReportToMServer(final LocationReportEvent.LocationReportBean LocationReportBean) {
        String peerId = LocationReportBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(LocationReportBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.LOCATION_REPORT, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("LocationReportNotiry#sendLocationReportToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            LocationReportBean.replyId = id;
                            LocationReportBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(LocationReportEvent.BusinessFlow.SendParam
                                    .Send.SEND_LOCATION_REPORT_MSERVER_OK, LocationReportBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(LocationReportEvent.BusinessFlow.SendParam
                                    .Send.SEND_LOCATION_REPORT_MSERVER_FAILED, LocationReportBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("LocationReportNotiry#sendLocationReportToMServer failed");
                        buildAndTriggerBusinessFlow4Send(LocationReportEvent.BusinessFlow.SendParam
                                .Send.SEND_LOCATION_REPORT_MSERVER_FAILED, LocationReportBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("LocationReportNotiry#sendLocationReportToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(LocationReportEvent.BusinessFlow.SendParam
                                .Send.SEND_LOCATION_REPORT_MSERVER_FAILED, LocationReportBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            LocationReportEvent.BusinessFlow.SendParam.Send send,
            LocationReportEvent.LocationReportBean LocationReportBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        LocationReportEvent.BusinessFlow.SendParam sendParam = new LocationReportEvent.BusinessFlow
                .SendParam(send, LocationReportBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(LocationReportEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case PROCESS_LOCAL_CACHE_DATA_OK:
                LocationReportEvent externalEvent = new LocationReportEvent(LocationReportEvent.Event.LOCATION_REPORT_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
        }
    }



    /**
     * 同步本地方案投票数据
     */
    private void processLocalLocationData(final LocationReportEvent.LocationReportBean LocationReportBean) {

        buildAndTriggerBusinessFlow4Recv(LocationReportEvent.BusinessFlow.RecvParam
                        .Recv.PROCESS_LOCAL_CACHE_DATA_OK, LocationReportBean);
    }

    private void buildAndTriggerBusinessFlow4Recv(LocationReportEvent.BusinessFlow.RecvParam.Recv recv,LocationReportEvent.LocationReportBean LocationReportBean) {
        LocationReportEvent.BusinessFlow.RecvParam recvParam
                = new LocationReportEvent.BusinessFlow.RecvParam(recv, LocationReportBean);
        triggerEvent(recvParam);
    }
}
