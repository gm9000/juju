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
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.ViewHolderUtil;
import com.juju.app.view.RoundImageView;
import com.juju.app.view.SwipeLayoutView;

import java.util.ArrayList;
import java.util.List;


public class MyInviteListAdapter extends BaseSwipeAdapter{
    private Context context;

    private List<Invite> inviteList = new ArrayList<Invite>();
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
        // TODO Auto-generated method stub
        return inviteList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        View convertView = renderInvite(position, null, parent);
        return convertView;
    }

    @Override
    public void fillValues(int position, View convertView) {
        renderInvite(position, convertView, null);
    }

    public View renderInvite(final int position, View view, ViewGroup parent) {

        Invite invite = inviteList.get(position);

        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.my_invite_item, parent, false);
        }
        SwipeLayoutView layout_swipe = ViewHolderUtil.get(view, R.id.swipe);


        TextView waitingProcess = (TextView) view.findViewById(R.id.waiting_process);
        RoundImageView imgHead = (RoundImageView) view.findViewById(R.id.img_head);
        TextView txtNickName = (TextView) view.findViewById(R.id.txt_nick_name);
        TextView txtTime = (TextView) view.findViewById(R.id.txt_time);
        TextView txtInviteInfo = (TextView) view.findViewById(R.id.txt_invite_info);
        TextView txtStatus = (TextView) view.findViewById(R.id.txt_status);
        TextView txtPass = (TextView) view.findViewById(R.id.txt_pass);
        TextView txtNoPass = (TextView) view.findViewById(R.id.txt_no_pass);




        switch(invite.getStatus()){
            case 0:
                layout_swipe.setSwipeEnabled(true);
                txtStatus.setText(R.string.unpass);
                txtPass.setText(R.string.delete);
                txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
                txtNoPass.setVisibility(View.GONE);
                waitingProcess.setVisibility(View.GONE);
                break;
            case 1:
                layout_swipe.setSwipeEnabled(true);
                txtStatus.setText(R.string.pass);
                txtPass.setText(R.string.delete);
                txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
                txtNoPass.setVisibility(View.GONE);
                waitingProcess.setVisibility(View.GONE);
                break;
            case -1:
                layout_swipe.setSwipeEnabled(true);
                if(invite.getFlag() == 0){
                    txtStatus.setText(R.string.uncheck);
                    txtPass.setText(R.string.delete);
                    txtPass.setBackgroundColor(context.getResources().getColor(R.color.red));
                    txtNoPass.setVisibility(View.GONE);
                }else{
                    txtStatus.setText(R.string.unprocess);
                    txtPass.setText(R.string.check_pass);
                    txtPass.setBackgroundColor(context.getResources().getColor(R.color.green));
                    txtNoPass.setText(R.string.check_fail);
                    txtNoPass.setBackgroundColor(context.getResources().getColor(R.color.blue1));
                    waitingProcess.setVisibility(View.VISIBLE);
                }
                break;
        }

        if(invite.getFlag() == 0){
            txtInviteInfo.setText("邀请TA加入 " + invite.getGroupName());
        }else{
            txtInviteInfo.setText("邀请您加入 " + invite.getGroupName());
        }

        BitmapUtilFactory.getInstance(context).bind(imgHead, HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + invite.getUserNo(), BitmapUtilFactory.Option.imageOptions());
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

        return view;
    }

}
