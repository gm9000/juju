package com.juju.app.activity.user;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.CustomDialog;
import com.juju.app.view.XEditText;
import com.lidroid.xutils.ViewUtils;
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
import java.util.HashMap;
import java.util.Map;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

@ContentView(R.layout.activity_properties_setting)
public class PropertiesSettingActivity extends BaseActivity implements XEditText.DrawableRightListener,HttpCallBack {

    private static final String TAG = "PropertiesSetting";

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


    @ViewInject(R.id.txt_property)
    private XEditText txt_property;

    @ViewInject(R.id.txt_male)
    private TextView txt_male;
    @ViewInject(R.id.txt_female)
    private TextView txt_female;

    private int propertyType;
    private String propertyValue;

    @ViewInject(R.id.txt_phone_title)
    private TextView txtPhoneTitle;
    @ViewInject(R.id.txt_phone_hint)
    private TextView txtPhoneHint;
    @ViewInject(R.id.btn_change_phone)
    private Button btnChangePhone;
    @ViewInject(R.id.layout_phone)
    private RelativeLayout layoutPhone;
    @ViewInject(R.id.txt_phone)
    private TextView txtPhone;
    @ViewInject(R.id.txt_yzm)
    private TextView txtYzm;
    @ViewInject(R.id.btn_yzm)
    private Button btnYzm;
    @ViewInject(R.id.btn_submit)
    private Button btnSubmit;

    private User userInfo;

    private Drawable drawable;

    private String newPhone;

