package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.event.notify.PlanVoteEvent;
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
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;

import java.util.UUID;

public class PlanVoteNotify extends BaseNotify<PlanVoteEvent.PlanVoteBean> {

    private Logger logger = Logger.getLogger(PlanVoteNotify.class);

    private volatile static PlanVoteNotify inst;

    public static PlanVoteNotify instance() {
        if(inst == null) {
            synchronized (PlanVoteNotify.class) {
                if (inst == null) {
                    inst = new PlanVoteNotify();
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
    public void executeCommand4Send(PlanVoteEvent.PlanVoteBean PlanVoteBean) {
        sendPlanVoteToMServer(PlanVoteBean);
    }

    @Override
    public void executeCommand4Recv(PlanVoteEvent.PlanVoteBean PlanVoteBean) {
        processLocalVoteData(PlanVoteBean);
    }


    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(PlanVoteEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_PLAN_VOTE_MSERVER_OK:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.operation_success, 0);
                    }
                });
                break;
            case SEND_PLAN_VOTE_MSERVER_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.operation_fail, 0);
                    }
                });
                break;
        }
    }

    public void sendPlanVoteToMServer(final PlanVoteEvent.PlanVoteBean planVoteBean) {
        String peerId = planVoteBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(planVoteBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.PLAN_VOTE, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("PlanVoteNotify#sendPlanVoteToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            planVoteBean.replyId = id;
                            planVoteBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            imOtherManager.updateGroupNotify(planVoteBean.getGroupId(),replyTime);
                            buildAndTriggerBusinessFlow4Send(PlanVoteEvent.BusinessFlow.SendParam
                                    .Send.SEND_PLAN_VOTE_MSERVER_OK, planVoteBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(PlanVoteEvent.BusinessFlow.SendParam
                                    .Send.SEND_PLAN_VOTE_MSERVER_FAILED, planVoteBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("PlanVoteNotify#sendPlanVoteToMServer failed");
                        buildAndTriggerBusinessFlow4Send(PlanVoteEvent.BusinessFlow.SendParam
                                .Send.SEND_PLAN_VOTE_MSERVER_FAILED, planVoteBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("PlanVoteNotify#sendPlanVoteToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(PlanVoteEvent.BusinessFlow.SendParam
                                .Send.SEND_PLAN_VOTE_MSERVER_FAILED, planVoteBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            PlanVoteEvent.BusinessFlow.SendParam.Send send,
            PlanVoteEvent.PlanVoteBean PlanVoteBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        PlanVoteEvent.BusinessFlow.SendParam sendParam = new PlanVoteEvent.BusinessFlow
                .SendParam(send, PlanVoteBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(PlanVoteEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case PROCESS_LOCAL_CACHE_DATA_OK:
                PlanVoteEvent externalEvent = new PlanVoteEvent(PlanVoteEvent.Event.PLAN_VOTE_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case PROCESS_LOCAL_CACHE_DATA_FAILED:
                break;
            case SYNCHRONIZE_PARTY_DATA_OK:
                PartyNotifyEvent partyNotifyEvent = new PartyNotifyEvent(PartyNotifyEvent.Event.PARTY_RECRUIT_OK);
                triggerEvent(partyNotifyEvent);
                break;
            case SYNCHRONIZE_PARTY_DATA_FAILED:
                break;
        }
    }



    /**
     * 同步本地方案投票数据
     */
    private void processLocalVoteData(final PlanVoteEvent.PlanVoteBean planVoteBean) {

        Plan plan = null;
        try {
            plan = JujuDbUtils.getInstance().selector(Plan.class).where("id", "=", planVoteBean.getPlanId()).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
            buildAndTriggerBusinessFlow4Recv(PlanVoteEvent.BusinessFlow.RecvParam
                    .Recv.PROCESS_LOCAL_CACHE_DATA_FAILED, planVoteBean);
        }

        if(plan==null){
            PartyNotifyEvent.PartyNotifyBean bean = new PartyNotifyEvent.PartyNotifyBean();
            bean.setPartyId(planVoteBean.getPartyId());
            PartyRecruitNotify.instance().synLocalPartyData(bean, false, new PartyRecruitNotify.CallBack() {
                @Override
                public void afterProcessSuccess() {
                    buildAndTriggerBusinessFlow4Recv(PlanVoteEvent.BusinessFlow.RecvParam
                            .Recv.SYNCHRONIZE_PARTY_DATA_OK, planVoteBean);
                }

                @Override
                public void afterProcessFail() {
                    buildAndTriggerBusinessFlow4Recv(PlanVoteEvent.BusinessFlow.RecvParam
                            .Recv.SYNCHRONIZE_PARTY_DATA_FAILED, planVoteBean);
                }
            });
        }

        if(planVoteBean.getVote()==0) {
            WhereBuilder whereBuilder = WhereBuilder.b("attender_no", "=", planVoteBean.getUserNo());
            whereBuilder.and("plan_id", "=", planVoteBean.getPlanId());
            try {
                int count = JujuDbUtils.getInstance().delete(PlanVote.class,whereBuilder);
                if(count>0){
                    plan.setAddtendNum(plan.getAddtendNum()-count);
                    JujuDbUtils.saveOrUpdate(plan);
                }
            } catch (DbException e) {
                e.printStackTrace();
                buildAndTriggerBusinessFlow4Recv(PlanVoteEvent.BusinessFlow.RecvParam
                        .Recv.PROCESS_LOCAL_CACHE_DATA_FAILED, planVoteBean);
            }
        }else{
            PlanVote planVote = null;
            try {
                planVote = JujuDbUtils.getInstance().selector(PlanVote.class).where("attender_no", "=", planVoteBean.getUserNo()).and("plan_id", "=", planVoteBean.getPlanId()).findFirst();
            } catch (DbException e) {
                e.printStackTrace();
            }
            if(planVote == null) {
                planVote = new PlanVote();
                planVote.setPlanId(planVoteBean.getPlanId());
                planVote.setAttenderNo(planVoteBean.getUserNo());
                JujuDbUtils.save(planVote);
                plan.setAddtendNum(plan.getAddtendNum() + 1);
                JujuDbUtils.saveOrUpdate(plan);
            }

        }

        buildAndTriggerBusinessFlow4Recv(PlanVoteEvent.BusinessFlow.RecvParam
                        .Recv.PROCESS_LOCAL_CACHE_DATA_OK, planVoteBean);
    }

    private void buildAndTriggerBusinessFlow4Recv(PlanVoteEvent.BusinessFlow.RecvParam.Recv recv,PlanVoteEvent.PlanVoteBean PlanVoteBean) {
        PlanVoteEvent.BusinessFlow.RecvParam recvParam
                = new PlanVoteEvent.BusinessFlow.RecvParam(recv, PlanVoteBean);
        triggerEvent(recvParam);
    }
}
