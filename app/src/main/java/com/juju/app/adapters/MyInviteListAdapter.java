package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.juju.app.R;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Invite;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.ViewHolderUtil;
import com.juju.app.view.RoundImageView;
import com.juju.app.view.SwipeLayoutView;

import java.util.ArrayList;
import java.util.List;


public class MyInviteListAdapter extends BaseSwipeAdapter {
    private Context context;
    private List<Invite> inviteList = new ArrayList<>();
    private Callback mCallback;


    public interface Callback {
        public void checkFailInvite(int position);
        public void passOrDeleteInvite(int position);
    }

    public MyInviteListAdapter(Context context, List<Invite> list, Callback callback) {
        this.context = context;
        if (list != null) {
            this.inviteList = list;
        }
        this.mCallback = callback;
    }

    public void setInviteList(List<Invite> inviteList) {
        this.inviteList = inviteList;
    }

    @Override
    public int getCount() {
        return inviteList.size();
    }

    @Override
    public Object getItem(int position) {
        return inviteList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        View convertView = renderView(position, null, parent);
        return convertView;
    }

    @Override
    public void fillValues(int position, View convertView) {
        renderView(position, convertView, null);
    }


    private View renderView(final int position, View convertView, ViewGroup parent) {
        Invite invite = inviteList.get(position);
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.my_invite_item, parent, false);
        }
        SwipeLayoutView layout_swipe = ViewHolderUtil.get(convertView, R.id.swipe);
        TextView waitingProcess = ViewHolderUtil.get(convertView, R.id.waiting_process);
        RoundImageView imgHead = ViewHolderUtil.get(convertView, R.id.img_head);
        TextView txtNickName = ViewHolderUtil.get(convertView, R.id.txt_nick_name);
        TextView txtTime = ViewHolderUtil.get(convertView, R.id.txt_time);
        TextView txtInviteInfo = ViewHolderUtil.get(convertView, R.id.txt_invite_info);
        TextView txtStatus = ViewHolderUtil.get(convertView, R.id.txt_status);
        TextView txtPass = ViewHolderUtil.get(convertView, R.id.txt_pass);
        TextView txtNoPass = ViewHolderUtil.get(convertView, R.id.txt_no_pass);
        layout_swipe.close();

//        switch(invite.getStatus()){
//            case 0:
//                layout_swipe.setSwipeEnabled(true);
//                txtStatus.setText(R.string.unpass);
//                txtPass.setText(R.string.delete);
//                txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
//                txtNoPass.setVisibility(View.GONE);
//                waitingProcess.setVisibility(View.GONE);
//                break;
//            case 1:
//                layout_swipe.setSwipeEnabled(true);
//                txtStatus.setText(R.string.pass);
//                txtPass.setText(R.string.delete);
//                txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
//                txtNoPass.setVisibility(View.GONE);
//                waitingProcess.setVisibility(View.GONE);
//                break;
//            case -1:
//                layout_swipe.setSwipeEnabled(true);
//                if(invite.getFlag() == 0){
//                    txtStatus.setText(R.string.uncheck);
//                    txtPass.setText(R.string.delete);
//                    txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
//                    txtNoPass.setVisibility(View.GONE);
//                }else{
//                    txtStatus.setText(R.string.unprocess);
//                    txtPass.setText(R.string.check_pass);
//                    txtPass.setBackgroundColor(context.getResources().getColor(R.color.green));
//                    txtNoPass.setText(R.string.check_fail);
//                    txtNoPass.setBackgroundColor(context.getResources().getColor(R.color.blue1));
//                    waitingProcess.setVisibility(View.VISIBLE);
//                }
//                break;
//        }

        switch(invite.getStatus()) {
            //邀请加群
            case 0:
                layout_swipe.setSwipeEnabled(true);
                if(invite.getFlag() == 0) {
                    txtStatus.setText(R.string.waiting_ta);

                    //隐藏图像左上角文字
                    waitingProcess.setVisibility(View.GONE);
                    //滑动操作按钮（删除）
                    txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
                    txtPass.setVisibility(View.VISIBLE);
                    txtPass.setText(R.string.delete);
                    txtNoPass.setVisibility(View.GONE);
                } else {
                    txtStatus.setText(R.string.waiting_you);
                    //显示图像左上角文字
                    waitingProcess.setVisibility(View.VISIBLE);
                    //滑动操作按钮（加入 拒绝）
                    txtPass.setVisibility(View.VISIBLE);
                    txtPass.setText(R.string.check_pass);
                    txtPass.setBackgroundColor(context.getResources().getColor(R.color.green));
                    txtNoPass.setVisibility(View.VISIBLE);
                    txtNoPass.setText(R.string.check_fail);
                    txtNoPass.setBackgroundColor(context.getResources().getColor(R.color.blue1));
                }
                break;
            case 1:
                layout_swipe.setSwipeEnabled(true);
                txtStatus.setText(R.string.pass);
                //隐藏图像左上角文字
                waitingProcess.setVisibility(View.GONE);
                //滑动操作按钮（删除）
                txtPass.setVisibility(View.VISIBLE);
                txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
                txtPass.setText(R.string.delete);
                txtNoPass.setVisibility(View.GONE);
                break;

            case 2:
                layout_swipe.setSwipeEnabled(true);
                txtStatus.setText(R.string.unpass);
                //隐藏图像左上角文字
                waitingProcess.setVisibility(View.GONE);
                //滑动操作按钮（删除）
                txtPass.setVisibility(View.VISIBLE);
                txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
                txtPass.setText(R.string.delete);
                txtNoPass.setVisibility(View.GONE);
                break;
        }

        if(invite.getFlag() == 0){
            txtInviteInfo.setText("邀请TA加入 " + invite.getGroupName());
        }else{
            txtInviteInfo.setText("邀请您加入 " + invite.getGroupName());
        }

        ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo="
                + invite.getUserNo(), imgHead, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);

        txtNickName.setText(invite.getNickName());
        txtTime.setText(DateUtil.getPastTimeDisplay(invite.getTime()));

        txtNoPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.checkFailInvite(position);
            }
        });

        txtPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.passOrDeleteInvite(position);
        }
        });

        return convertView;
    }




}
