package com.juju.app.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.juju.app.R;
import com.juju.app.adapter.SearchAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.ui.base.TitleBaseFragment;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.libs.tools.ScreenTools;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.List;

/**
 *
 */
@ContentView(R.layout.tt_fragment_search)
@CreateFragmentUI(viewId = R.layout.tt_fragment_search)
public class SearchFragment extends TitleBaseFragment implements CreateUIHelper {

	private Logger logger = Logger.getLogger(SearchFragment.class);
	private View curView = null;

    @ViewInject(R.id.search)
	private ListView listView;

    @ViewInject(R.id.layout_no_search_result)
    private View noSearchResultView;

	private SearchAdapter adapter;
	IMService imService;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imServiceConnector.connect(this.getActivity());
    }

    private IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onIMServiceConnected() {
            logger.d("config#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            //init set adapter service
            initAdapter();
        }
        @Override
        public void onServiceDisconnected() {
        }
    };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        initTopBar();
        return super.onCreateView(inflater, container, savedInstanceState);
	}



	@Override
	public void onResume() {
		super.onResume();
	}

	private void initTopBar() {
        //显示左边区域
        showLeftAll(0, 0);
        //显示搜索框
        showSearchAll();
        //隐藏右边区域
        hideRightAll();


        //设置边距
//        topLeftBtn.setPadding(0, 0, ScreenTools.instance(getActivity()).dip2px(30), 0);
		topLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ActivityUtil.finish4UP(getActivity());
            }
        });

        topLetTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ActivityUtil.finish4UP(getActivity());
            }
        });

        //搜索监听事件
		topSearchEdt.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String key = s.toString();
				adapter.setSearchKey(key);
                if(key.isEmpty())
                {
                    adapter.clear();
                    noSearchResultView.setVisibility(View.GONE);
                }else{
                    searchEntityLists(key);
                }
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

    private void initAdapter(){
        adapter = new SearchAdapter(getActivity(),imService);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(adapter);
        listView.setOnItemLongClickListener(adapter);
    }

    // 文字高亮search 模块
	private void searchEntityLists(String key) {
        //用户暂时不需要搜索
//        List<User> contactList = imService.getContactManager().getSearchContactList(key);
//        int contactSize = contactList.size();
//        adapter.putUserList(contactList);

        List<GroupEntity> groupList = imService.getGroupManager().getSearchAllGroupList(key);
        int groupSize = groupList.size();
        adapter.putGroupList(groupList);


        int sum = groupSize;
        adapter.notifyDataSetChanged();
        if(sum <= 0){
            noSearchResultView.setVisibility(View.VISIBLE);
        }else{
            noSearchResultView.setVisibility(View.GONE);
        }
	}

  	@Override
	protected void initHandler() {
	}

    @Override
    public void onDestroy() {
        imServiceConnector.disconnect(getActivity());
        super.onDestroy();
    }

    @Override
    public void loadData() {

    }

    @Override
    public void initView() {

    }


}
