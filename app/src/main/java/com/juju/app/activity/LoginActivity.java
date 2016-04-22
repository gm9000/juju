package com.juju.app.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.user.RegistActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.MD5Util;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.ClearEditText;
import com.juju.app.view.RoundImageView;
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

@ContentView(R.layout.activity_login)
@CreateUI(isLoadData = true, isInitView = true)
public class LoginActivity extends BaseActivity implements HttpCallBack, CreateUIHelper {

    private final String TAG = getClass().getName();

    /**
    *******************************************Activity组件***************************************
     */
    @ViewInject(R.id.userNoTxt)
    private ClearEditText txt_userNo;

    private String jujuNo;
    private String token;

    @ViewInject(R.id.passwordTxt)
    private ClearEditText txt_password;

    @ViewInject(R.id.portrait)
    private RoundImageView portrait;

    @ViewInject(R.id.nickName)
    private TextView nickName;


//    @ViewInject(R.id.rememberPwdChk)
//    private CheckBox chk_rememberPwd;
//
//    @ViewInject(R.id.autoLoginChk)
//    private CheckBox chk_autoLogin;

    @ViewInject(R.id.login_main)
    private RelativeLayout layout_login_main;

    @ViewInject(R.id.loginBtn)
    private Button btn_login;

    @ViewInject(R.id.txt_regist_newuser)
    private TextView txt_regist_newuser;

    @ViewInject(R.id.txt_foreget_psw)
    private TextView txt_foreget_psw;



    /**
     *******************************************全局属性******************************************
     */
    private String userNo;
    private String pwd;

    private IMService iMService;



