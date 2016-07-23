package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.juju.app.R;
import com.juju.app.entity.Party;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ViewHolderUtil;
import com.juju.app.view.SwipeLayoutView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class MyPartyListAdapter extends BaseSwipeAdapter {
    private Context context;

    public void setPartyList(List<Party> partyList) {
        this.partyList = partyList;
    }

    private List<Party> partyList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private Callback mCallback;


    public interface Callback {
        public void deleteParty(int position);
    }

    public MyPartyListAdapter(Context context,List<Party> list, Callback callback) {
        this.context = context;
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
        return position;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        View convertView = renderDraftParty(position, null, parent);
        return convertView;
    }

    @Override
    public void fillValues(int position, View convertView) {
        renderDraftParty(position, convertView, null);
    }

    private View renderDraftParty(final int position, View view, ViewGroup parent) {

        Party party = partyList.get(position);

        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.my_party_item, parent, false);
        }
        SwipeLayoutView layout_swipe = ViewHolderUtil.get(view, R.id.swipe);

        if(party.getStatus() != -1) {
            layout_swipe.setSwipeEnabled(false);
        }

        ImageView imgPlanType = (ImageView)view.findViewById(R.id.img_plan_type);
        TextView partyName = (TextView) view.findViewById(R.id.party_name);
        TextView time = (TextView) view.findViewById(R.id.time);
        TextView partyDesc = (TextView) view.findViewById(R.id.partyDesc);
        ImageView img_status = (ImageView) view.findViewById(R.id.img_status);
        TextView txt_status = (TextView) view.findViewById(R.id.txt_status);
        TextView operate = (TextView) view.findViewById(R.id.txt_operate);
        TextView waitingProcess = (TextView) view.findViewById(R.id.waiting_process);

        if(!StringUtils.empty(party.getCoverUrl())) {
            if (party.getCoverUrl().startsWith("http:")){
                ImageLoaderUtil.getImageLoaderInstance().displayImage(party.getCoverUrl(), imgPlanType, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
            }else{
                final int resId = ((BaseActivity) context).getResValue(party.getCoverUrl().toLowerCase(), "mipmap");
                imgPlanType.setImageResource(resId);
            }
        }else{
            imgPlanType.setImageResource(R.mipmap.plan_no);
        }
        partyName.setText(party.getName());
        if(party.getTime()!=null) {
            time.setText(dateFormat.format(party.getTime()));
        }else{
            time.setText("暂无任何方案");
        }

        partyDesc.setText(party.getDesc());

        switch (party.getStatus()){
            case -1:
                img_status.setImageResource(R.mipmap.description);
                txt_status.setText(R.string.drafts);
                waitingProcess.setVisibility(View.VISIBLE);
                break;
            case 0:
                img_status.setImageResource(R.mipmap.flag_red);
                txt_status.setText(R.string.calling);
                waitingProcess.setVisibility(View.GONE);
                break;
            case 1:
                img_status.setImageResource(R.mipmap.flag_green);
                txt_status.setText(R.string.running);
                waitingProcess.setVisibility(View.GONE);
                break;
            case 2:
                img_status.setImageResource(R.mipmap.flag_gray);
                txt_status.setText(R.string.finished);
                waitingProcess.setVisibility(View.GONE);
                break;
        }

        operate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SwipeLayoutView)v.getParent().getParent()).close();
                mCallback.deleteParty(position);
            }
        });

        return view;
    }

}
