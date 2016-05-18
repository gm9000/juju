package com.juju.app.activity.party;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.user.SettingActivity;
import com.juju.app.adapters.PlanVoteListAdapter;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.bean.json.PlanVoteBean;
import com.juju.app.bean.json.PlanVoteReqBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;
import com.juju.app.entity.User;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.scroll.NoScrollGridView;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.List;

@ContentView(R.layout.activity_plan_detail)
public class PlanDetailActivity extends BaseActivity implements HttpCallBack, RadioGroup.OnCheckedChangeListener,AdapterView.OnItemClickListener {

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

    @ViewInject(R.id.plan_index_group)
    private RadioGroup planIndexGroup;
    @ViewInject(R.id.plan_first)
    private RadioButton plan_first;
    @ViewInject(R.id.plan_second)
    private RadioButton plan_second;
    @ViewInject(R.id.plan_third)
    private RadioButton plan_third;

    @ViewInject(R.id.txt_time)
    private TextView txt_time;
    @ViewInject(R.id.txt_description)
    private TextView txt_description;
    @ViewInject(R.id.txt_attend_num)
    private TextView txt_attendNum;

    @ViewInject(R.id.img_weather)
    private ImageView img_weather;
    @ViewInject(R.id.txt_weather)
    private TextView txt_weather;

    @ViewInject(R.id.img_location)
    private ImageView img_location;
    @ViewInject(R.id.img_mapped)
    private ImageView img_mapped;
    @ViewInject(R.id.txt_location)
    private TextView txt_location;


    @ViewInject(R.id.btn_operate)
    private Button btn_operate;

    @ViewInject(R.id.gridview_user)
    private NoScrollGridView gridview_user;

    private String partyId;
    private String planId;
    private List<Plan> planList;
    private int planIndex;

