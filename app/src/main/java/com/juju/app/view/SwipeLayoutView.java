package com.juju.app.view;

import android.content.Context;
import android.util.AttributeSet;

import com.daimajia.swipe.SwipeLayout;
/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/5/31 19:54
 * 版本：V1.0.0
 */
public class SwipeLayoutView extends SwipeLayout {

    private boolean mScrolling;
    private float touchDownX;

    public SwipeLayoutView(Context context) {
        super(context);
    }

    public SwipeLayoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwipeLayoutView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                touchDownX = event.getX();
//                mScrolling = false;
//                break;
//            case MotionEvent.ACTION_MOVE:
//                if (Math.abs(touchDownX - event.getX()) >= ViewConfiguration.get(
//                        getContext()).getScaledTouchSlop()) {
//                    mScrolling = true;
//                } else {
//                    mScrolling = false;
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//                mScrolling = false;
//                break;
//        }
//        return mScrolling;
//    }
}
