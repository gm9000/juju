package com.juju.app.fragment;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.party.PartyActivity;
import com.juju.app.adapters.PartyListAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.entity.Party;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.dialog.titlemenu.ActionItem;
import com.juju.app.view.dialog.titlemenu.TitlePopup;
import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.bitmap.BitmapCommonUtils;
import com.lidroid.xutils.bitmap.BitmapDisplayConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：聚会—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:10
 * 版本：V1.0.0
 */
@CreateFragmentUI(viewId = R.layout.layout_group_list)
public class GroupPartyFragment extends BaseFragment implements CreateUIHelper, RadioGroup.OnCheckedChangeListener,TextWatcher,ListView.OnItemClickListener,PartyListAdapter.Callback {
    private ListView listView;

    private BitmapUtils bitmapUtils;
    private BitmapDisplayConfig bdConfig;
    private PartyListAdapter partyListAdapter;
    private LayoutInflater inflater;
    private EditText searchInput;

    private RadioGroup partyTypeGroup;
    private RadioButton allBtn;
    private RadioButton attendBtn;
    private RadioButton followBtn;
    List<Party> partyList;


    @Override
    public void onStop() {
        super.onStop();
        System.out.println("GroupPartyFragment onStop:" + inflater.toString());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        this.inflater = inflater;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void loadData() {
        partyTypeGroup = (RadioGroup) findViewById(R.id.party_type);
        allBtn = (RadioButton) findViewById(R.id.all_party);
        attendBtn = (RadioButton) findViewById(R.id.attend_party);
        followBtn = (RadioButton) findViewById(R.id.follow_party);
        searchInput = (EditText) findViewById(R.id.txt_search);


        partyTypeGroup.setOnCheckedChangeListener(this);
        searchInput.addTextChangedListener(this);

        listView = (ListView) findViewById(R.id.listPartyView);

        listView.setOnItemClickListener(this);

        bitmapUtils = new BitmapUtils(getContext());
        //  配置缓存大小100K
        bitmapUtils.configDefaultCacheExpiry(128 * 1024);
        bitmapUtils.configDiskCacheEnabled(true);
        bitmapUtils.configDefaultCacheExpiry(2048 * 1024);

        bdConfig = new BitmapDisplayConfig();

        //设置显示图片特性
        bdConfig.setBitmapConfig(Bitmap.Config.ARGB_4444);
        bdConfig.setBitmapMaxSize(BitmapCommonUtils.getScreenSize(getActivity())); //图片的最大尺寸
//        bdConfig.setLoadingDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载时显示的图片
//        bdConfig.setLoadFailedDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载失败时显示的图片
        bdConfig.setShowOriginal(false); //不显示源图片
//        bdConfig.setAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_from_top));
        bitmapUtils.configDefaultDisplayConfig(bdConfig);

        partyList = new ArrayList<Party>();
        Party p1 = new Party();
        p1.setId(1);
        p1.setName("郊区小聚");
        p1.setDescription("好久不见，坐下来俩聊！");
        p1.setStartTime("2015年12月12日 09:00");
        p1.setCreatorId("100000011");
        p1.setCreatorName("聚龙小子");
        p1.setStatus(0);
        p1.setAttendFlag(1);
        p1.setFollowFlag(0);
        partyList.add(p1);

        Party p2 = new Party();
        p2.setId(1);
        p2.setName("郊区小聚2");
        p2.setDescription("好久不见，坐下来俩聊2！");
        p2.setStartTime("2015年12月12日 09:00");
        p2.setCreatorId("100000011");
        p2.setCreatorName("聚龙小子2");
        p2.setStatus(0);
        p2.setAttendFlag(1);
        p2.setFollowFlag(1);
        partyList.add(p2);

        Party p3 = new Party();
        p3.setId(1);
        p3.setName("郊区小聚3");
        p3.setDescription("好久不见，坐下来俩聊3！");
        p3.setStartTime("2015年12月12日 09:00");
        p3.setCreatorId("100000009");
        p3.setCreatorName("聚龙小子3");
        p3.setStatus(1);
        p3.setAttendFlag(0);
        p3.setFollowFlag(0);
        partyList.add(p3);
        wrapPartyList(partyList);

    }

    @Override
    public void initView() {

    }

    private void wrapPartyList(List<Party> partyList) {
        partyListAdapter = new PartyListAdapter(inflater, bitmapUtils, bdConfig, partyList,this);
        listView.setAdapter(partyListAdapter);
        listView.setCacheColorHint(0);
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (partyTypeGroup.getId() == group.getId()) {
            //  处理渲染用户相关的所有聚会
            if (checkedId == allBtn.getId()) {
                partyListAdapter.getFilter().filter("all");
            }
            //  处理渲染用户参加的所有聚会
            if (checkedId == attendBtn.getId()) {
                partyListAdapter.getFilter().filter("attend");
            }
            //  处理渲染用户关注的所有聚会
            if (checkedId == followBtn.getId()) {
                partyListAdapter.getFilter().filter("follow");
            }
        }

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }
    //  搜索框搜索处理
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        partyListAdapter.getFilter().filter(s);

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Party curParty = (Party)listView.getItemAtPosition(position);
        ToastUtil.showShortToast(getActivity(),curParty.getName(),1);
        switch(curParty.getStatus()){

            case 0: // 召集中
                ActivityUtil.startActivity(getActivity(), PartyActivity.class);
                break;
            case 1: //  进行中
                ActivityUtil.startActivity(getActivity(), PartyActivity.class);
                break;
            case 2: //  已结束
                ActivityUtil.startActivity(getActivity(), PartyActivity.class);
                break;
        }
    }

    @Override
    public void click(View v) {
        ToastUtil.showShortToast(getActivity(), ((Integer)v.getTag(R.id.tag_index)).toString() , 1);
        //  TODO    增加是否关注的修改、保存。

    }
}
