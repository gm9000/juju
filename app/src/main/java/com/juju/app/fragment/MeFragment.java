package com.juju.app.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.party.MyInviteListActivity;
import com.juju.app.activity.party.MyPartyListlActivity;
import com.juju.app.activity.user.SettingActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Invite;
import com.juju.app.entity.Party;
import com.juju.app.entity.User;
import com.juju.app.event.NotificationMessageEvent;
import com.juju.app.event.NotifyMessageEvent;
import com.juju.app.golobal.BitmapUtilFactory;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ImageUtils;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.SpfUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.RoundImageView;

import org.apache.http.message.BasicNameValuePair;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 项目名称：juju
 * 类描述：我—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:11
 * 版本：V1.0.0
 */
@ContentView(R.layout.fragment_me)
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

    @ViewInject(R.id.tab_invite_notify)
    private View tab_invite_notify;

    @ViewInject(R.id.lin_invite)
    private ViewGroup lin_invite;


    @ViewInject(R.id.lin_party)
    private ViewGroup lin_party;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(MeFragment.this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(MeFragment.this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

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
        // TODO 通过消息驱动生成相关的邀请信息，并删除模拟数据生成的相关代码
//        generateInviteDate();
    }



    @Override
    public void initView() {
        Drawable rightDrawable = getResources().getDrawable(R.mipmap.right);
        Drawable settingDrawable = getResources().getDrawable(R.mipmap.setting);
        int drawableEdge = ScreenUtil.dip2px(getContext(),20);
        settingDrawable.setBounds(0, 0, drawableEdge, drawableEdge);
        txt_setting.setCompoundDrawables(settingDrawable, null, rightDrawable, null);

        Drawable partyDrawable = null;
        Drawable inviteDrawable = null;
        try {
            if(JujuDbUtils.getInstance().selector(Invite.class).where("status", "=", -1).and("flag", "=", 1).count()>0){
                inviteDrawable = getResources().getDrawable(R.mipmap.invite_new);
            }else{
                inviteDrawable = getResources().getDrawable(R.mipmap.invite);
            }
            if(JujuDbUtils.getInstance().selector(Party.class).where("status", "=", -1).count()>0){
                partyDrawable = getResources().getDrawable(R.mipmap.party_new);
            }else{
                partyDrawable = getResources().getDrawable(R.mipmap.party);
            }

        } catch (DbException e) {
            e.printStackTrace();
        }

        partyDrawable.setBounds(0, 0, drawableEdge, drawableEdge);
        txt_party.setCompoundDrawables(partyDrawable, null, rightDrawable, null);

        inviteDrawable.setBounds(0, 0, drawableEdge, drawableEdge);
        txt_invite.setCompoundDrawables(inviteDrawable, null, rightDrawable, null);

    }

    @Override
    public void onResume(){
        Log.d(TAG, "onResume");
        super.onResume();
        if(JujuDbUtils.needRefresh(Party.class) || JujuDbUtils.needRefresh(Invite.class)){
            initView();
            JujuDbUtils.closeRefresh(Party.class);
            JujuDbUtils.closeRefresh(Invite.class);
        }
        loadUserInfo();
    }

    public void loadUserInfo() {
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        BitmapUtilFactory.getInstance(getActivity()).bind(img_head,HttpConstants.getUserUrl()+"/getPortraitSmall?targetNo="+userInfoBean.getJujuNo());

        String userInfoStr = (String) SpfUtil.get(getActivity().getApplicationContext(), Constants.USER_INFO,null);

        User userInfo = null;
        if(userInfoStr != null){
            userInfo = JacksonUtil.turnString2Obj(userInfoStr,User.class);
        }
        if(userInfo == null){

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
//        txt_party.setOnClickListener(this);
//        txt_invite.setOnClickListener(this);
        txt_setting.setOnClickListener(this);
        view_user.setOnClickListener(this);
        lin_invite.setOnClickListener(this);
        lin_party.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lin_party:
                ActivityUtil.startActivity(getActivity(), MyPartyListlActivity.class);
                break;
            case R.id.lin_invite:
                tab_invite_notify.setVisibility(View.GONE);
                ActivityUtil.startActivity(getActivity(), MyInviteListActivity.class);
                break;
            case R.id.txt_setting:
                ActivityUtil.startActivity(this.getActivity(), SettingActivity.class);
                break;
            case R.id.view_user:
                UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
                ActivityUtil.startActivity(this.getActivity(), SettingActivity.class,
                        new BasicNameValuePair(Constants.USER_NO,userInfoBean.getJujuNo()));
                break;
        }
    }

    @Override
    public void onSuccess(Object obj, int accessId, Object inputParameter) {
        switch (accessId) {
            case R.id.txt_property:
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

                            JujuDbUtils.saveOrUpdate(userInfo);
                            SpfUtil.put(getActivity().getApplicationContext(),Constants.USER_INFO,userInfo);

                            img_gender.setImageResource(userInfo.getGender() == 0 ? R.mipmap.ic_sex_female : R.mipmap.ic_sex_male);
                            txt_jujuNo.setText(userInfo.getUserNo());
                            txt_nickName.setText(userInfo.getNickName());

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



//    @Override
//    public void onSuccess(ResponseInfo<String> responseInfo, int accessId, Object... obj) {
//        switch (accessId) {
//            case R.id.txt_property:
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
//                            JujuDbUtils.saveOrUpdate(userInfo);
//                            SpfUtil.put(getActivity().getApplicationContext(),Constants.USER_INFO,userInfo);
//
//                            img_gender.setImageResource(userInfo.getGender() == 0 ? R.mipmap.ic_sex_female : R.mipmap.ic_sex_male);
//                            txt_jujuNo.setText(userInfo.getUserNo());
//                            txt_nickName.setText(userInfo.getNickName());
//
//                        } else {
//                        }
//                    } catch (JSONException e) {
//                        Log.e(TAG, "回调解析失败", e);
//                        e.printStackTrace();
//                    }
//                }
//                break;
//        }
//    }
//
//    @Override
//    public void onFailure(HttpException error, String msg, int accessId) {
//        System.out.println("accessId:" + accessId + "\r\n msg:" + msg + "\r\n code:" +
//                error.getExceptionCode());
//    }

    /**
     * 接收通知信息
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4OtherMessage(NotificationMessageEvent event) {
//        switch (event.event) {
//            case INVITE_GROUP_NOTIFY_REQ_RECEIVED:
//            case INVITE_GROUP_NOTIFY_RES_RECEIVED:
//                tab_invite_notify.setVisibility(View.VISIBLE);
//                break;
//            case INVITE_GROUP_NOTIFY_OPEN_ACTIVITY:
//                tab_invite_notify.setVisibility(View.GONE);
//                break;
//        }
    }
}
