package com.juju.app.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.activity.MainActivity;
import com.juju.app.activity.chat.ChatActivity;
import com.juju.app.adapter.GroupChatListAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.RecentInfo;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.SessionEvent;
import com.juju.app.event.SmackSocketEvent;
import com.juju.app.event.UnreadEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.manager.IMUnreadMsgManager;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.ui.base.TitleBaseFragment;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.NetWorkUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.apache.http.message.BasicNameValuePair;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：群聊—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:09
 * 版本：V1.0.0
 *
 */
@ContentView(R.layout.fragment_group_chat)
@CreateFragmentUI(viewId = R.layout.fragment_group_chat)
public class GroupChatFragment extends TitleBaseFragment implements CreateUIHelper {

    protected static Logger logger = Logger.getLogger(GroupChatFragment.class);

    private Handler uiHandler = new Handler();

//    private MainActivity parentActivity;
//

//    @ViewInject(R.id.rl_error_item)
//    public RelativeLayout errorItem;
//    /**
//     * 异常提示文本组建
//     */
//    @ViewInject(R.id.tv_connect_errormsg)
//    public TextView errorText;

    /**
     * 群组列表
     */
    @ViewInject(R.id.contactListView)
    private ListView contactListView;


    @ViewInject(R.id.layout_no_chat)
    private View noChatView;

    @ViewInject(R.id.progress_bar)
    private ProgressBar progressbar;

    @ViewInject(R.id.layout_no_network)
    private View noNetworkView;

    @ViewInject(R.id.progressbar_reconnect)
    private ProgressBar reconnectingProgressBar;

    @ViewInject(R.id.imageWifi)
    private ImageView notifyImage;

    @ViewInject(R.id.disconnect_text)
    private TextView displayView;

    //    private List<GroupChatInitBean> groupChats;
    private GroupChatListAdapter contactAdapter = null;
    private IMService imService;

    //是否是手动点击重练。fasle:不显示各种弹出小气泡. true:显示小气泡直到错误出现
    private volatile boolean isManualMConnect = false;

    //聊天服务连接器
    private IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onServiceDisconnected() {

        }
        @Override
        public void onIMServiceConnected() {
            logger.d("groupchatfragment#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            if (imService == null) {
                return;
            }
            // 依赖联系人会话、未读消息、用户的信息三者的状态
            onRecentContactDataReady();
            checkNetWork();
        }
    };




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(GroupChatFragment.this);
        imServiceConnector.connect(getActivity());
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    //重连
    @Override
    public void onResume() {
        super.onResume();
//        imService.getLoginManager().reConnect();
    }

    @Override
    protected void initHandler() {

    }


    @Override
    public void onDestroy() {
        if(EventBus.getDefault().isRegistered(GroupChatFragment.this)){
            EventBus.getDefault().unregister(GroupChatFragment.this);
        }
        imServiceConnector.disconnect(getActivity());
        if(EventBus.getDefault().isRegistered(GroupChatFragment.this)){
            EventBus.getDefault().unregister(GroupChatFragment.this);
        }
        super.onDestroy();


    }


    @Event(value = R.id.contactListView, type = AdapterView.OnItemClickListener.class)
    private void onImageItemClick(AdapterView<?> parent, View view, int position, long id) {
        //点击群聊列表，跳转到群聊面板
        if(contactAdapter.getGroupChats().size() > 0) {
            RecentInfo bean = contactAdapter.getGroupChats().get(position);
            List<BasicNameValuePair> valuePairs = new ArrayList<>();
            BasicNameValuePair markerIdValue = new BasicNameValuePair(Constants.SESSION_ID_KEY,
                    bean.getSessionKey());


            valuePairs.add(markerIdValue);
            ActivityUtil.startActivity(getActivity(), ChatActivity.class,
                    valuePairs.toArray(new BasicNameValuePair[]{}));
        }
    }