    private TimeCount time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ViewUtils.inject(this);
        initParam();
        initData();
        initView();

    }

    private void initData() {

        String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
        userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);

        drawable = getResources().getDrawable(R.mipmap.right_icon);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());//必须设置图片大小，否则不显示

    }

    private void initView() {

        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.cancel);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txt_left.getLayoutParams();
        layoutParams.leftMargin = 15;
        txt_left.setLayoutParams(layoutParams);
        txt_right.setText(R.string.save);
        txt_right.setTextColor(0xFF27AE60);
        img_right.setVisibility(View.GONE);

        switch(propertyType){
            case R.id.txt_nick_name:
                txt_title.setText(R.string.nickName);
                txt_property.setText(propertyValue);
                txt_property.setDrawableRightListener(this);
                break;
            case R.id.txt_gender:
                txt_property.setVisibility(View.GONE);
                txt_right.setVisibility(View.GONE);
                txt_title.setText(R.string.gender);

                txt_male.setVisibility(View.VISIBLE);
                txt_female.setVisibility(View.VISIBLE);
                if(propertyValue.equals(getString(R.string.male))){
                    txt_male.setCompoundDrawables(null,null,drawable,null);
                }else{
                    txt_female.setCompoundDrawables(null,null,drawable,null);
                }

                break;
            case R.id.txt_phoneNo:
                txt_property.setVisibility(View.GONE);
                txt_right.setVisibility(View.GONE);
                txt_title.setText(R.string.phoneNo);
                txtPhoneHint.setVisibility(View.VISIBLE);
                txtPhoneTitle.setVisibility(View.VISIBLE);
                txtPhoneHint.setText(propertyValue);
                btnChangePhone.setVisibility(View.VISIBLE);
                initSDK();
                break;
        }


    }


    public void initParam() {
        propertyType = Integer.parseInt(getIntent().getStringExtra(Constants.PROPERTY_TYPE));
        propertyValue = getIntent().getStringExtra(Constants.PROPERTY_VALUE);
    }

    @OnClick(R.id.txt_left)
    private void cancelModify(View view){
        ActivityUtil.finish(this);
    }


    @OnClick(R.id.txt_male)
    private void selectMale(View view){
        if(propertyValue.equals(getString(R.string.female))){
            txt_female.setCompoundDrawables(null,null,null,null);
            txt_male.setCompoundDrawables(null, null, drawable, null);
            userInfo.setGender(1);
            userInfo.setUpdate(true);
            SpfUtil.put(getApplicationContext(),Constants.USER_INFO,userInfo);
        }
        ActivityUtil.finish(this);
    }

    @OnClick(R.id.txt_female)
    private void selectFemale(View view){
        if(propertyValue.equals(getString(R.string.male))){
            txt_male.setCompoundDrawables(null,null,null,null);
            txt_female.setCompoundDrawables(null,null,drawable,null);
            userInfo.setGender(0);
            userInfo.setUpdate(true);
            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
        }
        ActivityUtil.finish(this);
    }

    @OnClick(R.id.btn_change_phone)
    private void changePhone(View view){
        btnChangePhone.setVisibility(View.GONE);
        layoutPhone.setVisibility(View.VISIBLE);
        txtYzm.addTextChangedListener(new TextChange());
        btnSubmit.setTextColor(0xFFD0EFC6);
        btnSubmit.setEnabled(false);
    }

    @OnClick(R.id.btn_yzm)
    private void getYzm(View view){
        if(txtPhone.getText().toString().equals(propertyValue)){
            ToastUtil.showShortToast(this,getString(R.string.phone_same),1);
            return;
        }
        if(!StringUtils.isMobileNO(txtPhone.getText().toString())) {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.regist_phone_error, 0);
            txtPhone.requestFocus();
            return;
        }
        showMobileDialog();
    }

    private void showMobileDialog() {
        CustomDialog.Builder builder = new CustomDialog.Builder(this);
        builder.setMessage(getString(R.string.login_mobile_confirm) + txtPhone.getText().toString());
        builder.setPositiveButton(R.string.confirm,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        requestValidateCode();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(R.string.negative,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    private void requestValidateCode() {
        newPhone = txtPhone.getText().toString();
        SMSSDK.getVerificationCode("86", newPhone);
        time = new TimeCount(60000, 1000);//构造CountDownTimer对象
        time.start();
        txtPhone.setEnabled(false);
        btnYzm.setClickable(false);
    }

    private void initSDK() {
        SMSSDK.initSDK(this, Constants.APPKEY, Constants.APPSECRET);
        SMSSDK.registerEventHandler(eh); //注册短信回调
    }

    @OnClick(R.id.btn_submit)
    private void submitChangePhone(View view){
        String yzm = txtYzm.getText().toString();
        SMSSDK.submitVerificationCode("86",newPhone, yzm);
    }



    @OnClick(R.id.txt_right)
    private void save(View view){

        Map<String, Object> valueMap = new HashMap<String, Object>();
        UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
        valueMap.put("userNo", userTokenInfoBean.getJujuNo());
        valueMap.put("token", userTokenInfoBean.getToken());

        switch(propertyType){
            case R.id.txt_nick_name:
                userInfo.setNickName(txt_property.getText().toString());
                break;
        }
        valueMap.put("nickName", userInfo.getNickName());
        valueMap.put("gender", userInfo.getGender());

        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                R.id.txt_property, HttpConstants.getUserUrl() + "/updateUserInfo", this, valueMap,
                JSONObject.class);
        try {
            client.sendPost();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
        switch (accessId) {
            case R.id.txt_property:
                if(obj != null && obj.length > 0) {
                    JSONObject jsonRoot = (JSONObject)obj[0];
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            userInfo.setUpdate(false);
                            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
                            JujuDbUtils.saveOrUpdate(userInfo);
                            ActivityUtil.finish(this);
                        } else {
                            Log.e(TAG,"return status code:"+status);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.txt_phone:
                if(obj != null && obj.length > 0) {
                    JSONObject jsonRoot = (JSONObject)obj[0];
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            userInfo.setUpdate(false);
                            userInfo.setUserPhone(newPhone);
                            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
                            JujuDbUtils.saveOrUpdate(userInfo);
                            ActivityUtil.finish(this);
                        } else {
                            String desc = jsonRoot.getString("desc");
                            if(desc != null) {
                                showMsgDialog(getResValue(desc));
                            }else{
                               ToastUtil.showShortToast(this,"unknown error",1);
                            }

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
    public void onFailure(HttpException error, String msg, int accessId) {
        System.out.println("accessId:" + accessId + "\r\n msg:" + msg + "\r\n code:" +
                error.getExceptionCode());
    }


    @Override
    public void onDrawableRightClick(View view) {
        txt_property.setText("");
    }


    /**
     *******************************************回调函数******************************************
     */
    EventHandler eh = new EventHandler(){
        @Override
        public void afterEvent(int event, int result, Object data) {
            if (result == SMSSDK.RESULT_COMPLETE) {
                //回调完成
                if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                    //提交验证码成功
                    Log.i(TAG, "更换绑定手机成功");
                    updateUserPhone();
                }else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE){
                    //获取验证码成功
                    Log.i(TAG, "获取验证码成功");
                }else if (event ==SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES){
                    //返回支持发送验证码的国家列表
                }
            }else{
                ((Throwable)data).printStackTrace();
                txtYzm.requestFocus();
                showMsgDialog(R.string.login_vercode_error);
                Log.d(TAG, "请输入正确的验证码");
            }
        }
    };

    private void updateUserPhone() {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
        valueMap.put("userNo", userTokenInfoBean.getJujuNo());
        valueMap.put("token", userTokenInfoBean.getToken());
        valueMap.put("phone",newPhone);

        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                R.id.txt_phone, HttpConstants.getUserUrl() + "/updatePhone", this, valueMap,
                JSONObject.class);
        try {
            client.sendPost();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     *******************************************内部类******************************************
     */
    //计时器
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }
        @Override
        public void onFinish() {//计时完毕时触发
            btnYzm.setClickable(true);
            btnYzm.setText("重新发送");
            txtPhone.setEnabled(true);
        }
        @Override
        public void onTick(long millisUntilFinished){//计时过程显示
            long leftTime = millisUntilFinished / 1000;
            String tishi = leftTime+"秒";
            btnYzm.setText(tishi);
        }
    }

    // EditText监听器
    class TextChange implements TextWatcher {

        @Override
        public void afterTextChanged(Editable arg0) {

        }

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {

        }

        @Override
        public void onTextChanged(CharSequence cs, int start, int before,
                                  int count) {

            boolean sign1 = txtYzm.getText().length() > 3;
            if (sign1 && newPhone!=null) {
                btnSubmit.setTextColor(0xFFFFFFFF);
                btnSubmit.setEnabled(true);
            }
            // 在layout文件中，对Button的text属性应预先设置默认值，否则刚打开程序的时候Button是无显示的
            else {
                btnSubmit.setTextColor(0xFFD0EFC6);
                btnSubmit.setEnabled(false);
            }
        }
    }
}
