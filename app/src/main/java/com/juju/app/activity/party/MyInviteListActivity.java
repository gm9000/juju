package com.juju.app.activity.party;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.chat.ChatActivity;
import com.juju.app.adapters.MyInviteListAdapter;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.InviteDaoImpl;
import com.juju.app.entity.Invite;
import com.juju.app.entity.Party;
import com.juju.app.entity.chat.OtherMessageEntity;
import com.juju.app.entity.chat.RecentInfo;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.NotificationMessageEvent;
import com.juju.app.event.NotifyMessageEvent;
import com.juju.app.event.user.InviteGroupEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;
import com.juju.app.service.im.service.SocketService;
import com.juju.app.service.im.service.XMPPServiceImpl;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;

import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ContentView(R.layout.layout_my_invite_list)
public class MyInviteListActivity extends BaseActivity implements MyInviteListAdapter.Callback {

    private static final String TAG = "MyInviteListActivity";

    private Logger logger = Logger.getLogger(MyInviteListActivity.class);



    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.txt_right)
    private TextView txt_right;
    @ViewInject(R.id.img_right)
    private ImageView img_right;


    @ViewInject(R.id.listview_invite)
    private ListView listViewInvite;

    private MyInviteListAdapter inviteListAdapter;

//    private Map<String, Invite> inviteMap;

    private List<Invite> inviteList;

    private String userNo;

    private DaoSupport inviteDao;

    private IMService imService;

    private UserInfoBean userInfoBean;

    private InviteGroupTask inviteGroupTask = null;

    private Handler uiHandler = null;

    private final static int INVITE_GROUP_NOTIFY_REQ = 0x01;
    private final static int INVITE_GROUP_NOTIFY_RES = 0x02;


//    /**
//     * IMServiceConnector
//     */
//    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
//        @Override
//        public void onIMServiceConnected() {
//            logger.d("MyInviteListActivity#onIMServiceConnected");
//            imService = imServiceConnector.getIMService();
//            inviteGroupTask = new InviteGroupTask();
//            uiHandler = new Handler() {
//                @Override
//                public void handleMessage(Message msg) {
//                    super.handleMessage(msg);
//                    NotifyMessageEvent event = (NotifyMessageEvent)msg.obj;
//                    OtherMessageEntity otherMessageEntity = OtherMessageEntity.buildMessage4Recv(event.msgType,
//                            event.message, event.message.getStanzaId());
//                    //TODO 是否通知IMNotificationManager
//                    InviteGroupEvent inviteGroupEvent = new InviteGroupEvent();
//                    switch (msg.what) {
//                        case INVITE_GROUP_NOTIFY_REQ:
//                            Invite inviteReq = Invite.buildInviteReq4Recv(otherMessageEntity);
//                            inviteDao.save(inviteReq);
//                            inviteGroupEvent.event = InviteGroupEvent.Event.INVITE_GROUP_NOTIFY_REQ_SUCCESS;
//                            inviteGroupEvent.invite = inviteReq;
//                            triggerEvent(inviteGroupEvent);
//                            break;
//                        case INVITE_GROUP_NOTIFY_RES:
//                            IMBaseDefine.InviteGroupNotifyResBean resBean = (IMBaseDefine.InviteGroupNotifyResBean)
//                                    JacksonUtil.turnString2Obj(otherMessageEntity.getContent(),
//                                            IMBaseDefine.NotifyType.INVITE_GROUP_NOTIFY_RES.getCls());
//                            if(resBean != null
//                                    && StringUtils.isNotBlank(resBean.code)
//                                    && StringUtils.isNotBlank(resBean.groupId)) {
//                                //通过 code+groupId 确认消息
//                                Invite dbInvite = (Invite) inviteDao
//                                        .findUniByProperty("invite_code,group_id", resBean.code, resBean.groupId);
//                                Invite inviteRes = Invite.buildInviteRes4Recv(dbInvite, otherMessageEntity);
//                                inviteDao.saveOrUpdate(inviteRes);
//                                inviteGroupEvent.event = InviteGroupEvent.Event.INVITE_GROUP_NOTIFY_RES_SUCCESS;
//                                inviteGroupEvent.invite = inviteRes;
//                                triggerEvent(inviteGroupEvent);
//
//                                //通知更新groupEntity
//                                imService.getGroupManager().updateGroup4Members(resBean.groupId,
//                                        resBean.userNo, inviteRes.getTime().getTime());
//                            }
//                            break;
//                    }
//
//                }
//            };
//        }
//
//        @Override
//        public void onServiceDisconnected() {
//
//        }
//    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        imServiceConnector.connect(MyInviteListActivity.this);
//        EventBus.getDefault().register(MyInviteListActivity.this);
        super.onCreate(savedInstanceState);
        initParam();
        initData();
        initView();

        triggerEvent(new NotificationMessageEvent(NotificationMessageEvent.Event
                .INVITE_GROUP_NOTIFY_OPEN_ACTIVITY));
