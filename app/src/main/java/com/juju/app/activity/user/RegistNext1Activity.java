package com.juju.app.activity.user;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.LoginActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.SessionConstants;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.MD5Util;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

@ContentView(R.layout.activity_regist_next1)
@CreateUI(isLoadData = true, isInitView = true)
public class RegistNext1Activity extends BaseActivity implements CreateUIHelper, HttpCallBack {

    private final String TAG = getClass().getName();

    @ViewInject(R.id.img_back)
    private ImageView img_back;

    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    @ViewInject(R.id.txt_title)
    private TextView txt_title;

    @ViewInject(R.id.img_right)
    private ImageView img_right;

    @ViewInject(R.id.tv_phone)
    private TextView tv_phone;

    @ViewInject(R.id.et_yzm)
    private EditText et_yzm;

    @ViewInject(R.id.tv_djs)
    private TextView tv_djs;

    @ViewInject(R.id.btn_register_next)
    private Button btn_register_next;


    private TimeCount time;
    private int code = 1;
    private String phone;
    private String password;
    private String nickName;

    private DaoSupport userDao;
    private IMService imService;


    /**
     * IMServiceConnector
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("main_activity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
        }

        @Override
        public void onServiceDisconnected() {

        }
    };



    /**
     *******************************************公共函数******************************************
     */


    @Override
    public void loadData() {
        HttpConstants.initURL();
        imServiceConnector.connect(RegistNext1Activity.this);
        userDao = new UserDaoImpl(getApplicationContext());
        phone = getIntent().getStringExtra(Constants.PHONE);
        password = getIntent().getStringExtra(Constants.PASSWORD);
        nickName = getIntent().getStringExtra(Constants.NICKNAME);
        setListeners();
    }

    @Override
    public void initView() {
        //构造验证定时器
        time = new TimeCount(60000, 1000);//构造CountDownTimer对象
        time.start();
        String _phone = StringUtils.formatMobileNO(phone);
        tv_phone.setText(_phone);
        img_back.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.btn_register);
        txt_left.setVisibility(View.VISIBLE);
        txt_title.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);
        initSDK();
        SMSSDK.getVerificationCode("86", phone);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
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
    }

    @Override
    protected void onDestroy() {
        imServiceConnector.disconnect(RegistNext1Activity.this);
        SMSSDK.unregisterEventHandler(eh);
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }



    /**
     *******************************************事件函数******************************************
     */
    @Event(R.id.btn_register_next)
    private void onClick4BtnRegisterNext(View view) {
        String yzm = et_yzm.getText().toString();
//        SMSSDK.submitVerificationCode("86", phone, yzm);
        registUser(nickName, 1, password, phone, null);
//        registUser(nickName, 1, password, phone, null);

    }

    //结束注册
    @Event(R.id.img_back)
    private void onClickImgBack(ImageView view) {
        ActivityUtil.finish(RegistNext1Activity.this);
    }

    //结束注册
    @Event(R.id.txt_left)
    private void onClickTxtLeft(TextView view) {
        ActivityUtil.finish(RegistNext1Activity.this);
    }


    //重新发送
    @Event(R.id.tv_djs)
    private void onClickTvDjs(View view) {
        SMSSDK.getVerificationCode("86", phone);
        time = new TimeCount(60000, 1000);//构造CountDownTimer对象
        time.start();
        tv_djs.setClickable(false);
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
                    //注册
//                    ToastUtil.TextIntToast(RegistNext1Activity.this.getApplicationContext(),
//                            R.string.register_success, 0);
                    Log.i(TAG, "注册成功");
                    //TODO 发送注册协议
                    registUser(nickName, 1, password, phone, null);
                }else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE){
                    //获取验证码成功
                    Log.i(TAG, "获取验证码成功");
                }else if (event ==SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES){
                    //返回支持发送验证码的国家列表
                }
            }else{
                ((Throwable)data).printStackTrace();
//                ToastUtil.TextIntToast(RegistNext1Activity.this.getApplicationContext(),
//                        R.string.login_vercode_error, 0);
                et_yzm.requestFocus();
                Log.d(TAG, "请输入正确的验证码");
            }
        }
    };

