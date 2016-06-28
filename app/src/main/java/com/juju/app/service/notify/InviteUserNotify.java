package com.juju.app.service.notify;

import android.content.Context;
import android.os.Handler;

import com.juju.app.R;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.Invite;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMGroupManager;
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
public class InviteUserNotify {

    private Logger logger = Logger.getLogger(InviteUserNotify.class);

    private final static int GETGROUPINFO_TIMEOUT = 10;

    private Thread mUiThread = Thread.currentThread();

    private Handler uiHandler = new Handler();



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
    private UserInfoBean userInfoBean;
    private SocketService socketService;
    private DaoSupport inviteDao;
    private Context context;


    public void start(Context context, IMGroupManager imGroupManager, UserInfoBean userInfoBean,
                      SocketService socketService, DaoSupport inviteDao) {
        this.context = context;
        this.imGroupManager = imGroupManager;
        this.userInfoBean = userInfoBean;
        this.socketService = socketService;
        this.inviteDao = inviteDao;
        if(!EventBus.getDefault().isRegistered(inst)) {
            EventBus.getDefault().register(inst);
        }
    }

    /**
     * 执行发送命令
     */
    public void executeCommand4Send(InviteUserEvent.InviteUserBean inviteUserBean) {
        sendInviteUserToBServer4Send(inviteUserBean);
    }

    /**
     * 执行接收命令
     */
    public void executeCommand4Recv(InviteUserEvent.InviteUserBean inviteUserBean) {
        sendGetGroupInfoToBServer4Recv(inviteUserBean);
    }

    public void stop() {
        this.context = null;
        this.imGroupManager = null;
        this.userInfoBean = null;
        this.socketService = null;
        this.inviteDao = null;
        if(EventBus.getDefault().isRegistered(inst)) {
            EventBus.getDefault().unregister(inst);
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(InviteUserEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_INVITE_USER_BSERVER_OK:
                sendInviteUserToMServer4Send(sendParam.bean);
                break;
            case SEND_INVITE_USER_MSERVER_OK:
                imGroupManager.updateGroup4Members(sendParam.bean.groupId,
                        sendParam.bean.userNo, sendParam.replayTime);
                buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam
                        .Send.UPDATE_LOCAL_CACHE_DATA_OK, sendParam.bean);
                break;
            case UPDATE_LOCAL_CACHE_DATA_OK:
                InviteUserEvent inviteUserEvent = new InviteUserEvent(InviteUserEvent.Event
                        .INVITE_USER_OK);
                triggerEvent(inviteUserEvent);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ToastUtil.TextIntToast(context, R.string.select_group_member_empty, 10);
//                    }
//                });
                break;
            case SEND_INVITE_USER_BSERVER_FAILED:
            case SEND_INVITE_USER_MSERVER_FAILED:
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
                                buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_BSERVER_OK, inviteUserBean);
                            } else {
                                logger.d("status is not 0");
                                buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_BSERVER_FAILED, inviteUserBean);
                            }
                        }
                    }
                    @Override
                    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                        buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam.Send
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

        //通知群组
        socketService.notifyMessage(peerId, message,
                IMBaseDefine.NotifyType.INVITE_USER, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("InviteUserTask#sendInviteUserToMServer success");
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
                                buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_MSERVER_OK, inviteUserBean, id, replayTime);

                            } else {
                                buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam
                                        .Send.SEND_INVITE_USER_MSERVER_FAILED, inviteUserBean);
                            }
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("InviteUserTask#sendInviteUserToMServer failed");
                        buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam
                                .Send.SEND_INVITE_USER_MSERVER_FAILED, null);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("InviteUserTask#sendInviteUserToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(InviteUserEvent.BusinessFlow.SendParam
                                .Send.SEND_INVITE_USER_MSERVER_FAILED, null);
                    }
                });
    }


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
                //不需要打开会话窗口

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
                buildAndTriggerBusinessFlow4Recv(InviteUserEvent.BusinessFlow.RecvParam
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
                groupId, groupName, desc, creatorNo, createTimeDate);
        try {
            countDownLatch.await(GETGROUPINFO_TIMEOUT, TimeUnit.SECONDS);
            buildAndTriggerBusinessFlow4Recv(InviteUserEvent.BusinessFlow.RecvParam.Recv
                    .SEND_GET_GROUP_USERS_BSERVER_OK, inviteUserBean);
//                                    imService.getGroupManager().joinChatRooms(imService
//                                            .getGroupManager().getGroupMap().values());
//                                    inviteGroupEvent.event = InviteGroupEvent.Event.GETGROUPINFO_SUCCESS;
//                                    triggerEvent(inviteGroupEvent);
        } catch (InterruptedException e) {
            buildAndTriggerBusinessFlow4Recv(InviteUserEvent.BusinessFlow.RecvParam.Recv
                    .SEND_GET_GROUP_USERS_BSERVER_FAILED, inviteUserBean);

        }

    }

    //TODO 不合理 需要结合GetGroupUserThread调整
    public void sendJoinChatRoomToMServer4Recv() {
        imGroupManager.joinChatRooms(imGroupManager.getGroupMap().values());
        buildAndTriggerBusinessFlow4Recv(InviteUserEvent.BusinessFlow.RecvParam.Recv
                .JOIN_CHAT_ROOM_MSERVER_OK, null);
    }

    /**
     * 构建业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            InviteUserEvent.BusinessFlow.SendParam.Send send,
            InviteUserEvent.InviteUserBean inviteUserBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        InviteUserEvent.BusinessFlow.SendParam sendParam = new InviteUserEvent.BusinessFlow
                .SendParam(send, inviteUserBean);
        triggerEvent(sendParam);
    }

    /**
     * 构建业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            InviteUserEvent.BusinessFlow.SendParam.Send send,
            InviteUserEvent.InviteUserBean inviteUserBean, String replayId, long replayTime) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        InviteUserEvent.BusinessFlow.SendParam sendParam = new InviteUserEvent.BusinessFlow
                .SendParam(send, inviteUserBean);
        sendParam.replayId = replayId;
        sendParam.replayTime = replayTime;
        triggerEvent(sendParam);
    }

    /**
     * 构建业务流
     */
    private void buildAndTriggerBusinessFlow4Recv(
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
    private void buildAndTriggerBusinessFlow4Recv(
            InviteUserEvent.BusinessFlow.RecvParam recvParam) {
        triggerEvent(recvParam);
    }


    private void triggerEvent(Object paramObject) {
        EventBus.getDefault().post(paramObject);
    }

    private final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            uiHandler.post(action);
        } else {
            action.run();
        }
    }
}
