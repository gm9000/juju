package com.juju.app.service.im.manager;

import android.content.Context;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.InviteDaoImpl;
import com.juju.app.biz.impl.OtherMessageDaoImpl;
import com.juju.app.entity.Invite;
import com.juju.app.entity.chat.OtherMessageEntity;
import com.juju.app.event.NotificationMessageEvent;
import com.juju.app.event.NotifyMessageEvent;
import com.juju.app.event.notify.ApplyInGroupEvent;
import com.juju.app.event.notify.ExitGroupEvent;
import com.juju.app.event.notify.InviteInGroupEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.event.notify.LiveNotifyEvent;
import com.juju.app.event.notify.LocationReportEvent;
import com.juju.app.event.notify.MasterTransferEvent;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.event.notify.PlanVoteEvent;
import com.juju.app.event.notify.RemoveGroupEvent;
import com.juju.app.event.notify.SeizeNotifyEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.notify.ApplyInGroupNotify;
import com.juju.app.service.notify.ExitGroupNotify;
import com.juju.app.service.notify.InviteInGroupNotify;
import com.juju.app.service.notify.InviteUserNotify;
import com.juju.app.service.notify.LiveSeizeReportNotify;
import com.juju.app.service.notify.LiveSeizeStartNotify;
import com.juju.app.service.notify.LiveSeizeStopNotify;
import com.juju.app.service.notify.LiveStartNotify;
import com.juju.app.service.notify.LiveStopNotify;
import com.juju.app.service.notify.LocationReportNotify;
import com.juju.app.service.notify.MasterTransferNotify;
import com.juju.app.service.notify.PartyCancelNotify;
import com.juju.app.service.notify.PartyConfirmNotify;
import com.juju.app.service.notify.PartyRecruitNotify;
import com.juju.app.service.notify.PlanVoteNotify;
import com.juju.app.service.notify.RemoveGroupNotify;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;

import org.apache.commons.lang.math.NumberUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.packet.Message;

/**
 * 项目名称：juju
 * 类描述：处理非聊天相关的事件
 * 创建人：gm
 * 日期：2016/5/6 11:30
 * 版本：V1.0.0
 */
public class IMOtherManager extends IMManager {

    private Logger logger = Logger.getLogger(IMOtherManager.class);

    private volatile static IMOtherManager inst;

    private DaoSupport otherMessageDao;

    private DaoSupport inviteDao;

    private UserInfoBean userInfoBean;


    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMOtherManager instance() {
        if(inst == null) {
            synchronized (IMOtherManager.class) {
                if (inst == null) {
                    inst = new IMOtherManager();
                }
            }
        }
        return inst;
    }



    @Override
    public void doOnStart() {
        userInfoBean = AppContext.getUserInfoBean();
    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        socketService = null;
        otherMessageDao = null;
        inviteDao = null;
        EventBus.getDefault().unregister(inst);
        InviteUserNotify.instance().stop();
        InviteInGroupNotify.instance().stop();
        RemoveGroupNotify.instance().stop();
        MasterTransferNotify.instance().stop();
        ExitGroupNotify.instance().stop();
        ApplyInGroupNotify.instance().stop();
        PartyRecruitNotify.instance().stop();
        PartyCancelNotify.instance().stop();
        PartyConfirmNotify.instance().stop();
        PlanVoteNotify.instance().stop();
        LocationReportNotify.instance().stop();
        LiveStartNotify.instance().stop();
        LiveStopNotify.instance().stop();
        LiveSeizeStartNotify.instance().stop();
        LiveSeizeStopNotify.instance().stop();
        LiveSeizeReportNotify.instance().stop();
    }

    //网络登陆
    public void onNormalLoginOk(){
        onLocalLoginOk();
        onLocalNetOk();
    }


    //本地登陆
    public void onLocalLoginOk(){
        if (!EventBus.getDefault().isRegistered(inst)) {
            EventBus.getDefault().register(inst);
        }
    }


    //断线重连
    public void onLocalNetOk() {

    }

    public SocketService getSocketService() {
        return socketService;
    }

    public Context getContext() {
        return ctx;
    }

    public UserInfoBean getUserInfoBean() {
        return userInfoBean;
    }

