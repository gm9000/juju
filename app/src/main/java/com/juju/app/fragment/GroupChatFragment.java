package com.juju.app.fragment;

import android.app.Activity;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.MainActivity;
import com.juju.app.adapter.base.GroupChatListAdpter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.entity.http.GroupChat;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.NetWorkUtil;

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

    private List<GroupChat> groupChats;


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
        GroupChatListAdpter adpter = new GroupChatListAdpter(getActivity(), groupChats);
        lvContact.setAdapter(adpter);
    }

    /**
     * 测试数据
     */
    private void initTestData() {
        groupChats = new ArrayList<GroupChat>();
        GroupChat gc1 = new GroupChat();
        gc1.setId(1l);
        gc1.setName("休闲娱乐");

        GroupChat gc2 = new GroupChat();
        gc2.setId(2l);
        gc2.setName("学术交流");

        groupChats.add(gc1);
        groupChats.add(gc2);
    }
}
