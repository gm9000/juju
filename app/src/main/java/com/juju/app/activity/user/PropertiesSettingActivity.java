package com.juju.app.activity.user;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.SpfUtil;
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

@ContentView(R.layout.activity_properties_setting)
public class PropertiesSettingActivity extends AppCompatActivity implements XEditText.DrawableRightListener,HttpCallBack {

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

    private User userInfo;

    private Drawable drawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewUtils.inject(this);
        initParam();
        initData();
        initView();

    }

    private void initData() {
        try {
            userInfo = JujuDbUtils.getInstance(this).findFirst(Selector.from(User.class).where("userNo","=",BaseApplication.getInstance().getUserInfoBean().getJujuNo()));
        } catch (DbException e) {
            e.printStackTrace();
        }
        SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);

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
                txt_left.setVisibility(View.GONE);
                txt_right.setVisibility(View.GONE);
                txt_title.setText(R.string.gender);

                txt_male.setVisibility(View.VISIBLE);
                txt_female.setVisibility(View.VISIBLE);
                if(propertyValue.equals(getResources().getString(R.string.male))){
                    txt_male.setCompoundDrawables(null,null,drawable,null);
                }else{
                    txt_female.setCompoundDrawables(null,null,drawable,null);
                }

                break;
            case R.id.txt_phoneNo:
                txt_title.setText(R.string.phoneNo);
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
        if(propertyValue.equals(getResources().getString(R.string.female))){
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
        if(propertyValue.equals(getResources().getString(R.string.male))){
            txt_male.setCompoundDrawables(null,null,null,null);
            txt_female.setCompoundDrawables(null,null,drawable,null);
            userInfo.setGender(0);
            userInfo.setUpdate(true);
            SpfUtil.put(getApplicationContext(), Constants.USER_INFO, userInfo);
        }
        ActivityUtil.finish(this);
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
}
