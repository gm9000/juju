package com.juju.app.activity.party;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
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
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.service.notify.PartyRecruitNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.dialog.WarnTipDialog;
import com.juju.app.view.groupchat.IMGroupAvatar;
import com.rey.material.app.BottomSheetDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ContentView(R.layout.activity_party_create)
public class PartyCreateActivity extends BaseActivity implements HttpCallBack{

    private static final String TAG = "PartyCreateActivity";

    private static final int ADD_PLAN = 0x0001;
    private static final int EDIT_PLAN = 0x0002;

    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.img_right)
    private ImageView img_right;
    @ViewInject(R.id.txt_right)
    private TextView txt_right;

    @ViewInject(R.id.iv_head)
    private IMGroupAvatar groupHead;
    @ViewInject(R.id.txt_group_name)
    private TextView txtGroupName;
    @ViewInject(R.id.txt_party_title)
    private EditText txt_partyTitle;
    @ViewInject(R.id.txt_description)
    private EditText txt_description;


    @ViewInject(R.id.layout_plan1)
    private RelativeLayout layoutPlan1;
    @ViewInject(R.id.img_plan1)
    private ImageView imgPlan1;
    @ViewInject(R.id.txt_plan_type1)
    private TextView txtPlanType1;
    @ViewInject(R.id.txt_plan_desc1)
    private TextView txtPlanDesc1;
    @ViewInject(R.id.txt_plan_location1)
    private TextView txtPlanLocation1;
    @ViewInject(R.id.txt_plan_time1)
    private TextView txtPlanTime1;
    @ViewInject(R.id.img_plan_delete1)
    private ImageView imgPlanDelete1;



    @ViewInject(R.id.layout_plan2)
    private RelativeLayout layoutPlan2;
    @ViewInject(R.id.img_plan2)
    private ImageView imgPlan2;
    @ViewInject(R.id.txt_plan_type2)
    private TextView txtPlanType2;
    @ViewInject(R.id.txt_plan_desc2)
    private TextView txtPlanDesc2;
    @ViewInject(R.id.txt_plan_location2)
    private TextView txtPlanLocation2;
    @ViewInject(R.id.txt_plan_time2)
    private TextView txtPlanTime2;
    @ViewInject(R.id.img_plan_delete2)
    private ImageView imgPlanDelete2;


    @ViewInject(R.id.layout_plan3)
    private RelativeLayout layoutPlan3;
    @ViewInject(R.id.img_plan3)
    private ImageView imgPlan3;
    @ViewInject(R.id.txt_plan_type3)
    private TextView txtPlanType3;
    @ViewInject(R.id.txt_plan_desc3)
    private TextView txtPlanDesc3;
    @ViewInject(R.id.txt_plan_location3)
    private TextView txtPlanLocation3;
    @ViewInject(R.id.txt_plan_time3)
    private TextView txtPlanTime3;
    @ViewInject(R.id.img_plan_delete3)
    private ImageView imgPlanDelete3;


    @ViewInject(R.id.layout_plan_add)
    private RelativeLayout layoutPlanAdd;

    private String groupId;
    private String partyId;
    private Party party;
    private List<Plan> planList;

    private GroupDaoImpl groupDao;

    private InputMethodManager inputManager = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private BottomSheetDialog msgDialog;


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
    }

    private void initData() {

        groupDao = new GroupDaoImpl(this);
        if(partyId!=null){
            try {
                party = JujuDbUtils.getInstance().selector(Party.class).where("id", "=", partyId).findFirst();
                groupId = party.getGroupId();
                wrapParty(party);

                planList = JujuDbUtils.getInstance().selector(Plan.class).where("party_id", "=", partyId).findAll();
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
        this.planList = planList;
        for(int i=0; i<planList.size(); i++){
            refreshPlanInfo(i);
        }
    }


    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        txt_title.getRootView().setBackgroundColor(getResources().getColor(R.color.background));
        txt_title.setText(R.string.add_party);
        img_right.setVisibility(View.GONE);
        txt_right.setText(R.string.publish);
        txt_right.setVisibility(View.VISIBLE);


        GroupEntity group = groupDao.findUniByProperty("id",groupId);
        if(group != null) {
            txtGroupName.setText(group.getMainName());
            Set<String> userNos = group.getlistGroupMemberIds();
            List<String> avatarUrlList = new ArrayList<String>();
            for (String userNo : userNos) {
                User entity = IMContactManager.instance().findContact(userNo);
                if (entity != null) {
                    avatarUrlList.add(entity.getAvatar());

                }
                if (avatarUrlList.size() >= 9) {
                    break;
                }
            }
            groupHead.setAvatarUrls((ArrayList<String>) avatarUrlList);
        }else{
            WarnTipDialog tipdialog = new WarnTipDialog(context,"已退出相应群组，及时删除记录！");
            tipdialog.setBtnOkLinstener( new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityUtil.finish(PartyCreateActivity.this);
                }
            });
            tipdialog.hiddenBtnCancel();
            tipdialog.show();
        }

        imgPlan1.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        imgPlan2.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        imgPlan3.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

    }


    public void initParam() {
        groupId = getIntent().getStringExtra(Constants.GROUP_ID);
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
    }

    @Event(R.id.txt_left)
    private void goCancel(View view){
        cancelOperation();
    }
    @Event(value=R.id.img_back)
    private void goBack(View view){
        cancelOperation();
    }
    private void cancelOperation(){
        WarnTipDialog tipdialog = new WarnTipDialog(context,"是否保存草稿？");
        tipdialog.setOkLable(getResources().getString(R.string.save));
        tipdialog.setBtnOkLinstener( new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(StringUtils.empty(txt_partyTitle.getText().toString())){
                    ToastUtil.showShortToast(PartyCreateActivity.this, "请设置聚会主题", 1);
                    return;
                }else {
                    savePartyToServer(false);
                }
            }
        });
        tipdialog.setBtnCancelLinstener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityUtil.finish(PartyCreateActivity.this);
            }
        });
        tipdialog.show();
    }



    @Event(value=R.id.txt_right)
    private void publishParty(View view){

        if(StringUtils.empty(txt_partyTitle.getText().toString())){
            ToastUtil.showShortToast(this, "请设置聚会主题", 1);
            return;
        }

        if(planList.size()==0){
            ToastUtil.showShortToast(this, "请添加聚会方案", 1);
            return;
        }

        WarnTipDialog tipdialog = new WarnTipDialog(context,"发布后不能修改\n确认发布该聚会？");
        tipdialog.setOkLable(getResources().getString(R.string.save));
        tipdialog.setBtnOkLinstener( new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                savePartyToServer(true);
            }
        });
        tipdialog.show();

    }

    private void savePartyToServer(boolean isPublish) {

        if(StringUtils.empty(txt_partyTitle.getText().toString())){
            ToastUtil.showShortToast(this, "请设置聚会主题", 1);
            return;
        }

        //  正式发布聚会
        if (isPublish){

            UserInfoBean userTokenInfoBean = AppContext.getUserInfoBean();
            PartyReqBean reqBean = new PartyReqBean();
            reqBean.setUserNo(userTokenInfoBean.getUserNo());
            reqBean.setToken(userTokenInfoBean.getToken());
            reqBean.setGroupId(groupId);

            PartyBean partyBean = new PartyBean();
            partyBean.setName(txt_partyTitle.getText().toString());
            partyBean.setDesc(txt_description.getText().toString());
            reqBean.setParty(partyBean);

            List<PlanBean> plans = new ArrayList<PlanBean>();
            for (Plan plan : planList) {
                PlanBean planBean = new PlanBean();
                planBean.setAddress(plan.getAddress());
                planBean.setStartTime(plan.getStartTime());
                planBean.setDesc(plan.getDesc());
                planBean.setLatitude(plan.getLatitude());
                planBean.setLongitude(plan.getLongitude());
                planBean.setType(plan.getType());
                planBean.setCoverUrl(plan.getCoverUrl());
                plans.add(planBean);
            }
            reqBean.setPlans(plans);

            JlmHttpClient<PartyReqBean> client = new JlmHttpClient<PartyReqBean>(
                    R.id.party_name, HttpConstants.getUserUrl() + "/addPartyAndPlan", this, reqBean,
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
            party.setUserNo(AppContext.getUserInfoBean().getUserNo());
            party.setGroupId(groupId);
            party.setNew(false);

            JujuDbUtils.saveOrUpdate(party);

            party.setId(String.valueOf(party.getLocalId()));
            if(planList==null || planList.size() ==0){
                JujuDbUtils.saveOrUpdate(party);
            }
            for(int i=0; i<planList.size(); i++) {
                Plan plan = planList.get(i);

                if(i == 0){
                    party.setTime(plan.getStartTime());
                    party.setCoverUrl(plan.getCoverUrl());
                    JujuDbUtils.saveOrUpdate(party);
                }

                plan.setPartyId(String.valueOf(party.getLocalId()));
                JujuDbUtils.save(plan);
            }
            completeLoading();
            ActivityUtil.finish(this);
        }
    }


    @Event(value=R.id.layout_plan_add)
    private void addPlan(View view){
        startActivityForResultNew(this, PlanCreateActivity.class, ADD_PLAN);
    }

    @Event(value = {R.id.layout_plan1,R.id.layout_plan2,R.id.layout_plan3})
    private void editPlan(View view){
        int index = -1;
        switch(view.getId()){
            case R.id.layout_plan1:
                index = 0;
                imgPlanDelete1.setVisibility(View.GONE);
                break;
            case R.id.layout_plan2:
                index = 1;
                imgPlanDelete2.setVisibility(View.GONE);
                break;
            case R.id.layout_plan3:
                index = 2;
                imgPlanDelete3.setVisibility(View.GONE);
                break;
        }
        if(index > -1) {
            Plan plan = planList.get(index);
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("index", String.valueOf(index));
            valueMap.put("planStr", JacksonUtil.turnObj2String(plan));
            startActivityForResultNew(this, PlanCreateActivity.class,EDIT_PLAN, valueMap);
        }
    }

    @Event(value = {R.id.layout_plan1,R.id.layout_plan2,R.id.layout_plan3},type=View.OnLongClickListener.class)
    private boolean longClickPlan(View view){
        switch(view.getId()){
            case R.id.layout_plan1:
                imgPlanDelete1.setVisibility(View.VISIBLE);
                imgPlanDelete2.setVisibility(View.GONE);
                imgPlanDelete3.setVisibility(View.GONE);
                break;
            case R.id.layout_plan2:
                imgPlanDelete1.setVisibility(View.GONE);
                imgPlanDelete2.setVisibility(View.VISIBLE);
                imgPlanDelete3.setVisibility(View.GONE);
                break;
            case R.id.layout_plan3:
                imgPlanDelete1.setVisibility(View.GONE);
                imgPlanDelete2.setVisibility(View.GONE);
                imgPlanDelete3.setVisibility(View.VISIBLE);
                break;
        }
        return true;
    }

    @Event(value = {R.id.img_plan_delete1,R.id.img_plan_delete2,R.id.img_plan_delete3})
    private void deletePlan(View view){
        int deleteIndex = -1;
        switch(view.getId()){
            case R.id.img_plan_delete1:
                deleteIndex = 0;
                break;
            case R.id.img_plan_delete2:
                deleteIndex = 1;
                break;
            case R.id.img_plan_delete3:
                deleteIndex = 2;
                break;
        }
        if(deleteIndex > -1){
            final int index = deleteIndex;
            WarnTipDialog tipdialog = new WarnTipDialog(this,"确定要删除该方案吗？");
            tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Plan curPlan = planList.get(index);
                    if(curPlan.getPartyId()!=null){
                        JujuDbUtils.delete(curPlan);
                    }
                    planList.remove(index);
                    removePlanLayout(index);
                }
            });
            tipdialog.show();
        }
        view.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ADD_PLAN:
                    String planJson = data.getStringExtra("planJson");
                    Plan plan = JacksonUtil.turnString2Obj(planJson,Plan.class);
                    planList.add(plan);
                    refreshPlanInfo(planList.size()-1);
                    break;

                case EDIT_PLAN:
                    int index = data.getIntExtra("index",-1);
                    if(index > -1) {
                        planList.remove(index);
                        planList.add(index,JacksonUtil.turnString2Obj(data.getStringExtra("planJson"), Plan.class));
                        refreshPlanInfo(index);
                    }
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);

        }else{
        }
    }

    private void refreshPlanInfo(int index) {
        Plan plan = planList.get(index);
        switch(index){
            case 0:
                layoutPlan1.setVisibility(View.VISIBLE);
                txtPlanType1.setText(getResValue(plan.getType()));
                if(plan.getCoverUrl()!=null && plan.getCoverUrl().startsWith("http:")){
                    ImageLoaderUtil.getImageLoaderInstance().displayImage(plan.getCoverUrl(), imgPlan1, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
                }else {
                    imgPlan1.setImageResource(getResValue(plan.getType().toLowerCase(), "drawable"));
                }
                txtPlanDesc1.setText(plan.getDesc());
                txtPlanLocation1.setText(plan.getAddress());
                txtPlanTime1.setText(dateFormat.format(plan.getStartTime()));
                break;
            case 1:
                layoutPlan2.setVisibility(View.VISIBLE);
                txtPlanType2.setText(getResValue(plan.getType()));
                if(plan.getCoverUrl()!=null && plan.getCoverUrl().startsWith("http:")){
                    ImageLoaderUtil.getImageLoaderInstance().displayImage(plan.getCoverUrl(), imgPlan2, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
                }else {
                    imgPlan2.setImageResource(getResValue(plan.getType().toLowerCase(), "drawable"));
                }
                txtPlanDesc2.setText(plan.getDesc());
                txtPlanLocation2.setText(plan.getAddress());
                txtPlanTime2.setText(dateFormat.format(plan.getStartTime()));
                break;
            case 2:
                layoutPlan3.setVisibility(View.VISIBLE);
                layoutPlanAdd.setVisibility(View.GONE);
                txtPlanType3.setText(getResValue(plan.getType()));
                if(plan.getCoverUrl()!=null && plan.getCoverUrl().startsWith("http:")){
                    ImageLoaderUtil.getImageLoaderInstance().displayImage(plan.getCoverUrl(), imgPlan3, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
                }else {
                    imgPlan3.setImageResource(getResValue(plan.getType().toLowerCase(), "drawable"));
                }
                txtPlanDesc3.setText(plan.getDesc());
                txtPlanLocation3.setText(plan.getAddress());
                txtPlanTime3.setText(dateFormat.format(plan.getStartTime()));
                break;
        }
    }


    private void removePlanLayout(int index) {
        switch(index){
            case 0:
                switch(planList.size()){
                    case 0:
                        layoutPlan1.setVisibility(View.GONE);
                        break;
                    case 1:
                        layoutPlan2.setVisibility(View.GONE);
                        moveUpPlanLayout(1);
                        break;
                    case 2:
                        layoutPlan3.setVisibility(View.GONE);
                        layoutPlanAdd.setVisibility(View.VISIBLE);
                        moveUpPlanLayout(1);
                        moveUpPlanLayout(2);
                        break;
                }
                break;
            case 1:
                switch(planList.size()){
                    case 1:
                        layoutPlan2.setVisibility(View.GONE);
                        break;
                    case 2:
                        layoutPlan3.setVisibility(View.GONE);
                        layoutPlanAdd.setVisibility(View.VISIBLE);
                        moveUpPlanLayout(2);
                        break;
                }
                break;
            case 2:
                layoutPlan3.setVisibility(View.GONE);
                layoutPlanAdd.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void moveUpPlanLayout(int index) {
        switch (index){
            case 1:
                txtPlanType1.setText(txtPlanType2.getText());
                imgPlan1.setImageDrawable(imgPlan2.getDrawable());
                txtPlanDesc1.setText(txtPlanDesc2.getText());
                txtPlanLocation1.setText(txtPlanLocation2.getText());
                txtPlanTime1.setText(txtPlanTime2.getText());
                break;
            case 2:
                txtPlanType2.setText(txtPlanType3.getText());
                imgPlan2.setImageDrawable(imgPlan3.getDrawable());
                txtPlanDesc2.setText(txtPlanDesc3.getText());
                txtPlanLocation2.setText(txtPlanLocation3.getText());
                txtPlanTime2.setText(txtPlanTime3.getText());
                break;
        }
    }

    @Override
    public void onSuccess(Object obj, int accessId, Object inputParameter) {
        switch (accessId) {
            case R.id.party_name:
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
                            party.setGroupId(groupId);
                            party.setUserNo(AppContext.getUserInfoBean().getUserNo());

                            party.setStatus(0); //  召集中
                            party.setNew(false);

                            String planIds = jsonRoot.getString("planIds");
                            if(planIds==null || planIds.equals("")){
                                JujuDbUtils.saveOrUpdate(party);
                            }
                            String[] planIdArray = planIds.split(",");
                            if(planList.size() == planIdArray.length){
                                for(int i=0; i<planList.size(); i++){
                                    Plan plan = planList.get(i);
                                    plan.setPartyId(partyId);
                                    plan.setId(planIdArray[i]);
                                    plan.setSigned(1);

                                    if(i==0){
                                        party.setTime(plan.getStartTime());
                                        party.setCoverUrl(plan.getCoverUrl());
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
                                                AppContext.getUserInfoBean().getUserNo()
                                                ,AppContext.getUserInfoBean().getNickName());
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
