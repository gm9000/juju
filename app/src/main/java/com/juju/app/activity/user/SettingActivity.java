package com.juju.app.activity.user;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.WelcomeActivity;
import com.juju.app.activity.LoginActivity;
import com.juju.app.activity.party.PartyCreateActivity;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.RoundImageView;
import com.juju.app.view.dialog.WarnTipDialog;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

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

    @ViewInject(R.id.layout_setting)
    private RelativeLayout layout_setting;
    @ViewInject(R.id.img_rememberPwd)
    private ImageView img_rememberPwd;
    @ViewInject(R.id.img_autoLogin)
    private ImageView img_autoLogin;

    @ViewInject(R.id.logoutBtn)
    private Button logoutBtn;

    private String userNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);
        initParam();
        initView();
        loadUserInfo();
    }

    private void initParam() {
        userNo = getIntent().getStringExtra(Constants.USER_NO);
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
        String targetNo = userNo==null?BaseApplication.getInstance().getUserInfoBean().getJujuNo():userNo;

        BitmapUtilFactory.getInstance(this).bind(headImg, HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + targetNo, BitmapUtilFactory.Option.imageOptions());
        User userInfo = null;
        if(userNo == null) {
            String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
            if (userInfoStr != null) {
                userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
            }
        }else{
            try {
                userInfo = JujuDbUtils.getInstance(this).selector(User.class).where("user_no", "=", userNo).findFirst();
            } catch (DbException e) {
                e.printStackTrace();
            }
            if(userInfo != null){
                txt_title.setText(userInfo.getNickName());
            }
        }
        if(userInfo == null){
            UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
            Map<String, Object> valueMap = new HashMap<String, Object>();
            valueMap.put("userNo", userInfoBean.getJujuNo());
            valueMap.put("token", userInfoBean.getToken());
            valueMap.put("targetNo", userNo==null?userInfoBean.getJujuNo():userNo);

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
            txt_phoneNo.setText(userInfo.getUserPhone());
            txt_nickName.setText(userInfo.getNickName());
        }
    }

    private void initView() {

        if(userNo == null) {
            txt_title.setText(R.string.settings);
            txt_left.setText(R.string.me);

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

        }else{
            txt_title.setText(R.string.basic_info);
            txt_left.setText(R.string.top_left_back);

            layout_setting.setVisibility(View.GONE);
            logoutBtn.setVisibility(View.GONE);

            txt_gender.setClickable(false);
            txt_gender.setCompoundDrawables(null, null, null, null);
            txt_phoneNo.setClickable(false);
            txt_phoneNo.setCompoundDrawables(null, null, null, null);
            txt_nickName.setClickable(false);
            txt_nickName.setCompoundDrawables(null,null,null,null);

        }
        txt_left.setVisibility(View.VISIBLE);
        img_back.setVisibility(View.VISIBLE);
        img_right.setVisibility(View.GONE);

    }



    @Event(R.id.head)
    private void showHeadImg(View view){
        ActivityUtil.startActivity(this, UploadPhotoActivity.class, new BasicNameValuePair(Constants.USER_NO, userNo));
    }

    @Event(R.id.layout_nick_name)
    private void modifyNickName(View view){
        BasicNameValuePair typeValue = new BasicNameValuePair(Constants.PROPERTY_TYPE,String.valueOf(R.id.txt_nick_name));
        BasicNameValuePair valueValue = new BasicNameValuePair(Constants.PROPERTY_VALUE,txt_nickName.getText().toString());
        ActivityUtil.startActivity(this, PropertiesSettingActivity.class, typeValue, valueValue);
    }

    @Event(R.id.layout_gender)
    private void modifyGender(View view){
        BasicNameValuePair typeValue = new BasicNameValuePair(Constants.PROPERTY_TYPE,String.valueOf(R.id.txt_gender));
        BasicNameValuePair valueValue = new BasicNameValuePair(Constants.PROPERTY_VALUE,txt_gender.getText().toString());
        ActivityUtil.startActivity(this, PropertiesSettingActivity.class, typeValue, valueValue);
    }

    @Event(R.id.layout_phone)
    private void modifyPhone(View view){
        BasicNameValuePair typeValue = new BasicNameValuePair(Constants.PROPERTY_TYPE,String.valueOf(R.id.txt_phoneNo));
        BasicNameValuePair valueValue = new BasicNameValuePair(Constants.PROPERTY_VALUE,txt_phoneNo.getText().toString());
        ActivityUtil.startActivity(this, PropertiesSettingActivity.class, typeValue, valueValue);
    }


    @Event(R.id.img_rememberPwd)
    private void remenberPwd(View view){
        boolean rememberPwd = (boolean) SpfUtil.get(getApplicationContext(),Constants.REMEMBER_PWD,true);
        SpfUtil.put(getApplicationContext(), Constants.REMEMBER_PWD, !rememberPwd);
        if(rememberPwd){
            img_rememberPwd.setImageResource(R.mipmap.switch_off);
            SpfUtil.put(getApplicationContext(), Constants.AUTO_LOGIN, false);
            img_autoLogin.setImageResource(R.mipmap.switch_off);
        }else{
            img_rememberPwd.setImageResource(R.mipmap.switch_on);
        }
    }

    @Event(R.id.img_autoLogin)
    private void autoLogin(View view){
        boolean autoLogin = (boolean) SpfUtil.get(getApplicationContext(),Constants.AUTO_LOGIN,true);
        SpfUtil.put(getApplicationContext(), Constants.AUTO_LOGIN, !autoLogin);
        if(autoLogin){
            img_autoLogin.setImageResource(R.mipmap.switch_off);
        }else{
            SpfUtil.put(getApplicationContext(), Constants.REMEMBER_PWD, true);
            img_autoLogin.setImageResource(R.mipmap.switch_on);
            img_rememberPwd.setImageResource(R.mipmap.switch_on);
        }
    }

    @Event(R.id.img_back)
    private void imgBack(View view) {
        toMe();
    }
    @Event(R.id.txt_left)
    private void txtBack(View view){
        toMe();
    }

    private void toMe() {
        ActivityUtil.finish(this);
    }

    @Event(R.id.logoutBtn)
    private void clickLogout(View view){
        WarnTipDialog tipdialog = new WarnTipDialog(this,"您确定要退出吗？");
        tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logout();
            }
        });
        tipdialog.show();
    }

    private void logout(){
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
//    @Override
//    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
//        switch (accessId) {
//            case R.id.logoutBtn:
//                if(obj != null && obj.length > 0) {
//                    JSONObject jsonRoot = (JSONObject)obj[0];
//                    try {
//                        int status = jsonRoot.getInt("status");
//                        if(status == 0) {
//                            SpfUtil.remove(getApplicationContext(), Constants.USER_INFO);
//                            Intent intent = new Intent(this, LoginActivity.class);
//                            intent.putExtra(Constants.AUTO_LOGIN,false);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            this.startActivity(intent);
//                        } else {
//                        }
//                    } catch (JSONException e) {
//                        Log.e(TAG, "回调解析失败", e);
//                        e.printStackTrace();
//                    }
//                }
//                break;
//            case R.id.txt_jujuNo:
//                if(obj != null && obj.length > 0) {
//                    JSONObject jsonRoot = (JSONObject)obj[0];
//                    try {
//                        int status = jsonRoot.getInt("status");
//                        if(status == 0) {
//                            JSONObject userJson = jsonRoot.getJSONObject("user");
//                            //   "userNo":"100000001","nickName":"别名-1","userPhone":"13800000001","birthday":1451889752445,"gender":1,"createTime":1451889752445}
//                            User userInfo = new User();
//                            userInfo.setUserNo(userJson.getString("userNo"));
//                            userInfo.setNickName(userJson.getString("nickName"));
//                            userInfo.setUserPhone(userJson.getString("userPhone"));
//                            userInfo.setGender(userJson.getInt("gender"));
//
//                            txt_gender.setText(userInfo.getGender() == 0 ? "女" : "男");
//                            txt_jujuNo.setText(userInfo.getUserNo());
//                            txt_phoneNo.setText(userInfo.getUserPhone());
//                            txt_nickName.setText(userInfo.getNickName());
//                            JujuDbUtils.saveOrUpdate(userInfo);
//                            if(userNo == null) {
//                                SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
//                            }else{
//                                txt_title.setText(userInfo.getNickName());
//                            }
//
//                        } else {
//                        }
//                    } catch (JSONException e) {
//                        Log.e(TAG, "回调解析失败", e);
//                        e.printStackTrace();
//                    }
//                }
//                break;
//            case R.id.txt_property:
//                if(obj != null && obj.length > 0) {
//                    JSONObject jsonRoot = (JSONObject)obj[0];
//                    try {
//                        int status = jsonRoot.getInt("status");
//                        if(status == 0) {
//                            String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
//                            User userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
//                            userInfo.setUpdate(false);
//                            JujuDbUtils.saveOrUpdate(userInfo);
//                            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
//                        } else {
//                            Log.e(TAG,"return status code:"+status);
//                        }
//                    } catch (JSONException e) {
//                        Log.e(TAG, "回调解析失败", e);
//                        e.printStackTrace();
//                    }
//                }
//                break;
//
//        }
//    }
//
//    @Override
//    public void onFailure(HttpException error, String msg, int accessId) {
//        System.out.println("accessId:" + accessId + "\r\n msg:" + msg + "\r\n code:" +
//                error.getExceptionCode());
//    }

    @Override
    public void onSuccess(Object obj, int accessId) {
        switch (accessId) {
            case R.id.logoutBtn:
                if(obj != null) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            SpfUtil.remove(getApplicationContext(), Constants.USER_INFO);
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.putExtra(Constants.AUTO_LOGIN,false);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            this.startActivity(intent);
                        } else {
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.txt_jujuNo:
                if(obj != null) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            JSONObject userJson = jsonRoot.getJSONObject("user");
                            //   "userNo":"100000001","nickName":"别名-1","userPhone":"13800000001","birthday":1451889752445,"gender":1,"createTime":1451889752445}
                            User userInfo = new User();
                            userInfo.setUserNo(userJson.getString("userNo"));
                            userInfo.setNickName(userJson.getString("nickName"));
                            userInfo.setUserPhone(userJson.getString("userPhone"));
                            userInfo.setGender(userJson.getInt("gender"));

                            txt_gender.setText(userInfo.getGender() == 0 ? "女" : "男");
                            txt_jujuNo.setText(userInfo.getUserNo());
                            txt_phoneNo.setText(userInfo.getUserPhone());
                            txt_nickName.setText(userInfo.getNickName());
                            JujuDbUtils.saveOrUpdate(userInfo);
                            if(userNo == null) {
                                SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
                            }else{
                                txt_title.setText(userInfo.getNickName());
                            }

                        } else {
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "回调解析失败", e);
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.txt_property:
                if(obj != null) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
                            User userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
                            userInfo.setUpdate(false);
                            JujuDbUtils.saveOrUpdate(userInfo);
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
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId) {
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
