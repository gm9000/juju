package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.config.HttpConstants;
import com.juju.app.event.notify.DiscussNotifyEvent;
import com.juju.app.event.notify.LiveEnterNotifyEvent;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.utils.ImageLoaderUtil;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class DiscussListAdapter extends BaseAdapter {
    private Context context;


    private List discussList;

    public void setDiscussList(List discussList) {
        this.discussList = discussList;
    }


    public DiscussListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return discussList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return discussList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.discuss_item, parent, false);
        }

        CircleImageView headImage = (CircleImageView) view.findViewById(R.id.img_user);
        TextView txtContent = (TextView) view.findViewById(R.id.txt_content);

        Object itemObject = discussList.get(position);

        if(itemObject instanceof DiscussNotifyEvent.DiscussNotifyBean) {
            final DiscussNotifyEvent.DiscussNotifyBean discuss = (DiscussNotifyEvent.DiscussNotifyBean)discussList.get(position);

            ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo="
                    + discuss.getUserNo(), headImage, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);

            StringBuffer contentBuffer = new StringBuffer();
            contentBuffer.append(discuss.getNickName());
            contentBuffer.append("：");
            int endIndex = contentBuffer.length();
            contentBuffer.append(discuss.getContent());
            IMUIHelper.setTextStartHilighted(txtContent, contentBuffer.toString(), endIndex);

        }else if(itemObject instanceof LiveEnterNotifyEvent.LiveEnterNotifyBean) {
            final LiveEnterNotifyEvent.LiveEnterNotifyBean liveEnter = (LiveEnterNotifyEvent.LiveEnterNotifyBean)discussList.get(position);

            ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo="
                    + liveEnter.getUserNo(), headImage, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);

            StringBuffer contentBuffer = new StringBuffer();
            contentBuffer.append(liveEnter.getNickName());
            contentBuffer.append(" ");
            contentBuffer.append(liveEnter.getType()==0?context.getString(R.string.live_enter):context.getString(R.string.live_exit));
            IMUIHelper.setTextStartHilighted(txtContent, contentBuffer.toString(), contentBuffer.length());
        }

        return view;
    }

}
