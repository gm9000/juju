package com.juju.app.service.notify;

import android.content.Context;
import android.os.Handler;

import com.juju.app.R;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.InviteDaoImpl;
import com.juju.app.entity.Invite;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;

import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 项目名称：juju
 * 类描述：加群邀请通知。暂时将消息发送到主线程消息队列 （若消息处理耗时，需要使用HandlerThread
 * 或EVENTBUS（子线程）解决）
 * 创建人：gm
 * 日期：2016/6/28 14:23
 * 版本：V1.0.0
 */
public class InviteUserNotify extends BaseNotify<InviteUserEvent.InviteUserBean>  {

    private Logger logger = Logger.getLogger(InviteUserNotify.class);
    private final static int GETGROUPINFO_TIMEOUT = 10;

    private InviteUserNotify() {

    }

    private volatile static InviteUserNotify inst;

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static InviteUserNotify instance() {
        if(inst == null) {
            synchronized (InviteUserNotify.class) {
                if (inst == null) {
                    inst = new InviteUserNotify();
                }
            }
        }
        return inst;
    }

    private IMGroupManager imGroupManager;
    private DaoSupport inviteDao;


    public void start(IMOtherManager imOtherManager, IMGroupManager imGroupManager) {
        super.start(imOtherManager);
        this.imGroupManager = imGroupManager;
        inviteDao = new InviteDaoImpl(context);
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    /**
     * 执行发送命令
    */
    public void executeCommand4Send(InviteUserEvent.InviteUserBean inviteUserBean) {
        sendInviteUserToBServer4Send(inviteUserBean);
    }

    /**
     * 执行接收命令(加群邀请通知)
     */
    public void executeCommand4Recv(InviteUserEvent.InviteUserBean inviteUserBean) {
        sendGetGroupInfoToBServer4Recv(inviteUserBean);
    }

    /**
     * 执行接收命令（申请方式加入群组通知）
     */
    public void executeCommand4Recv(InviteUserEvent.ApplyInGroupBean applyInGroupBean) {
        //TODO 业务简单，直接处理 （如果打开了会话窗口，需要文字提醒）

    }

    public void stop() {
        super.stop();
        this.imGroupManager = null;
        this.inviteDao = null;
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(InviteUserEvent.BusinessFlow.SendParam sendParam) {
        /**
         * 发送步骤:
         * 1: 发送"加入群组请求"（业务服务）
         * 2：发送"加群邀请通知--1001"（消息服务）
         * 3：发送"请方式加入群组通知--1009" (消息服务)
         * 4：更新缓存
         * 5：通知发送成功
         * 注意：按顺序发送，某个步骤出现异常，后面步骤停止
         */
        switch (sendParam.send) {
            case SEND_INVITE_USER_BSERVER_OK:
                sendInviteUserToMServer4Send(sendParam.bean);
                break;
            case SEND_INVITE_USER_MSERVER_OK:
                sendApplyInGroupToMServer4Send(sendParam.applyInGroupBean);
                break;
            case SEND_APPLY_IN_GROUP_MSERVER_OK:
                imGroupManager.updateGroup4Members(sendParam.applyInGroupBean.groupId,
                        sendParam.applyInGroupBean.userNo, sendParam.applyInGroupBean.replayTime, 0);
                buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                        .Send.UPDATE_LOCAL_CACHE_DATA_OK, sendParam.applyInGroupBean);
                break;
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //通知群组
                InviteUserEvent.InviteUserBean inviteUserBean = InviteUserEvent.InviteUserBean
                        .valueOf(sendParam.applyInGroupBean.groupId, "",
                                sendParam.applyInGroupBean.userNo, sendParam.applyInGroupBean.nickName);
                InviteUserEvent inviteUserEvent = new InviteUserEvent(inviteUserBean,
                        InviteUserEvent.Event.INVITE_USER_OK);
                triggerEvent(inviteUserEvent);
                break;
            case SEND_INVITE_USER_BSERVER_FAILED:
            case SEND_INVITE_USER_MSERVER_FAILED:
            case SEND_APPLY_IN_GROUP_MSERVER_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.invite_user_send_failed, 3);
                    }
                });
                break;
        }
    }

    //发送加入群组请求——业务服务器
    public void sendInviteUserToBServer4Send(final InviteUserEvent.InviteUserBean inviteUserBean) {
        CommandActionConstant.HttpReqParam INVITEUSER =
                CommandActionConstant.HttpReqParam.INVITEUSER;
        Map<String, Object> valueMap = HttpReqParamUtil.instance()
                .buildMap("memberNo, groupId", inviteUserBean.userNo, inviteUserBean.groupId);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                INVITEUSER.code(), INVITEUSER.url(),
                new HttpCallBack4OK() {
                    @Override
                    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                        if(obj != null && obj instanceof JSONObject) {
                            JSONObject jsonRoot = (JSONObject)obj;
                            int status = JSONUtils.getInt(jsonRoot, "status", -1);
                            if(status == 0) {
                                buildAndTriggerBusinessFlow4SendInviteUser(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_BSERVER_OK, inviteUserBean);
                            } else {
                                logger.d("status is not 0");
                                buildAndTriggerBusinessFlow4SendInviteUser(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_BSERVER_FAILED, inviteUserBean);
                            }
                        }
                    }
                    @Override
                    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                        buildAndTriggerBusinessFlow4SendInviteUser(InviteUserEvent.BusinessFlow.SendParam.Send
                                .SEND_INVITE_USER_BSERVER_FAILED, inviteUserBean);
                    }
                }, valueMap, JSONObject.class);
        try {
            client.sendPost4OK();
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        } catch (JSONException e) {
            logger.error(e);
        }
    }


    //发送加入群组请求——消息服务器
    public void sendInviteUserToMServer4Send(final InviteUserEvent.InviteUserBean inviteUserBean) {
        String peerId = inviteUserBean.userNo+"@"+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(inviteUserBean);
        String  uuid = UUID.randomUUID().toString();
        final InviteUserEvent.ApplyInGroupBean applyInGroupBean = InviteUserEvent.ApplyInGroupBean
                .valueOf(inviteUserBean.groupId, inviteUserBean.userNo, inviteUserBean.nickName);
        //通知用户
        notifyMessage4User(peerId, message,
                IMBaseDefine.NotifyType.INVITE_USER, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("InviteUserNotify#sendInviteUserToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            Invite dbEntity = (Invite)
                                    inviteDao.findUniByProperty("id", id);

                            if(dbEntity != null) {
                                Invite.buildInviteReq4SendOnAck(dbEntity,
                                        Long.parseLong(time));
                                //更新时间
                                inviteDao.saveOrUpdate(dbEntity);
                                long replayTime = Long.parseLong(time);
                                inviteUserBean.replayId = id;
                                inviteUserBean.replayTime = replayTime;


                                buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_MSERVER_OK, applyInGroupBean);

                            } else {
                                buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_MSERVER_FAILED, applyInGroupBean);
                            }
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("InviteUserNotify#sendInviteUserToMServer failed");
                        buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                                .Send.SEND_INVITE_USER_MSERVER_FAILED, applyInGroupBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("InviteUserNotify#sendInviteUserToMServer timeout");
                        buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                                .Send.SEND_INVITE_USER_MSERVER_FAILED, applyInGroupBean);
                    }
                });
    }

    //发送通知
    public void sendApplyInGroupToMServer4Send(final InviteUserEvent.ApplyInGroupBean applyInGroupBean) {
        String peerId = applyInGroupBean.groupId+"@"
                +userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(applyInGroupBean);
        String  uuid = UUID.randomUUID().toString();

        notifyMessage4Group(peerId, message, IMBaseDefine.NotifyType.APPLY_IN_GROUP, uuid, true,
                new XMPPServiceCallbackImpl() {

            @Override
            public void onSuccess(Object t) {
                logger.d("InviteUserNotify#sendApplyInGroupToMServer4Send success");
                if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                    XMPPServiceImpl.ReplayMessageTime messageTime =
                            (XMPPServiceImpl.ReplayMessageTime) t;
                    String id = messageTime.getId();
                    String time = messageTime.getTime();
                    imOtherManager.updateOtherMessage(id, Long.parseLong(time));

                    //处理回复请求
                    applyInGroupBean.replayId = id;
                    applyInGroupBean.replayTime = Long.parseLong(time);
                    buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                            .Send.SEND_APPLY_IN_GROUP_MSERVER_OK, applyInGroupBean);
                } else {
                    buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                            .Send.SEND_APPLY_IN_GROUP_MSERVER_FAILED, applyInGroupBean);
                }
            }


            @Override
            public void onFailed() {
                logger.d("InviteUserNotify#sendApplyInGroupToMServer4Send failed");
                buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                        .Send.SEND_APPLY_IN_GROUP_MSERVER_FAILED, applyInGroupBean);
            }

            @Override
            public void onTimeout() {
                logger.d("InviteUserNotify#sendApplyInGroupToMServer4Send failed");
                buildAndTriggerBusinessFlow4SendApplyInGroup(InviteUserEvent.BusinessFlow.SendParam
                        .Send.SEND_APPLY_IN_GROUP_MSERVER_FAILED, applyInGroupBean);
            }
        });
    }


    /**
     * 接收步骤:
     * 1: 发送"获取群组详情请求"（业务服务）
     * 2：发送"获取群组成员列表"（消息服务）
     * 3：发送"加入聊天室" (消息服务)
     * 4：更新缓存
     * 5：通知发送成功
     * 注意：按顺序发送，某个步骤出现异常，后面步骤停止
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(InviteUserEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case SEND_GET_GROUP_INFO_BSERVER_OK:
                sendGetGroupUsersToBServer4Recv(recvParam.bean, recvParam.groupId,
                        recvParam.groupName, recvParam.desc, recvParam.creatorNo, recvParam.masterNo,
                        recvParam.createTimeDate);
                break;
            case SEND_GET_GROUP_USERS_BSERVER_OK:
                sendJoinChatRoomToMServer4Recv();
                break;
            case JOIN_CHAT_ROOM_MSERVER_OK:
                break;

            case SEND_GET_GROUP_INFO_BSERVER_FAILED:
            case SEND_GET_GROUP_USERS_BSERVER_FAILED:
                ToastUtil.TextIntToast(context, R.string.invite_user_send_failed, 3);
                break;
        }
    }


    /**
     * 查询群组详情
     * @param inviteUserBean
     */
    public void sendGetGroupInfoToBServer4Recv(final InviteUserEvent.InviteUserBean inviteUserBean) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId", inviteUserBean.groupId);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETGROUPINFO;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj instanceof JSONObject) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    if(status == 0) {
                        JSONObject jsonGroup = null;
                        try {
                            jsonGroup = jsonRoot.getJSONObject("group");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(jsonGroup != null) {
                            String desc = JSONUtils.getString(jsonGroup, "desc");
                            String id = JSONUtils.getString(jsonGroup, "id");
                            String name = JSONUtils.getString(jsonGroup, "name");
                            String creatorNo = JSONUtils.getString(jsonGroup, "creatorNo");
                            String masterNo = JSONUtils.getString(jsonGroup, "masterNo");
                            String createTime = JSONUtils.getString(jsonGroup, "createTime");
                            Date createTimeDate = null;
                            try {
                                createTimeDate = DateUtils.parseDate(createTime, new String[] {"yyyy-MM-dd HH:mm:ss"});
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            InviteUserEvent.BusinessFlow.RecvParam recvParam = new InviteUserEvent
                                    .BusinessFlow.RecvParam(InviteUserEvent.BusinessFlow
                                    .RecvParam.Recv.SEND_GET_GROUP_INFO_BSERVER_OK, inviteUserBean);
                            recvParam.groupId = id;
                            recvParam.groupName = name;
                            recvParam.desc = desc;
                            recvParam.creatorNo = creatorNo;
                            recvParam.masterNo = masterNo;
                            recvParam.createTimeDate = createTimeDate;
                            buildAndTriggerBusinessFlow4Recv(recvParam);
                        }
                    }
                }
            }
            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                buildAndTriggerBusinessFlow4RecvInviteUser(InviteUserEvent.BusinessFlow.RecvParam
                        .Recv.SEND_GET_GROUP_INFO_BSERVER_FAILED, inviteUserBean);
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


    /**
     * 获取成员列表
     * @param inviteUserBean
     * @param groupId
     * @param groupName
     * @param desc
     * @param creatorNo
     * @param masterNo
     * @param createTimeDate
     */
    public void sendGetGroupUsersToBServer4Recv(InviteUserEvent.InviteUserBean inviteUserBean,
                                                String groupId, String groupName, String desc,
                                                String creatorNo, String masterNo, Date createTimeDate) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        imGroupManager.sendGetGroupUsersToBServer(countDownLatch,
                groupId, groupName, desc, creatorNo, masterNo, createTimeDate);
        try {
            countDownLatch.await(GETGROUPINFO_TIMEOUT, TimeUnit.SECONDS);
            buildAndTriggerBusinessFlow4RecvInviteUser(InviteUserEvent.BusinessFlow.RecvParam.Recv
                    .SEND_GET_GROUP_USERS_BSERVER_OK, inviteUserBean);
        } catch (InterruptedException e) {
            buildAndTriggerBusinessFlow4RecvInviteUser(InviteUserEvent.BusinessFlow.RecvParam.Recv
                    .SEND_GET_GROUP_USERS_BSERVER_FAILED, inviteUserBean);
        }
    }

    //TODO 不合理 需要结合GetGroupUserThread调整
    public void sendJoinChatRoomToMServer4Recv() {
        imGroupManager.joinChatRooms(imGroupManager.getGroupMap().values());
        buildAndTriggerBusinessFlow4RecvInviteUser(InviteUserEvent.BusinessFlow.RecvParam.Recv
                .JOIN_CHAT_ROOM_MSERVER_OK, null);
    }






    /**
     * 构建业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4SendInviteUser(
            InviteUserEvent.BusinessFlow.SendParam.Send send,
            InviteUserEvent.InviteUserBean inviteUserBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        InviteUserEvent.BusinessFlow.SendParam sendParam = new InviteUserEvent.BusinessFlow
                .SendParam(send, inviteUserBean);
        triggerEvent(sendParam);
    }

    private void buildAndTriggerBusinessFlow4SendApplyInGroup(
            InviteUserEvent.BusinessFlow.SendParam.Send send,
            InviteUserEvent.ApplyInGroupBean applyInGroupBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        InviteUserEvent.BusinessFlow.SendParam sendParam = new InviteUserEvent.BusinessFlow
                .SendParam(send, applyInGroupBean);
        triggerEvent(sendParam);
    }


    /**
     * 构建业务流
     */
    private void buildAndTriggerBusinessFlow4RecvInviteUser(
            InviteUserEvent.BusinessFlow.RecvParam.Recv recv,
            InviteUserEvent.InviteUserBean inviteUserBean) {
        if(recv == null)
            throw new IllegalArgumentException("InviteUserTask#recv is null");
        InviteUserEvent.BusinessFlow.RecvParam recvParam = new InviteUserEvent.BusinessFlow
                .RecvParam(recv, inviteUserBean);
        triggerEvent(recvParam);
    }

    /**
     * 构建业务流
     */
    private void buildAndTriggerBusinessFlow4RecvApplyInGroup(
            InviteUserEvent.BusinessFlow.RecvParam.Recv recv,
            InviteUserEvent.ApplyInGroupBean applyInGroupBean) {
        if(recv == null)
            throw new IllegalArgumentException("InviteUserTask#recv is null");
        InviteUserEvent.BusinessFlow.RecvParam recvParam = new InviteUserEvent.BusinessFlow
                .RecvParam(recv, applyInGroupBean);
        triggerEvent(recvParam);
    }

    /**
     * 构建业务流
     */
    private void buildAndTriggerBusinessFlow4Recv(
            InviteUserEvent.BusinessFlow.RecvParam recvParam) {
        triggerEvent(recvParam);
    }





}
