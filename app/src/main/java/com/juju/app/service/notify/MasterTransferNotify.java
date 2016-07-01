package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.MasterTransferEvent;
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
 * 类描述：转让群主
 * 创建人：gm
 * 日期：2016/7/1 11:22
 * 版本：V1.0.0
 */
public class MasterTransferNotify extends BaseNotify<MasterTransferEvent.MasterTransferBean> {

    private Logger logger = Logger.getLogger(MasterTransferNotify.class);

    private volatile static MasterTransferNotify inst;

    public static MasterTransferNotify instance() {
        if(inst == null) {
            synchronized (MasterTransferNotify.class) {
                if (inst == null) {
                    inst = new MasterTransferNotify();
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
    public void executeCommand4Send(MasterTransferEvent.MasterTransferBean masterTransferBean) {
        sendUpdateGroupMasterToBServer(masterTransferBean);
    }

    @Override
    public void executeCommand4Recv(MasterTransferEvent.MasterTransferBean masterTransferBean) {
        recvUpdateLocalData(masterTransferBean);
    }


    public void stop() {
        super.stop();
        imGroupManager = null;
        groupDao = null;
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(MasterTransferEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_UPDATE_GROUP_MASTER_BSERVER_OK:
                sendMasterTransferToMServer(sendParam.bean);
                break;
            case SEND_MASTER_TRANSFER_MSERVER_OK:
                GroupEntity groupEntity = imGroupManager.findGroupById(sendParam.bean.groupId);
                groupEntity.setMasterId(sendParam.bean.masterNo);
                GroupEntity dbGroup = (GroupEntity) groupDao.findUniByProperty("id", groupEntity.getId());
                if(dbGroup != null) {
                    dbGroup.setMasterId(sendParam.bean.masterNo);
                    groupDao.update(dbGroup);
                }
                buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam.Send
                        .UPDATE_LOCAL_CACHE_DATA_OK, sendParam.bean);
                break;
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                MasterTransferEvent externalEvent = new MasterTransferEvent(MasterTransferEvent.Event
                        .SEND_MASTER_TRANSFER_OK, sendParam.bean);
                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.master_transfer_send_success, 0);
                    }
                });
                break;

            case SEND_UPDATE_GROUP_MASTER_BSERVER_FAILED:
            case SEND_MASTER_TRANSFER_MSERVER_FAILED:
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.master_transfer_send_failed, 0);
                    }
                });
                break;

        }
    }


    private void sendUpdateGroupMasterToBServer(final MasterTransferEvent.MasterTransferBean
                                                        masterTransferBean) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId,masterNo",
                masterTransferBean.groupId, masterTransferBean.masterNo);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam
                .UPDATEGROUPMASTER;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject  jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    String desc = JSONUtils.getString(jsonRoot, "desc");
                    if(status == 0) {
                        buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam
                                .Send.SEND_UPDATE_GROUP_MASTER_BSERVER_OK, masterTransferBean);
                    } else {
                        buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam
                                .Send.SEND_UPDATE_GROUP_MASTER_BSERVER_FAILED, masterTransferBean);
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam
                        .Send.SEND_UPDATE_GROUP_MASTER_BSERVER_FAILED, masterTransferBean);
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

    public void sendMasterTransferToMServer(final MasterTransferEvent.MasterTransferBean masterTransferBean) {
        String peerId = masterTransferBean.masterNo+"@"+userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(masterTransferBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4User(peerId, message,
                IMBaseDefine.NotifyType.MASTER_TRANSFER, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("MasterTransferNotify#sendMasterTransferToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            masterTransferBean.replyId = id;
                            masterTransferBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam
                                    .Send.SEND_MASTER_TRANSFER_MSERVER_OK, masterTransferBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam
                                    .Send.SEND_MASTER_TRANSFER_MSERVER_FAILED, masterTransferBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("MasterTransferNotify#sendMasterTransferToMServer failed");
                        buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam
                                .Send.SEND_MASTER_TRANSFER_MSERVER_FAILED, masterTransferBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("MasterTransferNotify#sendMasterTransferToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(MasterTransferEvent.BusinessFlow.SendParam
                                .Send.SEND_MASTER_TRANSFER_MSERVER_FAILED, masterTransferBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            MasterTransferEvent.BusinessFlow.SendParam.Send send,
            MasterTransferEvent.MasterTransferBean masterTransferBean) {
        if(send == null)
            throw new IllegalArgumentException("InviteUserTask#send is null");

        MasterTransferEvent.BusinessFlow.SendParam sendParam = new MasterTransferEvent.BusinessFlow
                .SendParam(send, masterTransferBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(MasterTransferEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                MasterTransferEvent externalEvent = new MasterTransferEvent(MasterTransferEvent.Event
                        .RECV_MASTER_TRANSFER_OK, recvParam.bean);
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

    private void recvUpdateLocalData(MasterTransferEvent.MasterTransferBean masterTransferBean) {
        GroupEntity groupEntity = imGroupManager.findGroupById(masterTransferBean.groupId);
        if(groupEntity != null) {
            groupEntity.setMasterId(masterTransferBean.masterNo);
            buildAndTriggerBusinessFlow4Recv(MasterTransferEvent.BusinessFlow
                    .RecvParam.Recv.UPDATE_LOCAL_CACHE_DATA_OK, masterTransferBean);
        }
    }

    /**
     * 构建接收业务流
     * @param recv
     */
    private void buildAndTriggerBusinessFlow4Recv(
            MasterTransferEvent.BusinessFlow.RecvParam.Recv recv,
            MasterTransferEvent.MasterTransferBean masterTransferBean) {
        if(recv == null)
            throw new IllegalArgumentException("MasterTransferNotify#recv is null");

        MasterTransferEvent.BusinessFlow.RecvParam recvParam = new MasterTransferEvent.BusinessFlow
                .RecvParam(recv, masterTransferBean);
        triggerEvent(recvParam);
    }
}
