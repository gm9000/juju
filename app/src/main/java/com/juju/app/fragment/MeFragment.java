package com.juju.app.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.ui.base.BaseFragment;

/**
 * 项目名称：juju
 * 类描述：我—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:11
 * 版本：V1.0.0
 */
public class MeFragment extends BaseFragment implements View.OnClickListener {

    private Activity ctx;
    private View layout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (layout == null) {
            ctx = this.getActivity();
            layout = ctx.getLayoutInflater().inflate(R.layout.fragment_me, null);
            initViews();
            initData();
            setOnListener();
        } else {
            ViewGroup parent = (ViewGroup) layout.getParent();
            if (parent != null) {
                parent.removeView(layout);
            }
        }
        return layout;
    }

    private void initViews() {

    }

    private void setOnListener() {
        layout.findViewById(R.id.view_user).setOnClickListener(this);
        layout.findViewById(R.id.txt_album).setOnClickListener(this);
        layout.findViewById(R.id.txt_collect).setOnClickListener(this);
        layout.findViewById(R.id.txt_money).setOnClickListener(this);
        layout.findViewById(R.id.txt_card).setOnClickListener(this);
        layout.findViewById(R.id.txt_smail).setOnClickListener(this);
        layout.findViewById(R.id.txt_setting).setOnClickListener(this);
    }

    private void initData() {

    }

    @Override
    public void onClick(View v) {

    }
}
