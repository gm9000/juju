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

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
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


    public void start(IMOtherManager imOtherManager) {
        super.start(imOtherManager);
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
        synLocalPartyData(partyNotifyBean,true,null);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(PartyNotifyEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_PARTY_RECRUIT_MSERVER_OK:
//                PartyNotifyEvent externalEvent = new PartyNotifyEvent(PartyNotifyEvent.Event
//                        .PARTY_RECRUIT_OK, sendParam.bean);
//                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.party_recruit_send_success, 0);
                    }
                });
                break;
            case SEND_PARTY_RECRUIT_MSERVER_FAILED:
//                PartyNotifyEvent failEvent = new PartyNotifyEvent(PartyNotifyEvent.Event
//                        .PARTY_RECRUIT_FAILED, sendParam.bean);
//                triggerEvent(failEvent);
                break;
        }
    }

    public void sendPartyRecruitToMServer(final PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        String peerId = partyNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(partyNotifyBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
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
            case ADD_LOCAL_CACHE_DATA_OK:
                PartyNotifyEvent externalEvent = new PartyNotifyEvent(PartyNotifyEvent.Event.PARTY_RECRUIT_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case ADD_LOCAL_CACHE_DATA_FAILED:
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
    public void synLocalPartyData(final PartyNotifyEvent.PartyNotifyBean partyNotifyBean, final boolean notify, final CallBack callBack) {

        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("partyId", partyNotifyBean.getPartyId());
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETPARTYINFO;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj instanceof JSONObject) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    if(status == 0) {
                        try {
                            Party party = new Party();
                            party.setId(jsonRoot.getString("id"));
                            party.setName(jsonRoot.getString("name"));
                            party.setDesc(jsonRoot.getString("partyDesc"));
                            party.setTime(new Date(JSONUtils.getLong(jsonRoot,"time",System.currentTimeMillis())));
                            party.setUserNo(jsonRoot.getString("creatorNo"));
                            party.setGroupId(jsonRoot.getString("groupId"));
                            party.setStatus(jsonRoot.getInt("partyStatus"));
                            party.setCoverUrl(jsonRoot.getString("coverUrl"));
                            party.setNew(true);
                            JujuDbUtils.save(party);

                            JSONArray planJsonArray = jsonRoot.getJSONArray("plans");
                            for(int i=0; i<planJsonArray.length(); i++){
                                JSONObject planJson = planJsonArray.getJSONObject(i);
                                Plan plan = new Plan();
                                plan.setId(planJson.getString("id"));
                                plan.setAddress(planJson.getString("address"));
                                plan.setDesc(planJson.getString("desc"));
                                plan.setStartTime(new Date(JSONUtils.getLong(planJson,"startTime",System.currentTimeMillis())));
                                plan.setLongitude(planJson.getDouble("longitude"));
                                plan.setLatitude(planJson.getDouble("latitude"));
                                plan.setType(planJson.getString("type"));
                                plan.setCoverUrl(planJson.getString("coverUrl"));
                                plan.setPartyId(party.getId());
                                String voters = planJson.getString("voters");
                                String[] voterNos = voters.split(",");
                                plan.setAddtendNum(voterNos.length);
                                JujuDbUtils.save(plan);

                                for(String userNo:voterNos){
                                    PlanVote planVote = new PlanVote();
                                    planVote.setPlanId(plan.getId());
                                    planVote.setAttenderNo(userNo);
                                    JujuDbUtils.save(planVote);
                                }
                            }

                            if(notify) {
                                buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                                        .Recv.ADD_LOCAL_CACHE_DATA_OK, partyNotifyBean);
                            }else{
                                callBack.afterProcessSuccess();
                            }
                        }catch(JSONException e){
                            e.printStackTrace();
                            if(notify) {
                                buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                                        .Recv.ANALYZE_PARTY_INFO_FAILED, partyNotifyBean);
                            }else{
                                callBack.afterProcessFail();
                            }
                        }
                    }else{
                        if(notify) {
                            buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                                    .Recv.GET_PARTY_INFO_FAILED, partyNotifyBean);
                        }else{
                            callBack.afterProcessFail();
                        }
                    }
                }
            }
            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                if(notify) {
                    buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam
                            .Recv.ADD_LOCAL_CACHE_DATA_FAILED, partyNotifyBean);
                }else{
                    callBack.afterProcessFail();
                }
            }
        }, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        } catch (JSONException e) {
            logger.error(e);
        }
    }

    private void buildAndTriggerBusinessFlow4Recv(PartyNotifyEvent.BusinessFlow.RecvParam.Recv recv,PartyNotifyEvent.PartyNotifyBean partyNotifyBean) {
        PartyNotifyEvent.BusinessFlow.RecvParam recvParam
                = new PartyNotifyEvent.BusinessFlow.RecvParam(recv, partyNotifyBean);
        triggerEvent(recvParam);
    }

    public interface CallBack{
        public void afterProcessSuccess();
        public void afterProcessFail();
    }
}