//        INVITE_GROUP_NOTIFY_RES_RECEIVED
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(MyInviteListActivity.this);
//        imServiceConnector.disconnect(MyInviteListActivity.this);
        super.onDestroy();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(JujuDbUtils.needRefresh(Party.class)){
            initData();
        }
    }



    private void initData() {
        inviteList = inviteDao.findAll4Order("local_id:desc");
        wrapInviteList(inviteList);
    }

    private void wrapInviteList(List<Invite> inviteList) {
        inviteListAdapter = new MyInviteListAdapter(this,inviteList,this);
        listViewInvite.setAdapter(inviteListAdapter);
        listViewInvite.setCacheColorHint(0);
    }


    private void initView() {
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.group_invite);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);
    }


    public void initParam() {
//        inviteMap = new HashMap<>();
        inviteDao = new InviteDaoImpl(getApplicationContext());
        userInfoBean = AppContext.getUserInfoBean();
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }

    @Event(R.id.img_back)
    private void goBack(View view){
        ActivityUtil.finish(this);
    }

    @Override
    public void checkFailInvite(int position) {
//        Invite invite = inviteList.get(position);
////        invite.setStatus(0);
////        JujuDbUtils.saveOrUpdate(invite);
//        //TODO 邀请忽略，将通知后台服务
//        if(inviteGroupTask != null) {
//            //初始化流程
//            inviteGroupTask.initTaskParam(invite, 0);
//            //回复消息
//            inviteGroupTask.sendInviteGroupNotifyResToMServer(invite, 0);
//            //拒绝
////            imService.getOtherManager().sendInviteGroupNotifyResToMServer(invite, 0);
//        }

    }

    @Override
    public void passOrDeleteInvite(int position) {
        Invite invite = inviteList.get(position);
        //  接收的邀请消息，接收的操作请求
        if(invite.getFlag() == 1
                && invite.getStatus() == 0){
            if(inviteGroupTask != null) {
                //初始化流程
                inviteGroupTask.initTaskParam(invite, 1);
                //加入群组
                inviteGroupTask.sendJoinInGroup2BServer(invite);
            }
        //  发出的邀请信息，进行删除操作
        }else{
            Invite dbInvite = (Invite) inviteDao.findUniByProperty("id", invite.getId());
            if(dbInvite != null) {
                inviteDao.delete(dbInvite);
            }
            inviteList.remove(position);
            inviteListAdapter.setInviteList(inviteList);
            inviteListAdapter.notifyDataSetChanged();
        }
    }



