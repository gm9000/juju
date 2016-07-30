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
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class PartyCancelNotify extends BaseNotify<PartyNotifyEvent.PartyNotifyBean> {

    private Logger logger = Logger.getLogger(PartyCancelNotify.class);

    private volatile static PartyCancelNotify inst;

    public static PartyCancelNotify instance() {
        if(inst == null) {
            synchronized (PartyCancelNotify.class) {
                if (inst == null) {
                    inst = new PartyCancelNotify();
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
        sendPartyCancelToMServer(partyNotifyBean);
    }

    @Override
    public void executeCommand4Recv(PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        clearLocalPartyData(partyNotifyBean);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(PartyNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_PARTY_CANCEL_MSERVER_OK:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.party_cancel_send_success, 0);
                    }
                });
                break;
            case SEND_PARTY_CANCEL_MSERVER_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.party_cancel_send_failed, 0);
                    }
                });
                break;
        }
    }

    public void sendPartyCancelToMServer(final PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        String peerId = partyNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(partyNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.PARTY_CANCEL, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("PartyCancelNotify#sendPartyConfirmToMServer success");
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
                                    .Send.SEND_PARTY_CANCEL_MSERVER_OK, partyNotifyBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                    .Send.SEND_PARTY_CANCEL_MSERVER_FAILED, partyNotifyBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PartyRecruitNotify#sendPartyConfirmToMServer failed");
                        buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_PARTY_CANCEL_MSERVER_FAILED, partyNotifyBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PartyRecruitNotify#sendPartyConfirmToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(PartyNotifyEvent.BusinessFlow.SendParam
                                .Send.SEND_PARTY_CANCEL_MSERVER_FAILED, partyNotifyBean);
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
            case CLEAR_LOCAL_CACHE_DATA_OK:
                PartyNotifyEvent externalEvent = new PartyNotifyEvent(PartyNotifyEvent.Event.PARTY_CANCEL_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case CLEAR_LOCAL_CACHE_DATA_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextStringToast(context,"clear local party data failue",0);
                    }
                });
                break;
        }
    }



    /**
     * 同步本地聚会方案数据
     */
    private void clearLocalPartyData(final PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        String delSql = "delete from plan_vote where plan_id in (select id from plan where party_id=\""+partyNotifyBean.getPartyId()+"\")";
        try {
            JujuDbUtils.getInstance().execNonQuery(delSql);
            JujuDbUtils.getInstance().delete(Plan.class, WhereBuilder.b("party_id", "=", partyNotifyBean.getPartyId()));
            JujuDbUtils.getInstance().delete(Party.class, WhereBuilder.b("id", "=", partyNotifyBean.getPartyId()));
            JujuDbUtils.openRefresh(Party.class);

            buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                    .Recv.CLEAR_LOCAL_CACHE_DATA_OK, partyNotifyBean);
        } catch (DbException e) {
            e.printStackTrace();
            buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                    .Recv.CLEAR_LOCAL_CACHE_DATA_FAILED, partyNotifyBean);
        }


    }

    private void buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam.Recv recv,PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        PartyNotifyEvent.BusinessFlow.RecvParam recvParam
                = new PartyNotifyEvent.BusinessFlow.RecvParam(recv, partyNotifyBean);
        triggerEvent(recvParam);
    }
}
