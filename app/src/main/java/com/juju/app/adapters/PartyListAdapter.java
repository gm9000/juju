package com.juju.app.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.entity.Party;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.view.RoundImageView;
import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.bitmap.BitmapDisplayConfig;


public class PartyListAdapter extends BaseAdapter implements View.OnClickListener {
    private DataFilter dataFilter;
    private LayoutInflater inflater;
    private List<Party> allList = new ArrayList<Party>();
    private List<Party> filteredPartyList = new ArrayList<Party>();
    private BitmapUtils bitmapUtils;
    private BitmapDisplayConfig bdCofig;
    private Callback mCallback;
    private int filterType = 0;

    public interface Callback {
        public void click(View v);
    }

    public PartyListAdapter(LayoutInflater inflater, BitmapUtils bitmapUtils, BitmapDisplayConfig bdCofig, List<Party> list, Callback callback) {
        this.inflater = inflater;
        if (list != null) {
            this.filteredPartyList = list;
            allList.addAll(filteredPartyList);
        }
        this.bitmapUtils = bitmapUtils;
        this.bdCofig = bdCofig;
        this.mCallback = callback;
    }

    @Override
    public int getCount() {
        return filteredPartyList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return filteredPartyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Party party = filteredPartyList.get(position);
        H h = null;
        if (view == null) {
            h = new H();
            view = inflater.inflate(R.layout.party_item, parent, false);
            h.pic = (RoundImageView) view.findViewById(R.id.creatorImage);
            h.name = (TextView) view.findViewById(R.id.party_name);
            h.time = (TextView) view.findViewById(R.id.time);
            h.partDesc = (TextView) view.findViewById(R.id.partyDesc);
            h.followIcon = (ImageView) view.findViewById(R.id.follow_icon);
            h.followIcon.setTag(R.id.tag_follow_status,party.getFollowFlag());
            h.followIcon.setTag(R.id.tag_index,position);

            view.setTag(h);
        } else {
            h = (H) view.getTag();
        }
        h.followIcon.setOnClickListener(this);
        String portraitUrl = "http://" + GlobalVariable.serverIp + ":" + GlobalVariable.serverPort + "/juju/bServer/user/getPortrait?userNo=" + GlobalVariable.userNo + "&token=" + GlobalVariable.token + "&targetNo=" + party.getCreatorId();
//        h.pic.setImageURI(Uri.parse(portraitUrl));
        h.name.setText(party.getName());
        h.time.setText(party.getStartTime());
        h.partDesc.setText(party.getDescription());
        h.followIcon.setImageResource(party.getFollowFlag() == 0 ? R.drawable.heart : R.drawable.heart_fill);

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

        ImageView heartIcon = (ImageView) v;
        if(((Integer)v.getTag(R.id.tag_follow_status)).intValue()==0){
            v.setTag(R.id.tag_follow_status,1);
            heartIcon.setImageResource(R.drawable.heart_fill);
        }else{
            v.setTag(R.id.tag_follow_status,0);
            heartIcon.setImageResource(R.drawable.heart);
        }
        mCallback.click(v);
    }

    class DataFilter extends Filter {
        //执行筛选
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults filterResults = new FilterResults();
            filteredPartyList = new ArrayList<Party>();
            for (Iterator<Party> iterator = allList.iterator(); iterator.hasNext(); ) {
                Party party = iterator.next();
                if (charSequence.equals("all")) {
                    filterType = 0;
                    filteredPartyList.add(party);
                } else if (charSequence.equals("attend")) {
                    filterType = 1;
                    if (party.getAttendFlag() == 1) {
                        filteredPartyList.add(party);
                    }
                } else if (charSequence.equals("follow")) {
                    filterType = 2;
                    if (party.getFollowFlag() == 1) {
                        filteredPartyList.add(party);
                    }
                } else {
                    if (party.getName().contains(charSequence)) {
                        if (filterType == 0 || (filterType == 1 ? (party.getAttendFlag() == 1) : (party.getFollowFlag()) == 1)) {
                            filteredPartyList.add(party);
                        }
                    }
                }
            }
            filterResults.values = filteredPartyList;
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
    }


}
