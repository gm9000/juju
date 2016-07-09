package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.entity.Plan;
import com.juju.app.ui.base.BaseActivity;


public class PlanTypeAdapter extends BaseAdapter {
    private Context mContext;


    public PlanTypeAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return Plan.Type.values().length;
    }

    @Override
    public Object getItem(int position) {
        return Plan.Type.values()[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final Plan.Type planType = Plan.Type.values()[position];
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.plan_type_item, parent, false);
        }

        ImageView imgPlanType = (ImageView) view.findViewById(R.id.img_plan_type);
        imgPlanType.setImageResource(((BaseActivity)mContext).getResValue(planType.name().toLowerCase()+"_icon","mipmap"));

        TextView txtPlanType = (TextView) view.findViewById(R.id.txt_plan_type);
        txtPlanType.setText(((BaseActivity)mContext).getResValue(planType.name()));

        return view;
    }

}
