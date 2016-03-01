package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.entity.VideoProgram;
import com.juju.app.utils.ViewHolderUtil;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：群聊列表数据源
 * 创建人：gm
 * 日期：2016/2/21 17:09
 * 版本：V1.0.0
 */
public class VideoProgramListAdpter extends BaseAdapter {

    private Context context;
    private List<VideoProgram> videoProgramList;
    private LayoutInflater layoutInflater;

    public VideoProgramListAdpter(Context context, List<VideoProgram> videoProgramList) {
        this.context = context;
        this.videoProgramList = videoProgramList;
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
        if(convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.party_live_item, parent, false);
        }
        ImageView liveCapture = ViewHolderUtil.get(convertView,
                R.id.live_capture);
        TextView creatorName = ViewHolderUtil.get(convertView, R.id.live_user_name);
        TextView liveTypeTxt = ViewHolderUtil.get(convertView, R.id.live_type_txt);
        TextView liveTimeTxt = ViewHolderUtil.get(convertView, R.id.live_time_txt);


        final VideoProgram videoProgram = videoProgramList.get(position);
        creatorName.setText(videoProgram.getCreatorName());
        if(videoProgram.getStatus() == 0){
            liveTypeTxt.setText("视频直播");
            liveTimeTxt.setText("正在直播...");
            liveTimeTxt.setTextColor(0xFFFF0000);
        }else{
            liveTypeTxt.setText("直播节目");
            liveTimeTxt.setText(videoProgram.getEndTime());
            liveTimeTxt.setTextColor(0xFF000000);
        }
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
