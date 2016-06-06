package com.juju.app.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.party.PartyCreateActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.annotation.SystemColor;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.JoinChatRoomEvent;
import com.juju.app.event.UnreadEvent;
import com.juju.app.fragment.GroupChatFragment;
import com.juju.app.fragment.GroupPartyFragment;
import com.juju.app.fragment.MeFragment;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMMessageManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;

import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.view.dialog.titlemenu.ActionItem;
import com.juju.app.view.dialog.titlemenu.TitlePopup;
import com.juju.app.view.dialog.titlemenu.TitlePopup.OnItemOnClickListener;
import com.rey.material.app.Dialog;

import org.apache.http.message.BasicNameValuePair;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ContentView(R.layout.activity_main)
@CreateUI(showTopView = true)
public class MainActivity extends BaseActivity implements CreateUIHelper, HttpCallBack4OK {

    private final String TAG = getClass().getSimpleName();


    private final int CREATE_GROUP = 0x01;

    private final int DELETE_GROUP = 0x02;

//    private final int GET_GROUP_INVITE_CODE = 0x03;

    private final int JOIN_IN_GROUP = 0x03;

    private Handler uiHandler = new Handler();



    private Logger logger = Logger.getLogger(MainActivity.class);


//    @ViewInject(R.id.img_right)
//    private ImageView img_right;
//
//    @ViewInject(R.id.txt_title)
//    private TextView txt_title;
//
    @ViewInject(R.id.layout_bar)
    private RelativeLayout layout_bar;

    @ViewInject(R.id.unread_msg_number)
    private TextView tx_unread_msg_number;


    private GroupChatFragment groupChatFragment;
    private GroupPartyFragment groupPartyFragment;
    private MeFragment meFragment;
    private TitlePopup titlePopup;


    private Fragment[] fragments;
    private ImageView[] imagebuttons;
    private TextView[] textviews;
    private int index;
    private int currentTabIndex;// 当前fragment的index

    private UserInfoBean userInfoBean;

    private IMService imService;

    private DaoSupport groupDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onDestroy() {
        imServiceConnector.disconnect(MainActivity.this);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void loadData() {
        imServiceConnector.connect(MainActivity.this);
        if(GlobalVariable.isSkipLogin()){
            return;
        }
        groupDao = new GroupDaoImpl(getApplicationContext());
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
    }

    @Override
    public void initView() {
        long begin = System.currentTimeMillis();
        initTopTile();
        initTabView();
        initPopWindow();
        long end = System.currentTimeMillis();
        Log.d(TAG, "init MainActivity cost time:" + (end - begin) + "毫秒");
    }

    /**
     * 初始化TabView
     */
    private void initTabView() {
        groupChatFragment = new GroupChatFragment();
        groupPartyFragment = new GroupPartyFragment();
        meFragment = new MeFragment();
        fragments = new Fragment[] { groupChatFragment, groupPartyFragment,
                meFragment };
        imagebuttons = new ImageView[3];
        imagebuttons[0] = (ImageView) findViewById(R.id.ib_group_chat);
        imagebuttons[1] = (ImageView) findViewById(R.id.ib_group_party);
        imagebuttons[2] = (ImageView) findViewById(R.id.ib_profile);
        imagebuttons[0].setSelected(true);

        textviews = new TextView[3];
        textviews[0] = (TextView) findViewById(R.id.tv_group_chat);
        textviews[1] = (TextView) findViewById(R.id.tv_group_party);
        textviews[2] = (TextView) findViewById(R.id.tv_profile);
        textviews[0].setTextColor(getResources().getColor(R.color.blue));

        // 添加显示第一个fragment

//        getFragmentManager().beginTransaction().

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, groupChatFragment)
                .add(R.id.fragment_container, groupPartyFragment)
                .add(R.id.fragment_container, meFragment)
                .hide(groupPartyFragment).hide(meFragment).show(groupChatFragment).commit();

//        loadingCommon(R.string.add_party);
    }

