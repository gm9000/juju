package com.juju.app.fragment.party;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.party.PlanDetailActivity;
import com.juju.app.activity.party.PlanLocationActivity;
import com.juju.app.activity.user.SettingActivity;
import com.juju.app.adapters.PlanVoteListAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.bean.json.PlanVoteBean;
import com.juju.app.bean.json.PlanVoteReqBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Plan;
import com.juju.app.entity.PlanVote;
import com.juju.app.entity.User;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.event.notify.PlanVoteEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.golobal.StatusCode;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.notify.PartyRecruitNotify;
import com.juju.app.service.notify.PlanVoteNotify;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.scroll.NoScrollGridView;

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

@SuppressLint("ValidFragment")
@ContentView(R.layout.plan_detail_item)
@CreateFragmentUI(viewId = R.layout.plan_detail_item)
public class PlanDetailFragment extends BaseFragment implements CreateUIHelper,HttpCallBack,AdapterView.OnItemClickListener {

    private static final String TAG = "LocationFragment";
    private final PlanDetailActivity activity;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");


    @ViewInject(R.id.img_plan_type)
    private ImageView imgPlanType;
    @ViewInject(R.id.txt_plan_type)
    private TextView txtPlanType;
    @ViewInject(R.id.img_selected)
    private ImageView imgSelected;
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

    @ViewInject(R.id.gridview_user)
    private NoScrollGridView gridview_user;

    @ViewInject(R.id.btn_operate)
    private Button btn_operate;

    public boolean needRrefresh() {
        return needRrefresh;
    }

    public void setNeedRrefresh(boolean needRrefresh) {
        this.needRrefresh = needRrefresh;
    }

    public void refresh(){
        if(plan.getStatus()==0){
            imgSelected.setImageResource(R.mipmap.un_selected);
            needRrefresh = true;
        }
    }

    private boolean needRrefresh = false;

    private String groupId;
    private Plan plan;


    private PlanVoteListAdapter planVoteListAdapter;
    private List<PlanVote> planVoteList;
    private boolean isOwner;
    private boolean isSigned;

    public PlanDetailFragment(PlanDetailActivity activity,String groupId,Plan plan, boolean isOwner){
        super();
        this.activity = activity;
        this.groupId = groupId;
        this.plan = plan;
        this.isOwner = isOwner;
    }




    @Override
    protected void findViews() {
    }

