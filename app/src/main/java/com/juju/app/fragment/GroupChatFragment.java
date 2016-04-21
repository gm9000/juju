package com.juju.app.fragment;

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
import com.juju.app.entity.http.Group;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.NetWorkUtil;

import org.apache.http.message.BasicNameValuePair;

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
public class GroupChatFragment extends BaseFragment implements CreateUIHelper,
        View.OnClickListener, AdapterView.OnItemClickListener {


    private MainActivity parentActivity;

    public RelativeLayout errorItem;

    /**
     * 异常提示文本组建
     */
    public TextView errorText;

    /**
     * 群组列表
     */
    private ListView lvContact;

    private List<GroupChatInitBean> groupChats;

    private GroupChatListAdapter adapter = null;


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_error_item:
                NetWorkUtil.openSetNetWork(getActivity());
                break;
            default:
                break;
        }
    }

    @Override
    public void setOnListener() {
        lvContact.setOnItemClickListener(this);
        errorItem.setOnClickListener(this);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //点击群聊列表，跳转到群聊面板
        if(adapter.getGroupChats().size() > 0) {
            GroupChatInitBean bean = adapter.getGroupChats().get(position);
            List<BasicNameValuePair> valuePairs = new ArrayList<BasicNameValuePair>();
            BasicNameValuePair markerIdValue = new BasicNameValuePair(Constants.SESSION_ID_KEY,
                    bean.getSessionId());
            BasicNameValuePair nameValue = new BasicNameValuePair(Constants.GROUP_NAME_KEY,
                    bean.getGroup().getName());
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
        parentActivity = (MainActivity) getActivity();
        lvContact = (ListView) findViewById(R.id.listview);
        errorItem = (RelativeLayout) findViewById(R.id.rl_error_item);
        errorText = (TextView) errorItem.findViewById(R.id.tv_connect_errormsg);
    }

    @Override
    public void loadData() {
        initTestData();
    }

    @Override
    public void initView() {
        adapter = new GroupChatListAdapter(getActivity(), groupChats);
        lvContact.setAdapter(adapter);
    }

    /**
     * 测试数据
     */
    private void initTestData() {
        groupChats = new ArrayList<GroupChatInitBean>();
        for(int i = 1; i<=9 ; i++) {
            Group group = new Group();
            group.setId("00000"+i);
            group.setName("休闲娱乐"+i);
            group.setMemberNum(i);
            GroupChatInitBean groupChat = new GroupChatInitBean(String.valueOf(i), group, "送达", "今天晚上我请客，" +
                    "暂定苏州桥同一首歌碰面。", "刚刚", String.valueOf(i));
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
}
