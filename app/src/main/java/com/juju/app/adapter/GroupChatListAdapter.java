package com.juju.app.adapter;

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
import com.juju.app.bean.groupchat.GroupChatInitBean;
import com.juju.app.entity.http.Group;
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
public class GroupChatListAdapter extends BaseAdapter {

    private Context context;
    private List<GroupChatInitBean> groupChats;
    private LayoutInflater layoutInflater;


    public GroupChatListAdapter(Context context, List<GroupChatInitBean> groupChats) {
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
        final GroupChatInitBean groupChat = groupChats.get(position);

//        System.out.println("groupChat:"+groupChat.toString()+"\r\n"+"memberNum:"+groupChat.getGroup().getMemberNum());
        convertView = createConvertView(groupChat.getGroup().getMemberNum(), null);
        TextView txt_name = ViewHolderUtil.get(convertView, R.id.txt_name);
        TextView txt_state = ViewHolderUtil.get(convertView, R.id.txt_state);
        TextView txt_del = ViewHolderUtil.get(convertView, R.id.txt_del);
        TextView txt_content = ViewHolderUtil.get(convertView, R.id.txt_content);
        TextView txt_time = ViewHolderUtil.get(convertView, R.id.txt_time);
        TextView unreadLabel = ViewHolderUtil.get(convertView,
                R.id.unread_msg_number);
        TextView txt_unread_msg_number = ViewHolderUtil.get(convertView, R.id.unread_msg_number);
        SwipeLayout swipe = ViewHolderUtil.get(convertView, R.id.swipe);
        Group group = groupChat.getGroup();
        txt_name.setText(group.getName());
        txt_time.setText(groupChat.getTime());
        txt_state.setText(groupChat.getState());
        txt_content.setText(groupChat.getContent());
        txt_unread_msg_number.setText(groupChat.getTotal());

//        ImageView img_avar = ViewHolderUtil.get(convertView,
//                R.id.contactitem_avatar_iv);

        return convertView;
    }

    private View createConvertView(int size, ViewGroup parent) {
        View convertView = null;
        if(size <= 1) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg, parent, false);
        } else if (size == 2) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg2, parent, false);
        } else if (size == 3) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg3, parent, false);
        } else if (size == 4) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg4, parent, false);
        } else if (size == 5) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg5, parent, false);
        } else if (size == 6) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg6, parent, false);
        } else if (size == 7) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg7, parent, false);
        } else if (size == 8) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg8, parent, false);
        } else if (size >= 9) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_item_msg9, parent, false);
        }
        return convertView;
    }


    public List<GroupChatInitBean> getGroupChats() {
        return groupChats;
    }


}
