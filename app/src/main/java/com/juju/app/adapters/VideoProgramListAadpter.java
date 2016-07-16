package com.juju.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.party.PartyLiveActivity;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.VideoProgram;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.ViewHolderUtil;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class VideoProgramListAadpter extends BaseAdapter {

    private PartyLiveActivity context;

    public void setVideoProgramList(List<VideoProgram> videoProgramList) {
        this.videoProgramList = videoProgramList;
    }

    private List<VideoProgram> videoProgramList;
    private LayoutInflater layoutInflater;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private Callback callback;

    public VideoProgramListAadpter(PartyLiveActivity context, List<VideoProgram> videoProgramList) {
        this.context = context;
        this.videoProgramList = videoProgramList;
    }

    public interface Callback {
        public void playVideo(VideoProgram videoProgram);
    }

    @Override
    public int getCount() {
        return videoProgramList.size();
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
        if (position == 0) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.top_party_live_item, parent, false);
        } else {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.party_live_item, parent, false);
        }
        CircleImageView imgHead = ViewHolderUtil.get(convertView,
                R.id.img_head);
        TextView creatorName = ViewHolderUtil.get(convertView, R.id.live_user_name);
        TextView liveTimeTxt = ViewHolderUtil.get(convertView, R.id.live_time_txt);

        ImageView imgCaputure = ViewHolderUtil.get(convertView, R.id.img_capture);
        ImageView imgPlay = ViewHolderUtil.get(convertView, R.id.img_play);


        final VideoProgram videoProgram = videoProgramList.get(position);
        creatorName.setText(context.getIMContactManager().findContact(videoProgram.getCreatorNo()).getNickName());
        ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo="
                + videoProgram.getCreatorNo(), imgHead, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);

        ImageLoaderUtil.getImageLoaderInstance().displayImage(videoProgram.getCaptureUrl(), imgCaputure, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);

        if (videoProgram.getStatus() == 0) {
            liveTimeTxt.setText("直播中...");
            liveTimeTxt.setTextColor(0xFFFF0000);
        } else {
            liveTimeTxt.setText(DateUtil.format(videoProgram.getEndTime()));
            liveTimeTxt.setTextColor(0xFF000000);
        }


        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.playVideo(videoProgram);
            }
        });

        return convertView;

    }


}
