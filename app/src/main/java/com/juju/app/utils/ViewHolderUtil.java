package com.juju.app.utils;

import android.util.SparseArray;
import android.view.View;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/2/18 17:22
 * 版本：V1.0.0
 */
public class ViewHolderUtil {

    public static <T extends View> T get(View view, int id) {
        SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
        if (viewHolder == null) {
            viewHolder = new SparseArray<View>();
            view.setTag(viewHolder);
        }
        View childView = viewHolder.get(id);
        if (childView == null) {
            childView = view.findViewById(id);
            viewHolder.put(id, childView);
        }
        return (T) childView;
    }
}
