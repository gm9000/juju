package com.juju.app.activity.party;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.PlanTypeAdapter;
import com.juju.app.entity.Plan;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.wheel.dialog.SelectDateTimeDialog;
import com.rey.material.app.BottomSheetDialog;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@ContentView(R.layout.activity_plan_create)
public class PlanCreateActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "PlanCreateActivity";

    private static final int CMD_REQ_LOCATION = 1;

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

    @ViewInject(R.id.layout_plan)
    private LinearLayout layout_plan;
    @ViewInject(R.id.layout_cover)
    private RelativeLayout layoutCover;
    @ViewInject(R.id.img_cover)
    private ImageView imgCover;

    @ViewInject(R.id.layout_plan_type)
    private LinearLayout layoutPlanType;
    @ViewInject(R.id.txt_plan_type)
    private TextView txtPlanType;

    @ViewInject(R.id.layout_time)
    private LinearLayout layoutTime;
    @ViewInject(R.id.txt_time)
    private TextView txt_time;
    @ViewInject(R.id.txt_location)
    private EditText txt_location;
    @ViewInject(R.id.txt_plan_description)
    private EditText txt_planDescription;


    private double latitude = 0;
    private double longitude = 0;

    @ViewInject(R.id.img_select_location)
    private ImageView img_selectLocation;

    @ViewInject(R.id.btn_add_plan)
    private Button btnAddPlan;

    private InputMethodManager inputManager = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private BottomSheetDialog msgDialog;

    private int index = -1;
    private Plan plan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        initParam();
        initView();
        initListeners();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }

    private void initListeners() {
        layoutPlanType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePlanType();
            }
        });
    }


    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        txt_title.getRootView().setBackgroundColor(getResources().getColor(R.color.background));
        txt_title.setText(R.string.party_plan);
        img_right.setVisibility(View.GONE);
        txt_right.setVisibility(View.GONE);

        if(plan != null){
            txtPlanType.setText(getResValue(plan.getType()));
            imgCover.setImageResource(getResValue(plan.getType().toLowerCase(),"mipmap"));
            txt_time.setText(dateFormat.format(plan.getStartTime()));
            txt_location.setText(plan.getAddress());
            txt_planDescription.setText(plan.getDesc());
            latitude = plan.getLatitude();
            longitude = plan.getLongitude();

        }
    }


    public void initParam() {
        Intent intent = getIntent();
        String indexStr = intent.getStringExtra("index");
        if(!StringUtils.empty(indexStr)){
            index = Integer.parseInt(indexStr);
            String planStr = intent.getStringExtra("planStr");
            plan = JacksonUtil.turnString2Obj(planStr,Plan.class);
        }
    }

    @Event(R.id.txt_left)
    private void goCancel(View view){
        ActivityUtil.finish(this);
    }
    @Event(value=R.id.img_back)
    private void goBack(View view){
        ActivityUtil.finish(this);
    }

    @Event(value=R.id.layout_cover)
    private void uploadCover(View view){
        ToastUtil.showShortToast(this,"上传并获取封面地址",1);
    }


    @Event(value=R.id.layout_time)
    private void showSelectTimeDialog(View view) {
        SelectDateTimeDialog mSelectDateTimeDialog = new SelectDateTimeDialog(this);
        mSelectDateTimeDialog.setCanceledOnTouchOutside(true);
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
            String dateString = dateFormat.format(new Date().getTime()+24*3600*1000);
            mSelectDateTimeDialog.showTimeStr(dateString);
        }
    }


    @Event(value=R.id.img_select_location)
    private void selectLocation(View view){
        Intent intent = new Intent(this,PlanLocationActivity.class);
        intent.putExtra(Constants.EDIT_MODE,true);
        intent.putExtra(Constants.ADDRESS,txt_location.getText().toString());
        intent.putExtra(Constants.LONGITUDE, longitude);
        intent.putExtra(Constants.LATITUDE, latitude);
        startActivityForResult(intent, CMD_REQ_LOCATION);
    }



    @Event(R.id.btn_add_plan)
    private void addPlan(View view){

        if(StringUtils.empty(txtPlanType.getText().toString())){
            ToastUtil.showShortToast(this, "请选择活动类型", 1);
            return;
        }
        if(txt_time.getText().toString().equals("") || txt_location.getText().toString().equals("")) {
            ToastUtil.showShortToast(this, "时间和地址必须设定", 1);
            return;
        }

        if(plan==null){
            plan = new Plan();
        }
        try {
            plan.setStartTime(dateFormat.parse(txt_time.getText().toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        plan.setAddress(txt_location.getText().toString());
        plan.setDesc(txt_planDescription.getText().toString());
        plan.setLatitude(latitude);
        plan.setLongitude(longitude);
        if(txtPlanType.getTag()!=null && txtPlanType.getTag() instanceof Plan.Type) {
            plan.setType(((Plan.Type) txtPlanType.getTag()).name());
            if(plan.getCoverUrl()==null || !plan.getCoverUrl().startsWith("http:")){
                plan.setCoverUrl(plan.getType());
            }
        }

        Intent intent = getIntent();
        intent.putExtra("planJson", JacksonUtil.turnObj2String(plan));
        intent.putExtra("index",index);
        setResult(RESULT_OK,intent);
        ActivityUtil.finish(this);

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch(parent.getId()) {
            case R.id.grid_view_plan_type:
                Plan.Type planType = Plan.Type.values()[position];
                txtPlanType.setText(getResValue(planType.name()));
                txtPlanType.setTag(planType);
                imgCover.setImageResource(getResValue(planType.name().toLowerCase(),"mipmap"));
                msgDialog.dismiss();
                break;
        }

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

    private void choosePlanType() {

        msgDialog = new BottomSheetDialog(this);
        msgDialog.setCanceledOnTouchOutside(true);
        msgDialog.contentView(R.layout.layout_plan_type)
                .inDuration(300);
        TextView txtTitle = (TextView)msgDialog.findViewById(R.id.txt_title);
        txtTitle.setText(R.string.plan_type);
        GridView planTypeGridView = (GridView)msgDialog.findViewById(R.id.grid_view_plan_type);
        PlanTypeAdapter planTypeAdapter = new PlanTypeAdapter(this);
        planTypeGridView.setAdapter(planTypeAdapter);
        planTypeGridView.setOnItemClickListener(this);
        msgDialog.show();

    }

}
