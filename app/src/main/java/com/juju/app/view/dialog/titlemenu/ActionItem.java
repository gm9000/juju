package com.juju.app.view.dialog.titlemenu;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * 项目名称：juju
 * 类描述：弹窗内部子类项（绘制标题和图标）
 * 创建人：gm
 * 日期：2016/2/18 17:40
 * 版本：V1.0.0
 */
public class ActionItem {

    // 定义图片对象
    public Drawable mDrawable;
    // 定义文本对象
    public CharSequence mTitle;

    public ActionItem(Drawable drawable, CharSequence title) {
        this.mDrawable = drawable;
        this.mTitle = title;
    }

    public ActionItem(Context context, int titleId, int drawableId) {
        this.mTitle = context.getResources().getText(titleId);
        this.mDrawable = context.getResources().getDrawable(drawableId);
    }

    public ActionItem(Context context, CharSequence title, int drawableId) {
        this.mTitle = title;
        this.mDrawable = context.getResources().getDrawable(drawableId);
    }

    public ActionItem(Context context, CharSequence title) {
        this.mTitle = title;
        this.mDrawable = null;
    }
}