    @Override
    public void loadData() {

        isSigned = false;
        UserInfoBean userInfoBean = AppContext.getUserInfoBean();
        try {
            planVoteList = JujuDbUtils.getInstance().selector(PlanVote.class).where("plan_id", "=", plan.getId()).findAll();
            if(planVoteList != null) {
                for(PlanVote planVote : planVoteList) {
                    if(planVote.getAttenderNo().equals(userInfoBean.getUserNo())){
                        isSigned = true;
                        break;
                    }
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
        }

        if(isSigned){
            btn_operate.setBackgroundColor(getResources().getColor(R.color.red));
            btn_operate.setText(R.string.unsignup);

        }else{
            btn_operate.setBackgroundColor(getResources().getColor(R.color.blue));
            btn_operate.setText(R.string.signup);
        }

        planVoteListAdapter = new PlanVoteListAdapter(activity,planVoteList);
        gridview_user.setAdapter(planVoteListAdapter);

    }

    @Override
    public void initView() {
        if(isOwner){
            btn_operate.setVisibility(View.GONE);
            imgSelected.setVisibility(View.VISIBLE);
        }else{
            btn_operate.setVisibility(View.VISIBLE);
            imgSelected.setVisibility(View.GONE);
        }

        txt_attendNum.setText(String.valueOf(planVoteList.size()));
        txtPlanType.setText(activity.getResValue(plan.getType()));
        if(plan.getStatus()==0){
            imgSelected.setImageResource(R.mipmap.un_selected);
        }else{
            imgSelected.setImageResource(R.mipmap.selected);
        }

        if(plan.getDesc()==null || plan.getDesc().equals("")){
        }else{
            txt_description.setTextColor(getResources().getColor(R.color.black));
            txt_description.setText("\t\t"+plan.getDesc());
        }

        txt_time.setText(dateFormat.format(plan.getStartTime()));
        txt_location.setText(plan.getAddress());

        // TODO 根据地理位置对接天气
        txt_weather.setText("晴间多云  2-15 ℃");

        if(plan.getLatitude() == 0){
            img_mapped.setVisibility(View.GONE);
        }
        img_mapped.setRotation(45);

        imgPlanType.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        if(plan.getCoverUrl()!=null) {
            if (plan.getCoverUrl().startsWith("http:")){
                ImageLoaderUtil.getImageLoaderInstance().displayImage(plan.getCoverUrl(), imgPlanType, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
            }else{
                final int resId = activity.getResValue(plan.getCoverUrl().toLowerCase(), "mipmap");
                imgPlanType.setImageResource(resId);
            }
        }
    }

    @Override
    public void setOnListener() {
        gridview_user.setOnItemClickListener(this);
    }



    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();

    }

    @Event(R.id.layout_location)
    private void showMap(View view){
        if(plan.getLatitude()!=0 && plan.getLongitude()!=0) {
            Intent intent = new Intent(activity, PlanLocationActivity.class);
            intent.putExtra(Constants.LATITUDE, plan.getLatitude());
            intent.putExtra(Constants.LONGITUDE, plan.getLongitude());
            intent.putExtra(Constants.ADDRESS, plan.getAddress());
            startActivity(intent);
        }
    }

    @Event(value=R.id.img_selected)
    private void selectPlan(View view){
        activity.checkPlan(plan);
        imgSelected.setImageResource(plan.getStatus()==0?R.mipmap.un_selected:R.mipmap.selected);
    }

    @Event(R.id.btn_operate)
    private void changeSignFlag(View view){
        votePlanToServer(!isSigned);
    }

    private void votePlanToServer(boolean voteFlag) {

        UserInfoBean userTokenInfoBean = AppContext.getUserInfoBean();
        PlanVoteReqBean reqBean = new PlanVoteReqBean();
        reqBean.setUserNo(userTokenInfoBean.getUserNo());
        reqBean.setToken(userTokenInfoBean.getToken());

        PlanVoteBean planVote = new PlanVoteBean();
        planVote.setPlanId(plan.getId());
        planVote.setVote(voteFlag ? 1 : 0);
        reqBean.setPlanVote(planVote);

        JlmHttpClient<PlanVoteReqBean> client = new JlmHttpClient<PlanVoteReqBean>(R.id.party_name, HttpConstants.getUserUrl() + "/votePlan", this, reqBean,JSONObject.class);
        try {
            activity.loading(true, R.string.saving);
            client.sendPost();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
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
                        if(status==StatusCode.SUCCESS || status==StatusCode.VOTE_HAS_EXIST || status==StatusCode.VOTE_NOT_EXIST) {
                            UserInfoBean userTokenInfoBean = AppContext.getUserInfoBean();
                            if(isSigned){

                                WhereBuilder whereBuilder = WhereBuilder.b("attender_no", "=", userTokenInfoBean.getUserNo());
                                whereBuilder.and("plan_id", "=", plan.getId());
                                JujuDbUtils.getInstance().delete(PlanVote.class, whereBuilder);
                                plan.setAddtendNum(plan.getAddtendNum()-1);
                                plan.setSigned(0);
                                JujuDbUtils.saveOrUpdate(plan);

                                for(PlanVote planVote:planVoteList){
                                    if(planVote.getAttenderNo().equals(userTokenInfoBean.getUserNo())){
                                        planVoteList.remove(planVote);
                                        planVoteListAdapter.notifyDataSetChanged();
                                        txt_attendNum.setText(String.valueOf(planVoteList.size()));
                                        break;
                                    }
                                }

                            }else{
                                User user = JujuDbUtils.getInstance().selector(User.class).where("user_no", "=", userTokenInfoBean.getUserNo()).findFirst();
                                PlanVote planVote = new PlanVote();
                                planVote.setPlanId(plan.getId());
                                planVote.setAttenderNo(userTokenInfoBean.getUserNo());
                                JujuDbUtils.save(planVote);

                                plan.setAddtendNum(plan.getAddtendNum()+1);
                                plan.setSigned(1);
                                JujuDbUtils.saveOrUpdate(plan);

                                planVoteList.add(planVote);
                                planVoteListAdapter.notifyDataSetChanged();
                                txt_attendNum.setText(String.valueOf(planVoteList.size()));
                            }

                            //  通知 Plan投票发生变化
                            PlanVoteEvent.PlanVoteBean planVoteBean = PlanVoteEvent
                                    .PlanVoteBean.valueOf(groupId,plan.getId(),isSigned?0:1,
                                            AppContext.getUserInfoBean().getUserNo()
                                            ,AppContext.getUserInfoBean().getNickName());
                            planVoteBean.setPartyId(plan.getPartyId());
                            PlanVoteNotify.instance().executeCommand4Send(planVoteBean);

                            activity.completeLoading();
                            if(isSigned){
                                isSigned = false;
                                btn_operate.setBackgroundColor(getResources().getColor(R.color.blue));
                                btn_operate.setText(R.string.signup);
                            }else{
                                isSigned = true;
                                btn_operate.setBackgroundColor(getResources().getColor(R.color.red));
                                btn_operate.setText(R.string.unsignup);
                            }

                        } else {
                            activity.completeLoading();
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
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId, Object inputParameter) {
        activity.completeLoading();
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        ActivityUtil.startActivity(activity, SettingActivity.class,new BasicNameValuePair(Constants.USER_NO,planVoteList.get(position).getAttender().getUserNo()));
        startActivityNew(activity, SettingActivity.class, Constants.USER_NO,planVoteList
                .get(position).getAttenderNo());
    }

}