//    @Event(value = R.id.contactListView, type = AdapterView.OnItemLongClickListener.class)
//    private boolean onItemLongClick(AdapterView<?> parent, View view,
//                                   int position, long id) {
//
//        RecentInfo recentInfo = contactAdapter.getItem(position);
//        if (recentInfo == null) {
//            logger.e("recent#onItemLongClick null recentInfo -> position:%d", position);
//            return false;
//        }
//        if (recentInfo.getSessionType() == DBConstant.SESSION_TYPE_SINGLE) {
////            handleContactItemLongClick(getActivity(),recentInfo);
//        } else {
//            handleGroupItemLongClick(getActivity(), recentInfo);
//        }
//        return true;
//    }




    /**
     * 刷新页面
     */
    public void refresh() {

    }


    @Override
    public void loadData() {
        initGroupsInfo();
        topBar.setVisibility(View.GONE);
    }

    @Override
    public void initView() {
        showProgressBar();
        showSearchFrameLayout();
        contactAdapter = new GroupChatListAdapter(getActivity());
        contactListView.setAdapter(contactAdapter);
        contactListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(),
                true, true));
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4UnreadEvent(UnreadEvent event){
        logger.d("groupchat_fragment#UnreadEvent# -> %s", event);
        switch (event.event){
            case UNREAD_MSG_RECEIVED:
            case UNREAD_MSG_LIST_OK:
            case SESSION_READED_UNREAD_MSG:
                onRecentContactDataReady();
                break;
        }
    }

    //会话更新
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEventMainThread(SessionEvent sessionEvent){
        logger.d("groupchat_fragment#SessionEvent# -> %s", sessionEvent);
        switch (sessionEvent){
            case RECENT_SESSION_LIST_UPDATE:
            case RECENT_SESSION_LIST_SUCCESS:
            case SET_SESSION_TOP:
                onRecentContactDataReady();
                break;
        }
    }

    //群组事件回调(支持粘性通知，注册可以在通知之后进行)
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent4GroupEvent(GroupEvent event){
        logger.d("groupchat_fragment#GroupEvent# -> %s", event);
        switch (event.getEvent()){
            case GROUP_INFO_OK:
            case CHANGE_GROUP_MEMBER_SUCCESS:
                onRecentContactDataReady();
//                searchDataReady();
                break;

            case GROUP_INFO_UPDATED:
                onRecentContactDataReady();
//                searchDataReady();
                break;
            case SHIELD_GROUP_OK:
                // 更新最下栏的未读计数、更新session
                onShieldSuccess(event.getGroupEntity());
                break;
            case SHIELD_GROUP_FAIL:
            case SHIELD_GROUP_TIMEOUT:
//                onShieldFail();
                break;
        }
    }

    //登陆状态回调
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent4Login(LoginEvent loginEvent){
        logger.d("goupchatfragment#LoginEvent# -> %s", loginEvent);
        switch (loginEvent){
            case LOCAL_LOGIN_SUCCESS:
            case LOGINING: {
                logger.d("goupchatfragment#login#recv handleDoingLogin event");
                if (reconnectingProgressBar != null) {
                    reconnectingProgressBar.setVisibility(View.VISIBLE);
                }
            }
            break;

            case LOCAL_LOGIN_MSG_SERVICE:
            case LOGIN_OK: {
                isManualMConnect = false;
                logger.d("goupchatfragment#loginOk");
                if(noNetworkView != null) {
                    noNetworkView.setVisibility(View.GONE);
                }
            }
            break;

            case LOGIN_AUTH_FAILED:
            case LOGIN_INNER_FAILED:{
                onLoginFailure(loginEvent);
            }
            break;
            case LOGIN_MSG_FAILED:
                handleServerDisconnected();
                break;

            default: reconnectingProgressBar.setVisibility(View.GONE);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEventMain4SmackSocket(SmackSocketEvent socketEvent){
        switch (socketEvent){
            case MSG_SERVER_DISCONNECTED:
                handleServerDisconnected();
                break;

            case CONNECT_MSG_SERVER_FAILED:
                handleServerDisconnected();
                onSocketFailure(socketEvent);
                break;
        }
    }


    private void handleServerDisconnected() {
        logger.d("chatfragment#handleServerDisconnected");

        if (reconnectingProgressBar != null) {
            reconnectingProgressBar.setVisibility(View.GONE);
        }

        if (noNetworkView != null) {
            notifyImage.setImageResource(R.mipmap.warning);
            noNetworkView.setVisibility(View.VISIBLE);
            if(imService != null){
                if(imService.getLoginManager().isKickout()){
                    displayView.setText(R.string.disconnect_kickout);
                }else{
                    displayView.setText(R.string.no_network);
                }
            }
            /**重连【断线、被其他移动端挤掉】*/
            noNetworkView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logger.d("chatFragment#noNetworkView clicked");
//                    IMReconnectManager manager = imService.getReconnectManager();
                    if(NetWorkUtil.isNetworkConnected(getActivity())){
                        isManualMConnect = true;
                        imService.getLoginManager().reConnect();
                    }else{
                        Toast.makeText(getActivity(), R.string.no_network_toast,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reconnectingProgressBar.setVisibility(View.VISIBLE);
                }
            });
        }
    }



    /**
     * 查找最近消息
     */
    private void onRecentContactDataReady() {
        if(imService == null)
            return;
        IMContactManager contactMsgManager = imService.getContactManager();
        IMUnreadMsgManager unreadMsgManager = imService.getUnReadMsgManager();
        IMSessionManager sessionManager = imService.getSessionManager();
        IMGroupManager groupManager = imService.getGroupManager();

        boolean isUserData = contactMsgManager.isUserDataReady();
        boolean isSessionData = sessionManager.isSessionListReady();
        boolean isGroupData =  groupManager.isGroupReady();

        if (!(isUserData && isSessionData && isGroupData)) {
            return;
        }

        int totalUnreadMsgCnt = unreadMsgManager.getTotalUnreadCount();
        logger.d("unread#total cnt %d", totalUnreadMsgCnt);
        ((MainActivity) getActivity()).setUnreadMessageCnt(totalUnreadMsgCnt);

        //获取最新消息
        List<RecentInfo> recentSessionList = sessionManager.getRecentListInfo();

        //更新群组列表数据
        contactAdapter.setData(recentSessionList);
        hideProgressBar();
//        showSearchFrameLayout();
    }

    private void setNoChatView(List<RecentInfo> recentSessionList)
    {
        if(recentSessionList.size()==0)
        {
            noChatView.setVisibility(View.VISIBLE);
        }
        else
        {
            noChatView.setVisibility(View.GONE);
        }
    }

    private void getGroupChats4RecentList(List<RecentInfo> recentSessionList) {
//        for(GroupChatInitBean groupChatInitBean : groupChats) {
//            if(StringUtils.isNotBlank(groupChatInitBean.getGroup().getPeerId())) {
//                for(RecentInfo recentInfo : recentSessionList) {
//                    if(recentInfo.getPeerId().equals(groupChatInitBean.getGroup().getPeerId())) {
//                        groupChatInitBean.setUnReadCnt(recentInfo.getUnReadCnt());
//                        groupChatInitBean.setUpdateTime(recentInfo.getUpdateTime());
//                        groupChatInitBean.setContent(recentInfo.getLatestMsgData());
//                        groupChatInitBean.setSessionId(recentInfo.getSessionKey());
//                    }
//                }
//            }
//        }

    }

    /**
     * 初始化群聊消息
     */
    private void initGroupsInfo() {

    }

    /**搜索数据OK
     * 群组数据准备已经完毕
     * */
    public void searchDataReady(){
        if (imService.getGroupManager().isGroupReady()) {
//            showSearchFrameLayout();
        }
    }

    // 现在只有群组存在免打扰的
    private void handleGroupItemLongClick(final Context ctx, final RecentInfo recentInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(ctx, android.R.style.Theme_Holo_Light_Dialog));
        builder.setTitle(recentInfo.getName());

        final boolean isTop = false;
        final boolean isForbidden = recentInfo.isForbidden();
        int topMessageRes = isTop?R.string.cancel_top_message:R.string.top_message;
        int forbidMessageRes =isForbidden?R.string.cancel_forbid_group_message:R.string.forbid_group_message;

        String[] items = new String[]{ctx.getString(topMessageRes),ctx.getString(forbidMessageRes)};

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:{
//                        imService.getConfigSp().setSessionTop(recentInfo.getSessionKey(),!isTop);
                    }
                    break;
                    case 1:{
                        // 底层成功会事件通知
//                        int shieldType = isForbidden?DBConstant.GROUP_STATUS_ONLINE:DBConstant.GROUP_STATUS_SHIELD;
//                        imService.getGroupManager().reqShieldGroup(recentInfo.getPeerId(),shieldType);
                    }
                    break;
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    public void showProgressBar() {
        progressbar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        progressbar.setVisibility(View.GONE);
    }

    private void  onLoginFailure(LoginEvent event){
        if(!isManualMConnect){return;}
        isManualMConnect = false;
        String errorTip = getString(IMUIHelper.getLoginErrorTip(event));
        logger.d("login#errorTip:%s", errorTip);
        reconnectingProgressBar.setVisibility(View.GONE);
        Toast.makeText(getActivity(), errorTip, Toast.LENGTH_SHORT).show();
    }

    private void  onSocketFailure(SmackSocketEvent event){
        if(!isManualMConnect){return;}
        isManualMConnect = false;
        String errorTip = getString(IMUIHelper.getSocketErrorTip(event));
        logger.d("login#errorTip:%s", errorTip);
        reconnectingProgressBar.setVisibility(View.GONE);
        Toast.makeText(getActivity(), errorTip, Toast.LENGTH_SHORT).show();
    }

    //检查网络环境
    private void checkNetWork() {
        if(!NetWorkUtil.isNetworkConnected(getActivity())){
            noNetworkView.setVisibility(View.VISIBLE);
            /**重连【断线、被其他移动端挤掉】*/
            noNetworkView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logger.d("chatFragment#noNetworkView clicked");
//                    IMReconnectManager manager = imService.getReconnectManager();
                    if(NetWorkUtil.isNetworkConnected(getActivity())){
                        isManualMConnect = true;
                        IMLoginManager.instance().login();
                    }else{
                        Toast.makeText(getActivity(), R.string.no_network_toast,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reconnectingProgressBar.setVisibility(View.VISIBLE);
                }
            });
            displayView.setText(R.string.no_network);
            reconnectingProgressBar.setVisibility(View.VISIBLE);
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    reconnectingProgressBar.setVisibility(View.GONE);
                }
            }, 5000);
        }
    }

    // 更新页面以及 下面的未读总计数
    private void onShieldSuccess(GroupEntity entity){
        if(entity == null){
            return;
        }
        // 更新某个sessionId
        contactAdapter.updateRecentInfoByShield(entity);
        IMUnreadMsgManager unreadMsgManager =imService.getUnReadMsgManager();

        int totalUnreadMsgCnt = unreadMsgManager.getTotalUnreadCount();
        logger.d("unread#total cnt %d", totalUnreadMsgCnt);
        ((MainActivity) getActivity()).setUnreadMessageCnt(totalUnreadMsgCnt);
    }
}
