package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.juju.app.R;
import com.juju.app.activity.party.PartyDetailActivity;
import com.juju.app.entity.Plan;
import com.juju.app.utils.ViewHolderUtil;
import com.juju.app.view.SwipeLayoutView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：群聊列表数据源
 * 创建人：gm
 * 日期：2016/2/21 17:09
 * 版本：V1.0.0
 */
public class PlanInfoListAdapter extends BaseSwipeAdapter {

    private Context context;
    private List<Plan> planList;
    private SimpleDateFormat dateFormat;
    private boolean canSwipe;
    private LatLng myLocation;
    private DecimalFormat df = new DecimalFormat("#.0");

    private int changeIndex;

    public PlanInfoListAdapter(Context context, List<Plan> planList,boolean canSwipe,LatLng location) {
        this.context = context;
        this.planList = planList;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        this.canSwipe = canSwipe;
        this.myLocation = location;
    }


    @Override
    public int getCount() {
        return planList.size();
    }

    @Override
    public Object getItem(int position) {
        return planList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setPlanList(List<Plan> planList){
        this.planList = planList;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        View convertView = renderPlan(position, null, parent);
        return convertView;
    }

    @Override
    public void fillValues(int position, View convertView) {
        renderPlan(position, convertView, null);
    }

    public View renderPlan(final int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.plan_info_item, parent, false);
            SwipeLayoutView layout_swipe = ViewHolderUtil.get(convertView, R.id.swipe);
            layout_swipe.setSwipeEnabled(canSwipe);
        }
        TextView txt_time = ViewHolderUtil.get(convertView, R.id.txt_time);
        TextView txt_address = ViewHolderUtil.get(convertView, R.id.txt_address);
//        TextView txt_content = ViewHolderUtil.get(convertView, R.id.txt_fullDesc);
        TextView txt_attendNum = ViewHolderUtil.get(convertView, R.id.txt_attend_num);
        TextView txt_operate = ViewHolderUtil.get(convertView, R.id.txt_operate);
        LinearLayout layout_back = ViewHolderUtil.get(convertView, R.id.layout_back);
        ImageView img_selected =  ViewHolderUtil.get(convertView, R.id.img_selected);
        TextView txt_signedTag =  ViewHolderUtil.get(convertView, R.id.txt_tag_signed);
        TextView txt_distance = ViewHolderUtil.get(convertView, R.id.txt_distance);


        final Plan plan = planList.get(position);
        txt_time.setText(dateFormat.format(plan.getStartTime()));
        txt_address.setText(plan.getAddress());
        txt_attendNum.setText(String.valueOf(plan.getAddtendNum()));

//        if(plan.getDesc()!=null && !plan.getDesc().equals("")) {
//            txt_content.setTextColor(context.getResources().getColor(R.color.black));
//            txt_content.setText("\t\t" + plan.getDesc());
//        }
        switch(plan.getStatus()){
            case 0: //  未选中
                layout_back.setBackgroundColor(context.getResources().getColor(R.color.red));
                img_selected.setVisibility(View.GONE);
//                txt_selectedTag.setText(R.string.selected);
                txt_operate.setText(R.string.select);
                break;
            case 1: //  选中的方案
                layout_back.setBackgroundColor(context.getResources().getColor(R.color.blue1));
                img_selected.setVisibility(View.VISIBLE);
//                txt_selectedTag.setText(R.string.selected);
                txt_operate.setText(R.string.unselect);

        }

        switch(plan.getSigned()){
            case 0: //  未报名
                txt_signedTag.setVisibility(View.GONE);
                break;
            case 1: //  已报名
                txt_signedTag.setVisibility(View.VISIBLE);
                break;

        }
        if(plan.getLatitude()!=0 && myLocation!=null){
            double distance = DistanceUtil.getDistance(new LatLng(plan.getLatitude(),plan.getLongitude()),myLocation);
            txt_distance.setText(getDistanceStr(distance));
        }else if(plan.getLatitude() != 0){
            txt_distance.setText(R.string.un_location);
        }else{
            txt_distance.setText(R.string.un_tag);
        }


        txt_operate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SwipeLayoutView)v.getParent().getParent()).close();
                ((PartyDetailActivity)context).operatePlan(position);
            }
        });
        return convertView;

    }

    private String getDistanceStr(double distance) {
        if(distance < 1000){
            return ((int)distance)+"米";
        }else{
            return df.format(distance/1000)+"公里";
        }
    }


}