    /**
     * 初始化DAO和服务(退出登录后或者第一次加载需要初始化)
     */
    public void initDaoAndService() {
        if (otherMessageDao == null) {
            otherMessageDao = new OtherMessageDaoImpl(ctx);
        }
        if (inviteDao == null) {
            inviteDao = new InviteDaoImpl(ctx);
        }
        InviteUserNotify.instance().start(this, IMGroupManager.instance(), IMSessionManager.instance());
        InviteInGroupNotify.instance().start(this, IMGroupManager.instance(), IMContactManager.instance(), IMSessionManager.instance());
        RemoveGroupNotify.instance().start(this, IMGroupManager.instance(), IMContactManager.instance());
        MasterTransferNotify.instance().start(this, IMGroupManager.instance());
        PartyRecruitNotify.instance().start(this);
        PartyCancelNotify.instance().start(this);
        PartyConfirmNotify.instance().start(this);
        PlanVoteNotify.instance().start(this);
        LocationReportNotify.instance().start(this);
        ExitGroupNotify.instance().start(this, IMGroupManager.instance());
        ApplyInGroupNotify.instance().start(this, IMGroupManager.instance(), IMContactManager.instance());
        LiveStartNotify.instance().start(this);
        LiveStopNotify.instance().start(this);
        LiveSeizeStartNotify.instance().start(this);
        LiveSeizeStopNotify.instance().start(this);
        LiveSeizeReportNotify.instance().start(this);
    }

    /**
     * 保存通知消息
     * @param message
     * @param notifyType
     * @param uuid
     * @param reqEntity
     */
    public void saveOtherMessage(Message message, IMBaseDefine.NotifyType notifyType, String uuid,
                                 Object... reqEntity) {
        OtherMessageEntity otherMessageEntity = OtherMessageEntity
                .buildMessage4Send(notifyType, message, uuid);
        switch (notifyType) {
//            case INVITE_USER:
//                Invite inviteReq = Invite.buildInviteReq4Send(otherMessageEntity);
//                inviteDao.save(inviteReq);
//                break;
            default:
                otherMessageDao.save(otherMessageEntity);
                break;
        }
    }

    /**
     * 修改通知消息
     * @param id
     * @param replayTime
     */
    public void updateOtherMessage(String id, long replayTime) {
        OtherMessageEntity entityDb = (OtherMessageEntity) otherMessageDao.findUniByProperty("id", id);
        if(entityDb != null) {
            OtherMessageEntity.buildMessage4SendOnAck(entityDb, replayTime);
            otherMessageDao.update(entityDb);
        }
    }

//    /**
//     * 获取账号详情
//     * @param targetNo
//     */
//    public void getAccountInfo(String targetNo) {
//
//    }



