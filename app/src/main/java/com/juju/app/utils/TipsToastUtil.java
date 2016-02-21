package com.juju.app.utils;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;

/**
 * 项目名称：juju
 * 类描述：tip提示辅助类
 * 创建人：gm
 * 日期：2016/2/17 11:19
 * 版本：V1.0.0
 */
public class TipsToastUtil extends Toast {

    public TipsToastUtil(Context context) {
        super(context);
    }

    public static TipsToastUtil makeText(Context context, CharSequence text,
                                     int duration) {
        TipsToastUtil result = new TipsToastUtil(context);

        LayoutInflater inflate = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.view_tips, null);
        TextView tv = (TextView) v.findViewById(R.id.tips_msg);
        tv.setText(text);

        result.setView(v);
        // setGravity方法用于设置位置，此处为垂直居中
        result.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        result.setDuration(duration);

        return result;
    }

    public static TipsToastUtil makeText(Context context, int resId, int duration)
            throws Resources.NotFoundException {
        return makeText(context, context.getResources().getText(resId),
                duration);
    }

    public void setIcon(int iconResId) {
        if (getView() == null) {
            throw new RuntimeException(
                    "This Toast was not created with Toast.makeText()");
        }
        ImageView iv = (ImageView) getView().findViewById(R.id.tips_icon);
        if (iv == null) {
            throw new RuntimeException(
                    "This Toast was not created with Toast.makeText()");
        }
        iv.setImageResource(iconResId);
    }

    @Override
    public void setText(CharSequence s) {
        if (getView() == null) {
            throw new RuntimeException(
                    "This Toast was not created with Toast.makeText()");
        }
        TextView tv = (TextView) getView().findViewById(R.id.tips_msg);
        if (tv == null) {
            throw new RuntimeException(
                    "This Toast was not created with Toast.makeText()");
        }
        tv.setText(s);
    }
}
