package com.juju.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import com.juju.app.activity.LoginActivity;
import com.juju.app.config.HttpConstants;
import com.juju.app.ui.base.BaseActivity;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;

@ContentView(R.layout.activity_welcome)
public class WelcomeActivity extends BaseActivity implements Runnable  {

    private final String tag = getClass().getName();

    @ViewInject(R.id.main_layout)
    private LinearLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.inject(this);
       //TODO 加载配置文件
//        try {
//            Thread.currentThread().sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        startAnimation();
        //初始化配置URL
        HttpConstants.initURL();
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            Log.i(tag, "等待3秒切换到登陆界面。。。");
            Thread.sleep(3000);
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 渐变展示启动屏
     */
    private void startAnimation() {
        Animation aa = new Animation() {
        };
        aa.setDuration(3000);
        mainLayout.startAnimation(aa);
        aa.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {

            }
        });
    }
}
