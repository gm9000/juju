package com.juju.app.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.party.CitySelectActivity;
import com.juju.app.bean.CityBean;
import com.juju.app.entity.User;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.utils.Logger;

import java.util.ArrayList;
import java.util.List;


public class CitySelectAdapter extends BaseAdapter  implements SectionIndexer,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener{
    private Logger logger = Logger.getLogger(CitySelectAdapter.class);

    private List<CityBean> allCityList = new ArrayList<CityBean>();
    private List<CityBean>  backupList = new ArrayList<CityBean>();

    private boolean isSearchMode= false;
    private String searchKey;
    private CitySelectActivity ctx;

    public CitySelectAdapter(CitySelectActivity ctx){
        this.ctx = ctx;
    }

    public void setAllCityList(List<CityBean> cityList) {
        this.allCityList = cityList;
        this.backupList  = cityList;
    }

    public void recover(){
        isSearchMode = false;
        allCityList = backupList;
        notifyDataSetChanged();
    }

    public void onSearch(String key){
       isSearchMode = true;
       searchKey = key;
        List<CityBean> searchList = new ArrayList<>();
       for(CityBean city:backupList){
            if(IMUIHelper.handleCitySearch(searchKey,city)){
                searchList.add(city);
            }
        }
        allCityList = searchList;
        notifyDataSetChanged();
    }

    @Override
    public Object[] getSections() {
        return new Object[0];
    }


    // 在搜索模式下，直接返回
    @Override
    public int getPositionForSection(int sectionIndex) {
        logger.d("pinyin#getPositionForSection secton:%d", sectionIndex);
        int index = 0;
        for(CityBean entity: allCityList){
            int firstCharacter = entity.getSectionName().charAt(0);
            // logger.d("firstCharacter:%d", firstCharacter);
            if (firstCharacter == sectionIndex) {
                logger.d("pinyin#find sectionName");
                return index;
            }
            index++;
        }
        logger.e("pinyin#can't find such section:%d", sectionIndex);
        return -1;
    }


    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CityBean city = (CityBean) getItem(position);
        ctx.selectCity(city);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        User contact = (User) getItem(position);
//        IMUIHelper.handleContactItemLongClick(contact, ctx);
        return true;
    }

    @Override
    public int getCount() {
       int size = allCityList ==null?0: allCityList.size();
       return size;
    }

    @Override
    public Object getItem(int position) {
        return allCityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        CityBean cityBean = (CityBean) getItem(position);
        if(cityBean == null){
            logger.e("CitySelectAdapter#getView#cityBean is null!position:%d",position);
            return null;
        }

        if (view == null) {
            view = LayoutInflater.from(ctx).inflate(R.layout.adapter_city_item, parent,false);
        }

        TextView txtCity = (TextView) view.findViewById(R.id.txt_city);
        TextView sectionView = (TextView) view.findViewById(R.id.city_category_title);
        View divider = view.findViewById(R.id.city_divider);

        if(!isSearchMode) {
            txtCity.setText(cityBean.getCity());
            String sectionName = cityBean.getSectionName();
            String preSectionName = null;
            if (position > 0) {
                preSectionName = ((CityBean) getItem(position - 1)).getSectionName();
            }
            if (TextUtils.isEmpty(preSectionName) || !preSectionName.equals(sectionName)) {
                sectionView.setVisibility(View.VISIBLE);
                sectionView.setText(sectionName);
                divider.setVisibility(View.GONE);
            } else {
                sectionView.setVisibility(View.GONE);
                divider.setVisibility(View.VISIBLE);
            }
        }else{
            divider.setVisibility(View.VISIBLE);
            sectionView.setVisibility(View.GONE);
            IMUIHelper.setTextHilighted(txtCity, cityBean.getCity(),
                    cityBean.getSearchElement());
        }

        return view;
    }

}
