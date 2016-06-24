package com.juju.app.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/6/8 15:34
 * 版本：V1.0.0
 */
public class SwipeLinearLayout extends LinearLayout {


    public SwipeLinearLayout(Context context) {
        super(context);
    }

    public SwipeLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwipeLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        return true;
//    }
//
//
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return super.onInterceptTouchEvent(ev);
//    }
}
