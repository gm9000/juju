package com.juju.app.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.LoginActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.annotation.CreateUI;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;

import org.apache.http.message.BasicNameValuePair;

/**
 * 项目名称：juju
 * 类描述：我—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:11
 * 版本：V1.0.0
 */
@CreateFragmentUI(viewId = R.layout.fragment_me)
public class MeFragment extends BaseFragment implements CreateUIHelper, View.OnClickListener {


    private TextView txt_album;


    @Override
    protected void findViews() {
        super.findViews();
        txt_album = (TextView) findViewById(R.id.txt_album);
    }

    @Override
    public void loadData() {
        System.out.println("txt_album:"+txt_album.getText().toString());
    }

    @Override
    public void initView() {

    }

    @Override
    protected void setOnListener() {
        super.setOnListener();
        txt_album.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_album:// 相册
                ActivityUtil.startActivity(getActivity(), LoginActivity.class);
                break;
        }
    }
}
