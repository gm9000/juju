package com.juju.app.service.notify;

import com.fasterxml.jackson.databind.deser.Deserializers;
import com.juju.app.R;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.ExitGroupEvent;
import com.juju.app.event.notify.MasterTransferEvent;
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
 * 类描述：退出群组通知
 * 创建人：gm
 * 日期：2016/7/1 15:04
 * 版本：V1.0.0
 */
public class ExitGroupNotify extends BaseNotify<ExitGroupEvent.ExitGroupBean> {

    private Logger logger = Logger.getLogger(ExitGroupNotify.class);

    private volatile static ExitGroupNotify inst;

    public static ExitGroupNotify instance() {
        if(inst == null) {
            synchronized (ExitGroupNotify.class) {
                if (inst == null) {
                    inst = new ExitGroupNotify();
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
    public void executeCommand4Send(ExitGroupEvent.ExitGroupBean exitGroupBean) {
        sendQuitGroupMasterToBServer(exitGroupBean);
    }

    @Override
    public void executeCommand4Recv(ExitGroupEvent.ExitGroupBean exitGroupBean) {
//        recvUpdateLocalData(exitGroupBean);
    }


    public void stop() {
        super.stop();
        imGroupManager = null;
        groupDao = null;
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(ExitGroupEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_QUIT_GROUP_BSERVER_OK:
                sendExitGroupToMServer(sendParam.bean);
                break;
            case SEND_EXIT_GROUP_MSERVER_OK:
                GroupEntity groupEntity = imGroupManager.findGroupById(sendParam.bean.groupId);
                if(groupEntity != null) {
                    imGroupManager.getGroupMap().remove(groupEntity.getPeerId());
                    GroupEntity dbGroup = (GroupEntity) groupDao.findUniByProperty("id", groupEntity.getId());
                    if(dbGroup != null) {
                        groupDao.delete(dbGroup);
                    }
                }
                buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam.Send
                        .UPDATE_LOCAL_CACHE_DATA_OK, sendParam.bean);
                break;
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                ExitGroupEvent externalEvent = new ExitGroupEvent(ExitGroupEvent.Event
                        .SEND_EXIT_GROUP_OK, sendParam.bean);
                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.exit_group_send_success, 0);
                    }
                });
                break;
            case SEND_QUIT_GROUP_BSERVER_FAILED:
                int resId = getResValue(sendParam.bean.errorDesc);
                if(resId == 0)  resId = R.string.exit_group_send_failed;
                final int finalResId = resId;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, finalResId, 0);
                    }
                });
                break;
            case SEND_EXIT_GROUP_MSERVER_FAILED:
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.exit_group_send_failed, 0);
                    }
                });
                break;

        }
    }


    private void sendQuitGroupMasterToBServer(final ExitGroupEvent.ExitGroupBean
                                                        exitGroupBean) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId",
                exitGroupBean.groupId);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam
                .QUITGROUP;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject  jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    String desc = JSONUtils.getString(jsonRoot, "desc");
                    if(status == 0) {
                        buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_QUIT_GROUP_BSERVER_OK, exitGroupBean);
                    } else {
                        exitGroupBean.errorDesc = desc;
                        buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_QUIT_GROUP_BSERVER_FAILED, exitGroupBean);
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam
                        .Send.SEND_QUIT_GROUP_BSERVER_FAILED, exitGroupBean);
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

    public void sendExitGroupToMServer(final ExitGroupEvent.ExitGroupBean exitGroupBean) {
        String peerId = exitGroupBean.groupId+"@"+userInfoBean.getmMucServiceName()+"."
                +userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(exitGroupBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4User(peerId, message,
                IMBaseDefine.NotifyType.EXIT_GROUP, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("ExitGroupNotify#sendExitGroupToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            exitGroupBean.replyId = id;
                            exitGroupBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam
                                    .Send.SEND_EXIT_GROUP_MSERVER_OK, exitGroupBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam
                                    .Send.SEND_EXIT_GROUP_MSERVER_FAILED, exitGroupBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("ExitGroupNotify#sendExitGroupToMServer failed");
                        buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_EXIT_GROUP_MSERVER_FAILED, exitGroupBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("ExitGroupNotify#sendExitGroupToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(ExitGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_EXIT_GROUP_MSERVER_FAILED, exitGroupBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            ExitGroupEvent.BusinessFlow.SendParam.Send send,
            ExitGroupEvent.ExitGroupBean exitGroupBean) {
        if(send == null)
            throw new IllegalArgumentException("ExitGroupNotify#send is null");

        ExitGroupEvent.BusinessFlow.SendParam sendParam = new ExitGroupEvent.BusinessFlow
                .SendParam(send, exitGroupBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(ExitGroupEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                ExitGroupEvent externalEvent = new ExitGroupEvent(ExitGroupEvent.Event
                        .RECV_EXIT_GROUP_OK, recvParam.bean);
                triggerEvent(externalEvent);
                break;
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ToastUtil.TextIntToast(context, R.string.master_transfer_send_failed, 0);
//                    }
//                });
                break;
        }
    }

    private void recvUpdateLocalData(ExitGroupEvent.ExitGroupBean exitGroupBean) {
        GroupEntity groupEntity = imGroupManager.findGroupById(exitGroupBean.groupId);
        if(groupEntity != null) {
            if(exitGroupBean.flag == 0 && exitGroupBean.userNo.equals(userInfoBean.getJujuNo())) {
                //TODO 删除群组
            } else {
                //更新群组成员关系
            }
//            groupEntity.setUserList(groupEntity.getUserList().replaceAll(exitGroupBean.userNo));
            buildAndTriggerBusinessFlow4Recv(ExitGroupEvent.BusinessFlow
                    .RecvParam.Recv.UPDATE_LOCAL_CACHE_DATA_OK, exitGroupBean);
        }
    }

    /**
     * 构建接收业务流
     * @param recv
     */
    private void buildAndTriggerBusinessFlow4Recv(
            ExitGroupEvent.BusinessFlow.RecvParam.Recv recv,
            ExitGroupEvent.ExitGroupBean exitGroupBean) {
        if(recv == null)
            throw new IllegalArgumentException("ExitGroupNotify#recv is null");

        ExitGroupEvent.BusinessFlow.RecvParam recvParam = new ExitGroupEvent.BusinessFlow
                .RecvParam(recv, exitGroupBean);
        triggerEvent(recvParam);
    }


}
