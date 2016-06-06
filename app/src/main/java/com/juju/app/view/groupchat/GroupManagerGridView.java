package com.juju.app.view.groupchat;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.GridView;

import com.juju.app.utils.ScreenUtil;

/**
 * 项目名称：juju
 * 类描述：群组详情图片墙
 * 创建人：gm
 * 日期：2016/5/27 16:53
 * 版本：V1.0.0
 */
public class GroupManagerGridView extends GridView {

    private Context ctx;
    public GroupManagerGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
    }
    public GroupManagerGridView(Context context) {
        super(context);
        this.ctx = context;
    }
    public GroupManagerGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.ctx = context;
    }
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metric);
        int height =metric.heightPixels;
        int expandSpec = MeasureSpec.makeMeasureSpec(
                height - ScreenUtil.dip2px(ctx, 250), MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }

}
