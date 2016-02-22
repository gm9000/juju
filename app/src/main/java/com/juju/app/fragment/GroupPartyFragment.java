package com.juju.app.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.juju.app.R;
import com.juju.app.adapters.PartyListAdapter;
import com.juju.app.entity.Party;
import com.juju.app.ui.base.BaseFragment;
import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.bitmap.BitmapCommonUtils;
import com.lidroid.xutils.bitmap.BitmapDisplayConfig;
import com.lidroid.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：聚会—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:10
 * 版本：V1.0.0
 */
public class GroupPartyFragment extends BaseFragment {

    private Activity ctx;
    private View layout;
    private ListView listView;
    private BitmapUtils bitmapUtils;
    private BitmapDisplayConfig bdConfig;


    private PartyListAdapter partyListAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (layout == null) {
            ctx = this.getActivity();
            layout = ctx.getLayoutInflater().inflate(R.layout.fragment_group_party, null);
        } else {
            ViewGroup parent = (ViewGroup) layout.getParent();
            if (parent != null) {
                parent.removeView(layout);
            }
        }

        listView = (ListView) layout.findViewById(R.id.listPartyView);
        bitmapUtils = new BitmapUtils(ctx,"/sdcard/juju/cached");
        bitmapUtils.configMemoryCacheEnabled(true);
        //  配置缓存大小100K
        bitmapUtils.configDefaultCacheExpiry(128 * 1024);
        bitmapUtils.configDiskCacheEnabled(true);
        bitmapUtils.configDefaultCacheExpiry(2048 * 1024);

        bdConfig=new BitmapDisplayConfig();

        //设置显示图片特性
        bdConfig.setBitmapConfig(Bitmap.Config.ARGB_4444);
        bdConfig.setBitmapMaxSize(BitmapCommonUtils.getScreenSize(ctx)); //图片的最大尺寸
//        bdConfig.setLoadingDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载时显示的图片
//        bdConfig.setLoadFailedDrawable(GroupActivity.this.getResources().getDrawable(R.mipmap.ic_launcher)); //加载失败时显示的图片
        bdConfig.setShowOriginal(false); //不显示源图片
//        bdConfig.setAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_from_top));
        bitmapUtils.configDefaultDisplayConfig(bdConfig);

        List<Party> partyList = new ArrayList<Party>();
        Party p1 = new Party();
        p1.setId(1);
        p1.setName("郊区小聚");
        p1.setDescription("好久不见，坐下来俩聊！");
        p1.setStartTime("2015年12月12日 09:00");
        p1.setCreatorId("100000011");
        p1.setCreatorName("聚龙小子");
        p1.setStatus(0);
        partyList.add(p1);

        Party p2 = new Party();
        p2.setId(1);
        p2.setName("郊区小聚2");
        p2.setDescription("好久不见，坐下来俩聊2！");
        p2.setStartTime("2015年12月12日 09:00");
        p2.setCreatorId("100000011");
        p2.setCreatorName("聚龙小子2");
        p2.setStatus(0);
        partyList.add(p2);

        Party p3 = new Party();
        p3.setId(1);
        p3.setName("郊区小聚3");
        p3.setDescription("好久不见，坐下来俩聊3！");
        p3.setStartTime("2015年12月12日 09:00");
        p3.setCreatorId("100000009");
        p3.setCreatorName("聚龙小子3");
        p3.setStatus(0);
        partyList.add(p3);
        wrapPartyList(partyList);
        return layout;
    }

    private void wrapPartyList(List<Party> partyList) {
        partyListAdapter = new PartyListAdapter(ctx,bitmapUtils,bdConfig,partyList);
        listView.setAdapter(partyListAdapter);
        listView.setCacheColorHint(0);
    }
}
