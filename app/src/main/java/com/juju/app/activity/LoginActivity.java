package com.juju.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.activity.user.RegistActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.annotation.SystemColor;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.UnreadEvent;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
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
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.DBUtil;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
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
    @ViewInject(R.id.txt_loginId)
    private ClearEditText txt_loginId;

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
    private String loginId;   //手机号或聚聚号
    private String pwd;
    private String nickName;
    private boolean autoLogin = true;
    private boolean loginSuccess = false;
    private String userNo;
    private String token;
    DaoSupport userDao;

    private UserInfoBean userInfoBean;



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
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        loginId = (String)SpfUtil.get(LoginActivity.this, "loginId", "");
        userNo = (String)SpfUtil.get(LoginActivity.this, "userNo", "");
        pwd = (String)SpfUtil.get(LoginActivity.this, "pwd", "");
        nickName = (String)SpfUtil.get(LoginActivity.this, "nickName", "");
        token  = (String)SpfUtil.get(LoginActivity.this, "token", "");
        userDao = new UserDaoImpl(getApplicationContext());
        imServiceConnector.connect(LoginActivity.this);
    }

    @Override
    public void initView() {
        txt_loginId.setText(loginId);
        boolean rememberPwd = (boolean) SpfUtil.get(getApplicationContext(),Constants.REMEMBER_PWD, true);
        if(rememberPwd) {
            txt_password.setText(pwd);
        }
        setLoginBtnBackgroud();
        layout_login_main.setFocusableInTouchMode(true);

        if(StringUtils.isNotBlank(BaseApplication.getInstance().getUserInfoBean().getUserNo())){
            BitmapUtilFactory.getInstance(this).bind(portrait, HttpConstants.getUserUrl() +
                            "/getPortraitSmall?targetNo=" + BaseApplication.getInstance().getUserInfoBean().getUserNo(),
                    BitmapUtilFactory.Option.imageOptions());
        }

        if(BaseApplication.getInstance().getUserInfoBean().getNickName() != null){
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
        hideInput(LoginActivity.this, btn);
        Log.d(TAG, "threadId" + Thread.currentThread().getId());
        initGlobalVariable();
        boolean vsBool = validateLogin();

        if(vsBool) {
            loading(R.string.login_progress_signing_in);
            String password = MD5Util.MD5(pwd);
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("userNo", loginId);
            valueMap.put("password", password);
            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(
                    R.id.loginBtn, HttpConstants.getUserUrl() + "/login", this, valueMap,
                    JSONObject.class);
            try {
                client.sendPost4OK();
            } catch (UnsupportedEncodingException e) {
                logger.error(e);
            } catch (JSONException e) {
                logger.error(e);
            }
        }
    }

    @Event(value = R.id.txt_regist_newuser, type = View.OnClickListener.class)
    private void onClick4RegistNewUser(View view) {
        startActivityNew(LoginActivity.this, RegistActivity.class);
    }



    @Event(value = R.id.login_main)
    private void onClick4LoginMain(View view) {
        hideInput(view.getContext(), view);
    }

    /**
     *******************************************回调函数******************************************
     */
    @Override
    public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
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
                    final String description = JSONUtils.getString(jsonRoot, "desc", "");
                    if(status == 0) {
                        //TODO 登陆协议需返回用户昵称、域名、房间名称、MUC服务名称
                        userNo = JSONUtils.getString(jsonRoot, "userNo", "");
                        token = JSONUtils.getString(jsonRoot, "token", "");
                        nickName = "";
                        //初始化DB（针对http服务登陆）
                        DBUtil.instance().initDBHelp(getApplicationContext(), userNo);
                        LoginActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imService.getLoginManager().initDaoAndService();
                                //TODO 是否需要消息服务器登陆成功后再确认？目前以业务服务登陆为准
                                triggerEvent(LoginEvent.LOGIN_BSERVER_OK);
                            }
                        });
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
                logger.d("####################LOCAL_LOGIN_SUCCESS################");
                loginSuccess = true;
                saveUserInfo();
                startActivityNew(LoginActivity.this, MainActivity.class);
                if(!imService.getLoginManager().isAuthenticated()) {
                    imService.getLoginManager().login();
                }
                //TODO 本地登陆成功，有可能没有加入群组
                else {
                    triggerEvent4Sticky(new UnreadEvent(UnreadEvent.Event.UNREAD_MSG_LIST_OK));
                }
                finish(LoginActivity.this);
                break;
            case LOGIN_BSERVER_OK:
                logger.d("####################LOGIN_BSERVER_OK################");
                loginSuccess = true;
                saveUserInfo();
