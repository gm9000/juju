package com.juju.app.fragment;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.user.SettingActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.RoundImageView;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 项目名称：juju
 * 类描述：我—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:11
 * 版本：V1.0.0
 */
@CreateFragmentUI(viewId = R.layout.fragment_me)
public class MeFragment extends BaseFragment implements CreateUIHelper, View.OnClickListener, HttpCallBack {

    private static final String TAG = "MeFragment";

    private RelativeLayout view_user;
    private RoundImageView img_head;
    private TextView txt_nickName;
    private ImageView img_gender;
    private TextView txt_jujuNo;

    private TextView txt_party;
    private TextView txt_invite;
    private TextView txt_setting;



    @Override
    protected void findViews() {
        super.findViews();
        txt_party = (TextView) findViewById(R.id.txt_party);
        txt_invite = (TextView) findViewById(R.id.txt_invite);
        txt_setting = (TextView) findViewById(R.id.txt_setting);

        view_user = (RelativeLayout) findViewById(R.id.view_user);
        img_head = (RoundImageView) findViewById(R.id.head);
        txt_nickName = (TextView) findViewById(R.id.nick_name);
        img_gender = (ImageView) findViewById(R.id.iv_sex);
        txt_jujuNo = (TextView) findViewById(R.id.txt_jujuNo);


    }

    @Override
    public void loadData() {
        loadUserInfo();
    }
    @Override
    public void initView() {

    }

    @Override
    public void onResume(){
        Log.d(TAG,"onResume");
        super.onResume();
        loadUserInfo();
    }

    public void loadUserInfo() {
        BitmapUtilFactory.getInstance(getActivity()).display(img_head,HttpConstants.getUserUrl()+"/getPortraitSmall?targetNo="+BaseApplication.getInstance().getUserInfoBean().getJujuNo());
        String userInfoStr = (String) SpfUtil.get(getActivity().getApplicationContext(), Constants.USER_INFO, null);
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
                    R.id.txt_property, HttpConstants.getUserUrl() + "/getUserInfo", this, valueMap,
                    JSONObject.class);
            try {
                client.sendGet();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            img_gender.setImageResource(userInfo.getGender()==0?R.mipmap.ic_sex_female:R.mipmap.ic_sex_male);
            txt_jujuNo.setText(userInfo.getUserNo());
            txt_nickName.setText(userInfo.getNickName());
        }
    }

    @Override
    protected void setOnListener() {
        super.setOnListener();
        txt_party.setOnClickListener(this);
        txt_invite.setOnClickListener(this);
        txt_setting.setOnClickListener(this);
        view_user.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_party:
                ToastUtil.showShortToast(this.getActivity(),"聚会",1);
                break;
            case R.id.txt_invite:
                ToastUtil.showShortToast(this.getActivity(),"invite",1);
                break;
            case R.id.txt_setting:
                ToastUtil.showShortToast(this.getActivity(),"setting",1);
                break;
            case R.id.view_user:
                ActivityUtil.startActivity(this.getActivity(), SettingActivity.class);
                break;
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
                            JSONObject userJson = jsonRoot.getJSONObject("user");
                            //   "userNo":"100000001","nickName":"别名-1","userPhone":"13800000001","birthday":1451889752445,"gender":1,"createTime":1451889752445}
                            User userInfo = new User();
                            userInfo.setUserNo(userJson.getString("userNo"));
                            userInfo.setNickName(userJson.getString("nickName"));
                            userInfo.setPhone(userJson.getString("userPhone"));
                            userInfo.setGender(userJson.getInt("gender"));


                            img_gender.setImageResource(userInfo.getGender() == 0 ? R.mipmap.ic_sex_female : R.mipmap.ic_sex_male);
                            txt_jujuNo.setText(userInfo.getUserNo());
                            txt_nickName.setText(userInfo.getNickName());
                            SpfUtil.put(getActivity().getApplicationContext(), Constants.USER_INFO, userInfo.toString());

                        } else {
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