//    @Override
//    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
//        switch (accessId) {
//            case R.id.btn_register_next:
//                if(obj != null && obj.length > 0) {
//                    JSONObject jsonRoot = (JSONObject)obj[0];
//                    try {
//                        int status = jsonRoot.getInt("status");
//                        if(status == 0) {
//                            Log.i(TAG, "业务服务器注册成功");
//                            //赋值常量
//                            if(jsonRoot.has("token")) {
//                                SessionConstants.token = jsonRoot.getString("token");
//                            }
//                            if(jsonRoot.has("userNo")) {
//                                SessionConstants.userNo = jsonRoot.getString("userNo");
//                            }
//                            if(jsonRoot.has("inviteInfo")) {
//                                //获取聚会邀请根元素
//                                JSONArray inviteInfoArr = jsonRoot.getJSONArray("inviteInfo");
//                                //处理聚会邀请
//                                doInviteInfo(inviteInfoArr);
//                            }
//                            startActivity(RegistNext1Activity.this, LoginActivity.class);
//                        } else {
//                            showMsgDialog(R.string.regist_user_error);
//                        }
//                    } catch (JSONException e) {
//                        Log.i(TAG, "回调解析失败", e);
//                    }
//                }
//                break;
//        }
//    }
//
//    @Override
//    public void onFailure(HttpException error, String msg, int accessId) {
//
//    }


    @Override
    public void onSuccess(Object obj, int accessId, Object inputParameter) {
        switch (accessId) {
            case R.id.btn_register_next:
                if(obj != null) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    try {
                        int status = jsonRoot.getInt("status");
                        if(status == 0) {
                            Log.i(TAG, "业务服务器注册成功");
                            //赋值常量
                            if(jsonRoot.has("token")) {
                                SessionConstants.token = jsonRoot.getString("token");
                            }
                            if(jsonRoot.has("userNo")) {
                                SessionConstants.userNo = jsonRoot.getString("userNo");
                            }
                            if(jsonRoot.has("inviteInfo")) {
                                //获取聚会邀请根元素
                                JSONArray inviteInfoArr = jsonRoot.getJSONArray("inviteInfo");
                                //处理聚会邀请
                                doInviteInfo(inviteInfoArr);
                            }
                            if(inputParameter instanceof Map) {
                                Map<String, Object> parameterMap = (Map<String, Object>)inputParameter;
                                String nickName = (String)parameterMap.get("nickName");
                                Integer gender = (Integer)parameterMap.get("gender");
                                String userPhone = (String)parameterMap.get("userPhone");
//                                String password = (String)parameterMap.get("password");

//                                valueMap.put("nickName", nickName);
//                                valueMap.put("gender", gender);
//                                valueMap.put("userPhone", userPhone);
//                                valueMap.put("password", _password);

                                String avatar = HttpConstants.getPortraitUrl()+SessionConstants.userNo;
                                User user = User.buildForCreate(SessionConstants.userNo,
                                        userPhone, null, gender, nickName, null, new Date(), avatar);
                                userDao.replaceInto(user);

                                //向消息服务注册服务
                                imService.getLoginManager().createAccount(SessionConstants.userNo, password);
                            }
                            ToastUtil.TextIntToast(getApplicationContext(), R.string.register_success, 2);
                            startActivityNew(RegistNext1Activity.this, LoginActivity.class);
                        } else {
                            showMsgDialog(R.string.regist_user_error);
                        }
                    } catch (JSONException e) {
                        Log.i(TAG, "回调解析失败", e);
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



    /**
     *******************************************私有函数******************************************
     */
    private void initSDK() {
        SMSSDK.initSDK(this, Constants.APPKEY, Constants.APPSECRET);
        SMSSDK.registerEventHandler(eh); //注册短信回调
    }

    private void setListeners() {
        // 监听多个输入框
        et_yzm.addTextChangedListener(new TextChange());
    }


    //用户注册
    private void registUser(String nickName, Integer gender,
                            String password, String userPhone, String birthday) {
        String _password = MD5Util.MD5(password);
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("nickName", nickName);
        valueMap.put("gender", gender);
        valueMap.put("userPhone", userPhone);
        valueMap.put("password", _password);
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<Map<String, Object>>(
                R.id.btn_register_next, HttpConstants.getAUserUrl() + "/regist", this, valueMap,
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
     *
     * 方法名： doInviteInfo
     * 方法描述： 处理聚会邀请信息
     * 参数说明：inviteInfoRoot
     * 返回类型： void
     *
     */
    private void doInviteInfo(JSONArray inviteInfoArr) throws JSONException {
        if(inviteInfoArr != null && inviteInfoArr.length() >0) {
            for(int i = 0; i <inviteInfoArr.length() ; i++) {
                JSONObject arr =  inviteInfoArr.getJSONObject(i);
            }
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
            tv_djs.setClickable(true);
            tv_djs.setText("重新发送");
        }
        @Override
        public void onTick(long millisUntilFinished){//计时过程显示
            long leftTime = millisUntilFinished / 1000;
            String tishi = "接收短信大约需要"+leftTime+"秒钟";
            tv_djs.setText(tishi);
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

            boolean sign1 = et_yzm.getText().length() > 3;
            if (sign1) {
                btn_register_next.setTextColor(0xFFFFFFFF);
                btn_register_next.setEnabled(true);
            }
            // 在layout文件中，对Button的text属性应预先设置默认值，否则刚打开程序的时候Button是无显示的
            else {
                btn_register_next.setTextColor(0xFFD0EFC6);
                btn_register_next.setEnabled(false);
            }
        }
    }
}
