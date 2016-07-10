package com.juju.app.activity.party;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.adapters.GroupListAdapter;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.golobal.AppContext;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.app.view.SearchEditText;

import org.greenrobot.eventbus.EventBus;
import org.xutils.view.annotation.ContentView;

import java.util.List;


@ContentView(R.layout.activity_group_member_select)
@CreateUI(showTopView = true)
public class GroupSelectActivity extends BaseActivity implements CreateUIHelper{

    protected static final Logger logger = Logger.getLogger(GroupSelectActivity.class);


    private SearchEditText searchEditText;

    private GroupListAdapter adapter;

    private ListView groupListView;

    private UserInfoBean userInfoBean;
    private DaoSupport groupDao;


    @Override
    public void loadData() {
        groupDao = new GroupDaoImpl(getApplicationContext());
        userInfoBean = AppContext.getUserInfoBean();
    }

    @Override
    public void initView() {
        findViewById(R.id.dialog).setVisibility(View.GONE);
        findViewById(R.id.sidrbar).setVisibility(View.GONE);
        initRes();
        initGroupList();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    private void initGroupList() {
        adapter = new GroupListAdapter(GroupSelectActivity.this);
        List<GroupEntity> groupList = groupDao.findAll();
        adapter.setGroupList(groupList);
        groupListView.setAdapter(adapter);
        groupListView.setOnItemClickListener(adapter);
    }

    /**
     * @Description 初始化资源
     */
    private void initRes() {
        // 设置标题栏
        setTopTitle(getString(R.string.choose_group));
        setTopRightText(R.string.confirm);
        showTopLeftAll(R.string.cancel, 0);

        topRightTitleTxt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if(adapter.getSeletedGroupId() == null){
                    Toast.makeText(GroupSelectActivity.this,
                            getString(R.string.select_group_empty), Toast.LENGTH_SHORT).show();
                    return;
                }

                String groupId =  adapter.getSeletedGroupId();
                Intent intent = getIntent();
                intent.putExtra("selectedGroupId",groupId);
                GroupSelectActivity.this.setResult(RESULT_OK,intent);
                ActivityUtil.finish(GroupSelectActivity.this);

            }
        });



        groupListView = (ListView) findViewById(R.id.all_contact_list);
        groupListView.setOnScrollListener(new AbsListView.OnScrollListener() {
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
                }else{
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
    }

}
