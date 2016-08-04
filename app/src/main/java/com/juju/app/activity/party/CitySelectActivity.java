package com.juju.app.activity.party;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.CitySelectAdapter;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.CityBean;
import com.juju.app.entity.User;
import com.juju.app.golobal.CityDataSource;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.pinyin.PinYinUtil;
import com.juju.app.view.SearchEditText;
import com.juju.app.view.SortSideBar;

import org.xutils.view.annotation.ContentView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@ContentView(R.layout.activity_city_select)
@CreateUI(showTopView = false)
public class CitySelectActivity extends BaseActivity implements CreateUIHelper,
        SortSideBar.OnTouchingLetterChangedListener {

    protected static Logger logger = Logger.getLogger(CitySelectActivity.class);


    private SortSideBar sortSideBar;
    private SearchEditText searchEditText;
    private TextView dialog;

    private CitySelectAdapter adapter;
    private List<CityBean> cityList;

    private TextView txtCurrentCity;
    private ListView cityListView;
    private String currentCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        currentCity = getIntent().getStringExtra(Constants.CITY);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void loadData() {
        cityList = new ArrayList<CityBean>();
        for(String cityName: CityDataSource.cityList) {
            CityBean cityBean = new CityBean();
            cityBean.setCity(cityName);
            cityList.add(cityBean);
        }
    }

    @Override
    public void initView() {
        sortSideBar = (SortSideBar) findViewById(R.id.sidrbar);
        sortSideBar.setOnTouchingLetterChangedListener(this);

        dialog = (TextView) findViewById(R.id.dialog);
        sortSideBar.setTextView(dialog);

        cityListView = (ListView) findViewById(R.id.all_city_list);
        cityListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //如果存在软键盘，关闭掉
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                //txtName is a reference of an EditText Field
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        searchEditText = (SearchEditText) findViewById(R.id.filter_edit);
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String key = s.toString();
                if(TextUtils.isEmpty(key)){
                    adapter.recover();
                    sortSideBar.setVisibility(View.VISIBLE);
                }else{
                    sortSideBar.setVisibility(View.INVISIBLE);
                    adapter.onSearch(key);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        txtCurrentCity = (TextView)findViewById(R.id.txt_current_city);

        if(currentCity != null){
            txtCurrentCity.setText(currentCity);
        }

        txtCurrentCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUtil.finish(CitySelectActivity.this);
            }
        });

        wrapCityList();
    }

    private void wrapCityList() {

        Collections.sort(cityList, new Comparator<CityBean>() {
            @Override
            public int compare(CityBean entity1, CityBean entity2) {
                if (entity1.getPinyinElement().pinyin == null) {
                    PinYinUtil.getPinYin(entity1.getCity(), entity1.getPinyinElement());
                }
                if (entity2.getPinyinElement().pinyin == null) {
                    PinYinUtil.getPinYin(entity2.getCity(), entity2.getPinyinElement());
                }
                return entity1.getPinyinElement().pinyin.compareToIgnoreCase(entity2.getPinyinElement().pinyin);
            }
        });

        adapter = new CitySelectAdapter(CitySelectActivity.this);
        adapter.setAllCityList(cityList);
        cityListView.setAdapter(adapter);
        cityListView.setOnItemClickListener(adapter);
    }

    @Override
    public void onTouchingLetterChanged(String s) {
        int position = adapter.getPositionForSection(s.charAt(0));
        if (position != -1) {
            cityListView.setSelection(position);
        }
    }

    public void selectCity(CityBean cityBean){
        Intent intent = getIntent();
        intent.putExtra(Constants.SELECTED_CITY,cityBean.getCity());
        this.setResult(RESULT_OK,intent);
        ActivityUtil.finish(this);
    }

}