//                imService.getLoginManager().setUserNo(userNo);
                startActivityNew(LoginActivity.this, MainActivity.class);
                //登陆聊天服务
                if(imService != null) {
                    imService.getLoginManager().login();
                }
                finish(LoginActivity.this);
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
        txt_loginId.addTextChangedListener(new TextChange());
        txt_password.addTextChangedListener(new TextChange());

        txt_loginId.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //离开焦点
                if(!hasFocus) {
                    hideInput(LoginActivity.this, v);
                }
            }
        });
    }

    /**
     * 初始化全局变量
     */
    private void initGlobalVariable() {
        loginId = txt_loginId.getText().toString();
        pwd = txt_password.getText().toString();
//        pwdChecked =
//        autoLoginChecked = chk_autoLogin.isChecked();
    }

    /**
     * 验证登陆
     * @return
     */
    private boolean validateLogin() {
        if(loginId.trim().equals("")){
            txt_loginId.setShakeAnimation();
            ToastUtil.TextIntToast(getApplicationContext(), R.string.user_no_null, 0);
            txt_loginId.requestFocus();
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
        SpfUtil.put(this, "loginId", loginId);
        SpfUtil.put(this, "userNo", userNo);
        SpfUtil.put(this, "pwd", pwd);
        SpfUtil.put(this, "nickName", nickName);
        SpfUtil.put(this, "token", token);
        initUserInfoBean();
        HttpReqParamUtil.instance().setUserInfoBean(userInfoBean);
//        User loginUser = (User) userDao.findUniByProperty4Or("user_no,user_phone", userNo, userNo);
//        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
//        userInfoBean.setUserNo(loginUser.getUserNo());
//        userInfoBean.setToken(token);
//        userInfoBean.setmAccount(loginUser.getUserPhone());
//        userInfoBean.setmPassword(pwd);
//        HttpReqParamUtil.instance().setUserInfoBean(userInfoBean);
//
//        if( loginUser == null){
//            SpfUtil.remove(getApplicationContext(), Constants.USER_INFO);
//        }else{
//            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, JacksonUtil.turnObj2String(loginUser));
//            //需要调整
//            SpfUtil.put(this, "nickName", loginUser.getNickName());
//        }

    }

    /**
     * 清理用户信息
     */
    private void clearUserInfo(boolean clearUserNo, boolean clearPwd) {
        if(clearUserNo) {
            SpfUtil.remove(this, "loginId");
            txt_loginId.setText("");
        }
        if(clearPwd) {
            SpfUtil.remove(this, "pwd");
            txt_password.setText("");
        }
        //获取焦点
        if(clearUserNo) {
            txt_loginId.setFocusable(true);
            txt_loginId.requestFocus();
        } else {
            txt_password.setFocusable(true);
            txt_password.requestFocus();
        }
    }



    private void setLoginBtnBackgroud() {
        boolean sign1 = txt_loginId.getText().length() > 5;
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
//                ToastUtil.TextIntToast(getApplicationContext(), R.string.system_service_error, 0);
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
                } else {
                    //初始化DB（针对本地登陆）
                    DBUtil.instance().initDBHelp(getApplicationContext(), userNo);
                    imService.getLoginManager().initDaoAndService();
                    imService.getLoginManager().autoLogin(userNo, pwd);
                }
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

    private void initUserInfoBean() {
        userInfoBean.setUserNo(userNo);
        userInfoBean.setToken(token);
        userInfoBean.setmPassword(pwd);
    }

    /**
     * 强制隐藏输入法键盘
     */
    private void hideInput(Context context, View view){
        InputMethodManager inputMethodManager =
                (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
