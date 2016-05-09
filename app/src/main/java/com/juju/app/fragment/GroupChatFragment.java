package com.juju.app.fragment;

import android.os.Bundle;
import android.util.Log;
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
import com.juju.app.event.SessionEvent;
import com.juju.app.event.UnreadEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.service.im.manager.IMMessageManager;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.manager.IMUnreadMsgManager;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.NetWorkUtil;
import com.juju.app.utils.StringUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;
import com.lidroid.xutils.view.annotation.event.OnItemClick;

import org.apache.http.message.BasicNameValuePair;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
    @ViewInject(R.id.ContactListView)
    private ListView lvContact;


    @ViewInject(R.id.layout_no_chat)
    private View noChatView;


    private List<GroupChatInitBean> groupChats;
    private GroupChatListAdapter adapter = null;
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

    @OnItemClick(R.id.ContactListView)
    public void onItemClickListView(AdapterView<?> parent, View view, int position, long id) {
        //点击群聊列表，跳转到群聊面板
        if(adapter.getGroupChats().size() > 0) {
            GroupChatInitBean bean = adapter.getGroupChats().get(position);
            List<BasicNameValuePair> valuePairs = new ArrayList<BasicNameValuePair>();
            BasicNameValuePair markerIdValue = new BasicNameValuePair(Constants.SESSION_ID_KEY,
                    bean.getSessionId());
            BasicNameValuePair nameValue = new BasicNameValuePair(Constants.GROUP_NAME_KEY,
                    bean.getGroup().getMainName());
            valuePairs.add(markerIdValue);
            valuePairs.add(nameValue);
            ActivityUtil.startActivity(getActivity(), ChatActivity.class,
                    valuePairs.toArray(new BasicNameValuePair[]{}));
        }
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
        initTestData();


        initGroupsInfo();
    }

    @Override
    public void initView() {
        adapter = new GroupChatListAdapter(getActivity());
        adapter.setData(groupChats);
        lvContact.setAdapter(adapter);

//        IMLoginManager.instance().joinChatRoom();

        //查询所有消息（方便分析数据）
//        List<MessageEntity> messageEntities = IMMessageManager.instance().findAll4Order("updated:desc");
//        for(MessageEntity entity : messageEntities) {
//            logger.d(entity.toString());
//        }
//
//        List<SessionEntity> sessionEntities = IMSessionManager.instance().findAll();
//        for(SessionEntity entity : sessionEntities) {
//            logger.d(entity.toString());
//        }
//
//        List<UnreadEntity> unreadEntities = IMUnreadMsgManager.instance().findAll();
//        for(UnreadEntity entity : unreadEntities) {
//            logger.d(entity.toString());
//        }

    }

    /**
     * 测试数据
     */
    private void initTestData() {
        groupChats = new ArrayList<GroupChatInitBean>();
        for(int i = 1; i<=9 ; i++) {
            GroupEntity group = new GroupEntity();
            group.setId("00000" + i);
            group.setMainName("休闲娱乐" + i);
//            group.setMemberNum(i);
            List<String> avatar = new ArrayList<String>();
            if(i >= 1) {
                avatar.add("http://img4.duitang.com/uploads/item/201511/07/20151107174431_emPdc.jpeg");
            }
            if (i >= 2) {
                avatar.add("http://cdn.duitang.com/uploads/item/201511/07/20151107210255_UzQaN.thumb.700_0.jpeg");
            }
            if (i >= 3) {
                avatar.add("http://cdn.duitang.com/uploads/item/201511/08/20151108093409_2fCkn.thumb.700_0.jpeg");
            }
            if (i >= 4) {
                avatar.add("http://img5.duitang.com/uploads/item/201512/04/20151204095217_m8V5G.thumb.700_0.jpeg");
            }
            if (i >= 5) {
                avatar.add("http://img4.duitang.com/uploads/item/201512/04/20151204095410_2f4kT.thumb.700_0.jpeg");
            }
            if (i >= 6) {
                avatar.add("http://cdn.duitang.com/uploads/item/201601/08/20160108130846_MS4TW.thumb.700_0.png");
            }
            if (i >= 7) {
                avatar.add("http://cdn.duitang.com/uploads/item/201512/22/20151222165532_YetiV.thumb.700_0.jpeg");
            }
            if (i >= 8) {
                avatar.add("http://img4.duitang.com/uploads/item/201509/13/20150913094215_HPrai.thumb.700_0.png");
            }
            if (i >= 9) {
                avatar.add("http://img4.duitang.com/uploads/item/201601/06/20160106131952_xirGJ.thumb.700_0.jpeg");
            }
            int unReadCnt = 0;
            if(i == 1) {
                group.setMainName("测试讨论组");
                group.setPeerId("ceshi@conference.juju");
            }
            GroupChatInitBean groupChat = new GroupChatInitBean(String.valueOf(i), group, "", System.currentTimeMillis(), 0, avatar);
            if(i == 5) {
                groupChat.setSessionId("2_ceshi@conference.juju");
            }
            groupChats.add(groupChat);
        }
//        Group group2 = new Group();
//        group2.setId(2l);
//        group2.setName("学术交流");
//
//        GroupChatInitBean groupChat2 = new GroupChatInitBean(group2, "已读", "明天上午去国家" +
//                "图书馆碰面!", "12:11", "1");
//        groupChats.add(groupChat1);
//        groupChats.add(groupChat2);
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

    /**
     * 查找最近消息
     */
    private void onRecentContactDataReady() {
//        boolean isUserData = imService.getContactManager().isUserDataReady();
//        boolean isSessionData = imService.getSessionManager().isSessionListReady();
//        boolean isGroupData =  imService.getGroupManager().isGroupReady();
//
//        if ( !(isUserData&&isSessionData&&isGroupData)) {
//            return;
//        }
        IMUnreadMsgManager unreadMsgManager = IMUnreadMsgManager.instance();
        IMSessionManager sessionManager = IMSessionManager.instance();

        int totalUnreadMsgCnt = unreadMsgManager.getTotalUnreadCount();
        logger.d("unread#total cnt %d", totalUnreadMsgCnt);
        ((MainActivity) getActivity()).setUnreadMessageCnt(totalUnreadMsgCnt);

        //获取最新消息
        List<RecentInfo> recentSessionList = sessionManager.getRecentListInfo();
//        setNoChatView(recentSessionList);
        getGroupChats4RecentList(recentSessionList);
        //更新群组列表数据
        adapter.setData(groupChats);
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
        for(GroupChatInitBean groupChatInitBean : groupChats) {
            if(StringUtils.isNotBlank(groupChatInitBean.getGroup().getPeerId())) {
                for(RecentInfo recentInfo : recentSessionList) {
                    if(recentInfo.getPeerId().equals(groupChatInitBean.getGroup().getPeerId())) {
                        groupChatInitBean.setUnReadCnt(recentInfo.getUnReadCnt());
                        groupChatInitBean.setUpdateTime(recentInfo.getUpdateTime());
//                        groupChatInitBean.setIsTop(true);
                        groupChatInitBean.setContent(recentInfo.getLatestMsgData());
                        groupChatInitBean.setSessionId(recentInfo.getSessionKey());
//                        groupChatInitBean.setState();
                    }
                }
            }
        }
    }

    /**
     * 初始化群聊消息
     */
    private void initGroupsInfo() {

    }

}
