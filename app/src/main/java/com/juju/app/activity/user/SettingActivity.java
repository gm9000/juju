package com.juju.app.activity.user;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.LoginActivity;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.UserInfoChangeEvent;
import com.juju.app.event.notify.LiveEnterNotifyEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.dialog.WarnTipDialog;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.juju.app.event.UserInfoChangeEvent.Type;

@ContentView(R.layout.activity_setting)
public class SettingActivity extends BaseActivity implements HttpCallBack {

    private static final String TAG = "SettingActivity";

    private final int UPDATE_PHOTO_ACTIVITY = 1;

    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.img_right)
    private ImageView img_right;


    @ViewInject(R.id.head)
    private CircleImageView headImg;


    @ViewInject(R.id.layout_phone)
    private LinearLayout layoutPhone;
    @ViewInject(R.id.txt_phoneNo)
    private TextView txt_phoneNo;
    @ViewInject(R.id.layout_nick_name)
    private LinearLayout layoutNickName;
    @ViewInject(R.id.txt_nick_name)
    private TextView txt_nickName;
    @ViewInject(R.id.txt_jujuNo)
    private TextView txt_jujuNo;
    @ViewInject(R.id.layout_gender)
    private LinearLayout layoutGender;
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

    private IMService imService;
    /**
     * IMServiceConnector
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("SettingActivity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            EventBus.getDefault().register(SettingActivity.this);
            initParam();
            initView();
            loadUserInfo();
        }

        @Override
        public void onServiceDisconnected() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imServiceConnector.connect(this);
    }

    private void initParam() {
        userNo = getIntent().getStringExtra(Constants.USER_NO);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(userNo==null) {
            loadSelfUserInfo();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        updateUserInfo();
        imServiceConnector.disconnect(this);
        EventBus.getDefault().unregister(SettingActivity.this);

    }


    private void updateUserInfo() {
        String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
        User userInfo = null;
        if(userInfoStr!=null) {
            userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
        }
        if(userInfo != null && userInfo.isUpdate()){
            Map<String, Object> valueMap = new HashMap<String, Object>();
            UserInfoBean userTokenInfoBean = AppContext.getUserInfoBean();
            valueMap.put("userNo", userTokenInfoBean.getUserNo());
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
        String targetNo = userNo==null?AppContext.getUserInfoBean().getUserNo():userNo;
        if(targetNo.equals(AppContext.getUserInfoBean().getUserNo())){
            loadSelfUserInfo();
            ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + targetNo,headImg,ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
        }else{
            imService.getContactManager().updateContact(userNo);
            loadLocalUserInfo(targetNo);
            ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + targetNo,headImg,ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
        }
    }


    private void loadSelfUserInfo() {
        String userInfoStr = (String) SpfUtil.get(getApplicationContext(), Constants.USER_INFO, null);
        User userInfo = JacksonUtil.turnString2Obj(userInfoStr, User.class);
        if(userInfo != null) {
            txt_gender.setText(userInfo.getGender() == 0 ? "女" : "男");
            txt_jujuNo.setText(userInfo.getUserNo());
            txt_phoneNo.setText(userInfo.getUserPhone());
            txt_nickName.setText(userInfo.getNickName());
        }else{
            ToastUtil.showShortToast(this,getString(R.string.load_user_info_fail),1);
        }
    }

    private void loadLocalUserInfo(String userNo) {
        User  userInfo = null;
        try {
            userInfo = JujuDbUtils.getInstance().selector(User.class).where("user_no", "=", userNo).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
        if(userInfo!=null){
            txt_gender.setText(userInfo.getGender() == 0 ? "女" : "男");
            txt_jujuNo.setText(userInfo.getUserNo());
            txt_phoneNo.setText(userInfo.getUserPhone());
            txt_nickName.setText(userInfo.getNickName());
            txt_title.setText(userInfo.getNickName());
        }else{
            ToastUtil.showShortToast(this,getString(R.string.load_user_info_fail),1);
        }
    }


    private void initView() {

        if(userNo == null) {
            txt_title.setText(R.string.settings);
            txt_left.setText(R.string.me);

            boolean rememberPwd = (boolean) SpfUtil.get(getApplicationContext(),Constants.REMEMBER_PWD,true);
            boolean autoLogin = (boolean) SpfUtil.get(getApplicationContext(),Constants.AUTO_LOGIN,true);

            if(rememberPwd){
                img_rememberPwd.setImageResource(R.mipmap.check_box);
            }else{
                img_rememberPwd.setImageResource(R.mipmap.uncheck_box);
            }

            if(autoLogin){
                img_autoLogin.setImageResource(R.mipmap.check_box);
            }else{
                img_autoLogin.setImageResource(R.mipmap.uncheck_box);
            }

        }else{
            txt_title.setText(R.string.basic_info);
            txt_left.setText(R.string.top_left_back);

            layout_setting.setVisibility(View.GONE);
            logoutBtn.setVisibility(View.GONE);

            layoutGender.setClickable(false);
            txt_gender.setCompoundDrawables(null, null, null, null);
            layoutPhone.setClickable(false);
            txt_phoneNo.setCompoundDrawables(null, null, null, null);
            layoutNickName.setClickable(false);
            txt_nickName.setCompoundDrawables(null,null,null,null);

        }
        txt_left.setVisibility(View.VISIBLE);
        img_back.setVisibility(View.VISIBLE);
        img_right.setVisibility(View.GONE);

    }



    @Event(R.id.head)
    private void showHeadImg(View view){
        startActivityForResultNew(this, UploadPhotoActivity.class,UPDATE_PHOTO_ACTIVITY,
                Constants.USER_NO, userNo);
    }

    @Event(R.id.layout_nick_name)
    private void modifyNickName(View view){
        Map<String, Object> valueMap = new HashMap<String,Object>();
        valueMap.put(Constants.PROPERTY_TYPE, String.valueOf(R.id.txt_nick_name));
        valueMap.put(Constants.PROPERTY_VALUE, txt_nickName.getText().toString());
        startActivityNew(this, PropertiesSettingActivity.class, valueMap);
    }

    @Event(R.id.layout_gender)
    private void modifyGender(View view){
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.PROPERTY_TYPE, String.valueOf(R.id.txt_gender));
        valueMap.put(Constants.PROPERTY_VALUE, txt_gender.getText().toString());
        startActivityNew(this, PropertiesSettingActivity.class, valueMap);
    }

    @Event(R.id.layout_phone)
    private void modifyPhone(View view){
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.PROPERTY_TYPE, String.valueOf(R.id.txt_phoneNo));
        valueMap.put(Constants.PROPERTY_VALUE, txt_phoneNo.getText().toString());
        startActivityNew(this, PropertiesSettingActivity.class, valueMap);
    }


    @Event(R.id.img_rememberPwd)
    private void remenberPwd(View view){
        boolean rememberPwd = (boolean) SpfUtil.get(getApplicationContext(),Constants.REMEMBER_PWD,true);
        SpfUtil.put(getApplicationContext(), Constants.REMEMBER_PWD, !rememberPwd);
        if(rememberPwd){
            img_rememberPwd.setImageResource(R.mipmap.uncheck_box);
            SpfUtil.put(getApplicationContext(), Constants.AUTO_LOGIN, false);
            img_autoLogin.setImageResource(R.mipmap.uncheck_box);
        }else{
            img_rememberPwd.setImageResource(R.mipmap.check_box);
        }
    }

    @Event(R.id.img_autoLogin)
    private void autoLogin(View view){
        boolean autoLogin = (boolean) SpfUtil.get(getApplicationContext(),Constants.AUTO_LOGIN,true);
        SpfUtil.put(getApplicationContext(), Constants.AUTO_LOGIN, !autoLogin);
        if(autoLogin){
            img_autoLogin.setImageResource(R.mipmap.uncheck_box);
        }else{
            SpfUtil.put(getApplicationContext(), Constants.REMEMBER_PWD, true);
            img_autoLogin.setImageResource(R.mipmap.check_box);
            img_rememberPwd.setImageResource(R.mipmap.check_box);
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
        UserInfoBean userInfoBean = AppContext.getUserInfoBean();
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("userNo", userInfoBean.getUserNo());
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
    public void onSuccess(Object obj, int accessId, Object inputParameter) {
        switch (accessId) {
            case R.id.logoutBtn:
                if(obj != null) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            SpfUtil.remove(getApplicationContext(), Constants.USER_INFO);
                            Intent intent = new Intent(this, LoginActivity.class);
//                            intent.putExtra(Constants.AUTO_LOGIN, false);
                            SpfUtil.put(getApplicationContext(), Constants.AUTO_LOGIN, false);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.startActivity(intent);
                            triggerEvent(LoginEvent.LOGIN_OUT);
                        } else {
                            //TODO 先这样处理
                            SpfUtil.remove(getApplicationContext(), Constants.USER_INFO);
                            Intent intent = new Intent(this, LoginActivity.class);
//                            intent.putExtra(Constants.AUTO_LOGIN, false);
                            SpfUtil.put(getApplicationContext(), Constants.AUTO_LOGIN, false);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.startActivity(intent);
                            triggerEvent(LoginEvent.LOGIN_OUT);
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
                                SpfUtil.put(getApplicationContext(), Constants.USER_INFO, JacksonUtil.turnObj2String(userInfo));
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
                            userInfo.updateVersion();
                            userInfo.setUpdate(false);
                            JujuDbUtils.saveOrUpdate(userInfo);
                            imService.getContactManager().getUserMap().put(userInfo.getUserNo(),userInfo);
                            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, JacksonUtil.turnObj2String(userInfo));
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
    public void onFailure(Throwable ex, boolean isOnCallback, int accessId, Object inputParameter) {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case UPDATE_PHOTO_ACTIVITY:
                    setImageHead(data.getData().toString());
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setImageHead(String headUrl){

        MemoryCacheUtils.removeFromCache(headUrl,ImageLoaderUtil.getImageLoaderInstance().getMemoryCache());
        DiskCacheUtils.removeFromCache(headUrl,ImageLoaderUtil.getImageLoaderInstance().getDiskCache());

        ImageLoaderUtil.getImageLoaderInstance().displayImage(headUrl,headImg,ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUserUpdate(UserInfoChangeEvent event) {
        if(!event.getUserNo().equals(userNo)){
            return;
        }

        User userInfo = imService.getContactManager().findContact(userNo);
        switch (event.getChangeType()) {
            case BASIC_INFO_CHANGE:
                txt_gender.setText(userInfo.getGender() == 0 ? "女" : "男");
                txt_jujuNo.setText(userInfo.getUserNo());
                txt_phoneNo.setText(userInfo.getUserPhone());
                txt_nickName.setText(userInfo.getNickName());
                txt_title.setText(userInfo.getNickName());
                break;
            case PORTRAIT_CHANGE:
                ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + userNo,headImg,ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
                break;
            case ALL_CHANGE:
                txt_gender.setText(userInfo.getGender() == 0 ? "女" : "男");
                txt_jujuNo.setText(userInfo.getUserNo());
                txt_phoneNo.setText(userInfo.getUserPhone());
                txt_nickName.setText(userInfo.getNickName());
                txt_title.setText(userInfo.getNickName());
                ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + userNo,headImg,ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
                break;
        }
    }

}
