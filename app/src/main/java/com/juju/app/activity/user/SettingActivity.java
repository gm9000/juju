package com.juju.app.activity.user;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.LoginActivity;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.RoundImageView;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@ContentView(R.layout.activity_setting)
public class SettingActivity extends AppCompatActivity implements HttpCallBack {

    private static final String TAG = "SettingActivity";

    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.img_right)
    private ImageView img_right;


    @ViewInject(R.id.head)
    private RoundImageView headImg;

    @ViewInject(R.id.txt_phoneNo)
    private TextView txt_phoneNo;
    @ViewInject(R.id.txt_nick_name)
    private TextView txt_nickName;
    @ViewInject(R.id.txt_jujuNo)
    private TextView txt_jujuNo;
    @ViewInject(R.id.txt_gender)
    private TextView txt_gender;

    @ViewInject(R.id.img_rememberPwd)
    private ImageView img_rememberPwd;
    @ViewInject(R.id.img_autoLogin)
    private ImageView img_autoLogin;

    @ViewInject(R.id.logoutBtn)
    private Button logoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.inject(this);
        initView();
        loadUserInfo();
    }

    @Override
    protected void onResume(){
        super.onResume();
        loadUserInfo();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        udateUserInfo();

    }


    private void udateUserInfo() {
        String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
        User userInfo = null;
        if(userInfoStr!=null) {
            userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
        }
        if(userInfo != null && userInfo.isUpdate()){
            Map<String, Object> valueMap = new HashMap<String, Object>();
            UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
            valueMap.put("userNo", userTokenInfoBean.getJujuNo());
            valueMap.put("token", userTokenInfoBean.getToken());
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
    }


    private void loadUserInfo() {

        BitmapUtilFactory.getInstance(this).display(headImg,HttpConstants.getUserUrl()+"/getPortraitSmall?targetNo="+ BaseApplication.getInstance().getUserInfoBean().getJujuNo());

        String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
        User userInfo = null;
        if(userInfoStr!=null) {
            userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
        }
        if(userInfo == null){
            UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
            Map<String, Object> valueMap = new HashMap<String, Object>();
            valueMap.put("userNo", userInfoBean.getJujuNo());
            valueMap.put("token", userInfoBean.getToken());
            valueMap.put("targetNo", userInfoBean.getJujuNo());

            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                    R.id.txt_jujuNo, HttpConstants.getUserUrl() + "/getUserInfo", this, valueMap,
                    JSONObject.class);
            try {
                client.sendGet();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            txt_gender.setText(userInfo.getGender() == 0 ? "女" : "男");
            txt_jujuNo.setText(userInfo.getUserNo());
            txt_phoneNo.setText(userInfo.getPhone());
            txt_nickName.setText(userInfo.getNickName());
        }
    }

    private void initView() {

        txt_title.setText(R.string.settings);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.me);
        img_back.setVisibility(View.VISIBLE);
        img_right.setVisibility(View.GONE);

        txt_jujuNo.setText("");
        txt_phoneNo.setText("");
        txt_nickName.setText("");

        boolean rememberPwd = (boolean) SpfUtil.get(getApplicationContext(),Constants.REMEMBER_PWD,true);
        boolean autoLogin = (boolean) SpfUtil.get(getApplicationContext(),Constants.AUTO_LOGIN,true);

        if(rememberPwd){
            img_rememberPwd.setImageResource(R.mipmap.switch_on);
        }else{
            img_rememberPwd.setImageResource(R.mipmap.switch_off);
        }

        if(autoLogin){
            img_autoLogin.setImageResource(R.mipmap.switch_on);
        }else{
            img_autoLogin.setImageResource(R.mipmap.switch_off);
        }

    }



    @OnClick(R.id.head)
    private void showHeadImg(View view){
        ActivityUtil.startActivity(this, UploadPhotoActivity.class);
    }

    @OnClick(R.id.txt_nick_name)
    private void modifyNickName(View view){
        BasicNameValuePair typeValue = new BasicNameValuePair(Constants.PROPERTY_TYPE,String.valueOf(R.id.txt_nick_name));
        BasicNameValuePair valueValue = new BasicNameValuePair(Constants.PROPERTY_VALUE,txt_nickName.getText().toString());
        ActivityUtil.startActivity(this,PropertiesSettingActivity.class,typeValue,valueValue);
    }

    @OnClick(R.id.txt_gender)
    private void modifyGender(View view){
        BasicNameValuePair typeValue = new BasicNameValuePair(Constants.PROPERTY_TYPE,String.valueOf(R.id.txt_gender));
        BasicNameValuePair valueValue = new BasicNameValuePair(Constants.PROPERTY_VALUE,txt_gender.getText().toString());
        ActivityUtil.startActivity(this, PropertiesSettingActivity.class, typeValue, valueValue);
    }

    @OnClick(R.id.txt_phoneNo)
    private void modifyPhone(View view){
        ToastUtil.showShortToast(this,"phone",1);
    }


    @OnClick(R.id.img_rememberPwd)
    private void remenberPwd(View view){
        boolean rememberPwd = (boolean) SpfUtil.get(getApplicationContext(),Constants.REMEMBER_PWD,true);
        SpfUtil.put(getApplicationContext(),Constants.REMEMBER_PWD,!rememberPwd);
        if(rememberPwd){
            img_rememberPwd.setImageResource(R.mipmap.switch_off);
        }else{
            img_rememberPwd.setImageResource(R.mipmap.switch_on);
        }
    }

    @OnClick(R.id.img_autoLogin)
    private void autoLogin(View view){
        boolean autoLogin = (boolean) SpfUtil.get(getApplicationContext(),Constants.AUTO_LOGIN,true);
        SpfUtil.put(getApplicationContext(), Constants.AUTO_LOGIN, !autoLogin);
        if(autoLogin){
            img_autoLogin.setImageResource(R.mipmap.switch_off);
        }else{
            img_autoLogin.setImageResource(R.mipmap.switch_on);
        }
    }

    @OnClick(R.id.img_back)
    private void imgBack(View view) {
        toMe();
    }
    @OnClick(R.id.txt_left)
    private void txtBack(View view){
        toMe();
    }

    private void toMe() {
        ActivityUtil.finish(this);
    }

    @OnClick(R.id.logoutBtn)
    private void logout(View view){
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("userNo", userInfoBean.getJujuNo());
        valueMap.put("token", userInfoBean.getToken());

        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                R.id.logoutBtn, HttpConstants.getUserUrl() + "/logout", this, valueMap,
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
     *******************************************回调函数******************************************
     */
    @Override
    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
        switch (accessId) {
            case R.id.logoutBtn:
                if(obj != null && obj.length > 0) {
                    JSONObject jsonRoot = (JSONObject)obj[0];
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            ActivityUtil.startActivity(this, LoginActivity.class);
                            ActivityUtil.finish(this);
                        } else {
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.txt_jujuNo:
                if(obj != null && obj.length > 0) {
                    JSONObject jsonRoot = (JSONObject)obj[0];
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            JSONObject userJson = jsonRoot.getJSONObject("user");
                            //   "userNo":"100000001","nickName":"别名-1","userPhone":"13800000001","birthday":1451889752445,"gender":1,"createTime":1451889752445}
                            User userInfo = new User();
                            userInfo.setUserNo(userJson.getString("userNo"));
                            userInfo.setNickName(userJson.getString("nickName"));
                            userInfo.setPhone(userJson.getString("userPhone"));
                            userInfo.setGender(userJson.getInt("gender"));

                            txt_gender.setText(userInfo.getGender()==0?"女":"男");
                            txt_jujuNo.setText(userInfo.getUserNo());
                            txt_phoneNo.setText(userInfo.getPhone());
                            txt_nickName.setText(userInfo.getNickName());
                            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo.toString());

                        } else {
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.txt_property:
                if(obj != null && obj.length > 0) {
                    JSONObject jsonRoot = (JSONObject)obj[0];
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
                            User userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
                            userInfo.setUpdate(false);
                            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
                        } else {
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
}
