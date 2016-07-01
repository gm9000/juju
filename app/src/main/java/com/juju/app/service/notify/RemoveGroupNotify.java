package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.biz.impl.InviteDaoImpl;
import com.juju.app.entity.Invite;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.event.notify.RemoveGroupEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMGroupManager;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/6/30 10:12
 * 版本：V1.0.0
 */
public class RemoveGroupNotify extends BaseNotify<RemoveGroupEvent.RemoveGroupBean> {

    private Logger logger = Logger.getLogger(RemoveGroupNotify.class);


    private volatile static RemoveGroupNotify inst;

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static RemoveGroupNotify instance() {
        if(inst == null) {
            synchronized (RemoveGroupNotify.class) {
                if (inst == null) {
                    inst = new RemoveGroupNotify();
                }
            }
        }
        return inst;
    }

    private IMGroupManager imGroupManager;
    private DaoSupport groupDao;

    public void start(IMOtherManager imOtherManager, IMGroupManager imGroupManager) {
        super.start(imOtherManager);
        this.imGroupManager = imGroupManager;
        groupDao = new GroupDaoImpl(context);
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void executeCommand4Send(RemoveGroupEvent.RemoveGroupBean removeGroupBean) {
        sendDeleteGroupMemberToBServer(removeGroupBean);
    }

    @Override
    public void executeCommand4Recv(RemoveGroupEvent.RemoveGroupBean removeGroupBean) {
        receiveExitChatRoomToMServer(removeGroupBean);
    }


    public void stop() {
        super.stop();
        imGroupManager = null;
        groupDao = null;
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(RemoveGroupEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_DELETE_GROUP_MEMBER_BSERVER_OK:
                sendRemoveGroupToMServer(sendParam.bean);
                break;
            case SEND_REMOVE_GROUP_MSERVER_OK:
                imGroupManager.updateGroup4Members(sendParam.bean.groupId, sendParam.bean.userNo,
                        sendParam.bean.replyTime, 1);
                buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam.Send
                        .UPDATE_LOCAL_CACHE_DATA_OK, sendParam.bean);
                break;
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                RemoveGroupEvent externalEvent = new RemoveGroupEvent(RemoveGroupEvent.Event
                        .SEND_REMOVE_GROUP_OK, sendParam.bean);
                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.delete_group_member_send_success, 0);
                    }
                });
                break;

            case SEND_DELETE_GROUP_MEMBER_BSERVER_FAILED:
            case SEND_REMOVE_GROUP_MSERVER_FAILED:
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.delete_group_member_send_failed, 0);
                    }
                });
                break;

        }
    }


    private void sendDeleteGroupMemberToBServer(final RemoveGroupEvent.RemoveGroupBean removeGroupBean) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId,memberNo",
                removeGroupBean.groupId, removeGroupBean.userNo);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam
                .DELETEGROUPMEMBER;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject  jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    String desc = JSONUtils.getString(jsonRoot, "desc");
                    if(status == 0) {
                        buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_DELETE_GROUP_MEMBER_BSERVER_OK, removeGroupBean);
                    } else {
                        buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_DELETE_GROUP_MEMBER_BSERVER_FAILED, removeGroupBean);
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam
                        .Send.SEND_DELETE_GROUP_MEMBER_BSERVER_FAILED, removeGroupBean);
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

    public void sendRemoveGroupToMServer(final RemoveGroupEvent.RemoveGroupBean removeGroupBean) {
        String peerId = removeGroupBean.userNo+"@"+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(removeGroupBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4User(peerId, message,
                IMBaseDefine.NotifyType.REMOVE_GROUP, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("RemoveGroupNotify#sendRemoveGroupToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            removeGroupBean.replyId = id;
                            removeGroupBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam
                                    .Send.SEND_REMOVE_GROUP_MSERVER_OK, removeGroupBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam
                                    .Send.SEND_REMOVE_GROUP_MSERVER_FAILED, removeGroupBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("RemoveGroupNotify#sendRemoveGroupToMServer failed");
                        buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_REMOVE_GROUP_MSERVER_FAILED, removeGroupBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("RemoveGroupNotify#sendRemoveGroupToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(RemoveGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_REMOVE_GROUP_MSERVER_FAILED, removeGroupBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            RemoveGroupEvent.BusinessFlow.SendParam.Send send,
            RemoveGroupEvent.RemoveGroupBean removeGroupBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        RemoveGroupEvent.BusinessFlow.SendParam sendParam = new RemoveGroupEvent.BusinessFlow
                .SendParam(send, removeGroupBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(RemoveGroupEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case EXIT_CHAT_ROOM_MSERVER_OK:
                GroupEntity groupEntity = imGroupManager.findGroupById(recvParam.bean.groupId);
                if(groupEntity !=  null) {
                    imGroupManager.getGroupMap().remove(groupEntity.getPeerId());
                    GroupEntity dbGroup = (GroupEntity) groupDao
                            .findUniByProperty("id", recvParam.bean.groupId);
                    groupDao.delete(dbGroup);
                    buildAndTriggerBusinessFlow4Recv(RemoveGroupEvent.BusinessFlow.RecvParam.Recv
                            .UPDATE_LOCAL_CACHE_DATA_OK, recvParam.bean);
                }
                break;
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                RemoveGroupEvent externalEvent = new RemoveGroupEvent(RemoveGroupEvent.Event
                        .RECV_REMOVE_GROUP_OK, recvParam.bean);
                triggerEvent(externalEvent);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ToastUtil.TextIntToast(context, R.string.delete_group_member_send_success, 0);
//                    }
//                });
                break;
            case EXIT_CHAT_ROOM_MSERVER_FAILED:
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.delete_group_member_send_failed, 0);
                    }
                });
                break;

        }
    }

    private void receiveExitChatRoomToMServer(RemoveGroupEvent.RemoveGroupBean removeGroupBean) {
        GroupEntity groupEntity = imGroupManager.findGroupById(removeGroupBean.groupId);
        if(groupEntity != null) {
            boolean isLeave = imGroupManager.exitChatRoom(groupEntity.getPeerId());
            if(isLeave) {
                buildAndTriggerBusinessFlow4Recv(RemoveGroupEvent.BusinessFlow
                        .RecvParam.Recv.EXIT_CHAT_ROOM_MSERVER_OK, removeGroupBean);
            } else {
                buildAndTriggerBusinessFlow4Recv(RemoveGroupEvent.BusinessFlow
                        .RecvParam.Recv.EXIT_CHAT_ROOM_MSERVER_FAILED, removeGroupBean);
            }
        }
    }

    /**
     * 构建接收业务流
     * @param recv
     */
    private void buildAndTriggerBusinessFlow4Recv(
            RemoveGroupEvent.BusinessFlow.RecvParam.Recv recv,
            RemoveGroupEvent.RemoveGroupBean removeGroupBean) {
        if(recv == null)
            throw new IllegalArgumentException("InviteUserTask#recv is null");

        RemoveGroupEvent.BusinessFlow.RecvParam recvParam = new RemoveGroupEvent.BusinessFlow
                .RecvParam(recv, removeGroupBean);
        triggerEvent(recvParam);
    }



}
