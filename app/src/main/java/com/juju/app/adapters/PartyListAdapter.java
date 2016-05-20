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
import com.juju.app.entity.User;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.view.RoundImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class PartyListAdapter extends BaseAdapter{
    private DataFilter dataFilter;
    private LayoutInflater inflater;

    public void setPartyList(List<Party> partyList) {
        if(partyList != null){
            this.partyList = partyList;
        }

    }

    private List<Party> partyList = new ArrayList<Party>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private Callback mCallback;

    public void setFilterType(int filterType) {
        this.filterType = filterType;
    }

    private int filterType = 0;

    public interface Callback {
        public void click(View v);
        public void follow(Party party,int follow);
    }

    public PartyListAdapter(LayoutInflater inflater, List<Party> list, Callback callback) {
        this.inflater = inflater;
        if (list != null) {
            this.partyList = list;
        }
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
        final Party party = partyList.get(position);
        if (view == null) {
            view = inflater.inflate(R.layout.party_item, parent, false);
        }
        RoundImageView imgCreatorHead = (RoundImageView) view.findViewById(R.id.creatorImage);
        TextView txtCreatorName = (TextView) view.findViewById(R.id.creator_name);
        TextView txtPartyName = (TextView) view.findViewById(R.id.party_name);
        TextView txtTime = (TextView) view.findViewById(R.id.time);
        TextView txtPartyDesc = (TextView) view.findViewById(R.id.partyDesc);
        TextView txtStatus = (TextView) view.findViewById(R.id.txt_status);
        ImageView imgFollow = (ImageView) view.findViewById(R.id.follow_icon);
        TextView txtOperate = (TextView) view.findViewById(R.id.txt_operate);
        ImageView imgFlag = (ImageView) view.findViewById(R.id.img_flag);
        LinearLayout layoutBack = (LinearLayout) view.findViewById(R.id.layout_back);

        txtOperate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (party.getFollowFlag() == 0) {
                    mCallback.follow(party, 1);
                } else {
                    mCallback.follow(party, 0);
                }
            }
        });

        BitmapUtilFactory.getInstance(inflater.getContext()).bind(imgCreatorHead,
                HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo="
                        + party.getUserNo(), BitmapUtilFactory.Option.imageOptions());
        txtCreatorName.setText(party.getCreator().getNickName());
        txtPartyName.setText(party.getName());
        if(party.getTime()!=null) {
            txtTime.setText(dateFormat.format(party.getTime()));
        }else{
            txtTime.setText("暂无任何方案");
        }

        switch (party.getFollowFlag()){
            case 0:
                imgFollow.setImageResource(R.mipmap.heart_hollow);
                txtOperate.setText(R.string.follow);
                layoutBack.setBackgroundColor(inflater.getContext().getResources().getColor(R.color.red));
                break;
            case 1:
                imgFollow.setImageResource(R.mipmap.heart_red);
                txtOperate.setText(R.string.unfollow);
                layoutBack.setBackgroundColor(inflater.getContext().getResources().getColor(R.color.blue1));
                break;
        }

        switch (party.getStatus()){
            case -1:
                imgFlag.setImageResource(R.mipmap.description);
                txtStatus.setText(R.string.drafts);
                break;
            case 0:
                imgFlag.setImageResource(R.mipmap.flag_red);
                txtStatus.setText(R.string.calling);
                break;
            case 1:
                imgFlag.setImageResource(R.mipmap.flag_green);
                txtStatus.setText(R.string.running);
                break;
            case 2:
                imgFlag.setImageResource(R.mipmap.flag_gray);
                txtStatus.setText(R.string.finished);
                break;
        }
        txtPartyDesc.setText(party.getDesc());
        return view;
    }

    public Filter getFilter() {
        if (dataFilter == null) {
            dataFilter = new DataFilter();
        }
        return dataFilter;
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

}
