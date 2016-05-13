package com.juju.app.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import com.juju.app.entity.chat.RecentInfo;
import com.juju.app.golobal.Constants;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ViewHolderUtil;
import com.juju.app.view.SwipeLayout;
import com.juju.app.view.groupchat.IMGroupAvatar;
import com.mogujie.tools.ScreenTools;

import java.util.ArrayList;
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

    private Logger logger = Logger.getLogger(GroupChatListAdapter.class);

    private List<RecentInfo> recentSessionList = new ArrayList<>();


    private LayoutInflater mInflater = null;
    private Context context;
    private LayoutInflater layoutInflater;


    public GroupChatListAdapter(Context context) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return recentSessionList.size();
    }

    @Override
    public RecentInfo getItem(int position) {
        if (position >= recentSessionList.size() || position < 0) {
            return null;
        }
        return recentSessionList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = renderGroup(position, convertView, parent);
        return convertView;
    }


//    private View createConvertView(int size, ViewGroup parent) {
//        View convertView = null;
//        if(size <= 1) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg, parent, false);
//        } else if (size == 2) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg2, parent, false);
//        } else if (size == 3) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg3, parent, false);
//        } else if (size == 4) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg4, parent, false);
//        } else if (size == 5) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg5, parent, false);
//        } else if (size == 6) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg6, parent, false);
//        } else if (size == 7) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg7, parent, false);
//        } else if (size == 8) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg8, parent, false);
//        } else if (size >= 9) {
//            convertView = LayoutInflater.from(context).
//                    inflate(R.layout.layout_item_msg9, parent, false);
//        }
//        return convertView;
//    }

    private View renderGroup(int position,View convertView, ViewGroup parent){
        RecentInfo recentInfo = recentSessionList.get(position);
        GroupViewHolder holder;
        if (null == convertView) {
            convertView = mInflater.inflate(R.layout.adapter_item_chat_group, parent,false);
            holder = new GroupViewHolder();
            holder.avatarLayout = (IMGroupAvatar) convertView.findViewById(R.id.contact_portrait);
            holder.uname = (TextView) convertView.findViewById(R.id.shop_name);
            holder.lastContent = (TextView) convertView.findViewById(R.id.message_body);
            holder.lastTime = (TextView) convertView.findViewById(R.id.message_time);
            holder.msgCount = (TextView) convertView.findViewById(R.id.message_count_notify);
            holder.noDisturb = (ImageView)convertView.findViewById(R.id.message_time_no_disturb_view);
            convertView.setTag(holder);
        }else{
            holder = (GroupViewHolder)convertView.getTag();
        }

        if(recentInfo.isTop()){
            convertView.setBackgroundColor(Color.parseColor("#f4f4f4f4"));
        }else{
            convertView.setBackgroundColor(Color.WHITE);
        }

//        /**群屏蔽的设定*/
//        if(recentInfo.isForbidden())
//        {
//            holder.noDisturb.setVisibility(View.VISIBLE);
//        }
//        else
//        {
//            holder.noDisturb.setVisibility(View.GONE);
//        }

        handleGroupContact(holder, recentInfo);
        return convertView;
    }

    public final class GroupChatViewHolder {
//        public

    }

    public List<RecentInfo> getGroupChats() {
        return recentSessionList;
    }


    private void handleGroupContact(GroupViewHolder groupViewHolder,
                                    RecentInfo recentInfo) {
        String avatarUrl = null;
        String mainName = "";
        String lastContent = "";
        String lastTime = "";
        int unReadCount = 0;
//        int sessionType = DBConstant.SESSION_TYPE_SINGLE;

        mainName =recentInfo.getName();
        lastContent = recentInfo.getLatestMsgData();
        // todo 是不是每次都需要计算
        lastTime = DateUtil.getSessionTime(recentInfo.getUpdateTime());
        unReadCount = recentInfo.getUnReadCnt();
//        sessionType = recentInfo.getSessionType();
        // 设置未读消息计数 只有群组有的

        if (unReadCount > 0) {
            groupViewHolder.msgCount.setBackgroundResource(R.drawable.tt_message_notify);
            groupViewHolder.msgCount.setVisibility(View.VISIBLE);
            ((RelativeLayout.LayoutParams)groupViewHolder.msgCount.getLayoutParams()).leftMargin=ScreenTools.instance(this.mInflater.getContext()).dip2px(-10);
            ((RelativeLayout.LayoutParams)groupViewHolder.msgCount.getLayoutParams()).topMargin=ScreenTools.instance(this.mInflater.getContext()).dip2px(3);
            groupViewHolder.msgCount.getLayoutParams().width = RelativeLayout.LayoutParams.WRAP_CONTENT;
            groupViewHolder.msgCount.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            groupViewHolder.msgCount.setPadding(ScreenTools.instance(this.mInflater.getContext()).dip2px(3),0,ScreenTools.instance(this.mInflater.getContext()).dip2px(3),0);

            String strCountString=String.valueOf(unReadCount);
            if (unReadCount>99) {
                strCountString = "99+";
            }
            groupViewHolder.msgCount.setVisibility(View.VISIBLE);
            groupViewHolder.msgCount.setText(strCountString);
        } else {
            groupViewHolder.msgCount.setVisibility(View.GONE);
        }

        //头像设置
        setGroupAvatar(groupViewHolder, recentInfo.getAvatar());
        // 设置其它信息
        groupViewHolder.uname.setText(mainName);
        groupViewHolder.lastContent.setText(lastContent);
        groupViewHolder.lastTime.setText(lastTime);
    }

//    public void setData(List<GroupChatInitBean> groupChats) {
//        this.groupChats = groupChats;
//        notifyDataSetChanged();
//    }

    public void setData(List<RecentInfo> recentSessionList) {
        logger.d("recent#set New recent session list");
        logger.d("recent#notifyDataSetChanged");
        this.recentSessionList = recentSessionList;
        notifyDataSetChanged();
    }


    /**
     * 基本HOLDER
     */
    public static class ContactHolderBase{
        public TextView uname;
        public TextView lastContent;
        public TextView lastTime;
        public TextView msgCount;
        public ImageView noDisturb;
    }

    /**
     * 群组HOLDER
     */
    private final static class GroupViewHolder extends ContactHolderBase{
        public IMGroupAvatar avatarLayout;
    }

    /**
     * 设置群头像
     * @param holder
     * @param avatarUrlList
     */
    private void setGroupAvatar(GroupViewHolder holder, List<String> avatarUrlList){
        try {
            if (null == avatarUrlList) {
                return;
            }
            holder.avatarLayout.setAvatarUrlAppend(Constants.AVATAR_APPEND_32);
            holder.avatarLayout.setChildCorner(3);
            if (null != avatarUrlList) {
                holder.avatarLayout.setAvatarUrls(new ArrayList<String>(avatarUrlList));
            }
        }catch (Exception e){
           e.printStackTrace();
        }

    }


}
