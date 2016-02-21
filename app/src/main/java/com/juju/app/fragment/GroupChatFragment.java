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
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.utils.NetWorkUtil;

/**
 * 项目名称：juju
 * 类描述：群聊—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:09
 * 版本：V1.0.0
 */
public class GroupChatFragment extends BaseFragment  implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    private Activity ctx;
    private View rootView;
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            ctx = this.getActivity();
            rootView = ctx.getLayoutInflater().inflate(R.layout.fragment_group_chat, null);
                initView(rootView);
                setOnListener();
            } else {
                ViewGroup parent = (ViewGroup) rootView.getParent();
                if (parent != null) {
                    parent.removeView(rootView);
            }
        }
        return rootView;
    }

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

    private void setOnListener() {
        lvContact.setOnItemClickListener(this);
        errorItem.setOnClickListener(this);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    private void initView(View view) {
        parentActivity = (MainActivity) getActivity();
        lvContact = (ListView) view.findViewById(R.id.listview);
        errorItem = (RelativeLayout) view
                .findViewById(R.id.rl_error_item);
        errorText = (TextView) errorItem
                .findViewById(R.id.tv_connect_errormsg);
    }

    /**
     * 刷新页面
     */
    public void refresh() {

    }
}
