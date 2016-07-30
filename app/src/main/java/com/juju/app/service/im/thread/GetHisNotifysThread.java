package com.juju.app.service.im.thread;

import com.juju.app.entity.notify.GroupNotifyEntity;
import com.juju.app.event.notify.LiveNotifyEvent;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.event.notify.PlanVoteEvent;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.iq.RedisResIQ;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.notify.LiveCaptureNotify;
import com.juju.app.service.notify.LiveStartNotify;
import com.juju.app.service.notify.LiveStopNotify;
import com.juju.app.service.notify.PartyCancelNotify;
import com.juju.app.service.notify.PartyConfirmNotify;
import com.juju.app.service.notify.PartyEndNotify;
import com.juju.app.service.notify.PartyRecruitNotify;
import com.juju.app.service.notify.PlanVoteNotify;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.ex.DbException;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * 项目名称：juju
 * 类描述：合并消息线程
 * 创建人：gm
 * 日期：2016/5/4 11:49
 * 版本：V1.0.0
 */
public class GetHisNotifysThread implements Runnable{

    private final String COMMAND = "zrangebyscore";

    private Logger logger = Logger.getLogger(GetHisNotifysThread.class);

    //计数器
    private CountDownLatch countDownLatch;

    //聊天室ID
    private String chatRoomId;

    private SocketService socketService;

    private long time;

    private int length = Integer.MAX_VALUE;

    private Object obj = new Object();

    public GetHisNotifysThread(String chatRoomId, long time, SocketService socketService) {
        this.chatRoomId = chatRoomId;
        this.time = time;
        this.socketService = socketService;
    }

    public GetHisNotifysThread(CountDownLatch countDownLatch, String chatRoomId,
                               long time, SocketService socketService) {
        this.countDownLatch = countDownLatch;
        this.chatRoomId = chatRoomId;
        this.time = time;
        this.socketService = socketService;
    }

    @Override
    public void run() {
       execute();
    }