    private PlanVoteListAdapter planVoteListAdapter;
    private List<PlanVote> planVoteList;
    private boolean isOwner;
    private boolean isSigned;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initData();
        initView();
        initListeners();
    }

    private void initListeners() {
        planIndexGroup.setOnCheckedChangeListener(this);
        gridview_user.setOnItemClickListener(this);
    }

    private void initData() {

        try {
            planList = JujuDbUtils.getInstance(this).selector(Plan.class).where("party_id", "=", partyId).findAll();
        } catch (DbException e) {
            e.printStackTrace();
        }

        for(int i=0; i<planList.size(); i++){
            if(planList.get(i).getId().equals(planId)){
                planIndex = i;
            }
        }

        switch(planIndex){
            case 0:
                planIndexGroup.check(R.id.plan_first);
                break;
            case 1:
                planIndexGroup.check(R.id.plan_second);
                break;
            case 2:
                planIndexGroup.check(R.id.plan_third);
                break;
        }

        switch(planList.size()){
            case 1:
                plan_second.setVisibility(View.GONE);
                plan_third.setVisibility(View.GONE);
                break;
            case 2:
                plan_third.setVisibility(View.GONE);
                break;
            case 3:
                break;
        }

        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        PlanVote planVote = null;

        try {
            planVoteList = JujuDbUtils.getInstance(this).selector(PlanVote.class).where("plan_id", "=", planId).findAll();
            if(planVoteList != null) {
                for(PlanVote planVote1 : planVoteList) {
                    User dbUser = JujuDbUtils.getInstance(this).selector(User.class)
                            .where("user_no", "=", planVote1.getAttenderNo()).findFirst();
                    planVote1.setAttender(dbUser);
                }
            }
            planVote = JujuDbUtils.getInstance(this).selector(PlanVote.class).where("plan_id", "=", planId).and("attender_no", "=", userInfoBean.getJujuNo()).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }

        if(planVote == null){
            isSigned = false;
            btn_operate.setBackgroundColor(getResources().getColor(R.color.blue));
            btn_operate.setText(R.string.signup);
        }else{
            isSigned = true;
            btn_operate.setBackgroundColor(getResources().getColor(R.color.red));
            btn_operate.setText(R.string.unsignup);
        }



        planVoteListAdapter = new PlanVoteListAdapter(this,planVoteList);
        gridview_user.setAdapter(planVoteListAdapter);

        txt_attendNum.setText(String.valueOf(planVoteList.size()));

        Plan plan = planList.get(planIndex);
        if(plan.getDesc()==null || plan.getDesc().equals("")){
        }else{
            txt_description.setTextColor(getResources().getColor(R.color.black));
            txt_description.setText("\t\t"+plan.getDesc());
        }

        txt_time.setText(dateFormat.format(plan.getStartTime()));
        txt_location.setText(plan.getAddress());

        // TODO 根据地理位置对接天气
        txt_weather.setText("晴间多云  2-15 ℃");

    }

    @SuppressLint("NewApi")
    private void initView() {

        if(isOwner){
            btn_operate.setVisibility(View.GONE);
        }else{
            btn_operate.setVisibility(View.VISIBLE);
        }

        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_title.setText(R.string.party_plan);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);

        if(planList.get(planIndex).getLatitude() == 0){
            img_mapped.setVisibility(View.GONE);
        }

        img_mapped.setRotation(45);

    }


    public void initParam() {
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
        isOwner = getIntent().getBooleanExtra(Constants.IS_OWNER, false);
        planId = getIntent().getStringExtra(Constants.PLAN_ID);
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }


    @Event(R.id.btn_operate)
    private void changeSignFlag(View view){
        votePlanToServer(planId, !isSigned);
    }

    private void votePlanToServer(String planId,boolean voteFlag) {

        //TODO 增加本地保存

        UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
        PlanVoteReqBean reqBean = new PlanVoteReqBean();
        reqBean.setUserNo(userTokenInfoBean.getJujuNo());
        reqBean.setToken(userTokenInfoBean.getToken());

        PlanVoteBean planVote = new PlanVoteBean();
        planVote.setPlanId(planId);
        planVote.setVote(voteFlag ? 1 : 0);
        reqBean.setPlanVote(planVote);

        JlmHttpClient<PlanVoteReqBean> client = new JlmHttpClient<PlanVoteReqBean>(R.id.txt_party, HttpConstants.getUserUrl() + "/votePlan", this, reqBean,JSONObject.class);
        try {
            loading(true, R.string.saving);
            client.sendPost();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


//    @Override
//    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
//        switch (accessId) {
//            case R.id.txt_party:
//                if(obj != null && obj.length > 0) {
//                    JSONObject jsonRoot = (JSONObject)obj[0];
//                    try {
//                        int status = jsonRoot.getInt("status");
//                        if(status == 0) {
//                            UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
//                            if(isSigned){
//
//                                WhereBuilder whereBuilder = WhereBuilder.b("attenderNo", "=", userTokenInfoBean.getJujuNo());
//                                whereBuilder.and("planId", "=", planId);
//                                JujuDbUtils.getInstance(this).delete(PlanVote.class, whereBuilder);
//                                Plan plan = JujuDbUtils.getInstance(this).findFirst(Selector.from(Plan.class).where("id","=",planId));
//                                plan.setAddtendNum(plan.getAddtendNum()-1);
//                                plan.setSigned(0);
//                                JujuDbUtils.saveOrUpdate(plan);
//
//                            }else{
//                                User user = JujuDbUtils.getInstance(getContext()).findFirst(Selector.from(User.class).where("userNo", "=", userTokenInfoBean.getJujuNo()));
//                                PlanVote planVote = new PlanVote();
//                                planVote.setPlanId(planId);
//                                planVote.setAttender(user);
//                                JujuDbUtils.save(planVote);
//
//                                Plan plan = JujuDbUtils.getInstance(this).findFirst(Selector.from(Plan.class).where("id","=",planId));
//                                plan.setAddtendNum(plan.getAddtendNum()+1);
//                                plan.setSigned(1);
//                                JujuDbUtils.saveOrUpdate(plan);
//                            }
//                            //  TOTO    通知 Plan投票发生变化
//                            completeLoading();
//                            ActivityUtil.finish(this);
//                        } else {
//                            completeLoading();
//                            Log.e(TAG,"return status code:"+status);
//                        }
//                    } catch (JSONException e) {
//                        Log.e(TAG, "回调解析失败", e);
//                        e.printStackTrace();
//                    } catch(DbException e){
//                        e.printStackTrace();
//                    }
//                }
//                break;
//        }
//    }
//
//    @Override
//    public void onFailure(HttpException error, String msg, int accessId) {
//        completeLoading();
//        System.out.println("accessId:" + accessId + "\r\n msg:" + msg + "\r\n code:" +
//                error.getExceptionCode());
//    }

    @Override
    public void onSuccess(Object obj, int accessId) {
        switch (accessId) {
            case R.id.txt_party:
                if(obj != null) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
                            if(isSigned){

                                WhereBuilder whereBuilder = WhereBuilder.b("attender_no", "=", userTokenInfoBean.getJujuNo());
                                whereBuilder.and("planId", "=", planId);
                                JujuDbUtils.getInstance(this).delete(PlanVote.class, whereBuilder);
                                Plan plan = JujuDbUtils.getInstance(this).selector(Plan.class).where("id", "=", planId).findFirst();
                                plan.setAddtendNum(plan.getAddtendNum()-1);
                                plan.setSigned(0);
                                JujuDbUtils.saveOrUpdate(plan);

                            }else{
                                User user = JujuDbUtils.getInstance(getContext()).selector(User.class).where("user_no", "=", userTokenInfoBean.getJujuNo()).findFirst();
                                PlanVote planVote = new PlanVote();
                                planVote.setPlanId(planId);
                                planVote.setAttenderNo(userTokenInfoBean.getJujuNo());
                                planVote.setAttender(user);
                                JujuDbUtils.save(planVote);

                                Plan plan = JujuDbUtils.getInstance(this).selector(Plan.class).where("id", "=", planId).findFirst();
                                plan.setAddtendNum(plan.getAddtendNum()+1);
                                plan.setSigned(1);
                                JujuDbUtils.saveOrUpdate(plan);
                            }
                            //  TOTO    通知 Plan投票发生变化
                            completeLoading();
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
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId) {
        completeLoading();
        System.out.println("accessId:" + accessId + "\r\n isOnCallback:" + isOnCallback );
        Log.e(TAG, "onFailure", ex);
    }

    @Override
    public void onCancelled(Callback.CancelledException cex) {

    }

    @Override
    public void onFinished() {

    }



    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (planIndexGroup.getId() == group.getId()) {

            switch (checkedId){
                case R.id.plan_first:
                    planIndex = 0;
                    break;
                case R.id.plan_second:
                    planIndex = 1;
                    break;
                case R.id.plan_third:
                    planIndex = 2;
                    break;
            }

            gridview_user.setAdapter(planVoteListAdapter);

            Plan plan = planList.get(planIndex);
            planId = plan.getId();
            try {
                planVoteList = JujuDbUtils.getInstance(this).selector(PlanVote.class).where("plan_id", "=", planId).findAll();
                if(planVoteList != null) {
                    for(PlanVote planVote : planVoteList) {
                        User dbUser = JujuDbUtils.getInstance(this).selector(User.class)
                                .where("user_no", "=", planVote.getAttenderNo()).findFirst();
                        planVote.setAttender(dbUser);
                    }
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
            if(plan.getLatitude() == 0 ){
                img_mapped.setVisibility(View.GONE);
            }else{
                img_mapped.setVisibility(View.VISIBLE);
            }

            planVoteListAdapter.setPlanVoteList(planVoteList);
            planVoteListAdapter.notifyDataSetChanged();


            txt_attendNum.setText(String.valueOf(planVoteList.size()));

            if(plan.getDesc()==null){
                txt_description.setTextColor(getResources().getColor(R.color.gray));
                txt_description.setText("\t\t"+getResources().getString(R.string.nodescription));
            }else{
                txt_description.setTextColor(getResources().getColor(R.color.black));
                txt_description.setText("\t\t"+plan.getDesc());
            }

            txt_time.setText(dateFormat.format(plan.getStartTime()));
            txt_location.setText(plan.getAddress());
        }

    }

    @Event(R.id.layout_location)
    private void showMap(View view){
        Plan plan = planList.get(planIndex);
        if(plan.getLatitude()!=0 && plan.getLongitude()!=0) {
            Intent intent = new Intent(this, PlanLocationActivity.class);
            intent.putExtra(Constants.LATITUDE, plan.getLatitude());
            intent.putExtra(Constants.LONGITUDE, plan.getLongitude());
            intent.putExtra(Constants.ADDRESS, plan.getAddress());
            startActivity(intent);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        ActivityUtil.startActivity(this, SettingActivity.class,new BasicNameValuePair(Constants.USER_NO,planVoteList.get(position).getAttender().getUserNo()));

    }


}