    /**
     * 处理XMPP通知消息
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.POSTING,  priority = Constants.SERVICE_EVENTBUS_PRIORITY, sticky = true)
    public void onNotifyMessage4Event(NotifyMessageEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        OtherMessageEntity otherMessageEntity = OtherMessageEntity.buildMessage4Recv(event.notifyType,
                event.message, event.message.getStanzaId());
        NotificationMessageEvent notificationMessageEvent = new NotificationMessageEvent();
        notificationMessageEvent.entity = otherMessageEntity;
        switch (event.notifyType) {
            case INVITE_USER:
                //TODO inviteDao 是否保留
                Invite inviteReq = Invite.buildInviteReq4Recv(otherMessageEntity);
                inviteDao.save(inviteReq);
                //发送系统通知
                notificationMessageEvent.event = NotificationMessageEvent.Event.INVITE_USER_RECEIVED;
                triggerEvent4Sticky(notificationMessageEvent);
                InviteUserEvent.InviteUserBean inviteUserBean = (InviteUserEvent.InviteUserBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
                                IMBaseDefine.NotifyType.INVITE_USER.getCls());
                if(NumberUtils.isNumber(event.message.getThread())) {
                    inviteUserBean.replyId = event.message.getStanzaId();
                    inviteUserBean.replyTime = Long.parseLong(event.message.getThread());
                }
                InviteUserNotify.instance().executeCommand4Recv(inviteUserBean);
                break;
            case INVITE_IN_GROUP:
                InviteInGroupEvent.InviteInGroupBean inviteInGroupBean = (InviteInGroupEvent.InviteInGroupBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.INVITE_IN_GROUP.getCls());
                InviteInGroupNotify.instance().executeCommand4Recv(inviteInGroupBean);
                break;
            case REMOVE_GROUP:
                //发送系统通知
                notificationMessageEvent.event = NotificationMessageEvent.Event.REMOVE_GROUP_RECEIVED;
                triggerEvent4Sticky(notificationMessageEvent);
                RemoveGroupEvent.RemoveGroupBean removeGroupBean = (RemoveGroupEvent.RemoveGroupBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.REMOVE_GROUP.getCls());
                RemoveGroupNotify.instance().executeCommand4Recv(removeGroupBean);
                break;
            case MASTER_TRANSFER:
                //不需要系统通知
                MasterTransferEvent.MasterTransferBean masterTransferBean = (MasterTransferEvent.MasterTransferBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.MASTER_TRANSFER.getCls());
                MasterTransferNotify.instance().executeCommand4Recv(masterTransferBean);
                break;
            case APPLY_IN_GROUP:
                ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean = (ApplyInGroupEvent.ApplyInGroupBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.APPLY_IN_GROUP.getCls());
                if(NumberUtils.isNumber(event.message.getThread())) {
                    applyInGroupBean.replyId = event.message.getStanzaId();
                    applyInGroupBean.replyTime = Long.parseLong(event.message.getThread());
                }
                ApplyInGroupNotify.instance().executeCommand4Recv(applyInGroupBean);
                break;
            //退出群组通知
            case EXIT_GROUP:
                ExitGroupEvent.ExitGroupBean exitGroupBean = (ExitGroupEvent.ExitGroupBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.EXIT_GROUP.getCls());
                if(NumberUtils.isNumber(event.message.getThread())) {
                    exitGroupBean.replyId = event.message.getStanzaId();
                    exitGroupBean.replyTime = Long.parseLong(event.message.getThread());
                }
                ExitGroupNotify.instance().executeCommand4Recv(exitGroupBean);
                break;
            case PARTY_RECRUIT:
                PartyNotifyEvent.PartyNotifyBean partyRecruitBean = (PartyNotifyEvent.PartyNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.PARTY_RECRUIT.getCls());
                PartyRecruitNotify.instance().executeCommand4Recv(partyRecruitBean);
                break;
            case PARTY_CANCEL:
                PartyNotifyEvent.PartyNotifyBean partyCancelBean = (PartyNotifyEvent.PartyNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.PARTY_CANCEL.getCls());
                PartyCancelNotify.instance().executeCommand4Recv(partyCancelBean);
                break;
            case PARTY_CONFIRM:
                PartyNotifyEvent.PartyNotifyBean partyConfirmBean = (PartyNotifyEvent.PartyNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.PARTY_CONFIRM.getCls());
                PartyConfirmNotify.instance().executeCommand4Recv(partyConfirmBean);
                break;
            case PLAN_VOTE:
                PlanVoteEvent.PlanVoteBean planVoteBean = (PlanVoteEvent.PlanVoteBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.PLAN_VOTE.getCls());
                PlanVoteNotify.instance().executeCommand4Recv(planVoteBean);
                break;
            case LOCATION_REPORT:
                LocationReportEvent.LocationReportBean locationReportBean = (LocationReportEvent.LocationReportBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.LOCATION_REPORT.getCls());
                LocationReportNotify.instance().executeCommand4Recv(locationReportBean);
                break;
            case LIVE_START:
                LiveNotifyEvent.LiveNotifyBean liveNotifyBean = (LiveNotifyEvent.LiveNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.LIVE_START.getCls());
                LiveStartNotify.instance().executeCommand4Recv(liveNotifyBean);
                break;
            case LIVE_STOP:
                LiveNotifyEvent.LiveNotifyBean liveStopNotifyBean = (LiveNotifyEvent.LiveNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.LIVE_STOP.getCls());
                LiveStopNotify.instance().executeCommand4Recv(liveStopNotifyBean);
                break;
            case LIVE_RELAY_START:
                SeizeNotifyEvent.SeizeNotifyBean seizeStartNotifyBean = (SeizeNotifyEvent.SeizeNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.LIVE_RELAY_START.getCls());
                LiveSeizeStartNotify.instance().executeCommand4Recv(seizeStartNotifyBean);
                break;
            case LIVE_RELAY:
                SeizeNotifyEvent.SeizeNotifyBean seizeStopNotifyBean = (SeizeNotifyEvent.SeizeNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.LIVE_RELAY.getCls());
                LiveSeizeStopNotify.instance().executeCommand4Recv(seizeStopNotifyBean);
                break;
            case RELAY_COUNT:
                SeizeNotifyEvent.SeizeNotifyBean seizeReportNotifyBean = (SeizeNotifyEvent.SeizeNotifyBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(), IMBaseDefine.NotifyType.RELAY_COUNT.getCls());
                LiveSeizeReportNotify.instance().executeCommand4Recv(seizeReportNotifyBean);
                break;
            default:
                otherMessageDao.replaceInto(otherMessageEntity);
                break;
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEvent4BusinessFlowRecvEvent(InviteUserEvent.BusinessFlow.RecvParam recvParam) {
//        switch (recvParam.recv) {
//            case SEND_GET_GROUP_INFO_BSERVER_OK:
//                inviteUserTask.sendGetGroupUsersToBServer4Recv(recvParam.bean, recvParam.groupId,
//                        recvParam.groupName, recvParam.desc, recvParam.creatorNo, recvParam.masterNo,
//                        recvParam.createTimeDate);
//                break;
//            case SEND_GET_GROUP_USERS_BSERVER_OK:
//                inviteUserTask.sendJoinChatRoomToMServer4Recv();
//                break;
//            case JOIN_CHAT_ROOM_MSERVER_OK:
//                //不需要打开会话窗口
//
//                break;
//            case SEND_GET_GROUP_INFO_BSERVER_FAILED:
//            case SEND_GET_GROUP_USERS_BSERVER_FAILED:
//                ToastUtil.TextIntToast(ctx, R.string.invite_user_send_failed, 3);
//                break;
//        }
//    }





}
