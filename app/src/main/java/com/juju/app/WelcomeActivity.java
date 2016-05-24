package com.juju.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.ViewUtils;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import com.juju.app.activity.LoginActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.annotation.SystemColor;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Party;
import com.juju.app.entity.Plan;
import com.juju.app.entity.User;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;

import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;


@ContentView(R.layout.activity_welcome)
@SystemColor(isApply = false)
public class WelcomeActivity extends BaseActivity  {

    private final String tag = getClass().getName();

    @ViewInject(R.id.main_layout)
    private LinearLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if(SpfUtil.get(getApplicationContext(), Constants.USER_INFO,null) == null){
//            clearDatabase();
//        }

       //TODO 增加加载配置文件
//        try {
//            Thread.currentThread().sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        startAnimation();
        //初始化配置URL
        //透明状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //透明导航栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        HttpConstants.initURL();
        startAnimation();
//        new Thread(this).start();
    }

    private void clearDatabase() {
        try {
            JujuDbUtils.getInstance().dropTable(Party.class);
            JujuDbUtils.getInstance().dropTable(Plan.class);
            JujuDbUtils.getInstance().dropTable(User.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void run() {
//
////            Log.i(tag, "等待1秒切换到登陆界面。。。");
////            Thread.sleep(1000);
////            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
//
//            startAnimation();
//
//    }

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
                ActivityUtil.startActivity(WelcomeActivity.this, LoginActivity.class);
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
