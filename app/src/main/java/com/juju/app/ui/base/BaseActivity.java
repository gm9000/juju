package com.juju.app.ui.base;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.juju.app.annotation.CreateUI;
import com.juju.app.utils.TipsToastUtil;
import com.lidroid.xutils.ViewUtils;

/**
 * 项目名称：juju
 * 类描述：Activity父类
 * 创建人：gm
 * 日期：2016/2/17 10:34
 * 版本：V1.0.0
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static TipsToastUtil tipsToast;

    protected Context context = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.inject(this);
        //初始化数据
        if(this instanceof CreateUIHelper) {
            CreateUIHelper uiHelper = (CreateUIHelper) this;
            final CreateUI createUI = this.getClass().getAnnotation(CreateUI.class);
            if(createUI != null) {
                if(createUI.isLoadData()) {
                    uiHelper.loadData();
                }
                if(createUI.isInitView()) {
                    uiHelper.initView();
                }
            }
        }
        BaseApplication.getInstance().addActivity(this);
        context = this;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }


    protected Context getContext() {
        return context;
    }


    protected void startService() {
        //开启服务
    }

    protected void stopService() {
        //关闭服务
    }


    /**
     * 消息TIP
     * @param iconResId
     * @param tips
     */
    protected void showTips(int iconResId, String tips) {
        if (tipsToast != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                tipsToast.cancel();
            }
        } else {
            tipsToast = TipsToastUtil.makeText(getApplication().getBaseContext(),
                    tips, TipsToastUtil.LENGTH_SHORT);
        }
        tipsToast.show();
        tipsToast.setIcon(iconResId);
        tipsToast.setText(tips);
    }

    /**
     * 消息弹出框
     * @param text
     */
    protected void showMsgDialog(int text) {

    }



}