//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEvent4InviteGroup(InviteGroupEvent event) {
//        switch (event.event) {
//            case INVITE_GROUP_NOTIFY_REQ_SUCCESS:
//            case INVITE_GROUP_NOTIFY_RES_SUCCESS:
//                updateInviteList(event.invite);
//                sort(inviteList);
//                inviteListAdapter.setInviteList(inviteList);
//                inviteListAdapter.notifyDataSetChanged();
//                break;
//            //加入群组成功，需要获取群组详情
//            case JOINGROUP_SUCCESS:
//                inviteGroupTask.sendGetGroupInfoToBServer(event.invite);
//                break;
//
//            //获取群组详情成功，需要打开会话窗口
//            case GETGROUPINFO_SUCCESS:
//                //回复消息
//                inviteGroupTask.sendInviteGroupNotifyResToMServer(event.invite, event.status);
//                ToastUtil.TextIntToast(getApplicationContext(), R.string.pass, 5);
//
//                //通知群组更新
//                GroupEvent groupEvent = new GroupEvent(GroupEvent.Event.GROUP_INFO_UPDATED);
//                triggerEvent(groupEvent);
//                String peerId = event.invite.getGroupId()+"@"+userInfoBean.getmMucServiceName()
//                        +"."+userInfoBean.getmServiceName();
//                String sessionId = DBConstant.SESSION_TYPE_GROUP + "_" + peerId;
//                startActivityNew(MyInviteListActivity.this,
//                        ChatActivity.class, Constants.SESSION_ID_KEY, sessionId);
//                break;
//
//            case JOINGROUP_FAILED:
//            case GETGROUPINFO_FAILED:
//                ToastUtil.TextIntToast(getApplicationContext(), R.string.message_send_failed, 5);
//                break;
//            case INVITE_GROUP_NOTIFY_REQ_FAILED:
//            case INVITE_GROUP_NOTIFY_RES_FAILED:
//                ToastUtil.TextIntToast(getApplicationContext(), R.string.action_failed, 5);
//                break;
//        }
//    }


    /**
     * 处理NotifyMessageEvent事件，不需要传递到OtherMessageEvent
     */
//    @Subscribe(threadMode = ThreadMode.POSTING, priority = Constants.MESSAGE_EVENTBUS_PRIORITY)
//    public void onNotifyMessage4Event(NotifyMessageEvent event) {
//        OtherMessageEntity otherMessageEntity = OtherMessageEntity.buildMessage4Recv(event.msgType,
//                event.message, event.message.getStanzaId());
//        switch (event.msgType) {
//            case INVITE_GROUP_NOTIFY_REQ:
//                Message messageReq = Message.obtain();
//                messageReq.what = INVITE_GROUP_NOTIFY_REQ;
//                messageReq.obj = event;
//                uiHandler.sendMessage(messageReq);
//                EventBus.getDefault().cancelEventDelivery(event);
//                break;
//            case INVITE_GROUP_NOTIFY_RES:
//                Message messageRes = Message.obtain();
//                messageRes.what = INVITE_GROUP_NOTIFY_REQ;
//                messageRes.obj = event;
//                uiHandler.sendMessage(messageRes);
//                EventBus.getDefault().cancelEventDelivery(event);
//                break;
//        }
//
//    }