    /**
     *******************************************公共函数******************************************
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListeners();
    }

    @Override
    public void loadData() {
        Map<Object, View> viewMap = new HashMap<Object, View>();
//        pwdChecked = (boolean) SpfUtil.get(LoginActivity.this, "pwdChecked", false);
//        autoLoginChecked = (boolean) SpfUtil.get(LoginActivity.this,
//                "autoLoginChecked", false);
        userNo = (String)SpfUtil.get(LoginActivity.this, "userNo", "");
        pwd = (String)SpfUtil.get(LoginActivity.this, "pwd", "");
//        if(pwdChecked) {
//            pwd = (String)SpfUtil.get(LoginActivity.this, "pwd", "");
//        }


    }

    @Override
    public void initView() {
        txt_userNo.setText(userNo);
        txt_password.setText(pwd);
//        chk_rememberPwd.setChecked(pwdChecked);
//        chk_autoLogin.setChecked(autoLoginChecked);
        setLoginBtnBackgroud();
        layout_login_main.setFocusableInTouchMode(true);

        if(BaseApplication.getInstance().getUserInfoBean().getJujuNo()!=null){
            BitmapUtilFactory.getInstance(this).display(portrait, HttpConstants.getUserUrl() + "/getPortraitSmall?targetNo=" + BaseApplication.getInstance().getUserInfoBean().getJujuNo());
        }

        if(BaseApplication.getInstance().getUserInfoBean().getUserName()!=null){
            nickName.setText(BaseApplication.getInstance().getUserInfoBean().getUserName());
        }



//        bindXMPPService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        completeLoadingCommon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindXMPPService();
        Log.d(TAG, "onDestroy");

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }


    /**
     *******************************************事件函数******************************************
     */
    @OnClick(R.id.loginBtn)
    private void onClickBtnLogin(Button btn) {
        Log.d(TAG, "threadId" + Thread.currentThread().getId());
        if(GlobalVariable.isSkipLogin()){
            startActivity(LoginActivity.this, MainActivity.class);
            return;
        }
        initGlobalVariable();
        boolean vsBool = validateLogin();
        if(vsBool) {
            loadingCommon(R.string.login_progress_signing_in);
            String password = MD5Util.MD5(pwd);
            Map<String, Object> valueMap = new HashMap<String, Object>();
            valueMap.put("userNo", userNo);
            valueMap.put("password", password);
            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                    R.id.loginBtn, HttpConstants.getUserUrl() + "/login", this, valueMap,
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

    @OnClick(R.id.txt_regist_newuser)
    private void onClick4RegistNewUser(View view) {
        startActivity(LoginActivity.this, RegistActivity.class);
    }


    /**
     *******************************************回调函数******************************************
     */
    @Override
    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
        switch (accessId) {
            case R.id.loginBtn:
                if(obj != null && obj.length > 0) {
                JSONObject jsonRoot = (JSONObject)obj[0];
                try {
                    int status = jsonRoot.getInt("status");
                    String description = "";
                    if(jsonRoot.has("description")) {
                        description = jsonRoot.getString("description");
                    }
                    jujuNo = jsonRoot.getString("userNo");
                    token = jsonRoot.getString("token");
                    if(status == 0) {
                        saveUserInfo();
                        startActivity(LoginActivity.this, MainActivity.class);
                        //登陆聊天服务
                        IMLoginManager.instance().login();
                    } else {
                        final int resId = getResValue(description);
                        if(resId > 0) {
                            showMsgDialog(resId);
                        } else {
                            showMsgDialog(R.string.error_login_psw);
                        }
                        clearUserInfo(true, true);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "回调解析失败", e);
                    completeLoadingCommon();
                    showMsgDialog(R.string.error_login_psw);
                }
            }
                break;
        }
    }

    @Override
    public void onFailure(HttpException error, String msg, int accessId) {
        System.out.println("accessId:" + accessId + "\r\n msg:" + msg + "\r\n code:" +
                error.getExceptionCode());
        completeLoadingCommon();
        showMsgDialog(R.string.error_login_psw);
    }


    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            iMService = ((IMService.IMServiceBinder) service).getService();
//            iMService.getLoginManager().onStartIMManager(BaseApplication.getInstance());
//            Log.d(TAG, "开始连接XMPP服务器"+iMService.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
//            mXxService.unRegisterConnectionStatusCallback();
//            iMService = null;
//            Log.d(TAG, "断开XMPP服务器"+iMService.toString());
        }

    };

    /**
     *******************************************私有函数******************************************
     */
    private void setListeners() {
        txt_userNo.addTextChangedListener(new TextChange());
        txt_password.addTextChangedListener(new TextChange());
    }

    /**
     * 初始化全局变量
     */
    private void initGlobalVariable() {
        userNo = txt_userNo.getText().toString();
        pwd = txt_password.getText().toString();
//        pwdChecked = chk_rememberPwd.isChecked();
//        autoLoginChecked = chk_autoLogin.isChecked();
    }

    /**
     * 验证登陆
     * @return
     */
    private boolean validateLogin() {

        int a = Integer.MAX_VALUE >> 2;
        int height = View.MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2, View.MeasureSpec.AT_MOST);


        if(userNo.trim().equals("")){
            txt_userNo.setShakeAnimation();
            ToastUtil.TextIntToast(getApplicationContext(), R.string.user_no_null, 0);
            txt_userNo.requestFocus();
            return false;
        }
        if(pwd.trim().equals("")){
            txt_password.setShakeAnimation();
            ToastUtil.TextIntToast(getApplicationContext(), R.string.password_no_null, 0);
            txt_password.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * 保存用户信息
     */
    private void saveUserInfo() {
        //将登录信息保存到SharedPreferences
        SpfUtil.put(this, "userNo", userNo);
        SpfUtil.put(this, "pwd", pwd);

        User loginUser = null;
        try {
            loginUser = JujuDbUtils.getInstance(this).findFirst(Selector.from(User.class).where("userNo","=",userNo));
        } catch (DbException e) {
            e.printStackTrace();
        }

        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        userInfoBean.setJujuNo(jujuNo);
        userInfoBean.setToken(token);
        userInfoBean.setmAccount(userNo);
        userInfoBean.setmPassword(pwd);

        if( loginUser == null){
            SpfUtil.remove(getApplicationContext(), Constants.USER_INFO);
        }else{
            userInfoBean.setUserName(loginUser.getNickName());
            userInfoBean.setGender(loginUser.getGender());
            userInfoBean.setPhone(loginUser.getUserPhone());

            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, JacksonUtil.turnObj2String(userInfoBean));
        }





//        SpfUtil.put(this, "pwdChecked", pwdChecked);
//        SpfUtil.put(this, "autoLoginChecked", autoLoginChecked);
//        if(pwdChecked) {
//            SpfUtil.put(this, "pwd", pwd);
//        } else {
//            SpfUtil.put(this, "pwd", "");
//        }
    }

    /**
     * 清理用户信息
     */
    private void clearUserInfo(boolean clearUserNo, boolean clearPwd) {
        if(clearUserNo) {
            SpfUtil.remove(this, "userNo");
            txt_userNo.setText("");
        }
        if(clearPwd) {
            SpfUtil.remove(this, "pwd");
            txt_password.setText("");
        }
        SpfUtil.remove(this, "pwdChecked");
        SpfUtil.remove(this, "autoLoginChecked");
//        chk_rememberPwd.setChecked(false);
//        chk_autoLogin.setChecked(false);
        //获取焦点
        if(clearUserNo) {
            txt_userNo.setFocusable(true);
            txt_userNo.requestFocus();
        } else {
            txt_password.setFocusable(true);
            txt_password.requestFocus();
        }
    }



    private void setLoginBtnBackgroud() {
        boolean sign1 = txt_userNo.getText().length() > 5;
        boolean sign2 = txt_password.getText().length() > 5;
        if (sign1 && sign2) {
            btn_login.setEnabled(true);
        } else {
            btn_login.setEnabled(false);
        }
    }

    private void bindXMPPService() {
        Intent mServiceIntent = new Intent(this, IMService.class);
        mServiceIntent.setAction("com.juju.app.service.XMMP");
        bindService(mServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);

    }

    private void unbindXMPPService() {
//        try {
//            unbindService(mServiceConnection);
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        }
    }

    private void unBindService() {

    }


    /**
     *******************************************内部类******************************************
     */
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
            setLoginBtnBackgroud();
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onMessageEvent(String event){
//        System.out.println("线程ID=" + Thread.currentThread().getId());
//    }



}