    public void onTabClicked(View view) {
        //隐藏TOP右边信息
        hideTopRightAll();
        switch (view.getId()) {
            case R.id.re_group_chat:
//                img_right.setVisibility(View.VISIBLE);
                setTopRightButton(0);
                index = 0;
                if (groupChatFragment != null) {
                    groupChatFragment.refresh();
                }
//                txt_title.setText(R.string.group_chat);
//                img_right.setImageResource(R.mipmap.icon_add);
                if(imService.getUnReadMsgManager().isUnreadListReady()) {
                    setTopTitle(R.string.group_chat);
                } else {
                    setTopTitle(R.string.message_receive_loading);
                }
                setTopRightButton(R.mipmap.icon_add);
                break;
            case R.id.re_group_party:
                index = 1;
//                txt_title.setText(R.string.group_party);
//                img_right.setVisibility(View.VISIBLE);
//                img_right.setImageResource(R.mipmap.icon_add);
                setTopTitle(R.string.group_party);
                setTopRightButton(R.mipmap.icon_add);
                break;
            case R.id.re_profile:
                index = 2;
//                txt_title.setText(R.string.me);
                setTopTitle(R.string.me);
                break;
        }
        if (currentTabIndex != index) {
            FragmentTransaction trx = getSupportFragmentManager()
                    .beginTransaction();
            trx.hide(fragments[currentTabIndex]);
            if (!fragments[index].isAdded()) {
                trx.add(R.id.fragment_container, fragments[index]);
            }
            trx.show(fragments[index]).commit();
        }
        imagebuttons[currentTabIndex].setSelected(false);
        // 把当前tab设为选中状态
        imagebuttons[index].setSelected(true);
        textviews[currentTabIndex].setTextColor(getResources().getColor(R.color.gray));
        if(index == 1){
            textviews[index].setTextColor(getResources().getColor(R.color.white));
        }else {
            textviews[index].setTextColor(getResources().getColor(R.color.blue));
        }
        currentTabIndex = index;
    }


    private OnItemOnClickListener onitemClick = new OnItemOnClickListener() {

        @Override
        public void onItemClick(ActionItem item, int position) {
            switch (position) {
                case 0:// 创建群
                    showCreateGroupChatDialog(SelectType.ADD_GROUP);
//                    startActivity(MainActivity.this, AddGroupChatActivity.class);
                    break;
                case 1:// 扫一扫加群
                    break;
                case 2:// 邀请码加群
                    showCreateGroupChatDialog(SelectType.INVITE_GROUP);
                    break;
                default:
                    break;
            }
        }
    };

