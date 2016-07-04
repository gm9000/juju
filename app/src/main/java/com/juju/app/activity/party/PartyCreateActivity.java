package com.juju.app.activity.party;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.PlanListAdapter;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.bean.json.PartyBean;
import com.juju.app.bean.json.PartyReqBean;
import com.juju.app.bean.json.PlanBean;
import com.juju.app.biz.impl.GroupDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Party;
import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.MasterTransferEvent;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.manager.IMOtherManager;
import com.juju.app.service.notify.BaseNotify;
import com.juju.app.service.notify.MasterTransferNotify;
import com.juju.app.service.notify.PartyRecruitNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.scroll.NoScrollListView;
import com.juju.app.view.wheel.dialog.SelectDateTimeDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ContentView(R.layout.activity_party_create)
public class PartyCreateActivity extends BaseActivity implements HttpCallBack, AdapterView.OnItemClickListener {

    private static final String TAG = "PartyCreateActivity";

    private static final int CMD_REQ_LOCATION = 1;

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

    @ViewInject(R.id.txt_party_title)
    private EditText txt_partyTitle;
    @ViewInject(R.id.txt_description)
    private EditText txt_description;


    @ViewInject(R.id.layout_plan)
    private RelativeLayout layout_plan;
    @ViewInject(R.id.txt_time)
    private TextView txt_time;
    @ViewInject(R.id.txt_location)
    private EditText txt_location;
    @ViewInject(R.id.txt_plan_description)
    private EditText txt_planDescription;

    @ViewInject(R.id.layout_plan_info)
    private LinearLayout layout_planInfo;
    @ViewInject(R.id.txt_plan_time)
    private TextView txt_planTime;
    @ViewInject(R.id.txt_plan_address)
    private TextView txt_planAddress;
    @ViewInject(R.id.txt_plan_desc)
    private TextView txt_planDesc;


    private double latitude = 0;
    private double longitude = 0;

    @ViewInject(R.id.img_select_location)
    private ImageView img_selectLocation;

    @ViewInject(R.id.layout_plan_list)
    private LinearLayout layout_planList;
//    @ViewInject(R.id.txt_no_plan)
//    private TextView txt_noPlan;

    @ViewInject(R.id.img_add_plan)
    private ImageView img_addPlan;
    @ViewInject(R.id.btn_publish)
    private Button btn_publish;

    @ViewInject(R.id.listview_plan)
    private NoScrollListView listview_plan;

    private String groupId;
    private String partyId;
    private Party party;
    private PlanListAdapter planListAdapter;

    private GroupDaoImpl groupDao;


