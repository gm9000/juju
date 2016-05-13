package com.juju.app.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.MainActivity;
import com.juju.app.activity.chat.ChatActivity;
import com.juju.app.adapter.GroupChatListAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.bean.groupchat.GroupChatInitBean;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.RecentInfo;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.SessionEvent;
import com.juju.app.event.UnreadEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.manager.IMUnreadMsgManager;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
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
public class GroupChatFragment extends BaseFragment implements CreateUIHelper {

    protected static Logger logger = Logger.getLogger(GroupChatFragment.class);


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


//    private List<GroupChatInitBean> groupChats;
    private GroupChatListAdapter contactAdapter = null;
    private IMService imService;

    //聊天服务连接器
    private IMServiceConnector imServiceConnector = new IMServiceConnector(){

        @Override
        public void onServiceDisconnected() {
            if(EventBus.getDefault().isRegistered(GroupChatFragment.this)){
                EventBus.getDefault().unregister(GroupChatFragment.this);
            }
        }
        @Override
        public void onIMServiceConnected() {
            logger.d("groupchatfragment#recent#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            if (imService == null) {
                return;
            }
            // 依赖联系人会话、未读消息、用户的信息三者的状态
            onRecentContactDataReady();
            EventBus.getDefault().register(GroupChatFragment.this);
        }
    };




    @Override
    public void onCreate(Bundle savedInstanceState) {
        imServiceConnector.connect(getActivity());
        super.onCreate(savedInstanceState);

    }


    @Override
    public void onDestroy() {
        if(EventBus.getDefault().isRegistered(GroupChatFragment.this)){
            EventBus.getDefault().unregister(GroupChatFragment.this);
        }
        imServiceConnector.disconnect(getActivity());
        super.onDestroy();


    }


    @Event(value = R.id.contactListView, type = AdapterView.OnItemClickListener.class)
    private void onImageItemClick(AdapterView<?> parent, View view, int position, long id) {
        //点击群聊列表，跳转到群聊面板
        if(contactAdapter.getGroupChats().size() > 0) {
            RecentInfo bean = contactAdapter.getGroupChats().get(position);
            List<BasicNameValuePair> valuePairs = new ArrayList<BasicNameValuePair>();
            BasicNameValuePair markerIdValue = new BasicNameValuePair(Constants.SESSION_ID_KEY,
                    bean.getSessionKey());


            valuePairs.add(markerIdValue);
            ActivityUtil.startActivity(getActivity(), ChatActivity.class,
                    valuePairs.toArray(new BasicNameValuePair[]{}));
        }
    }


    @Event(value = R.id.contactListView, type = AdapterView.OnItemLongClickListener.class)
    private boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {

        RecentInfo recentInfo = contactAdapter.getItem(position);
        if (recentInfo == null) {
            logger.e("recent#onItemLongClick null recentInfo -> position:%d", position);
            return false;
        }
        if (recentInfo.getSessionType() == DBConstant.SESSION_TYPE_SINGLE) {
//            handleContactItemLongClick(getActivity(),recentInfo);
        } else {
            handleGroupItemLongClick(getActivity(), recentInfo);
        }
        return true;
    }




    /**
     * 刷新页面
     */
    public void refresh() {

    }

    @Override
    protected void findViews() {
        super.findViews();

    }

    @Override
    public void loadData() {
//        initTestData();
        initGroupsInfo();
    }

