package com.juju.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.juju.app.R;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Party;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.view.RoundImageView;
import com.juju.app.view.SwipeLayoutView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class PartyListBackupAdapter extends BaseSwipeAdapter {
    private LayoutInflater inflater;

    private boolean isSearchMode = false;
    private String searchKey;

    private boolean isStockMode = false;


    private List<Party> partyList = new ArrayList<Party>();
    private List<Party> matchPartyList = new ArrayList<Party>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private Callback mCallback;
    private Comparator<Party> comparator = new Comparator<Party>() {
        @Override
        public int compare(Party p1, Party p2) {
            if(p1.getFollowFlag()==p2.getFollowFlag()){
                Long.valueOf(p2.getLocalId()).compareTo(p1.getLocalId());
            }
            if(p1.getFollowFlag()==1){
                return -1;
            }else{
                return 1;
            }
        }
    };

    public void setPartyList(List<Party> partyList) {
        if (partyList != null) {
            this.partyList = partyList;
            this.matchPartyList = partyList;
        }

    }

    public void reOrderParty() {
        Collections.sort(partyList,comparator);
        if(isSearchMode()){
            Collections.sort(matchPartyList,comparator);
        }
    }


    public interface Callback {
        public void follow(Party party, int follow);
    }

    public PartyListBackupAdapter(LayoutInflater inflater, Callback callback) {
        this.inflater = inflater;
        this.mCallback = callback;
    }

    @Override
    public int getCount() {
        return matchPartyList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return matchPartyList.get(position);
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
        View convertView = renderParty(position, null, parent);
        return convertView;
    }

    @Override
    public void fillValues(int position, View convertView) {
        renderParty(position, convertView, null);
    }

    public View renderParty(int position, View view, ViewGroup parent) {
        final Party party = matchPartyList.get(position);
        if (view == null) {
            view = inflater.inflate(R.layout.party_item_backup, parent, false);
        }
        RoundImageView imgCreatorHead = (RoundImageView) view.findViewById(R.id.creatorImage);
        TextView txtCreatorName = (TextView) view.findViewById(R.id.creator_name);
        TextView txtPartyName = (TextView) view.findViewById(R.id.party_name);
        TextView txtTime = (TextView) view.findViewById(R.id.time);
        TextView txtPartyDesc = (TextView) view.findViewById(R.id.partyDesc);
        TextView txtStatus = (TextView) view.findViewById(R.id.txt_status);
        TextView txtViewFollow = (TextView) view.findViewById(R.id.flag_follow);
        TextView txtOperate = (TextView) view.findViewById(R.id.txt_follow);
        TextView txtStock = (TextView) view.findViewById(R.id.txt_stock);
        ImageView imgFlag = (ImageView) view.findViewById(R.id.img_flag);

        if(isStockMode){
            txtOperate.setVisibility(View.GONE);
            txtStock.setText("恢复");
        }

        txtOperate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SwipeLayoutView)v.getParent().getParent()).close();
                if (party.getFollowFlag() == 0) {
                    mCallback.follow(party, 1);
                } else {
                    mCallback.follow(party, 0);
                }
            }
        });

        txtStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SwipeLayoutView)v.getParent().getParent()).close();
                mCallback.follow(party, isStockMode?0:-1);
            }
        });


        ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo="
                + party.getUserNo(), imgCreatorHead, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);

        txtCreatorName.setText(party.getCreator().getNickName());

        if (isSearchMode) {
            // 高亮显示
            if(party.isDescMatch()) {
                IMUIHelper.setTextHilighted(txtPartyDesc, party.getDesc(),
                        party.getSearchElement());
                txtPartyName.setText(party.getName());
            }else{
                IMUIHelper.setTextHilighted(txtPartyName, party.getName(),
                        party.getSearchElement());
                txtPartyDesc.setText(party.getDesc());
            }
        } else {
            txtPartyName.setText(party.getName());
            txtPartyDesc.setText(party.getDesc());
        }

        if (party.getTime() != null) {
            txtTime.setText(dateFormat.format(party.getTime()));
        } else {
            txtTime.setText("暂无任何方案");
        }

        switch (party.getFollowFlag()) {
            case 0:
                txtViewFollow.setVisibility(View.GONE);
                txtOperate.setText(R.string.top_location);
                txtOperate.setBackgroundColor(inflater.getContext().getResources().getColor(R.color.red));
                break;
            case 1:
                txtViewFollow.setVisibility(View.VISIBLE);
                txtOperate.setText(R.string.cancel_top_message);
                txtOperate.setBackgroundColor(inflater.getContext().getResources().getColor(R.color.blue1));
                break;
        }

        switch (party.getStatus()) {
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
        return view;
    }

    public void recover() {
        isSearchMode = false;
        matchPartyList = partyList;
        notifyDataSetChanged();
    }

    public void onSearch(String key) {
        isSearchMode = true;
        searchKey = key;
        List<Party> searchList = new ArrayList<Party>();
        for (Party party : partyList) {
            if (IMUIHelper.handlePartySearch(searchKey, party)) {
                searchList.add(party);
            }
        }
        matchPartyList = searchList;
        notifyDataSetChanged();
    }

    public List<Party> getMatchPartyList() {
        return matchPartyList;
    }

    public boolean isSearchMode() {
        return isSearchMode;
    }

    public void setStockMode(boolean stockMode) {
        isStockMode = stockMode;
    }
}
