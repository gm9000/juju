package com.juju.app.activity.party;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.PlanInfoListAdapter;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Party;
import com.juju.app.entity.Plan;
import com.juju.app.entity.User;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.RoundImageView;
import com.juju.app.view.scroll.NoScrollListView;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ContentView(R.layout.activity_party_detail)
public class PartyDetailActivity extends BaseActivity implements HttpCallBack, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "PartyCreateActivity";

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


    @ViewInject(R.id.img_head)
    private RoundImageView img_head;
    @ViewInject(R.id.txt_nick_name)
    private TextView txt_nickName;
    @ViewInject(R.id.txt_party_title)
    private TextView txt_partyTitle;
    @ViewInject(R.id.txt_description)
    private TextView txt_description;


    @ViewInject(R.id.btn_start)
    private Button btn_start;

    @ViewInject(R.id.listview_plan)
    private NoScrollListView listview_plan;

    @ViewInject(R.id.txt_fullDesc)
    private TextView txt_fullDesc;

    private String partyId;
    private PlanInfoListAdapter planListAdapter;
    private List<Plan> planList;
    private boolean isOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initData();
        initView();
        initListeners();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }

    private void initListeners() {
        listview_plan.setOnItemClickListener(this);
        listview_plan.setOnItemLongClickListener(this);
    }

    private void initData() {

        Party party = null;
        try {
            party = JujuDbUtils.getInstance(getContext()).findFirst(Selector.from(Party.class).where("id","=",partyId));
        } catch (DbException e) {
            e.printStackTrace();
        }

        txt_partyTitle.setText(party.getName());
        txt_description.setText("\t\t" + party.getDesc());

        User creator = party.getCreator();
        BitmapUtilFactory.getInstance(this).display(img_head, HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + creator.getUserNo());
        txt_nickName.setText(creator.getNickName());

        isOwner = creator.getUserNo().equals(BaseApplication.getInstance().getUserInfoBean().getJujuNo());

        try {
            planList = JujuDbUtils.getInstance(this).findAll(Selector.from(Plan.class).where("partyId","=",partyId));
        } catch (DbException e) {
            e.printStackTrace();
        }
        if(planList == null){
            planList = new ArrayList<Plan>();
        }
        wrapPlanList(planList);
    }

    private void wrapPlanList(List<Plan> planList) {
        planListAdapter = new PlanInfoListAdapter(this,planList,isOwner);
        listview_plan.setAdapter(planListAdapter);
        listview_plan.setCacheColorHint(0);
    }


    private void initView() {

        if(isOwner){
            btn_start.setVisibility(View.VISIBLE);
        }else{
            btn_start.setVisibility(View.GONE);
        }

        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.group_party);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

    }


    public void initParam() {
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
    }

    @OnClick(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }


    @OnClick(R.id.btn_start)
    private void startParty(View view){
        String planId = null;
        for(Plan plan:planList){
            if(plan.getStatus() == 1){
                planId = plan.getId();
                break;
            }
        }

        if(planId == null){
            ToastUtil.showShortToast(this,"请选择确定的方案",1);
        }else{
            startPartyToServer(planId);
        }


    }

    private void startPartyToServer(String planId) {

        //TODO 增加本地保存

        UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
        Map<String,Object> reqBean = new HashMap<String,Object>();
        reqBean.put("userNo",userTokenInfoBean.getJujuNo());
        reqBean.put("token", userTokenInfoBean.getToken());
        reqBean.put("partyId", partyId);
        reqBean.put("planId", planId);

        JlmHttpClient<Map<String,Object>> client = new JlmHttpClient<Map<String,Object>>(R.id.txt_party, HttpConstants.getUserUrl() + "/confirmParty", this, reqBean,JSONObject.class);
        try {
            loading(true, R.string.starting);
            client.sendPost();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(JujuDbUtils.needRefresh(Plan.class)){
            try {
                planList = JujuDbUtils.getInstance(this).findAll(Selector.from(Plan.class).where("partyId","=",partyId));
            } catch (DbException e) {
                e.printStackTrace();
            }
            planListAdapter.setPlanList(planList);
            planListAdapter.notifyDataSetChanged();
            JujuDbUtils.closeRefresh(Plan.class);
        }

    }

    @Override
    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
        switch (accessId) {
            case R.id.txt_party:
                if(obj != null && obj.length > 0) {
                    JSONObject jsonRoot = (JSONObject)obj[0];
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            completeLoading();
                            partyId = jsonRoot.getString("partyId");
                            Party party = JujuDbUtils.getInstance(getContext()).findFirst(Selector.from(Party.class).where("id", "=", partyId));
                            party.setStatus(1);
                            party.setFollowFlag(1);
                            party.setAttendFlag(1);
                            JujuDbUtils.saveOrUpdate(party);
                            //  TOTO    通知 Party已经启动
                            ActivityUtil.finish(this);
                        } else {
                            completeLoading();
                            Log.e(TAG,"return status code:"+status);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    } catch(DbException e){
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onFailure(HttpException error, String msg, int accessId) {
        completeLoading();
        System.out.println("accessId:" + accessId + "\r\n msg:" + msg + "\r\n code:" +
                error.getExceptionCode());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Plan curPlan = (Plan)listview_plan.getItemAtPosition(position);

        Intent intent = new Intent();
        intent.setClass(this, PlanDetailActivity.class);
        intent.putExtra(Constants.PARTY_ID, curPlan.getPartyId());
        intent.putExtra(Constants.PLAN_ID,curPlan.getId());
        intent.putExtra(Constants.IS_OWNER,this.isOwner);

        this.startActivity(intent);
        this.overridePendingTransition(R.anim.push_left_in,
                R.anim.push_left_out);
    }

    public void operatePlan(int position) {
        Plan plan = planList.get(position);
        switch (plan.getStatus()){
            case 0:
                for(Plan curPlan:planList){
                    if(curPlan.getStatus()==1){
                        curPlan.setStatus(0);
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
        planListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Plan curPlan = (Plan)listview_plan.getItemAtPosition(position);
        if(curPlan.getDesc()!=null && !curPlan.getDesc().equals("")) {
            txt_fullDesc.setText("\t\t" + curPlan.getDesc());
        }
        txt_fullDesc.setVisibility(View.VISIBLE);
        return false;
    }

    @OnClick(R.id.txt_fullDesc)
    public void hiddenFullDesc(View v){
        txt_fullDesc.setVisibility(View.GONE);
        txt_fullDesc.setText(R.string.nodescription);
    }

}
