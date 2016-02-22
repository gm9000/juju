package com.juju.app.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.entity.Party;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.view.RoundImageView;
import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.bitmap.BitmapDisplayConfig;


public class PartyListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<Party> list = new ArrayList<Party>();
    private BitmapUtils bitmapUtils;
    private BitmapDisplayConfig bdCofig;

    public PartyListAdapter(LayoutInflater inflater,BitmapUtils bitmapUtils,BitmapDisplayConfig bdCofig,List<Party> list){
        this.inflater = inflater;
        this.list = list;
        this.bitmapUtils = bitmapUtils;
        this.bdCofig = bdCofig;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Party hh = list.get(position);
        H h = null;
        if(view==null){
            h = new H();
            view = inflater.inflate(R.layout.party_item, parent, false);
            h.pic = (RoundImageView)view.findViewById(R.id.creatorImage);
            h.name = (TextView)view.findViewById(R.id.party_name);
            h.time = (TextView)view.findViewById(R.id.time);
            h.partDesc = (TextView)view.findViewById(R.id.partyDesc);

            view.setTag(h);
        }else{
            h = (H)view.getTag();
        }

        String portraitUrl = "http://"+ GlobalVariable.serverIp+":"+GlobalVariable.serverPort+"/juju/bServer/user/getPortrait?userNo="+GlobalVariable.userNo+"&token="+GlobalVariable.token+"&targetNo="+hh.getCreatorId();
//        h.pic.setImageURI(Uri.parse(portraitUrl));
        h.name.setText(hh.getName());
        h.time.setText(hh.getStartTime());
        h.partDesc.setText(hh.getDescription());

        return view;
    }

    class H{
        RoundImageView pic;
        TextView name;
        TextView time;
        TextView partDesc;
    }



}
