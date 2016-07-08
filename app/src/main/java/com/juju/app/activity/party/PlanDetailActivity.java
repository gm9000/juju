package com.juju.app.activity.party;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.entity.Plan;
import com.juju.app.fragment.party.PlanDetailFragment;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;

import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ContentView(R.layout.activity_plan_detail)
public class PlanDetailActivity extends BaseActivity implements ViewPager.OnPageChangeListener {

    private static final String TAG = "PlanDetailActivity";

    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.txt_right)
    private TextView txt_right;
    @ViewInject(R.id.img_right)
    private ImageView img_right;

    @ViewInject(R.id.container)
    private ViewPager mPager;
    @ViewInject(R.id.dotGroup)
    private LinearLayout dotGroup;
    private ImageView[] tips;


    private String partyId;
    private String planId;
    private List<Plan> planList;
    private int planIndex;

    private PagePlanDetailAdapter planDetailAdapter;

    private boolean isOwner;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initData();
        initView();
        initListeners();
    }

    private void initListeners(){
    }

    private void initData() {

        try {
            planList = JujuDbUtils.getInstance().selector(Plan.class).where("party_id", "=", partyId).findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }

        for(int i=0; i<planList.size(); i++){
            if(planList.get(i).getId().equals(planId)){
                planIndex = i;
            }
        }

        planDetailAdapter = new PagePlanDetailAdapter(getSupportFragmentManager(),this,planList,isOwner);
        mPager.setAdapter(planDetailAdapter);
        mPager.setCurrentItem(planIndex);
        mPager.addOnPageChangeListener(this);

    }

    @SuppressLint("NewApi")
    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.party_plan);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

        if(planList.size()>1) {
            tips = new ImageView[planList.size()];
            for (int i = 0; i < tips.length; i++) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams dotLayoutParams = new LinearLayout.LayoutParams(ScreenUtil.dip2px(this,8),ScreenUtil.dip2px(this,8));
                dotLayoutParams.leftMargin = ScreenUtil.dip2px(this,8);
                dotLayoutParams.rightMargin = ScreenUtil.dip2px(this,8);
                imageView.setLayoutParams(dotLayoutParams);
                dotGroup.addView(imageView);

                tips[i] = imageView;
                if (i == planIndex) {
                    tips[i].setBackgroundResource(R.mipmap.dot_red);
                } else {
                    tips[i].setBackgroundResource(R.mipmap.dot_white);
                }
            }
        }

    }

    public void initParam() {
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
        isOwner = getIntent().getBooleanExtra(Constants.IS_OWNER, false);
        planId = getIntent().getStringExtra(Constants.PLAN_ID);
    }

    public void checkPlan(Plan plan){
        switch (plan.getStatus()){
            case 0:
                for(int i=0; i<planList.size();i++){
                    Plan curPlan = planList.get(i);
                    if(curPlan.getStatus()==1){
                        curPlan.setStatus(0);
                        if(planDetailAdapter.getFragment(i)!=null) {
                            planDetailAdapter.getFragment(i).setNeedRrefresh(true);
                        }
                        JujuDbUtils.saveOrUpdate(curPlan);
                    }
                }
                plan.setStatus(1);
                break;
            case 1:
                plan.setStatus(0);
                break;
        }
        JujuDbUtils.saveOrUpdate(plan);
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if(tips==null){
            return;
        }
        if(planDetailAdapter.getFragment(position)!=null && planDetailAdapter.getFragment(position).needRrefresh()){
            planDetailAdapter.getFragment(position).refresh();
        }
        for(int i=0; i<tips.length; i++){
            if(i==position){
                tips[i].setImageResource(R.mipmap.dot_red);
            }else{
                tips[i].setImageResource(R.mipmap.dot_white);
            }
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    private static final class PagePlanDetailAdapter extends FragmentStatePagerAdapter {

        private PlanDetailActivity activity;
        private List<Plan> planList;
        private boolean isOwner;
        private Map<Integer,PlanDetailFragment> fragmentMap = new HashMap<Integer,PlanDetailFragment>();

        public PagePlanDetailAdapter(FragmentManager fragmentManager, PlanDetailActivity activity, List<Plan> planList, boolean isOwner) {
            super(fragmentManager);
            this.activity = activity;
            this.planList = planList;
            this.isOwner = isOwner;
        }


        @Override
        public Fragment getItem(int position) {
            final PlanDetailFragment fragment = new PlanDetailFragment(activity,planList.get(position),isOwner);
            fragmentMap.put(position,fragment);
            return fragment;
        }

        public PlanDetailFragment getFragment(int position){
            return fragmentMap.get(position);
        }

        @Override
        public int getCount() {
            return planList.size();
        }

    }

}
