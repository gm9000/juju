package com.juju.app.service.im.manager;

import com.juju.app.R;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.biz.impl.InviteDaoImpl;
import com.juju.app.biz.impl.OtherMessageDaoImpl;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.entity.Invite;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.OtherMessageEntity;
import com.juju.app.event.NotificationMessageEvent;
import com.juju.app.event.NotifyMessageEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.event.user.InviteGroupEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.service.notify.InviteUserNotify;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
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
        InviteUserNotify.instance().start(ctx, IMGroupManager.instance(), userInfoBean,
                socketService, inviteDao);
    }

    //TODO 需要使用新线程（不确定xutils 是否对数据存储这块进行了优化）
    public void saveOtherMessage(Message message, IMBaseDefine.NotifyType notifyType, String uuid,
                                 Object... reqEntity) {
        OtherMessageEntity otherMessageEntity = OtherMessageEntity
                .buildMessage4Send(notifyType, message, uuid);
        switch (notifyType) {
            case INVITE_USER:
                Invite inviteReq = Invite.buildInviteReq4Send(otherMessageEntity);
                inviteDao.save(inviteReq);
                break;
            default:
                otherMessageDao.save(otherMessageEntity);
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING,  priority = Constants.SERVICE_EVENTBUS_PRIORITY)
    public void onNotifyMessage4Event(NotifyMessageEvent event) {
        OtherMessageEntity otherMessageEntity = OtherMessageEntity.buildMessage4Recv(event.notifyType,
                event.message, event.message.getStanzaId());
        NotificationMessageEvent notificationMessageEvent = new NotificationMessageEvent();
        notificationMessageEvent.entity = otherMessageEntity;
        switch (event.notifyType) {
            case INVITE_USER:
                //TODO inviteDao 是否保留
                Invite inviteReq = Invite.buildInviteReq4Recv(otherMessageEntity);
                inviteDao.save(inviteReq);

                InviteUserEvent.InviteUserBean inviteUserBean = (InviteUserEvent.InviteUserBean)
                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
                                IMBaseDefine.NotifyType.INVITE_USER.getCls());
//                inviteUserTask.executeCommand4Recv(inviteUserBean);

                //发送系统通知
                notificationMessageEvent.event = NotificationMessageEvent.Event.INVITE_USER_RECEIVED;
                triggerEvent4Sticky(notificationMessageEvent);
                break;
//            case INVITE_GROUP_NOTIFY_RES:
//                IMBaseDefine.InviteGroupNotifyResBean resBean = (IMBaseDefine.InviteGroupNotifyResBean)
//                        JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
//                                IMBaseDefine.NotifyType.INVITE_GROUP_NOTIFY_RES.getCls());
//                if(resBean != null
//                        && StringUtils.isNotBlank(resBean.code)
//                        && StringUtils.isNotBlank(resBean.groupId)) {
//                    //通过 code+groupId 确认消息
//                    Invite dbInvite = (Invite) inviteDao
//                            .findUniByProperty("invite_code,group_id", resBean.code, resBean.groupId);
//                    Invite inviteRes = Invite.buildInviteRes4Recv(dbInvite, otherMessageEntity);
//                    inviteDao.saveOrUpdate(inviteRes);
//
//                    if(inviteRes.getStatus() == 1) {
//                        //通知更新groupEntity
//                        IMGroupManager.instance().updateGroup4Members(resBean.groupId,
//                                resBean.userNo, inviteRes.getTime().getTime());
//                    }
//                    //发送系统通知
//                    notificationMessageEvent.event = NotificationMessageEvent.Event.INVITE_GROUP_NOTIFY_RES_RECEIVED;
//                    triggerEvent(notificationMessageEvent);
//                }
//                break;
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