    private void initPopWindow() {
        // 实例化标题栏弹窗
        titlePopup = new TitlePopup(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titlePopup.setItemOnClickListener(onitemClick);
        // 给标题栏弹窗添加子类
        titlePopup.addAction(new ActionItem(this, R.string.menu_group,
                R.mipmap.icon_menu_group));
        titlePopup.addAction(new ActionItem(this, R.string.menu_qrcode,
                R.mipmap.icon_menu_qrcode));
        titlePopup.addAction(new ActionItem(this, R.string.menu_invitecode,
                R.mipmap.icon_menu_invitecode));

    }


    @Event(value = R.id.img_right, type = View.OnClickListener.class)
    private void clickImgRight(View v) {
        switch(index) {
            case 0: //  群聊
                titlePopup.show(layout_bar);
                break;
            case 1: //  聚会
                //TODO 需要修改为从群聊中发起聚会
                String groupId = "570dbc6fe4b092891a647e32";
                BasicNameValuePair groupIdValue = new BasicNameValuePair(Constants.GROURP_ID,groupId);
                ActivityUtil.startActivity(this,PartyCreateActivity.class,groupIdValue);
                break;
        }
    }


//    private void sendMessage() {
//        int i = 0;
//        IMLoginManager.instance().sendMessage("ceshi@conference.juju", "在线吗？" + i);
//    }

    //加入聊天室
    private void joinChatRoom() {
//        try {
//            Thread.sleep(2000l);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        IMLoginManager.instance().joinChatRoom();
    }

//    public String getRunningServicesInfo(Context context) {
//        StringBuffer serviceInfo = new StringBuffer();
//        final ActivityManager activityManager = (ActivityManager) context
//                .getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(100);
//
//        Iterator<ActivityManager.RunningServiceInfo> l = services.iterator();
//        while (l.hasNext()) {
//            ActivityManager.RunningServiceInfo si = (ActivityManager.RunningServiceInfo) l.next();
//            serviceInfo.append("pid: ").append(si.pid);
//            serviceInfo.append("\nprocess: "+si.process);
//            serviceInfo.append("\nservice: ").append(si.service);
//            serviceInfo.append("\ncrashCount: ").append(si.crashCount);
//            serviceInfo.append("\nclientCount: ").append(si.clientCount);
//            serviceInfo.append("\nactiveSince: ").append(si.activeSince);
//            serviceInfo.append("\nlastActivityTime: ").append(si.activeSince);
//            serviceInfo.append(";");
//        }
//        return serviceInfo.toString();
//    }


    //显示未读消息总数
    public void setUnreadMessageCnt(int unreadNum) {

        logger.d("unread#setUreadNotify -> unreadNum:%d", unreadNum);
        if (0 == unreadNum) {
            tx_unread_msg_number.setVisibility(View.INVISIBLE);
            return;
        }

        String notify;
        if (unreadNum > 99) {
            notify = "99+";
        } else {
            notify = Integer.toString(unreadNum);
        }
        tx_unread_msg_number.setText(notify);
        tx_unread_msg_number.setVisibility(View.VISIBLE);
    }

    Dialog mDialog;
    View view = null;
    SelectType selectType;
    private void showCreateGroupChatDialog(final SelectType _selectType) {
        this.selectType = _selectType;
        if(view == null) {
            view = MainActivity.this.getLayoutInflater()
                    .inflate(R.layout.activity_add_group_chat, null);
        }
        final com.rey.material.widget.EditText et_name = (com.rey.material.widget.EditText) view
                .findViewById(R.id.et_name);
        final com.rey.material.widget.EditText et_description = (com.rey.material.widget.EditText) view
                .findViewById(R.id.et_description);
        com.rey.material.widget.EditText et_invite_code = (com.rey.material.widget.EditText) view
                .findViewById(R.id.et_invite_code);
        int title = 0;
        int width = (ScreenUtil.getScreenWidth(MainActivity.this) / 3) * 2;
        int height = ScreenUtil.getScreenHeight(MainActivity.this) / 2;
        switch (selectType) {
            case ADD_GROUP:
                title = R.string.menu_group;
                et_name.setVisibility(View.VISIBLE);
                et_description.setVisibility(View.VISIBLE);
                et_invite_code.setVisibility(View.GONE);
                break;
            case INVITE_GROUP:
                title = R.string.menu_invitecode;
                et_name.setVisibility(View.GONE);
                et_description.setVisibility(View.GONE);
                et_invite_code.setVisibility(View.VISIBLE);
                height = (height/3) * 2;
                break;
        }


        if(mDialog == null) {
            mDialog = new Dialog(context, R.style.SimpleDialog);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog
                    .positiveAction(R.string.confirm)
                    .negativeAction(R.string.negative)
                    .contentView(view)
                    .maxWidth(width+100)
                    .maxHeight(height+200)
                    .positiveActionTextAppearance(R.style.other)
                    .negativeActionTextAppearance(R.style.other)
                    .cancelable(true);

            /**
             * 确定按钮监听事件
             */
            mDialog.positiveActionClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDialog != null) {
                        switch (selectType) {
                            case ADD_GROUP:
                                loadingCommon(R.string.chat_create_loading);
                                String name = et_name.getText().toString();
                                String description = et_description.getText().toString();
                                createGroup(name, description);
                                break;
                            case INVITE_GROUP:
                                //TODO
                                loadingCommon(R.string.invite_code_loading);
                                break;
                        }

                    }
                }
            });

            /**
             * 取消按钮监听事件
             */
            mDialog.negativeActionClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDialog != null) {
                        mDialog.hide();
                    }
                }
            });
        }
        mDialog.layoutParams(width, height);
        mDialog.setTitle(title);
        mDialog.show();

    }

    @Override
    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
        if(accessId == CREATE_GROUP) {
            completeLoadingCommon();
            if(obj instanceof  JSONObject) {
                JSONObject result = (JSONObject)obj;
                handlerHttpRsp4CreateGroup(result, inputParameter);
            }
        } else if (accessId == JOIN_IN_GROUP) {
            completeLoadingCommon();
            if(obj instanceof  JSONObject) {
                JSONObject result = (JSONObject)obj;
                handlerHttpRsp4JoinInGroup(result, inputParameter);
            }
        }
