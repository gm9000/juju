package com.juju.app.service.notify;

import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.biz.impl.InviteDaoImpl;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.InviteInGroupEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/7/2 18:08
 * 版本：V1.0.0
 */
public class InviteInGroupNotify extends BaseNotify<InviteInGroupEvent.InviteInGroupBean> {

    private Logger logger = Logger.getLogger(InviteInGroupNotify.class);

    private volatile static InviteInGroupNotify inst;

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static InviteInGroupNotify instance() {
        if(inst == null) {
            synchronized (InviteInGroupNotify.class) {
                if (inst == null) {
                    inst = new InviteInGroupNotify();
                }
            }
        }
        return inst;
    }

    private IMGroupManager imGroupManager;
    private IMContactManager imContactManager;
    private IMSessionManager imSessionManager;
    private DaoSupport groupDao;
    private DaoSupport userDao;

    public void start(IMOtherManager imOtherManager, IMGroupManager imGroupManager,
                      IMContactManager imContactManager, IMSessionManager imSessionManager) {
        super.start(imOtherManager);
        this.imGroupManager = imGroupManager;
        this.imContactManager = imContactManager;
        this.imSessionManager = imSessionManager;
        groupDao = new GroupDaoImpl(context);
        userDao = new UserDaoImpl(context);
    }

    public void stop() {
        super.stop();
        this.imContactManager = null;
        this.imGroupManager = null;
        this.imSessionManager = null;
        this.groupDao = null;
        this.userDao = null;
    }

    @Override
    public void executeCommand4Send(final InviteInGroupEvent.InviteInGroupBean inviteInGroupBean) {
        String peerId = inviteInGroupBean.groupId+"@"
                +userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(inviteInGroupBean);
        String  uuid = UUID.randomUUID().toString();
        notifyMessage4Group(peerId, message, IMBaseDefine.NotifyType.INVITE_IN_GROUP, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("InviteInGroupNotify#executeCommand4Send success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            imOtherManager.updateOtherMessage(id, Long.parseLong(time));

                            //处理回复请求
                            inviteInGroupBean.replyId = id;
                            inviteInGroupBean.replyTime = Long.parseLong(time);

                            String peerId = inviteInGroupBean.groupId+"@"+userInfoBean.getmMucServiceName()
                                    +"."+userInfoBean.getmServiceName();
                            String talkId = inviteInGroupBean.invitorNo+"@"+userInfoBean.getmServiceName();

                            //更新会话
                            imSessionManager.updateSessionEntity(peerId, DBConstant.SESSION_TYPE_GROUP,
                                    DBConstant.MSG_TYPE_GROUP_TEXT, "", talkId, inviteInGroupBean.replyTime);

                            InviteInGroupEvent inviteInGroupEvent = new InviteInGroupEvent(inviteInGroupBean,
                                    InviteInGroupEvent.Event.SEND_INVITE_IN_GROUP_OK);
                            triggerEvent(inviteInGroupEvent);
                        } else {
                            InviteInGroupEvent inviteInGroupEvent = new InviteInGroupEvent(inviteInGroupBean,
                                    InviteInGroupEvent.Event.SEND_INVITE_IN_GROUP_FAILED);
                            triggerEvent(inviteInGroupEvent);
                        }
                    }


                    @Override
                    public void onFailed() {
                        logger.d("InviteInGroupNotify#executeCommand4Send failed");
                        InviteInGroupEvent inviteInGroupEvent = new InviteInGroupEvent(inviteInGroupBean,
                                InviteInGroupEvent.Event.SEND_INVITE_IN_GROUP_FAILED);
                        triggerEvent(inviteInGroupEvent);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("InviteInGroupNotify#executeCommand4Send failed");
                        InviteInGroupEvent inviteInGroupEvent = new InviteInGroupEvent(inviteInGroupBean,
                                InviteInGroupEvent.Event.SEND_INVITE_IN_GROUP_FAILED);
                        triggerEvent(inviteInGroupEvent);
                    }
                });
    }

    @Override
    public void executeCommand4Recv(InviteInGroupEvent.InviteInGroupBean inviteInGroupBean) {
        //更新本地数据
        imGroupManager.updateGroup4Members(inviteInGroupBean.groupId,
                inviteInGroupBean.userNo, inviteInGroupBean.replyTime, 0);

        //更新会话
        String peerId = inviteInGroupBean.groupId+"@"+userInfoBean.getmMucServiceName()
                +"."+userInfoBean.getmServiceName();
        String talkId = inviteInGroupBean.invitorNo+"@"+userInfoBean.getmServiceName();
        imSessionManager.updateSessionEntity(peerId, DBConstant.SESSION_TYPE_GROUP,
                DBConstant.MSG_TYPE_GROUP_TEXT, "", talkId, inviteInGroupBean.replyTime);

        User user = imContactManager.findContact(inviteInGroupBean.userNo);
        if(user == null) {
            //获取用户详情
            imContactManager.sendGetUserInfo2BServer(inviteInGroupBean.userNo);
        }
        InviteInGroupEvent inviteInGroupEvent = new InviteInGroupEvent(inviteInGroupBean,
                InviteInGroupEvent.Event.RECV_INVITE_IN_GROUP_OK);
        triggerEvent(inviteInGroupEvent);
    }
}
