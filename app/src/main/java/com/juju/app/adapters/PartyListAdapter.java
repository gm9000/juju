package com.juju.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Party;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.view.RoundImageView;
import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.bitmap.BitmapDisplayConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class PartyListAdapter extends BaseAdapter implements View.OnClickListener {
    private DataFilter dataFilter;
    private LayoutInflater inflater;

    public void setPartyList(List<Party> partyList) {
        if(partyList != null){
            this.partyList = partyList;
        }

    }

    private List<Party> partyList = new ArrayList<Party>();
    private BitmapUtils bitmapUtils;
    private BitmapDisplayConfig bdCofig;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private Callback mCallback;

    public void setFilterType(int filterType) {
        this.filterType = filterType;
    }

    private int filterType = 0;

    public interface Callback {
        public void click(View v);
    }

    public PartyListAdapter(LayoutInflater inflater, BitmapUtils bitmapUtils, BitmapDisplayConfig bdCofig, List<Party> list, Callback callback) {
        this.inflater = inflater;
        if (list != null) {
            this.partyList = list;
        }
        this.bitmapUtils = bitmapUtils;
        this.bdCofig = bdCofig;
        this.mCallback = callback;
    }

    @Override
    public int getCount() {
        return partyList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return partyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Party party = partyList.get(position);
        H h = null;
        if (view == null) {
            h = new H();
            view = inflater.inflate(R.layout.party_item, parent, false);
            h.pic = (RoundImageView) view.findViewById(R.id.creatorImage);
            h.name = (TextView) view.findViewById(R.id.party_name);
            h.time = (TextView) view.findViewById(R.id.time);
            h.partDesc = (TextView) view.findViewById(R.id.partyDesc);
            h.status = (TextView) view.findViewById(R.id.txt_status);
            h.followIcon = (ImageView) view.findViewById(R.id.follow_icon);
            h.operate = (TextView) view.findViewById(R.id.txt_operate);
            h.operate.setTag(R.id.tag_index,position);
            h.operate.setTag(R.id.follow_icon,h.followIcon);
            h.flagIcon = (ImageView) view.findViewById(R.id.img_flag);
            h.layoutBack = (LinearLayout) view.findViewById(R.id.layout_back);

            view.setTag(h);
        } else {
            h = (H) view.getTag();
        }
        h.operate.setOnClickListener(this);
        BitmapUtilFactory.getInstance(inflater.getContext()).display(h.pic, HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + party.getCreator().getUserNo());
        h.name.setText(party.getName());
        if(party.getTime()!=null) {
            h.time.setText(dateFormat.format(party.getTime()));
        }else{
            h.time.setText("暂无任何方案");
        }

        switch (party.getFollowFlag()){
            case 0:
                h.followIcon.setImageResource(R.mipmap.heart_hollow);
                h.operate.setTag(R.id.tag_follow_status, party.getFollowFlag());
                h.operate.setText(R.string.follow);
                h.layoutBack.setBackgroundColor(inflater.getContext().getResources().getColor(R.color.red));
                break;
            case 1:
                h.followIcon.setImageResource(R.mipmap.heart_red);
                h.operate.setTag(R.id.tag_follow_status, party.getFollowFlag());
                h.operate.setText(R.string.unfollow);
                h.layoutBack.setBackgroundColor(inflater.getContext().getResources().getColor(R.color.blue1));
                break;
        }

        switch (party.getStatus()){
            case 0:
                h.flagIcon.setImageResource(R.mipmap.flag_red);
                h.status.setText("召集中");
                break;
            case 1:
                h.flagIcon.setImageResource(R.mipmap.flag_green);
                h.status.setText("进行中");
                break;
            case 2:
                h.flagIcon.setImageResource(R.mipmap.flag_gray);
                h.status.setText("已结束");
                break;
        }
        h.partDesc.setText(party.getDesc());
        return view;
    }

    public Filter getFilter() {
        if (dataFilter == null) {
            dataFilter = new DataFilter();
        }
        return dataFilter;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.txt_operate:
                ImageView heartIcon = (ImageView) v.getTag(R.id.follow_icon);

                int index = (Integer) v.getTag(R.id.tag_index);
                Party party = partyList.get(index);

                if (((Integer) v.getTag(R.id.tag_follow_status)).intValue() == 0) {
                    v.setTag(R.id.tag_follow_status, 1);
                    party.setFollowFlag(1);
                    heartIcon.setImageResource(R.mipmap.heart_red);
                } else {
                    v.setTag(R.id.tag_follow_status, 0);
                    heartIcon.setImageResource(R.mipmap.heart_hollow);
                    party.setFollowFlag(0);
                    if(filterType == 2){
                        partyList.remove(party);
                    }
                }
                JujuDbUtils.saveOrUpdate(party);
                notifyDataSetChanged();
                break;
        }
    }

    class DataFilter extends Filter {
        //执行筛选
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults filterResults = new FilterResults();
            List<Party> filterPartyList = new ArrayList<Party>();
            for (Party party : partyList) {
                if (party.getName().contains(charSequence) || party.getDesc().contains(charSequence)) {
                    filterPartyList.add(party);
                }
            }
            filterResults.values = filterPartyList;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence arg0, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    class H {
        RoundImageView pic;
        TextView name;
        TextView time;
        TextView partDesc;
        ImageView followIcon;
        TextView status;
        ImageView flagIcon;
        TextView operate;
        LinearLayout layoutBack;
    }


}