    private InputMethodManager inputManager = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        initParam();
        initData();
        initView();
        initListeners();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }

    private void initListeners() {
        listview_plan.setOnItemClickListener(this);
        layout_planInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout_planInfo.setVisibility(View.GONE);
            }
        });
    }

    private void initData() {

        groupDao = new GroupDaoImpl(this);
        if(partyId!=null){
            try {
                party = JujuDbUtils.getInstance().selector(Party.class).where("id", "=", partyId).findFirst();
                groupId = party.getGroupId();
                wrapParty(party);

                List<Plan> planList = JujuDbUtils.getInstance().selector(Plan.class).where("party_id", "=", partyId).findAll();
                if(planList!=null){
                    wrapPlanList(planList);
                }else{
                    wrapPlanList(new ArrayList<Plan>());
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
        }else {
            wrapPlanList(new ArrayList<Plan>());
        }
    }

    private void wrapParty(Party party) {
        txt_partyTitle.setText(party.getName());
        txt_description.setText(party.getDesc());
    }

    private void wrapPlanList(List<Plan> planList) {
        planListAdapter = new PlanListAdapter(this,planList);
        listview_plan.setAdapter(planListAdapter);
        listview_plan.setCacheColorHint(0);

//        if(planList.size()>0) {
//            txt_noPlan.setVisibility(View.GONE);
//        }
        if(planListAdapter.getCount()>=3){
            layout_plan.setVisibility(View.GONE);
        }
    }


    private void initView() {
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.cancel);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;

        txt_title.getRootView().setBackgroundColor(getResources().getColor(R.color.background));

        txt_title.setText(R.string.add_party);
        txt_left.setLayoutParams(layoutParams);
        txt_right.setText(R.string.save);
        txt_right.setTextColor(getResources().getColor(R.color.orange));
        txt_right.setVisibility(View.VISIBLE);
        img_right.setVisibility(View.GONE);

    }


    public void initParam() {
        groupId = getIntent().getStringExtra(Constants.GROURP_ID);
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
    }

    @Event(R.id.layout_plan_list)
    private void hiddenSoftKey(View view){
        inputManager.hideSoftInputFromWindow(layout_planList.getWindowToken(), 0);
    }

    @Event(R.id.txt_left)
    private void cancelOperation(View view){
        ActivityUtil.finish(this);
    }


    @Event(R.id.txt_time)
    private void showSelectTimeDialog(View view) {
        SelectDateTimeDialog mSelectDateTimeDialog = new SelectDateTimeDialog(this);
        mSelectDateTimeDialog.setOnClickListener(new SelectDateTimeDialog.OnClickListener() {
            @Override
            public boolean onSure(int mYear, int mMonth, int mDay, int hour, int minute) {
                Date date = new Date();
                date.setYear(mYear - 1900);
                date.setMonth(mMonth);
                date.setDate(mDay);
                date.setHours(hour);
                date.setMinutes(minute);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                txt_time.setText(dateFormat.format(date));
                return false;
            }

            @Override
            public boolean onCancel() {
                return false;
            }
        });
        if(txt_time.getText()!=null && !txt_time.getText().toString().equals("")) {

            mSelectDateTimeDialog.showTimeStr(txt_time.getText().toString());
        }else{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 09:00");
            String dateString = dateFormat.format(new Date());
            mSelectDateTimeDialog.showTimeStr(dateString);
        }
    }


    @Event(R.id.img_select_location)
    private void selectLocation(View view){
        Intent intent=new Intent(this,PlanLocationActivity.class);
        startActivityForResult(intent, CMD_REQ_LOCATION);
    }

    @Event(R.id.txt_right)
    private void save(View view){
        savePartyToServer(false);
    }

    @Event(R.id.btn_publish)
    private void publishParty(View view){
        savePartyToServer(true);
    }

    private void savePartyToServer(boolean isPublish) {

        //  正式发布聚会
        if (isPublish){
            UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
            PartyReqBean reqBean = new PartyReqBean();
            reqBean.setUserNo(userTokenInfoBean.getUserNo());
            reqBean.setToken(userTokenInfoBean.getToken());
            reqBean.setGroupId(groupId);

            PartyBean partyBean = new PartyBean();
            partyBean.setName(txt_partyTitle.getText().toString());
            partyBean.setDesc(txt_description.getText().toString());
            reqBean.setParty(partyBean);

            List<PlanBean> plans = new ArrayList<PlanBean>();
            List<Plan> planList = planListAdapter.getPlanList();
            for (Plan plan : planList) {
                PlanBean planBean = new PlanBean();
                planBean.setAddress(plan.getAddress());
                planBean.setStartTime(plan.getStartTime());
                planBean.setDesc(plan.getDesc());
                planBean.setLatitude(plan.getLatitude());
                planBean.setLongitude(plan.getLongitude());
                plans.add(planBean);
            }
            reqBean.setPlans(plans);

            JlmHttpClient<PartyReqBean> client = new JlmHttpClient<PartyReqBean>(
                    R.id.txt_party, HttpConstants.getUserUrl() + "/addPartyAndPlan", this, reqBean,
                    JSONObject.class);
            try {
                loading(true, R.string.saving);
                client.sendPost();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        //临时保存聚会
        }else{
            loading(true, R.string.saving);
            if(party==null){
                party = new Party();
            }
            party.setName(txt_partyTitle.getText().toString());
            party.setDesc(txt_description.getText().toString());
            party.setStatus(-1);
            party.setUserNo(BaseApplication.getInstance().getUserInfoBean().getUserNo());
            party.setGroupId(groupId);

            if(!StringUtils.isEmpty(party.getGroupId())){
                GroupEntity group = groupDao.findUniByProperty("id", party.getGroupId());
                // TODO 组信息正常保存后需要删除下面的代码
                if(group == null) {
                    group = new GroupEntity();
                    group.setId(groupId);
                    group.setMainName("聚龙小组");
                    group.setGroupType(DBConstant.GROUP_TYPE_NORMAL);
                    group.setCreatorId(party.getUserNo());
                    groupDao.save(group);
                }
                party.setGroupId(party.getGroupId());
            }
            JujuDbUtils.saveOrUpdate(party);

            party.setId(String.valueOf(party.getLocalId()));
            List<Plan> planList = planListAdapter.getPlanList();
            if(planList==null || planList.size() ==0){
                JujuDbUtils.saveOrUpdate(party);
            }
            for(int i=0; i<planList.size(); i++) {
                Plan plan = planList.get(i);

                if(i == 0){
                    party.setTime(plan.getStartTime());
                    JujuDbUtils.saveOrUpdate(party);
                }

                plan.setPartyId(String.valueOf(party.getLocalId()));
                JujuDbUtils.save(plan);
            }
            completeLoading();
            ActivityUtil.finish(this);
        }
    }


    @Event(R.id.img_add_plan)
    private void addPlan(View view){

        if(txt_time.getText().toString().equals("") || txt_location.getText().toString().equals("")) {
            ToastUtil.showShortToast(this, "时间和地址必须设定", 1);
            return;
        }

        Plan plan = new Plan();
        try {
            plan.setStartTime(dateFormat.parse(txt_time.getText().toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        plan.setAddress(txt_location.getText().toString());
        plan.setDesc(txt_planDescription.getText().toString());
        plan.setLatitude(latitude);
        plan.setLongitude(longitude);
        planListAdapter.getPlanList().add(plan);

        planListAdapter.notifyDataSetChanged();

//        txt_noPlan.setVisibility(View.GONE);
        if(planListAdapter.getCount()>=3){
            layout_plan.setVisibility(View.GONE);
        }
        clearPlanTxt();

    }

    private void clearPlanTxt() {
        txt_time.setText("");
        txt_location.setText("");
        txt_planDescription.setText("");
        latitude = 0;
        longitude = 0;
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
//                            completeLoading();
//                            partyId = jsonRoot.getString("partyId");
//
//
//                            if(party == null) {
//                                party = new Party();
//                            }
//                            party.setName(txt_partyTitle.getText().toString());
//                            party.setDesc(txt_description.getText().toString());
//                            party.setId(partyId);
//                            User creator = JujuDbUtils.getInstance(getContext()).findFirst(Selector.from(User.class).where("userNo", "=", BaseApplication.getInstance().getUserInfoBean().getJujuNo()));
//                            party.setCreator(creator);
//
//                            if(party.getGroup()==null){
//                                GroupEntity group = groupDao.findById(groupId);
//                                // TODO 组信息正常保存后需要删除下面的代码
//                                if(group == null) {
//                                    group = new GroupEntity();
//                                    group.setId(groupId);
//                                    group.setMainName("聚龙小组");
//                                    group.setGroupType(DBConstant.GROUP_TYPE_NORMAL);
//                                    group.setCreatorId(creator.getUserNo());
//                                    groupDao.save(group);
//                                }
//                                party.setGroup(group);
//                            }
//                            party.setStatus(0); //  召集中
//
//                            String planIds = jsonRoot.getString("planIds");
//                            if(planIds==null || planIds.equals("")){
//                                JujuDbUtils.saveOrUpdate(party);
//                            }
//                            String[] planIdArray = planIds.split(",");
//                            List<Plan> planList = planListAdapter.getPlanList();
//                            if(planList.size() == planIdArray.length){
//                                for(int i=0; i<planList.size(); i++){
//                                    Plan plan = planList.get(i);
//                                    plan.setPartyId(partyId);
//                                    plan.setId(planIdArray[i]);
//                                    plan.setSigned(1);
//
//                                    if(i==0){
//                                        party.setTime(plan.getStartTime());
//                                        JujuDbUtils.save(party);
//                                    }
//
//                                    PlanVote planVote = new PlanVote();
//                                    planVote.setPlanId(plan.getId());
//                                    planVote.setAttender(creator);
//                                    JujuDbUtils.save(planVote);
//
//                                    plan.setAddtendNum(1);
//                                    JujuDbUtils.save(plan);
//                                }
//                            }else{
//                                Log.e(TAG,"planId return length error:"+planIdArray.length);
//                            }
//
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Plan curPlan = (Plan)listview_plan.getItemAtPosition(position);
        txt_planTime.setText(dateFormat.format(curPlan.getStartTime()));
        txt_planAddress.setText(curPlan.getAddress());
        txt_planDesc.setText((curPlan.getDesc()==null||curPlan.getDesc().equals(""))?getResources().getString(R.string.nodescription):"\t\t"+curPlan.getDesc());
        layout_planInfo.setVisibility(View.VISIBLE);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CMD_REQ_LOCATION:
                    latitude = data.getDoubleExtra(Constants.LATITUDE,0f);
                    longitude = data.getDoubleExtra(Constants.LONGITUDE, 0f);
                    String address = data.getStringExtra(Constants.ADDRESS);
                    txt_location.setText(address);
                    break;

            }
            super.onActivityResult(requestCode, resultCode, data);

        }else{
        }
    }

    public void deletePlan(int deleteIndex) {
        List<Plan> planList = planListAdapter.getPlanList();
        Plan curPlan = planList.get(deleteIndex);
        if(curPlan.getPartyId()!=null){
            JujuDbUtils.delete(curPlan);
        }
        planList.remove(deleteIndex);
        planListAdapter.notifyDataSetChanged();
//        if(planListAdapter.getCount()==0) {
//            txt_noPlan.setVisibility(View.VISIBLE);
//        }
        layout_plan.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSuccess(Object obj, int accessId, Object inputParameter) {
        switch (accessId) {
            case R.id.txt_party:
                if(obj != null) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            completeLoading();
                            partyId = jsonRoot.getString("partyId");

                            if(party == null) {
                                party = new Party();
                            }
                            party.setName(txt_partyTitle.getText().toString());
                            party.setDesc(txt_description.getText().toString());
                            party.setId(partyId);
                            party.setUserNo(BaseApplication.getInstance().getUserInfoBean().getUserNo());

                            party.setStatus(0); //  召集中

                            String planIds = jsonRoot.getString("planIds");
                            if(planIds==null || planIds.equals("")){
                                JujuDbUtils.saveOrUpdate(party);
                            }
                            String[] planIdArray = planIds.split(",");
                            List<Plan> planList = planListAdapter.getPlanList();
                            if(planList.size() == planIdArray.length){
                                for(int i=0; i<planList.size(); i++){
                                    Plan plan = planList.get(i);
                                    plan.setPartyId(partyId);
                                    plan.setId(planIdArray[i]);
                                    plan.setSigned(1);

                                    if(i==0){
                                        party.setTime(plan.getStartTime());
                                        JujuDbUtils.save(party);
                                    }

                                    PlanVote planVote = new PlanVote();
                                    planVote.setPlanId(plan.getId());
                                    planVote.setAttenderNo(party.getUserNo());
                                    JujuDbUtils.save(planVote);

                                    plan.setAddtendNum(1);
                                    JujuDbUtils.save(plan);
                                }

                                PartyNotifyEvent.PartyNotifyBean partyNotifyBean = PartyNotifyEvent
                                        .PartyNotifyBean.valueOf(groupId,partyId,party.getName(),
                                                BaseApplication.getInstance().getUserInfoBean().getJujuNo()
                                                ,BaseApplication.getInstance().getUserInfoBean().getUserName());
                                PartyRecruitNotify.instance().executeCommand4Send(partyNotifyBean);



                            }else{
                                Log.e(TAG,"planId return length error:"+planIdArray.length);
                            }

                            ActivityUtil.finish(this);
                        } else {
                            completeLoading();
                            Log.e(TAG,"return status code:"+status);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    }
                }
                break;
        }

    }

    @Override
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId, Object inputParameter) {
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


}
