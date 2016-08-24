package com.juju.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.bean.SuggestionBean;

import java.util.List;

public class SuggestionListAdapter extends BaseAdapter {

    private Context context;
    private List<SuggestionBean> suggestionList;

    public SuggestionListAdapter(Context context, List<SuggestionBean> suggestionList) {
        this.context = context;
        this.suggestionList = suggestionList;
    }

    public List<SuggestionBean> getSuggestionList() {
        return suggestionList;
    }

    public void setSuggestionList(List<SuggestionBean> suggestionList) {
        this.suggestionList = suggestionList;
    }

    @Override
    public int getCount() {
        return suggestionList.size();
    }

    @Override
    public Object getItem(int position) {
        return suggestionList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_suggestion_item, parent, false);
        }

        SuggestionBean bean = suggestionList.get(position);

        ImageView imgSearchType = (ImageView) view.findViewById(R.id.img_search_type);
        TextView txtSuggesion = (TextView) view.findViewById(R.id.txt_sugession);
        TextView txtLocation = (TextView) view.findViewById(R.id.txt_location);

        txtSuggesion.setText(bean.getKey());

        if(bean.getAddress() == null){
            txtLocation.setText("点击查询");
            imgSearchType.setImageResource(R.drawable.search);
        }else{
            txtLocation.setText(bean.getAddress());
            imgSearchType.setImageResource(R.mipmap.location);
        }

        return view;
    }

}
