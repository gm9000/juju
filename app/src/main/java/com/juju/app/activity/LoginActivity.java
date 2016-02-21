package com.juju.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewStructure;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.json.LoginReqBean;
import com.juju.app.bean.json.LoginResBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.MD5Util;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.ClearEditText;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ContentView(R.layout.activity_login)
@CreateUI(isLoadData = true, isInitView = true)
public class LoginActivity extends BaseActivity implements HttpCallBack, CreateUIHelper {

    /**
     *******************************************Activity组件***************************************
     */
    @ViewInject(R.id.userNoTxt)
    private ClearEditText txt_userNo;

    @ViewInject(R.id.passwordTxt)
    private ClearEditText txt_password;

    @ViewInject(R.id.userRegistTxt)
    private TextView txt_userRegist;

    @ViewInject(R.id.login_main)
    private LinearLayout layout_login_main;

    @ViewInject(R.id.rememberPwdChk)
    private CheckBox chk_rememberPwd;

    @ViewInject(R.id.autoLoginChk)
    private CheckBox chk_autoLogin;

    /**
     *******************************************全局属性******************************************
     */
    private String userNo;
    private String pwd;
    private boolean pwdChecked;
    private boolean autoLoginChecked;


    /**
     *******************************************函数部分******************************************
     */
    @OnClick(R.id.loginBtn)
    private void userLogin(Button btn) {
        initGlobalVariable();
        boolean vsBool = validateLogin();
        if(vsBool) {
            String password = MD5Util.MD5(pwd);
            Map<String, Object> valueMap = new HashMap<String, Object>();
            valueMap.put("userNo", userNo);
            valueMap.put("password", password);
            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(0,
                    HttpConstants.getUserUrl() + "/login", this, valueMap, LoginResBean.class);

//            LoginReqBean reqBean = new LoginReqBean(userNo, password);
//            JlmHttpClient<LoginReqBean> client = new JlmHttpClient<LoginReqBean>(0,
//                    HttpConstants.getUserUrl() + "/login", this, reqBean, LoginResBean.class);
            try {
                client.sendPost();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
        switch (accessId) {
            case 0:
                System.out.println(responseInfo.result);
                if(obj != null && obj.length > 0) {
                    LoginResBean loginBean = (LoginResBean)obj[0];
                    System.out.println("loginBean state:"+loginBean.getStatus());
                }
                saveUserInfo();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onFailure(HttpException error, String msg, int accessId) {
        System.out.println("accessId:"+accessId+"\r\n msg:"+msg+"\r\n code:"+
                error.getExceptionCode());
    }

    @Override
    public void loadData() {
        Map<Object, View> viewMap = new HashMap<Object, View>();
        pwdChecked = (boolean) SpfUtil.get(LoginActivity.this, "pwdChecked", false);
        autoLoginChecked = (boolean) SpfUtil.get(LoginActivity.this,
                "autoLoginChecked", false);
        userNo = (String)SpfUtil.get(LoginActivity.this, "userNo", "");

        if(pwdChecked) {
            pwd = (String)SpfUtil.get(LoginActivity.this, "pwd", "");
        }
    }

    @Override
    public void initView() {
        txt_userNo.setText(userNo);
        txt_password.setText(pwd);
        chk_rememberPwd.setChecked(pwdChecked);
        chk_autoLogin.setChecked(autoLoginChecked);
    }


    /**
     * 初始化全局变量
     */
    private void initGlobalVariable() {
        userNo = txt_userNo.getText().toString();
        pwd = txt_password.getText().toString();
        pwdChecked = chk_rememberPwd.isChecked();
        autoLoginChecked = chk_autoLogin.isChecked();
    }

    /**
     * 验证登陆
     * @return
     */
    private boolean validateLogin() {
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
        SpfUtil.put(this, "pwdChecked", pwdChecked);
        SpfUtil.put(this, "autoLoginChecked", autoLoginChecked);
        if(pwdChecked) {
            SpfUtil.put(this, "pwd", pwd);
        } else {
            SpfUtil.put(this, "pwd", "");
        }
    }

}
