package com.juju.app.view.groupchat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.utils.DateUtil;

import java.util.Date;

/**
 * 项目名称：juju
 * 类描述：普通通知View
 * 创建人：gm
 * 日期：2016/7/2 16:44
 * 版本：V1.0.0
 */
public class NormalNotifyRenderView extends LinearLayout {

    private TextView notify_title;

    public NormalNotifyRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    public static NormalNotifyRenderView inflater(Context context,ViewGroup viewGroup){
        NormalNotifyRenderView normalNotifyRenderView = (NormalNotifyRenderView) LayoutInflater
                .from(context).inflate(R.layout.render_normal_notify_message_title_time, viewGroup, false);
        return normalNotifyRenderView;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        notify_title = (TextView) findViewById(R.id.time_title);
    }

    /**与数据绑定*/
    public void setNotifyMsg(String msg){
        notify_title.setText(msg);
    }
}
