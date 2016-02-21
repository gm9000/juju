package com.juju.app.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.juju.app.R;
import com.juju.app.ui.base.BaseFragment;

/**
 * 项目名称：juju
 * 类描述：聚会—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:10
 * 版本：V1.0.0
 */
public class GroupPartyFragment extends BaseFragment {

    private Activity ctx;
    private View layout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (layout == null) {
            ctx = this.getActivity();
            layout = ctx.getLayoutInflater().inflate(R.layout.fragment_group_party, null);
        } else {
            ViewGroup parent = (ViewGroup) layout.getParent();
            if (parent != null) {
                parent.removeView(layout);
            }
        }
        return layout;
    }
}
