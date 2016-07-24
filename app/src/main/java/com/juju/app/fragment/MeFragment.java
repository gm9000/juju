package com.juju.app.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.user.SettingActivity;
import com.juju.app.activity.user.UploadPhotoActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.Invite;
import com.juju.app.entity.Party;
import com.juju.app.entity.User;
import com.juju.app.event.NotificationMessageEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.https.HttpCallBack;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.ScreenUtil;
import com.juju.app.utils.SpfUtil;

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
import org.xutils.x;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

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

    @ViewInject(R.id.head)
    private CircleImageView imgHead;
    @ViewInject(R.id.txt_nick_name)
    private TextView txtNickName;
    @ViewInject(R.id.txt_phone)
    private TextView txtPhone;
    @ViewInject(R.id.txt_jujuNo)
    private TextView txtJujuNo;
    @ViewInject(R.id.txt_gender)
    private TextView txtGender;

    @ViewInject(R.id.txt_setting)
    private TextView txt_setting;


    private DaoSupport userDao;

    private String userNo;


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
    }

    @Override
    public void loadData() {
        userDao = new UserDaoImpl(getActivity().getApplicationContext());
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

//        Drawable partyDrawable = null;
//        Drawable inviteDrawable = null;
//        try {
//            if(JujuDbUtils.getInstance().selector(Invite.class).where("status", "=", -1).and("flag", "=", 1).count()>0){
//                inviteDrawable = getResources().getDrawable(R.mipmap.invite_new);
//            }else{
//                inviteDrawable = getResources().getDrawable(R.mipmap.invite);
//            }
//            if(JujuDbUtils.getInstance().selector(Party.class).where("status", "=", -1).count()>0){
//                partyDrawable = getResources().getDrawable(R.mipmap.party_new);
//            }else{
//                partyDrawable = getResources().getDrawable(R.mipmap.party);
//            }
//
//        } catch (DbException e) {
//            e.printStackTrace();
//        }
//
//        partyDrawable.setBounds(0, 0, drawableEdge, drawableEdge);
//        txt_party.setCompoundDrawables(partyDrawable, null, rightDrawable, null);
//
//        inviteDrawable.setBounds(0, 0, drawableEdge, drawableEdge);
//        txt_invite.setCompoundDrawables(inviteDrawable, null, rightDrawable, null);

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
        UserInfoBean userInfoBean = AppContext.getUserInfoBean();
        ImageLoaderUtil.getImageLoaderInstance().displayImage(HttpConstants.getUserUrl()+"/getPortraitSmall?targetNo="+userInfoBean.getUserNo(),imgHead,ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
        userNo = userInfoBean.getUserNo();

        String userInfoStr = (String) SpfUtil.get(getActivity().getApplicationContext(), Constants.USER_INFO,null);

        User userInfo = null;
        if(userInfoStr != null){
            userInfo = JacksonUtil.turnString2Obj(userInfoStr,User.class);
        }
        if(userInfo == null){
            try {
                userInfo = JujuDbUtils.getInstance().selector(User.class).where("user_no","=",userNo).findFirst();
                if(userInfo != null){
                    SpfUtil.put(getActivity().getApplicationContext(),Constants.USER_INFO,JacksonUtil.turnObj2String(userInfo));
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
        }
        if(userInfo == null){

            Map<String, Object> valueMap = new HashMap<String, Object>();
            valueMap.put("userNo", userInfoBean.getUserNo());
            valueMap.put("token", userInfoBean.getToken());
            valueMap.put("targetNo", userInfoBean.getUserNo());

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
            txtJujuNo.setText(userInfo.getUserNo());
            txtNickName.setText(userInfo.getNickName());
            txtPhone.setText(userInfo.getUserPhone());
            txtGender.setText(userInfo.getGender() == 0 ? R.string.female : R.string.male);
        }

    }

    @Override
    protected void setOnListener() {
        super.setOnListener();
        txt_setting.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_setting:
//                ActivityUtil.startActivity(this.getActivity(), SettingActivity.class);
                startActivityNew(this.getActivity(), SettingActivity.class);
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
                            SpfUtil.put(getActivity().getApplicationContext(),Constants.USER_INFO,JacksonUtil.turnObj2String(userInfo));

                            txtGender.setText(userInfo.getGender() == 0 ? R.string.female : R.string.male);
                            txtJujuNo.setText(userInfo.getUserNo());
                            txtNickName.setText(userInfo.getNickName());
                            txtPhone.setText(userInfo.getUserPhone());


                            UserInfoBean userInfoBean = AppContext.getUserInfoBean();
                            userInfoBean.setNickName(userInfo.getNickName());
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

    @Event(R.id.head)
    private void showHeadImg(View view){
        ActivityUtil.startActivityNew(getContext(),UploadPhotoActivity.class,Constants.USER_NO,userNo);
    }


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
