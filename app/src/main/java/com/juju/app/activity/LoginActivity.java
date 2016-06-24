package com.juju.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.activity.user.RegistActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.annotation.SystemColor;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.bean.json.PartyReqBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.event.JoinChatRoomEvent;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.UnreadEvent;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.Logger;
import com.juju.app.utils.MD5Util;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.view.ClearEditText;
import com.juju.app.view.RoundImageView;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


@ContentView(R.layout.activity_login)
@CreateUI(isLoadData = true, isInitView = true)
@SystemColor(isApply = false)
public class LoginActivity extends BaseActivity implements CreateUIHelper, HttpCallBack4OK {

    private Logger logger = Logger.getLogger(LoginActivity.class);

    private final String TAG = getClass().getName();

    private Handler uiHandler = new Handler();


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
    private TextView tv_nickName;

    @ViewInject(R.id.login_main)
    private RelativeLayout layout_login_main;

    @ViewInject(R.id.loginBtn)
    private Button btn_login;

   @ViewInject(R.id.splash_main)
    private View splash_main;

    @ViewInject(R.id.login_main)
    private View login_main;

    private IMService imService;




    /**
     *******************************************全局属性******************************************
     */
    private String userNo;
    private String pwd;
    private String nickName;
    private boolean autoLogin = true;
    private boolean loginSuccess = false;




    /**
     *******************************************公共函数******************************************
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        super.onCreate(savedInstanceState);
        setListeners();
    }

    @Override
    public void loadData() {
        HttpConstants.initURL();
        imServiceConnector.connect(LoginActivity.this);
        userNo = (String)SpfUtil.get(LoginActivity.this, "userNo", "");
        pwd = (String)SpfUtil.get(LoginActivity.this, "pwd", "");
        nickName = (String)SpfUtil.get(LoginActivity.this, "pwd", "");
    }

    @Override
    public void initView() {
        txt_userNo.setText(userNo);
        boolean rememberPwd = (boolean) SpfUtil.get(getApplicationContext(),Constants.REMEMBER_PWD, true);
        if(rememberPwd) {
            txt_password.setText(pwd);
        }
        setLoginBtnBackgroud();
        layout_login_main.setFocusableInTouchMode(true);

        if(BaseApplication.getInstance().getUserInfoBean().getJujuNo()!=null){
            BitmapUtilFactory.getInstance(this).bind(portrait, HttpConstants.getUserUrl() +
                    "/getPortraitSmall?targetNo=" + BaseApplication.getInstance().getUserInfoBean().getJujuNo(),
                    BitmapUtilFactory.Option.imageOptions());
        }

        if(BaseApplication.getInstance().getUserInfoBean().getUserName() != null){
            tv_nickName.setText(nickName);
        }

        //初始化自动登陆信息
        initAutoLogin();


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
        completeLoading();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        imServiceConnector.disconnect(LoginActivity.this);
        super.onDestroy();
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
    @Event(value = R.id.loginBtn, type = View.OnClickListener.class)
    private void onClickBtnLogin(Button btn) {
        Log.d(TAG, "threadId" + Thread.currentThread().getId());
        if(GlobalVariable.isSkipLogin()){
            startActivity(LoginActivity.this, MainActivity.class);
            return;
        }
        initGlobalVariable();
        boolean vsBool = validateLogin();

        if(vsBool) {
//            loadingCommon(R.string.login_progress_signing_in);
            loading(R.string.login_progress_signing_in);
            String password = MD5Util.MD5(pwd);
            Map<String, Object> valueMap = new HashMap<String, Object>();
            valueMap.put("userNo", userNo);
            valueMap.put("password", password);
            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                    R.id.loginBtn, HttpConstants.getUserUrl() + "/login", this, valueMap,
                    JSONObject.class);
            try {
                Log.d(TAG, "主线程:"+Thread.currentThread().getName());
                client.sendPost4OK();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Event(value = R.id.txt_regist_newuser, type = View.OnClickListener.class)
    private void onClick4RegistNewUser(View view) {
        startActivity(LoginActivity.this, RegistActivity.class);
    }


    /**
     *******************************************回调函数******************************************
     */

