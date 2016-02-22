package com.juju.app.adapter.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.entity.http.GroupChat;
import com.juju.app.utils.ViewHolderUtil;
import com.juju.app.view.SwipeLayout;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目名称：juju
 * 类描述：群聊列表数据源
 * 创建人：gm
 * 日期：2016/2/21 17:09
 * 版本：V1.0.0
 */
public class GroupChatListAdpter extends BaseAdapter {

    private Context context;
    private List<GroupChat> groupChats;
    private LayoutInflater layoutInflater;

    public GroupChatListAdpter(Context context, List<GroupChat> groupChats) {
        this.context = context;
        this.groupChats = groupChats;
    }


    @Override
    public int getCount() {
        return groupChats.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        layoutInflater = LayoutInflater.from(context);
        if(convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg, parent, false);
        }
        ImageView img_avar = ViewHolderUtil.get(convertView,
                R.id.contactitem_avatar_iv);
        TextView txt_name = ViewHolderUtil.get(convertView, R.id.txt_name);
        TextView txt_state = ViewHolderUtil.get(convertView, R.id.txt_state);
        TextView txt_del = ViewHolderUtil.get(convertView, R.id.txt_del);
        TextView txt_content = ViewHolderUtil.get(convertView, R.id.txt_content);
        TextView txt_time = ViewHolderUtil.get(convertView, R.id.txt_time);
        TextView unreadLabel = ViewHolderUtil.get(convertView,
                R.id.unread_msg_number);
        SwipeLayout swipe = ViewHolderUtil.get(convertView, R.id.swipe);
        final GroupChat gChat = groupChats.get(position);
        txt_name.setText(gChat.getName());
        txt_time.setText("11:11");
        txt_state.setText("送达");
        txt_content.setText("今天晚上有空吗？晚上一起去打球！");
//        txt_del.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                deleteID = position;
//                Tipdialog = new WarnTipDialog((Activity) context,
//                        "您确定要删除该聊天吗？");
//                Tipdialog.setBtnOkLinstener(onclick);
//                Tipdialog.show();
//            }
//        });
        return convertView;

    }


}
