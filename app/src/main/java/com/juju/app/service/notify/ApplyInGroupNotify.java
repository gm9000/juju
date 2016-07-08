package com.juju.app.service.notify;

import com.juju.app.R;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.ApplyInGroupEvent;
import com.juju.app.event.notify.MasterTransferEvent;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * 项目名称：juju
 * 类描述：申请方式加入群组通知
 * 创建人：gm
 * 日期：2016/7/5 18:08
 * 版本：V1.0.0
 */
public class ApplyInGroupNotify extends BaseNotify<ApplyInGroupEvent.ApplyInGroupBean> {

    private Logger logger = Logger.getLogger(MasterTransferNotify.class);

    private volatile static ApplyInGroupNotify inst;

    public static ApplyInGroupNotify instance() {
        if(inst == null) {
            synchronized (ApplyInGroupNotify.class) {
                if (inst == null) {
                    inst = new ApplyInGroupNotify();
                }
            }
        }
        return inst;
    }

    private IMGroupManager imGroupManager;
    private IMContactManager imContactManager;
    private DaoSupport groupDao;
    private DaoSupport userDao;

    public void start(IMOtherManager imOtherManager, IMGroupManager imGroupManager,
                      IMContactManager imContactManager) {
        super.start(imOtherManager);
        this.imGroupManager = imGroupManager;
        this.imContactManager = imContactManager;
        groupDao = new GroupDaoImpl(context);
        userDao = new UserDaoImpl(context);
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void executeCommand4Send(ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        sendJoinInGroupToBServer(applyInGroupBean);
    }

    @Override
    public void executeCommand4Recv(ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        recvUpdateLocalData(applyInGroupBean);
    }


    public void stop() {
        super.stop();
        imGroupManager = null;
        imContactManager = null;
        groupDao = null;
        userDao = null;
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowSendEvent(ApplyInGroupEvent.BusinessFlow.SendParam sendParam) {
        switch (sendParam.send) {
            case SEND_JOIN_IN_GROUP_BSERVER_OK:
                sendGetGroupInfoToBServer(sendParam.bean);
                break;
            case SEND_GET_GROUP_INFO_BSERVER_OK:
                sendGetGroupUsersToBServer(sendParam.bean);
                break;
            case SEND_GET_GROUP_USERS_BSERVER_OK:
                sendJoinChatRoomToMServer(sendParam.bean);
                break;
            case SEND_JOIN_CHAT_ROOM_MSERVER_OK:
                sendApplyInGroupToMServer(sendParam.bean);
                break;
            case SEND_APPLY_IN_GROUP_MSERVER_OK:
                buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam.Send
                        .UPDATE_LOCAL_CACHE_DATA_OK, sendParam.bean);
                break;
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                ApplyInGroupEvent externalEvent = new ApplyInGroupEvent(ApplyInGroupEvent.Event
                        .SEND_APPLY_IN_GROUP_OK, sendParam.bean);
                triggerEvent(externalEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.join_group_send_success, 0);
                    }
                });
                break;
            case SEND_JOIN_IN_GROUP_BSERVER_FAILED:
            case SEND_GET_GROUP_INFO_BSERVER_FAILED:
            case SEND_GET_GROUP_USERS_BSERVER_FAILED:
            case SEND_JOIN_CHAT_ROOM_MSERVER_FAILED:
            case SEND_APPLY_IN_GROUP_MSERVER_FAILED:
            case UPDATE_LOCAL_CACHE_DATA_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtil.TextIntToast(context, R.string.join_group_send_failed, 0);
                    }
                });
                break;

        }
    }


    private void sendJoinInGroupToBServer(final ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId,inviteCode",
                applyInGroupBean.groupId, applyInGroupBean.inviteCode);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam
                .JOININGROUP;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject  jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    String desc = JSONUtils.getString(jsonRoot, "desc");
                    if(status == 0) {
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_JOIN_IN_GROUP_BSERVER_OK, applyInGroupBean);
                    } else {
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_JOIN_IN_GROUP_BSERVER_FAILED, applyInGroupBean);
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                        .Send.SEND_JOIN_IN_GROUP_BSERVER_FAILED, applyInGroupBean);
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


    private void sendGetGroupInfoToBServer(final ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId",
                applyInGroupBean.groupId);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam
                .GETGROUPINFO;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject  jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    String desc = JSONUtils.getString(jsonRoot, "desc");
                    if(status == 0) {
                        //更新数据
                        JSONObject jsonGroup = JSONUtils.getJSONObject(jsonRoot, "group", null);
                        saveGroupInfo(jsonGroup);
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_GET_GROUP_INFO_BSERVER_OK, applyInGroupBean);
                    } else {
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_GET_GROUP_INFO_BSERVER_FAILED, applyInGroupBean);
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                        .Send.SEND_GET_GROUP_INFO_BSERVER_FAILED, applyInGroupBean);
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


    private void sendGetGroupUsersToBServer(final ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId",
                applyInGroupBean.groupId);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam
                .GETGROUPUSERS;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject  jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    String desc = JSONUtils.getString(jsonRoot, "desc");
                    if(status == 0) {
                        //更新数据
                        JSONArray jsonUsers = JSONUtils.getJSONArray(jsonRoot, "users", null);
                        saveGroupMembers(applyInGroupBean.groupId, jsonUsers);
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_GET_GROUP_USERS_BSERVER_OK, applyInGroupBean);
                    } else {
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_GET_GROUP_USERS_BSERVER_FAILED, applyInGroupBean);
                    }
                }
            }
            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                        .Send.SEND_GET_GROUP_USERS_BSERVER_FAILED, applyInGroupBean);
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

    private void sendJoinChatRoomToMServer(final ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        String peerId = applyInGroupBean.groupId+"@"+userInfoBean.getmMucServiceName()+"."
                +userInfoBean.getmServiceName();
        //加入聊天室
        try {
            socketService.joinChatRoom(peerId, System.currentTimeMillis());
            buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam.Send
                    .SEND_JOIN_CHAT_ROOM_MSERVER_OK, applyInGroupBean);
        } catch (JUJUXMPPException e) {
            buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam.Send
                    .SEND_JOIN_CHAT_ROOM_MSERVER_FAILED, applyInGroupBean);
            e.printStackTrace();
        } catch (XMPPException e) {
            buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam.Send
                    .SEND_JOIN_CHAT_ROOM_MSERVER_FAILED, applyInGroupBean);
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam.Send
                    .SEND_JOIN_CHAT_ROOM_MSERVER_FAILED, applyInGroupBean);
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam.Send
                    .SEND_JOIN_CHAT_ROOM_MSERVER_FAILED, applyInGroupBean);
            e.printStackTrace();
        }
    }

    private void sendApplyInGroupToMServer(final ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        String peerId = applyInGroupBean.groupId+"@"+userInfoBean.getmMucServiceName()+"."
                +userInfoBean.getmServiceName();
        String message = JacksonUtil.turnObj2String(applyInGroupBean);
        String uuid = UUID.randomUUID().toString();

        //通知用户
        notifyMessage4Group(peerId, message,
                IMBaseDefine.NotifyType.APPLY_IN_GROUP, uuid, true,
                new XMPPServiceCallbackImpl() {
                    @Override
                    public void onSuccess(Object t) {
                        logger.d("ApplyInGroupNotify#sendMasterTransferToMServer success");
                        if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
                            XMPPServiceImpl.ReplayMessageTime messageTime =
                                    (XMPPServiceImpl.ReplayMessageTime) t;
                            String id = messageTime.getId();
                            String time = messageTime.getTime();
                            long replyTime = Long.parseLong(time);
                            applyInGroupBean.replyId = id;
                            applyInGroupBean.replyTime = replyTime;
                            imOtherManager.updateOtherMessage(id, replyTime);
                            buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                    .Send.SEND_APPLY_IN_GROUP_MSERVER_OK, applyInGroupBean);
                        } else {
                            buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                    .Send.SEND_APPLY_IN_GROUP_MSERVER_FAILED, applyInGroupBean);
                        }
                    }

                    @Override
                    public void onFailed() {
                        logger.d("ApplyInGroupNotify#sendMasterTransferToMServer failed");
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_APPLY_IN_GROUP_MSERVER_FAILED, applyInGroupBean);
                    }

                    @Override
                    public void onTimeout() {
                        logger.d("ApplyInGroupNotify#sendMasterTransferToMServer timeout");
                        buildAndTriggerBusinessFlow4Send(ApplyInGroupEvent.BusinessFlow.SendParam
                                .Send.SEND_APPLY_IN_GROUP_MSERVER_FAILED, applyInGroupBean);
                    }
                });
    }

    /**
     * 构建请求业务流
     * @param send
     */
    private void buildAndTriggerBusinessFlow4Send(
            ApplyInGroupEvent.BusinessFlow.SendParam.Send send,
            ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        if(send == null)
            throw new IllegalArgumentException("ApplyInGroupNotify#send is null");

        ApplyInGroupEvent.BusinessFlow.SendParam sendParam = new ApplyInGroupEvent.BusinessFlow
                .SendParam(send, applyInGroupBean);
        triggerEvent(sendParam);
    }

    /*********************************处理响应**************************************************/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent4BusinessFlowRecvEvent(ApplyInGroupEvent.BusinessFlow.RecvParam recvParam) {
        switch (recvParam.recv) {
            case UPDATE_LOCAL_CACHE_DATA_OK:
                //抛出对外事件
                ApplyInGroupEvent externalEvent = new ApplyInGroupEvent(ApplyInGroupEvent.Event
                        .RECV_APPLY_IN_GROUP_OK, recvParam.bean);
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

    private void recvUpdateLocalData(ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        GroupEntity groupEntity = imGroupManager.findGroupById(applyInGroupBean.groupId);
        if(groupEntity != null) {
            imGroupManager.updateGroup4Members(applyInGroupBean.groupId, applyInGroupBean.userNo,
                    applyInGroupBean.replyTime, 0);
            buildAndTriggerBusinessFlow4Recv(ApplyInGroupEvent.BusinessFlow.RecvParam.
                    Recv.UPDATE_LOCAL_CACHE_DATA_OK, applyInGroupBean);
        }
    }

    /**
     * 构建接收业务流
     * @param recv
     */
    private void buildAndTriggerBusinessFlow4Recv(
            ApplyInGroupEvent.BusinessFlow.RecvParam.Recv recv,
            ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean) {
        if(recv == null)
            throw new IllegalArgumentException("ApplyInGroupNotify#recv is null");

        ApplyInGroupEvent.BusinessFlow.RecvParam recvParam = new ApplyInGroupEvent.BusinessFlow
                .RecvParam(recv, applyInGroupBean);
        triggerEvent(recvParam);
    }

    private void saveGroupInfo(JSONObject jsonObject) {
        String id = JSONUtils.getString(jsonObject, "id");
        String name = JSONUtils.getString(jsonObject, "name");
        String desc = JSONUtils.getString(jsonObject, "desc");
        String creatorNo = JSONUtils.getString(jsonObject, "creatorNo");
        String createTime = JSONUtils.getString(jsonObject, "createTime");
        String masterNo = JSONUtils.getString(jsonObject, "masterNo");
        String peerId = id+"@"+userInfoBean.getmMucServiceName()+"."+userInfoBean.getmServiceName();
        int groupType = DBConstant.GROUP_TYPE_NORMAL;
        String userNos = "";
        Date created = null;
        try {
            created = DateUtils.parseDate(createTime, new String[]{"yyyy-MM-dd HH:mm:ss"});
        } catch (ParseException e) {
            e.printStackTrace();
        }
        GroupEntity groupEntity = GroupEntity.buildForReceive(id,  peerId,  groupType,  name,
                 userNos,  creatorNo, masterNo,  desc, created, null);
        groupDao.replaceInto(groupEntity);
        imGroupManager.getGroupMap().put(peerId, groupEntity);
    }

    public void saveGroupMembers(String groupId, JSONArray jsonArray) {
        if(jsonArray != null) {
            StringBuffer sbf = new StringBuffer();
            for (int i = 0; i <jsonArray.length() ; i++) {
                try {
                    JSONObject jsonUser = jsonArray.getJSONObject(i);
                    String userNo = JSONUtils.getString(jsonUser, "userNo");
                    String nickName = JSONUtils.getString(jsonUser, "nickName");
                    String userPhone = JSONUtils.getString(jsonUser, "userPhone");
                    String birthday = JSONUtils.getString(jsonUser, "birthday");
                    int gender = JSONUtils.getInt(jsonUser, "gender", 2);
                    String createTime = JSONUtils.getString(jsonUser, "createTime");

                    Date birthdayDate = null;
                    Date createTimeDate = null;
                    try {
                        birthdayDate = DateUtils.parseDate(birthday, new String[]{"yyyy-MM-dd HH:mm:ss"});
                        createTimeDate = DateUtils.parseDate(createTime, new String[]{"yyyy-MM-dd HH:mm:ss"});
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    User user = User.buildForReceive(userNo,  userPhone,  null,  gender, nickName,
                            createTimeDate,  birthdayDate);
                    userDao.replaceInto(user);
                    imContactManager.getUserMap().put(user.getUserNo(), user);
                    sbf.append(userNo);
                    if(i < jsonArray.length() -1) {
                        sbf.append(",");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            GroupEntity dbGroup = (GroupEntity) groupDao.findUniByProperty("id", groupId);
            if(dbGroup != null) {
                dbGroup.setUserList(sbf.toString());
                dbGroup.setUserCnt(sbf.toString().split(",").length);
                groupDao.replaceInto(dbGroup);
                imGroupManager.getGroupMap().put(dbGroup.getPeerId(), dbGroup);
            }
        }
    }
}