    @Override
    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
        Log.d(TAG, "回调线程:" + Thread.currentThread().getName());
        switch (accessId) {
            case R.id.loginBtn:
                if(obj != null) {
                    LoginActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            completeLoading();
                        }
                    });
                    JSONObject jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    final String description = JSONUtils.getString(jsonRoot, "description", "");
                    jujuNo = JSONUtils.getString(jsonRoot, "userNo", "");
                    token = JSONUtils.getString(jsonRoot, "token", "");
                    //TODO 登陆协议需返回用户昵称、域名、房间名称、MUC服务名称
                    nickName = "聚龙小子";
                    if(status == 0) {

                        UserInfoBean userTokenInfoBean = BaseApplication.getInstance().getUserInfoBean();
                        userTokenInfoBean.setJujuNo(jujuNo);
                        userTokenInfoBean.setToken(token);

                        //TODO 是否需要消息服务器登陆成功后再确认？目前以业务服务登陆为准
                        triggerEvent(LoginEvent.LOGIN_BSERVER_OK);
                    } else {
                        LoginActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final int resId = getResValue(description);
                                if (resId > 0) {
                                    showMsgDialog(resId);
                                } else {
                                    showMsgDialog(R.string.error_login_psw);
                                }
                                clearUserInfo(true, true);

                            }
                        });
                    }
                }
                break;
        }
    }

    @Override
    public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
        LoginActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLoginPage();
                completeLoading();
                showMsgDialog(R.string.error_login_psw);
            }
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4Login(LoginEvent event) {
        switch (event) {
            case LOCAL_LOGIN_SUCCESS:
                loginSuccess = true;
                saveUserInfo();
                startActivity(LoginActivity.this, MainActivity.class);
                if(!imService.getLoginManager().isAuthenticated()) {
                    imService.getLoginManager().login();
                }
                //TODO 本地登陆成功，有可能没有加入群组
                else {
                    triggerEvent4Sticky(new UnreadEvent(UnreadEvent.Event.UNREAD_MSG_LIST_OK));
                }
                break;
            case LOGIN_BSERVER_OK:
                loginSuccess = true;
                saveUserInfo();
                startActivity(LoginActivity.this, MainActivity.class);
                //登陆聊天服务
                if(imService != null) {
                    imService.getLoginManager().login();
                }
                break;
            case LOGIN_AUTH_FAILED:
            case LOGIN_INNER_FAILED:
                if (!loginSuccess)
                    onLoginFailure(event);
                break;
        }
    }



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
        SpfUtil.put(this, "nickName", nickName);


        //TODO 登陆成功是否需要将用户插入本地数据库
        User loginUser = null;
        try {
            loginUser = JujuDbUtils.getInstance().selector(User.class).where("user_no","=",userNo).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        userInfoBean.setJujuNo(userNo);
        userInfoBean.setToken(token);
        userInfoBean.setmAccount(userNo);
        userInfoBean.setmPassword(pwd);

        if( loginUser == null){
            SpfUtil.remove(getApplicationContext(), Constants.USER_INFO);
        }else{
            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, loginUser);
            //需要调整
            SpfUtil.put(this, "nickName", loginUser.getNickName());
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

    private void showLoginPage() {
        splash_main.setVisibility(View.GONE);
        login_main.setVisibility(View.VISIBLE);
    }

    private void onLoginFailure(LoginEvent event) {
        logger.e("login#onLoginError -> errorCode:%s", event.name());
        showLoginPage();
        String errorTip = getString(IMUIHelper.getLoginErrorTip(event));
        logger.d("login#errorTip:%s", errorTip);
        completeLoading();
        Toast.makeText(this, errorTip, Toast.LENGTH_SHORT).show();
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

        /**
         * IMServiceConnector
         */
        private IMServiceConnector imServiceConnector = new IMServiceConnector() {
            @Override
            public void onIMServiceConnected() {
                logger.d("login_activity#onIMServiceConnected");
                imService = imServiceConnector.getIMService();
                try {
                    do {
                        if (imService == null) {
                            //后台服务启动链接失败
                            break;
                        }
                        IMLoginManager loginManager = imService.getLoginManager();
                        if (loginManager == null) {
                            // 无法获取登陆控制器
                            break;
                        }

                        if(StringUtils.isBlank(userNo)
                                || StringUtils.isBlank(pwd)) {
                            // 之前没有保存任何登陆相关的，跳转到登陆页面
                            break;
                        }

                        if (!autoLogin) {
                            break;
                        }
//                        loadingCommon(R.string.login_progress_signing_in);
                        imService.getLoginManager().setAutoLogin(autoLogin);
                        loading(R.string.login_progress_signing_in);
                        handleGotLoginIdentity(userNo, pwd);
                        return;
                    } while (false);
                    // 异常分支都会执行这个
                    imService.getLoginManager().setAutoLogin(false);
                    handleNoLoginIdentity();
                } catch (Exception e) {
                    logger.error(e);
                    imService.getLoginManager().setAutoLogin(false);
                    handleNoLoginIdentity();
                }

            }

            @Override
            public void onServiceDisconnected() {
//                showMsgDialog(R.string.system_service_error);
                ToastUtil.TextIntToast(getApplicationContext(), R.string.system_service_error, 0);
            }
        };


    /**
     * 自动登陆
     */
    private void handleGotLoginIdentity(final String userNo, final String pwd) {
        logger.i("login#handleGotLoginIdentity");

        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                logger.d("login#start auto login");
                if (imService == null || imService.getLoginManager() == null) {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_failed),
                            Toast.LENGTH_SHORT).show();

                    showLoginPage();
                }
                imService.getLoginManager().autoLogin(userNo, pwd);
            }
        }, 500);
    }

    /**
     * 跳转到登陆的页面
     */
    private void handleNoLoginIdentity() {
        logger.i("login#handleNoLoginIdentity");
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showLoginPage();
            }
        }, 1000);
    }

    private void initAutoLogin() {
        logger.i("login#initAutoLogin");
        autoLogin = shouldAutoLogin();
        splash_main.setVisibility(autoLogin ? View.VISIBLE : View.GONE);
        login_main.setVisibility(autoLogin ? View.GONE : View.VISIBLE);

//        if (autoLogin) {
//            Animation splashAnimation = AnimationUtils.loadAnimation(this, R.anim.login_splash);
//            if (splashAnimation == null) {
//                logger.e("login#loadAnimation login_splash failed");
//                return;
//            }
//
//            splashPage.startAnimation(splashAnimation);
//        }
    }

    // 主动退出的时候， 这个地方会有值,更具pwd来判断
    private boolean shouldAutoLogin() {
        Intent intent = getIntent();
        if (intent != null) {
            boolean autoLogin = (boolean) SpfUtil.get(getApplicationContext(), Constants.AUTO_LOGIN, true);
            logger.d("login#notAutoLogin:%s", autoLogin);
            return autoLogin;
        }
        return true;
    }
}
