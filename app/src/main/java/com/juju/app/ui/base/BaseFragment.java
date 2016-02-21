package com.juju.app.ui.base;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.annotation.CreateUI;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;

/**
 * 项目名称：juju
 * 类描述：Fragment基类
 * 创建人：gm
 * 日期：2016/2/18 15:07
 * 版本：V1.0.0
 */
public class BaseFragment extends Fragment {

    /**
     * fragment根视图
     */
    private View rootView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final CreateFragmentUI createFragmentUI = this.getClass().
                getAnnotation(CreateFragmentUI.class);
        if(createFragmentUI != null) {
            if(rootView == null) {
                rootView = (View) inflater.inflate(createFragmentUI.viewId(), null);
                findViews();
                setOnListener();
                if(this instanceof CreateUIHelper) {
                    CreateUIHelper uiHelper = (CreateUIHelper) this;
                    if(createFragmentUI.isLoadData()) {
                        uiHelper.loadData();
                    }
                    if(createFragmentUI.isInitView()) {
                        uiHelper.initView();
                    }
                }
                return rootView;
            } else {
                ViewGroup parent = (ViewGroup) rootView.getParent();
                if (parent != null) {
                    parent.removeView(rootView);
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * 根视图
     *
     * @return
     */
    View getRootView() {
        return rootView;
    }

    /**
     * 构建组建
     */
    protected void findViews() {

    }

    /**
     * 绑定事件
     */
    protected void setOnListener() {

    }

    protected View findViewById(int viewId) {
        if (rootView == null)
            return null;
        return rootView.findViewById(viewId);
    }
}