//        //获取群组邀请码
//        else if (accessId == GET_GROUP_INVITE_CODE) {
//            if(obj instanceof  JSONObject) {
//                JSONObject result = (JSONObject)obj;
//                int status = JSONUtils.getInt(result, "status");
//                if(status == 0) {
//                    Map<String, Object> parameter = (Map<String, Object>)inputParameter;
//                    String groupId = (String)parameter.get("groupId");
//                    String inviteCode = JSONUtils.getString(result, "inviteCode");
//                    String peerId = groupId+"@"+userInfoBean.getmMucServiceName()
//                            +"."+userInfoBean.getmServiceName();
//                    GroupEntity groupEntity = imService.getGroupManager().findGroup(peerId);
//                    if(groupEntity != null) {
//                        //保存二维码
//                        String qrCode =
//                                HttpConstants.getUserUrl() + "/joinInGroup?inviteCode="+inviteCode;
//                        groupEntity.setQrCode(qrCode);
//                        //保存邀请码
//                        groupEntity.setInviteCode(inviteCode);
//                        groupDao.replaceInto(groupEntity);
//                    }
//                }
//            }
//        }
    }

    @Override
    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
        if(accessId == CREATE_GROUP) {
            completeLoadingCommon();
            showMsgDialog(R.string.chat_create_error);
        } else if (accessId == JOIN_IN_GROUP) {
            completeLoadingCommon();
            showMsgDialog(R.string.invite_group_no_pass);
        }
    }


    /**
         * IMServiceConnector
         */
        private IMServiceConnector imServiceConnector = new IMServiceConnector() {
            @Override
            public void onIMServiceConnected() {
                logger.d("main_activity#onIMServiceConnected");
                imService = imServiceConnector.getIMService();
            }

            @Override
            public void onServiceDisconnected() {

            }
        };

    private void initTopTile() {
        setTopRightButton(0);
        setTopTitle(R.string.message_receive_loading);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent4Unread(UnreadEvent event) {
        switch (event.event){
            case UNREAD_MSG_LIST_OK:
                uiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setUnreadMessageCnt(imService.getUnReadMsgManager().getTotalUnreadCount());
                        if(index == 0) {
                            setTopTitle(R.string.group_chat);
                        }
                    }
                }, 1000);
                break;
        }
    }


    //创建群聊 （考虑放在IMGroupManager）
    private void createGroup(String name, String description) {
        if (StringUtils.isBlank(name)) {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.chat_name_null, 0);
            return;
        }
        if (name.length() > 20) {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.chat_name_over_length, 0);
            return;
        }
        Map<String, Object> valueMap = new HashMap<>();
        Map<String, Object> groupMap = new HashMap<>();

        valueMap.put("userNo", userInfoBean.getJujuNo());
        valueMap.put("token", userInfoBean.getToken());
        groupMap.put("name", name);
        if (StringUtils.isNotBlank(description)) {
            groupMap.put("desc", description);
        }
        valueMap.put("group", groupMap);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                CREATE_GROUP, HttpConstants.getUserUrl() + "/addGroup",
                MainActivity.this, valueMap, JSONObject.class);
        try {
            client.sendPost4OK();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //邀请码加群（考虑放在IMGroupManager）
    private void inviteGroup(String inViteCode) {
        if (StringUtils.isBlank(inViteCode)) {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.chat_name_null, 0);
            return;
        }
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("userNo", userInfoBean.getJujuNo());
        valueMap.put("token", userInfoBean.getToken());
        valueMap.put("inviteCode", inViteCode);


        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                JOIN_IN_GROUP, HttpConstants.getUserUrl() + "/joinInGroup",
                MainActivity.this, valueMap, JSONObject.class);
        try {
            client.sendPost4OK();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //处理创建群聊消息回复
    private void handlerHttpRsp4CreateGroup(JSONObject result, Object inputParameter) {
        int status = JSONUtils.getInt(result, "status");
        if(status == 0) {
            String groupId = JSONUtils.getString(result,"groupId");
            //消息服务器创建群组
            boolean bool = imService.getGroupManager().createChatRoom(groupId,
                    userInfoBean.getmMucServiceName(), userInfoBean.getmServiceName());
            //创建成功
            if(bool) {
                //刷新群组，待实现
                Log.d(TAG, "创建群聊成功，groupId："+groupId);
                //刷新群组列表
                imService.getGroupManager();
                String peerId = groupId+"@"+userInfoBean.getmMucServiceName()
                        +"."+userInfoBean.getmServiceName();

                String name = "";
                String desc = "";
                if(inputParameter instanceof Map) {
                    Map<String, Object> parameter = (Map<String, Object>) inputParameter;
                    Map<String, Object> group = (Map<String, Object>)parameter.get("group");
                    name = (String)group.get("name");
                    desc = (String)group.get("desc");
                }

                GroupEntity groupEntity = GroupEntity.buildForCreate(groupId, peerId,
                        DBConstant.GROUP_TYPE_NORMAL, name,  userInfoBean.getJujuNo(),  desc,  null);

                //更新数据
                groupDao.replaceInto(groupEntity);


                //设置置顶
                //暂时用APP时间
                Date created = new Date();
                groupEntity.setCreated(created.getTime());


                //发送群组更新通知
                imService.getGroupManager().getGroupMap().put(peerId, groupEntity);
                triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_UPDATED));

                //获取群组请码
                imService.getGroupManager().getGroupInviteCode(groupId);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        com.rey.material.widget.EditText et_name = (com.rey.material.widget.EditText) view
                                .findViewById(R.id.et_name);
                        com.rey.material.widget.EditText et_description = (com.rey.material.widget.EditText) view
                                .findViewById(R.id.et_description);

                        et_name.setText("");
                        et_description.setText("");
                    }
                });
            }
            //创建失败
            else {
                //删除业务服务器群组消息
                Map<String, Object> valueMap = new HashMap<String, Object>();
                valueMap.put("userNo", userInfoBean.getJujuNo());
                valueMap.put("token", userInfoBean.getToken());
                valueMap.put("groupId", groupId);
                JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                        DELETE_GROUP, HttpConstants.getUserUrl() + "/deleteGroup",
                        MainActivity.this, valueMap, JSONObject.class);
                try {
                    //不需要回复
                    client.sendPost4OK();
                } catch (UnsupportedEncodingException e) {
                    logger.error(e);
                } catch (JSONException e) {
                    logger.error(e);
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMsgDialog(R.string.chat_create_error);
                    }
                });
            }
        } else {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showMsgDialog(R.string.chat_create_error);
                }
            });
        }
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //退出创建提示框
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.hide();
                }
            }
        });
    }

    //处理创建群聊消息回复
    private void handlerHttpRsp4JoinInGroup(JSONObject result, Object inputParameter) {
        int status = JSONUtils.getInt(result, "status");
        if(status == 0) {
            int passFlag = JSONUtils.getInt(result, "passFlag");
            if(passFlag == 0) {
                showMsgDialog4Main(R.string.invite_group_no_pass);
            }
        } else {
            showMsgDialog4Main(R.string.invite_group_no_pass);
        }
    }

    public enum SelectType {
        ADD_GROUP, INVITE_GROUP
    }




}
