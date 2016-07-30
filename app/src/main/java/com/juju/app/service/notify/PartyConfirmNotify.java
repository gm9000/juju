package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.entity.Party;
import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.ex.DbException;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class PartyConfirmNotify extends BaseNotify<PartyNotifyEvent.PartyNotifyBean> {

    private Logger logger = Logger.getLogger(PartyConfirmNotify.class);

    private volatile static PartyConfirmNotify inst;

    public static PartyConfirmNotify instance() {
        if(inst == null) {
            synchronized (PartyConfirmNotify.class) {
                if (inst == null) {
                    inst = new PartyConfirmNotify();
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
    public void executeCommand4Send(PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        sendPartyConfirmToMServer(partyNotifyBean);
    }

    @Override
    public void executeCommand4Recv(PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        confirmLocalPartyData(partyNotifyBean);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(PartyNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_PARTY_CONFIRM_MSERVER_OK:
                PartyNotifyEvent externalEvent = new PartyNotifyEvent(PartyNotifyEvent.Event
                        .PARTY_CONFIRM_OK, sendParam.bean);
                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.party_confirm_send_success, 0);
                    }
                });
                break;
            case SEND_PARTY_CONFIRM_MSERVER_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.party_confirm_send_failed, 0);
                    }
                });
                break;
        }
    }

    public void  sendPartyConfirmToMServer(final PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        String peerId = partyNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(partyNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.PARTY_CONFIRM, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("PartyConfirmNotify# sendPartyConfirmToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            partyNotifyBean.replyId = id;
                            partyNotifyBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            imOtherManager.updateGroupNotify(partyNotifyBean.getGroupId(),replyTime);
                            buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_PARTY_CONFIRM_MSERVER_OK, partyNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_PARTY_CONFIRM_MSERVER_FAILED, partyNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify# sendPartyConfirmToMServer failed");
                        buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_PARTY_CONFIRM_MSERVER_FAILED, partyNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify# sendPartyConfirmToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_PARTY_CONFIRM_MSERVER_FAILED, partyNotifyBean);
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
                PartyNotifyEvent externalEvent = new PartyNotifyEvent(PartyNotifyEvent.Event.PARTY_CONFIRM_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextStringToast(context,"confirm party update data failue",0);
                    }
                });
                break;
        }
    }



    /**
     * 同步本地聚会方案数据
     */
    private void confirmLocalPartyData(final PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {

        try {
            Plan plan = JujuDbUtils.getInstance().selector(Plan.class).where("id", "=", partyNotifyBean.getPlanId()).findFirst();
            if(plan==null){
                PartyRecruitNotify.instance().synLocalPartyData(partyNotifyBean, false, new PartyRecruitNotify.CallBack() {
                    @Override
                    public void afterProcessSuccess() {
                        buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                                .Recv.UPDATE_LOCAL_CACHE_DATA_OK, partyNotifyBean);
                    }

                    @Override
                    public void afterProcessFail() {
                        buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                                .Recv.UPDATE_LOCAL_CACHE_DATA_FAILED, partyNotifyBean);
                    }
                });
            }else {
                plan.setStatus(1);
                JujuDbUtils.saveOrUpdate(plan);

                Party party = JujuDbUtils.getInstance().selector(Party.class).where("id", "=", partyNotifyBean.getPartyId()).findFirst();
                party.setStatus(1);
                party.setCoverUrl(plan.getCoverUrl());
                party.setTime(plan.getStartTime());
                JujuDbUtils.saveOrUpdate(party);

                buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                        .Recv.UPDATE_LOCAL_CACHE_DATA_OK, partyNotifyBean);
            }

        } catch (DbException e) {
            e.printStackTrace();
            buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                    .Recv.UPDATE_LOCAL_CACHE_DATA_FAILED, partyNotifyBean);
        }

    }

    private void buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam.Recv recv,PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        PartyNotifyEvent.BusinessFlow.RecvParam recvParam
                = new PartyNotifyEvent.BusinessFlow.RecvParam(recv, partyNotifyBean);
        triggerEvent(recvParam);
    }
}