    @Override
    public void initView() {
        contactAdapter = new GroupChatListAdapter(getActivity());
        contactListView.setAdapter(contactAdapter);

        //监听长按事件

//        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                //点击群聊列表，跳转到群聊面板
//                if(contactAdapter.getGroupChats().size() > 0) {
//                    RecentInfo bean = contactAdapter.getGroupChats().get(position);
//                    List<BasicNameValuePair> valuePairs = new ArrayList<BasicNameValuePair>();
//                    BasicNameValuePair markerIdValue = new BasicNameValuePair(Constants.SESSION_ID_KEY,
//                            bean.getSessionKey());
//
//
//                    valuePairs.add(markerIdValue);
//                    ActivityUtil.startActivity(getActivity(), ChatActivity.class,
//                            valuePairs.toArray(new BasicNameValuePair[]{}));
//                }
//            }
//        });

        contactListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(),
                true, true));
    }

    /**
     * 测试数据
     */
    private void initTestData() {
//        groupChats = new ArrayList<GroupChatInitBean>();
//        for(int i = 1; i<=9 ; i++) {
//            GroupEntity group = new GroupEntity();
//            group.setId("00000" + i);
//            group.setMainName("休闲娱乐" + i);
////            group.setMemberNum(i);
//            List<String> avatar = new ArrayList<String>();
//            if(i >= 1) {
//                avatar.add("http://img4.duitang.com/uploads/item/201511/07/20151107174431_emPdc.jpeg");
//            }
//            if (i >= 2) {
//                avatar.add("http://cdn.duitang.com/uploads/item/201511/07/20151107210255_UzQaN.thumb.700_0.jpeg");
//            }
//            if (i >= 3) {
//                avatar.add("http://cdn.duitang.com/uploads/item/201511/08/20151108093409_2fCkn.thumb.700_0.jpeg");
//            }
//            if (i >= 4) {
//                avatar.add("http://img5.duitang.com/uploads/item/201512/04/20151204095217_m8V5G.thumb.700_0.jpeg");
//            }
//            if (i >= 5) {
//                avatar.add("http://img4.duitang.com/uploads/item/201512/04/20151204095410_2f4kT.thumb.700_0.jpeg");
//            }
//            if (i >= 6) {
//                avatar.add("http://cdn.duitang.com/uploads/item/201601/08/20160108130846_MS4TW.thumb.700_0.png");
//            }
//            if (i >= 7) {
//                avatar.add("http://cdn.duitang.com/uploads/item/201512/22/20151222165532_YetiV.thumb.700_0.jpeg");
//            }
//            if (i >= 8) {
//                avatar.add("http://img4.duitang.com/uploads/item/201509/13/20150913094215_HPrai.thumb.700_0.png");
//            }
//            if (i >= 9) {
//                avatar.add("http://img4.duitang.com/uploads/item/201601/06/20160106131952_xirGJ.thumb.700_0.jpeg");
//            }
//            int unReadCnt = 0;
//            if(i == 1) {
//                group.setMainName("测试讨论组");
//                group.setPeerId("ceshi@conference.juju");
//            }
//            GroupChatInitBean groupChat = new GroupChatInitBean(String.valueOf(i), group, "", System.currentTimeMillis(), 0, avatar);
//            if(i == 5) {
//                groupChat.setSessionId("2_ceshi@conference.juju");
//            }
//            groupChats.add(groupChat);
//        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4UnreadEvent(UnreadEvent event){
        switch (event.event){
            case UNREAD_MSG_RECEIVED:
            case UNREAD_MSG_LIST_OK:
            case SESSION_READED_UNREAD_MSG:
                onRecentContactDataReady();
                break;
        }
    }

    //会话更新
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SessionEvent sessionEvent){
        logger.d("chatfragment#SessionEvent# -> %s", sessionEvent);
        switch (sessionEvent){
            case RECENT_SESSION_LIST_UPDATE:
            case RECENT_SESSION_LIST_SUCCESS:
            case SET_SESSION_TOP:
                onRecentContactDataReady();
                break;
        }
    }

    //群组事件回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4GroupEvent(GroupEvent event){
        switch (event.getEvent()){
            case GROUP_INFO_OK:
            case CHANGE_GROUP_MEMBER_SUCCESS:
                onRecentContactDataReady();
//                searchDataReady();
                break;

            case GROUP_INFO_UPDATED:
                onRecentContactDataReady();
                searchDataReady();
                break;
            case SHIELD_GROUP_OK:
                // 更新最下栏的未读计数、更新session
//                onShieldSuccess(event.getGroupEntity());
                break;
            case SHIELD_GROUP_FAIL:
            case SHIELD_GROUP_TIMEOUT:
//                onShieldFail();
                break;
        }
    }




    /**
     * 查找最近消息
     */
    private void onRecentContactDataReady() {
        IMUnreadMsgManager unreadMsgManager = IMUnreadMsgManager.instance();
        IMSessionManager sessionManager = IMSessionManager.instance();
        IMGroupManager groupManager = IMGroupManager.instance();

        boolean isUserData = imService.getContactManager().isUserDataReady();
        boolean isSessionData = sessionManager.isSessionListReady();
        boolean isGroupData =  groupManager.isGroupReady();
//
//        if (!(isUserData && isSessionData && isGroupData)) {
//            return;
//        }

        int totalUnreadMsgCnt = unreadMsgManager.getTotalUnreadCount();
        logger.d("unread#total cnt %d", totalUnreadMsgCnt);
        ((MainActivity) getActivity()).setUnreadMessageCnt(totalUnreadMsgCnt);

        //获取最新消息
        List<RecentInfo> recentSessionList = sessionManager.getRecentListInfo();

//        setNoChatView(recentSessionList);
//        getGroupChats4RecentList(recentSessionList);
        //更新群组列表数据
        contactAdapter.setData(recentSessionList);
//        hideProgressBar();
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

}
