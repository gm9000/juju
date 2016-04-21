package com.juju.app.ui.base;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.golobal.Constants;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.TipsToastUtil;
import com.juju.app.view.CustomDialog;
import com.lidroid.xutils.ViewUtils;

import org.apache.http.message.BasicNameValuePair;

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

    private RelativeLayout loading_layout;

    private TextView txt_status_message;

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
        initPublicViews();
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
        CustomDialog.Builder builder = new CustomDialog.Builder(
                BaseActivity.this);
        builder.setMessage(text);
        builder.setNegativeButton("确定",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int paramInt) {
                        dialog.dismiss();
                        completeLoading();
                    }
                });
        builder.create().show();
    }

    /**
     * 打开Activity
     *
     * @param activity
     * @param cls
     * @param name
     */
    protected void startActivity(Activity activity, Class<?> cls,
                                 BasicNameValuePair... name) {
        ActivityUtil.startActivity(activity, cls, name);
    }

    /**
     * 打开Activity，带返回值
     * @param activity
     * @param cls
     * @param requestCode
     * @param name
     */
    protected void startActivityForResult(Activity activity, Class<?> cls, int requestCode,
                                          BasicNameValuePair... name) {
        ActivityUtil.startActivityForResult(activity, cls, requestCode, name);
    }

    /**
     * 关闭 Activity
     *
     * @param activity
     */
    protected void finish(Activity activity) {
        ActivityUtil.finish(activity);
    }


    /**
     * 开始加载
     * @param objs  第一个参数：是否显示文本（boolean）
     *              第二个参数：文本内容
     */
    protected void loading(Object... objs){
        if(loading_layout != null) {
            if(objs == null) {
                _loading();
            } else if (objs.length == 1
                    && objs[0] instanceof Boolean) {
                _loading((Boolean)objs[0]);
            } else if (objs.length == 2
                    && objs[0] instanceof Boolean
                    && objs[1] instanceof Integer) {
                _loading((Boolean)objs[0], (Integer)objs[1]);
            } else {
                _loading();
            }
        }
    }

    /**
     * 完成加载
     */
    protected void completeLoading(){

        txt_status_message.setText(R.string.completed);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(loading_layout != null) {
            loading_layout.setVisibility(View.GONE);
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(resultCode == Constants.REQUEST_CODE) {
//            completeLoading();
//        }
//    }




    /**
     *******************************************私有函数部分，可封装******************************
     */
    private void initPublicViews() {
        loading_layout = (RelativeLayout)findViewById(R.id.loading_layout);
        txt_status_message = (TextView)findViewById(R.id.txt_status_message);
    }

    private void _loading(){
        if(loading_layout != null) {
            loading_layout.setVisibility(View.VISIBLE);
            if(txt_status_message != null) {
                txt_status_message.setVisibility(View.VISIBLE);
            }
        }
    }

    private void _loading(boolean isShowMsg){
        if(loading_layout != null) {
            loading_layout.setVisibility(View.VISIBLE);
            if(txt_status_message != null && isShowMsg) {
                txt_status_message.setVisibility(View.VISIBLE);
            }
        }
    }

    private void _loading(boolean isShowMsg, int msg){
        if(loading_layout != null) {
            loading_layout.setVisibility(View.VISIBLE);
            if(txt_status_message != null && isShowMsg) {
                if(msg != 0) {
                    txt_status_message.setText(msg);
                }
                txt_status_message.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setListeners() {

    }





}
