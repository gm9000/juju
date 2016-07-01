package com.juju.app.fragment;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.juju.app.R;
import com.juju.app.adapter.SingleCheckAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.PaintUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.pinyin.PinYinUtil;
import com.juju.app.view.SearchEditText;
import com.juju.app.view.SortSideBar;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/6/30 17:50
 * 版本：V1.0.0
 */
@ContentView(R.layout.fragment_single_check)
@CreateFragmentUI(viewId = R.layout.fragment_single_check)
public class SingleCheckFragment extends BaseFragment
        implements CreateUIHelper, SortSideBar.OnTouchingLetterChangedListener,
        SingleCheckAdapter.ListenerInfo.OnClickListener {

    @ViewInject(R.id.all_contact_list)
    private ListView listView;

    @ViewInject(R.id.filter_edit)
    private SearchEditText searchEditText;

    @ViewInject(R.id.sidrbar)
    private SortSideBar sortSideBar;

    @ViewInject(R.id.dialog)
    private TextView dialog;


    private SingleCheckAdapter singleCheckAdapter;

    private String data;


    @Override
    public void loadData() {
        data = getActivity().getIntent().getStringExtra(Constants.SINGLE_CHECK_LIST_DATA);
    }

    @Override
    public void initView() {
        singleCheckAdapter = new SingleCheckAdapter(getContext(), SingleCheckFragment.this);
        if(StringUtils.isNotBlank(data)) {
            List<SingleCheckAdapter.ItemBean> list = JacksonUtil
                    .turnString2Obj(data, new TypeReference<List<SingleCheckAdapter.ItemBean>>() {});
            for(SingleCheckAdapter.ItemBean itemBean : list) {
                PinYinUtil.getPinYin(itemBean.getMainName(), itemBean.getPinyinElement());
            }
            singleCheckAdapter.setAllItemList(list);
        }
        listView.setAdapter(singleCheckAdapter);
        listView.setOnItemClickListener(singleCheckAdapter);
    }

    @Override
    protected void setOnListener() {
        super.setOnListener();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //如果存在软键盘，关闭掉
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                //txtName is a reference of an EditText Field
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        sortSideBar.setOnTouchingLetterChangedListener(this);
        dialog = (TextView) findViewById(R.id.dialog);
        sortSideBar.setTextView(dialog);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String key = s.toString();
                if(TextUtils.isEmpty(key)){
                    singleCheckAdapter.recover();
                    sortSideBar.setVisibility(View.VISIBLE);
                }else{
                    sortSideBar.setVisibility(View.INVISIBLE);
                    singleCheckAdapter.onSearch(key);
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
    }

    @Override
    public void onTouchingLetterChanged(String s) {
        int position = singleCheckAdapter.getPositionForSection(s.charAt(0));
        if (position != -1) {
            listView.setSelection(position);
        }
    }


    private SingleCheckAdapter.ListenerInfo.OnClickListener mOnClickListener;

    public void setOnClickListener(SingleCheckAdapter.ListenerInfo.OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public void removeOnClickListener(SingleCheckAdapter.ListenerInfo.OnClickListener onClickListener) {
        mOnClickListener = null;
    }

    @Override
    public void onItemClick(SingleCheckAdapter.ItemBean itemBean) {
        if(mOnClickListener != null) {
            mOnClickListener.onItemClick(itemBean);
        }
    }
}
