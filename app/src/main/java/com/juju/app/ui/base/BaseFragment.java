package com.juju.app.ui.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.v4.app.Fragment;
import android.support.v7.widget.ViewUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.utils.ActivityUtil;

import org.xutils.x;


/**
 * 项目名称：juju
 * 类描述：Fragment基类
 * 创建人：gm
 * 日期：2016/2/18 15:07
 * 版本：V1.0.0
 */
public abstract class BaseFragment extends Fragment {

    /**
     * fragment根视图
     */
    protected View rootView;

    private boolean injected = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!injected) {
            x.view().inject(this, this.getView());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final CreateFragmentUI createFragmentUI = this.getClass().
                getAnnotation(CreateFragmentUI.class);
        if(createFragmentUI != null) {
            if(rootView == null) {
                rootView = x.view().inject(this, inflater, container);
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
            }
            injected = true;
            return rootView;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
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

    /**
     * parameter参数长度不能大于2，支持两种格式
     * 例子：1: key,value 2: Map (参数个数超过两个使用map)
     * @param context
     * @param cls
     * @param parameter
     */
    protected void startActivityNew(Context context, Class<?> cls,
                                    Object... parameter) {
        ActivityUtil.startActivityNew(context, cls, parameter);
    }

}