    /**
     * 执行查询
     */
    private void execute() {
        //获取最近未接收的通知
        String uuid = UUID.randomUUID().toString();
        socketService.findHisNotifys(COMMAND, chatRoomId, String.valueOf(time), "",
                uuid, 0, length, new XMPPServiceCallbackImpl(1) {
                    @Override
                    public void onSuccess(Object t) {
                        RedisResIQ redisResIQ = (RedisResIQ) t;

                        if(StringUtils.isNotBlank(redisResIQ.getContent())) {
                            JSONObject jsonBody = null;
                            try {
                                jsonBody = new JSONObject(redisResIQ.getContent());

                                String bodyArray = jsonBody.getString("body");
                                JSONArray jsonArray = new JSONArray(bodyArray);
                                if(jsonArray == null || jsonArray.length() == 0) {
                                    return;
                                }
                                String body = jsonArray.getString(0);
                                String code = jsonBody.getString("code");
                                String thread = jsonBody.getString("thread");
                                if(StringUtils.isBlank(code)){
                                    return;
                                }else{
                                    //  聚会发起
                                    if(code.equals(IMBaseDefine.NotifyType.PARTY_RECRUIT.code())){
                                        processNotify(IMBaseDefine.NotifyType.PARTY_RECRUIT,body,Long.parseLong(thread));
                                    }
                                    //  聚会取消
                                    else if(code.equals(IMBaseDefine.NotifyType.PARTY_CANCEL.code())){
                                        processNotify(IMBaseDefine.NotifyType.PARTY_CANCEL,body,Long.parseLong(thread));
                                    }
                                    //  聚会启动
                                    else if(code.equals(IMBaseDefine.NotifyType.PARTY_CONFIRM.code())){
                                        processNotify(IMBaseDefine.NotifyType.PARTY_CONFIRM,body,Long.parseLong(thread));
                                    }
                                    //  聚会结束
                                    else if(code.equals(IMBaseDefine.NotifyType.PARTY_END.code())){
                                        processNotify(IMBaseDefine.NotifyType.PARTY_END,body,Long.parseLong(thread));
                                    }
                                    //  方案投票
                                    else if(code.equals(IMBaseDefine.NotifyType.PLAN_VOTE.code())){
                                        processNotify(IMBaseDefine.NotifyType.PLAN_VOTE,body,Long.parseLong(thread));
                                    }
                                    //  直播开始
                                    else if(code.equals(IMBaseDefine.NotifyType.LIVE_START.code())){
                                        processNotify(IMBaseDefine.NotifyType.LIVE_START,body,Long.parseLong(thread));
                                    }
                                    //  直播截屏
                                    else if(code.equals(IMBaseDefine.NotifyType.LIVE_CAPTURE.code())){
                                        processNotify(IMBaseDefine.NotifyType.LIVE_CAPTURE,body,Long.parseLong(thread));
                                    }
                                    //  直播停止
                                    else if(code.equals(IMBaseDefine.NotifyType.LIVE_STOP.code())){
                                        processNotify(IMBaseDefine.NotifyType.LIVE_STOP,body,Long.parseLong(thread));
                                    }else{

                                    }
                                }


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        synchronized (obj) {
                            obj.notify();
                        }

                    }


                    /**
                     * 消息异常
                     */
                    @Override
                    public void onFailed() {
                        logger.i("MergeMessageThread#run is failed");
                        synchronized (obj) {
                            obj.notify();
                        }
                    }

                    /**
                     * 消息超时
                     */
                    @Override
                    public void onTimeout() {
                        logger.i("MergeMessageThread#run is timeout");
                        synchronized (obj) {
                            obj.notify();
                        }
                    }
                });

        try {
            System.out.println("before wait...............");
            //最长等待时间12秒
            synchronized (obj) {
                obj.wait(12000l);
            }
            System.out.println("end wait...............");
        } catch (InterruptedException e) {
            logger.error(e);
        }
    }

    private void processNotify(IMBaseDefine.NotifyType notifyType,String content,long time){

        switch (notifyType){
            case PARTY_RECRUIT:
                PartyNotifyEvent.PartyNotifyBean partyRecruitBean = (PartyNotifyEvent.PartyNotifyBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.PARTY_RECRUIT.getCls());
                PartyRecruitNotify.instance().executeCommand4Recv(partyRecruitBean);
                updateGroupNotify(partyRecruitBean.getGroupId(),time);
                break;
            case PARTY_CANCEL:
                PartyNotifyEvent.PartyNotifyBean partyCancelBean = (PartyNotifyEvent.PartyNotifyBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.PARTY_CANCEL.getCls());
                PartyCancelNotify.instance().executeCommand4Recv(partyCancelBean);
                updateGroupNotify(partyCancelBean.getGroupId(),time);
                break;
            case PARTY_CONFIRM:
                PartyNotifyEvent.PartyNotifyBean partyConfirmBean = (PartyNotifyEvent.PartyNotifyBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.PARTY_CONFIRM.getCls());
                PartyConfirmNotify.instance().executeCommand4Recv(partyConfirmBean);
                updateGroupNotify(partyConfirmBean.getGroupId(),time);
                break;
            case PARTY_END:
                PartyNotifyEvent.PartyNotifyBean partyEndBean = (PartyNotifyEvent.PartyNotifyBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.PARTY_END.getCls());
                PartyEndNotify.instance().executeCommand4Recv(partyEndBean);
                updateGroupNotify(partyEndBean.getGroupId(),time);
                break;
            case PLAN_VOTE:
                PlanVoteEvent.PlanVoteBean planVoteBean = (PlanVoteEvent.PlanVoteBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.PLAN_VOTE.getCls());
                PlanVoteNotify.instance().executeCommand4Recv(planVoteBean);
                updateGroupNotify(planVoteBean.getGroupId(),time);
                break;
            case LIVE_START:
                LiveNotifyEvent.LiveNotifyBean liveNotifyBean = (LiveNotifyEvent.LiveNotifyBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.LIVE_START.getCls());
                LiveStartNotify.instance().executeCommand4Recv(liveNotifyBean);
                updateGroupNotify(liveNotifyBean.getGroupId(),time);
                break;
            case LIVE_CAPTURE:
                LiveNotifyEvent.LiveNotifyBean liveCaptureNotifyBean = (LiveNotifyEvent.LiveNotifyBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.LIVE_CAPTURE.getCls());
                LiveCaptureNotify.instance().executeCommand4Recv(liveCaptureNotifyBean);
                updateGroupNotify(liveCaptureNotifyBean.getGroupId(),time);
                break;
            case LIVE_STOP:
                LiveNotifyEvent.LiveNotifyBean liveStopNotifyBean = (LiveNotifyEvent.LiveNotifyBean)
                        JacksonUtil.turnString2Obj(content, IMBaseDefine.NotifyType.LIVE_STOP.getCls());
                LiveStopNotify.instance().executeCommand4Recv(liveStopNotifyBean,time);
                updateGroupNotify(liveStopNotifyBean.getGroupId(),time);
                break;
        }
    }

    private void updateGroupNotify(String groupId,long time){
        try {
            GroupNotifyEntity groupNotifyEntity = JujuDbUtils.getInstance().selector(GroupNotifyEntity.class).where("id","=",groupId).findFirst();
            if(groupNotifyEntity == null){
               return;
            }
            if(groupNotifyEntity.getTime()<time) {
                groupNotifyEntity.setTime(time);
                JujuDbUtils.getInstance().saveOrUpdate(groupNotifyEntity);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

}