//    private void updateInviteList(Invite invite) {
//        boolean isExists = false;
//        for (Invite oldInvite : inviteList) {
//            if(oldInvite.getInviteCode().equals(invite.getInviteCode())
//                    && oldInvite.getGroupId().equals(invite.getGroupId())) {
//                oldInvite.setMsgStatus(invite.getMsgStatus());
//                oldInvite.setTime(invite.getTime());
//                oldInvite.setStatus(invite.getStatus());
//                isExists = true;
//                break;
//            }
//        }
//        if(!isExists) {
//            inviteList.add(invite);
//        }
//    }

    //按时间排序
    private void sort(List<Invite> data) {
        Collections.sort(data, new Comparator<Invite>() {
            public int compare(Invite o1, Invite o2) {
                Long a = o1.getTime().getTime();
                Long b = o2.getTime().getTime();
                return b.compareTo(a);
            }
        });
    }


    /**
     * 处理加入群组业务任务，执行邀请加入群组事件流
     *
     *
     */
    static class InviteGroupTask {

        final static int GETGROUPINFO_TIMEOUT = 10;

        private InviteGroupEvent inviteGroupEvent;


        /**
         * 初始化任务参数
         * @param invite 邀请对象
         * @param status 状态 0：拒绝 1：加入
         */
        public void initTaskParam(Invite invite, int status) {
            inviteGroupEvent = new InviteGroupEvent();
//            inviteGroupEvent.event = InviteGroupEvent.Event.INVITE_GROUP_NOTIFY_REQ_SUCCESS;
            inviteGroupEvent.invite = invite;
            inviteGroupEvent.status = status;
        }

        /**
         *  加入群组
         *
         * @param invite
         */
        public void sendJoinInGroup2BServer(final Invite invite) {
//            Map<String, Object> valueMap = HttpReqParamUtil.instance()
//                    .buildMap("groupId, inviteCode", invite.getGroupId(), invite.getInviteCode());
//            CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.JOININGROUP;
//            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
//                    httpReqParam.url(), new HttpCallBack4OK() {
//                @Override
//                public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
//                    if(obj instanceof JSONObject) {
//                        JSONObject jsonRoot = (JSONObject)obj;
//                        int status = JSONUtils.getInt(jsonRoot, "status", -1);
//                        int passFlag = JSONUtils.getInt(jsonRoot, "passFlag", 0);
//                        //加入成功
//                        if(status == 0 && passFlag == 1) {
//                            inviteGroupEvent.event = InviteGroupEvent.Event
//                                    .JOINGROUP_SUCCESS;
//                            triggerEvent(inviteGroupEvent);
//                        }
//                        //加入失败
//                        else {
//                            inviteGroupEvent.event = InviteGroupEvent.Event
//                                    .JOINGROUP_FAILED;
//                            triggerEvent(inviteGroupEvent);
//                        }
//                    }
//                }
//
//                @Override
//                public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
//                    inviteGroupEvent.event = InviteGroupEvent.Event
//                            .JOINGROUP_FAILED;
//                    triggerEvent(inviteGroupEvent);
//                }
//            }, valueMap, JSONObject.class);
//            try {
//                client.sendPost4OK();
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            } catch (JSONException e) {
//                logger.error(e);
//            }
        }

        /**
         * 1: 获取群组详情
         * 2：获取群组成员列表
         * 3：加入群组
         * 4：通知（我——邀请）列表打开会话窗口
         * 应该拆分
         *
         * @param invite
         */
//        public void sendGetGroupInfoToBServer(final Invite invite) {
//            Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId", invite.getGroupId());
//            CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETGROUPINFO;
//            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
//                    httpReqParam.url(), new HttpCallBack4OK() {
//                @Override
//                public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
//                    if(obj instanceof JSONObject) {
//                        JSONObject jsonRoot = (JSONObject)obj;
//                        int status = JSONUtils.getInt(jsonRoot, "status", -1);
//                        if(status == 0) {
//                            JSONObject jsonGroup = null;
//                            try {
//                                jsonGroup = jsonRoot.getJSONObject("group");
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//
//                            if(jsonGroup != null) {
//                                String desc = JSONUtils.getString(jsonGroup, "desc");
//                                String id = JSONUtils.getString(jsonGroup, "id");
//                                String name = JSONUtils.getString(jsonGroup, "name");
//                                String creatorNo = JSONUtils.getString(jsonGroup, "creatorNo");
//                                String createTime = JSONUtils.getString(jsonGroup, "createTime");
//                                Date createTimeDate = null;
//                                try {
//                                    createTimeDate = DateUtils.parseDate(createTime, new String[] {"yyyy-MM-dd HH:mm:ss"});
//                                } catch (ParseException e) {
//                                    e.printStackTrace();
//                                }
//                                CountDownLatch countDownLatch = new CountDownLatch(1);
////                                imService.getGroupManager().sendGetGroupUsersToBServer(countDownLatch,
////                                        id, name, desc, creatorNo, createTimeDate);
//                                //TODO 是否需要考虑获取用户列表异常情况
//                                try {
//                                    countDownLatch.await(GETGROUPINFO_TIMEOUT, TimeUnit.SECONDS);
//                                    //TODO 加入消息服务群组,需要考虑失败的情况(暂时这样处理)
////                                    imService.getGroupManager().joinChatRooms(imService
////                                            .getGroupManager().getGroupMap().values());
////                                    inviteGroupEvent.event = InviteGroupEvent.Event.GETGROUPINFO_SUCCESS;
////                                    triggerEvent(inviteGroupEvent);
//                                } catch (InterruptedException e) {
////                                    logger.error(e);
//                                }
//                            }
//
//                        }
//                    }
//                }
//                @Override
//                public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
//                    inviteGroupEvent.event = InviteGroupEvent.Event.GETGROUPINFO_FAILED;
//                    triggerEvent(inviteGroupEvent);
//                }
//            }, valueMap, JSONObject.class);
//            try {
//                client.sendGet4OK();
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            } catch (JSONException e) {
//                logger.error(e);
//            }
//        }


        //发送加群、拒绝加群消息
//        public void sendInviteGroupNotifyResToMServer(final Invite invite, int inviteGroupNotifyBeanStatus) {
//            IMBaseDefine.InviteGroupNotifyResBean inviteGroupNotifyBean = IMBaseDefine.InviteGroupNotifyResBean
//                    .valueOf(invite.getInviteCode(), invite.getGroupId(), invite.getGroupName(), userInfoBean.getJujuNo(),
//                            userInfoBean.getUserName(), inviteGroupNotifyBeanStatus);
//            String message = JacksonUtil.turnObj2String(inviteGroupNotifyBean);
//            String uuid = UUID.randomUUID().toString();
//            imService.getOtherManager().getSocketService().notifyMessage(invite.getUserNo()+"@juju", message,
//                    IMBaseDefine.NotifyType.INVITE_GROUP_NOTIFY_RES, uuid, true,
//                    new XMPPServiceCallbackImpl() {
//                        @Override
//                        public void onSuccess(Object t) {
//                            logger.d("sendInviteGroupNotifyResToMServer success");
//                            if(t instanceof XMPPServiceImpl.ReplayMessageTime) {
//                                XMPPServiceImpl.ReplayMessageTime messageTime =
//                                        (XMPPServiceImpl.ReplayMessageTime) t;
//                                String id = messageTime.getId();
//                                String time = messageTime.getTime();
//
//                                Invite dbEntity = (Invite)
//                                        inviteDao.findUniByProperty("id", invite.getId());
//
//                                if(dbEntity != null) {
//                                    Invite.buildInviteRes4SendOnAck(dbEntity,
//                                            Long.parseLong(time));
//                                    //更新时间
//                                    inviteDao.saveOrUpdate(dbEntity);
//                                    inviteGroupEvent.event = InviteGroupEvent.Event
//                                            .INVITE_GROUP_NOTIFY_RES_SUCCESS;
//                                    inviteGroupEvent.invite = dbEntity;
//                                    triggerEvent(inviteGroupEvent);
//                                } else {
//                                    inviteGroupEvent.event = InviteGroupEvent.Event
//                                            .INVITE_GROUP_NOTIFY_RES_FAILED;
//                                    triggerEvent(inviteGroupEvent);
//                                }
//                            }
//                        }
//                        @Override
//                        public void onFailed() {
//                            logger.d("sendInviteGroupNotifyResToMServer failed");
//                            inviteGroupEvent.event = InviteGroupEvent.Event
//                                    .INVITE_GROUP_NOTIFY_RES_FAILED;
//                            triggerEvent(inviteGroupEvent);
//                        }
//
//                        @Override
//                        public void onTimeout() {
//                            logger.d("sendInviteGroupNotifyResToMServer timeout");
//                            inviteGroupEvent.event = InviteGroupEvent.Event
//                                    .INVITE_GROUP_NOTIFY_RES_FAILED;
//                            triggerEvent(inviteGroupEvent);
//                        }
//                    }, invite);
//        }
    }





}
