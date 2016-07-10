package com.juju.app.activity.chat;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.ApplyInGroupEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.Constants;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.notify.ApplyInGroupNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.view.groupchat.IMGroupAvatar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ContentView(R.layout.activity_group_join_in)
@CreateUI(showTopView = true)
public class GroupJoinInActivity extends BaseActivity implements CreateUIHelper {

    protected static Logger logger = Logger.getLogger(GroupJoinInActivity.class);

//    private int GET_GROUP_INFO_OK = 1;
//
//    private int GET_GROUP_INFO_FAILED = 2;

    @ViewInject(R.id.tv_name)
    private TextView tv_name;

    @ViewInject(R.id.tv_num)
    private TextView tv_num;

    @ViewInject(R.id.progress_bar)
    private ProgressBar progressbar;

    @ViewInject(R.id.iv_head)
    private IMGroupAvatar iv_head;

    @ViewInject(R.id.tv_join_group)
    private TextView tv_join_group;

    @ViewInject(R.id.lay_info)
    private ViewGroup lay_info;


    //邀请码
    private String code;
    private String groupId;

    private UserInfoBean userInfoBean;
    private IMService imService;


    /**
     * IMServiceConnector
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("GroupJoinInActivity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
        }

        @Override
        public void onServiceDisconnected() {

        }
    };

    @Override
    public void loadData() {
        imServiceConnector.connect(GroupJoinInActivity.this);
        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        code = getIntent().getStringExtra("code");
        groupId = getIntent().getStringExtra("groupId");
        userInfoBean = AppContext.getUserInfoBean();
    }

    @Override
    public void initView() {
        showTopLeftAll(0, 0);
        setTopTitle(R.string.join_in_group);
        showProgressBar();
//        sendGetGroupInfo2BServer(groupId);

        sendGetGroupOutline2BServer(groupId, code);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        imServiceConnector.disconnect(GroupJoinInActivity.this);
        super.onDestroy();
    }

    @Event(value = R.id.tv_join_group)
    private void onClickJoinGroup(View view) {
        showProgressBar();
        ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean = ApplyInGroupEvent.ApplyInGroupBean
                .valueOf(groupId, userInfoBean.getUserNo(), userInfoBean.getNickName(), code);
        ApplyInGroupNotify.instance().executeCommand4Send(applyInGroupBean);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4ApplyInGroup(ApplyInGroupEvent applyInGroupEvent) {
        hideProgressBar();
        switch (applyInGroupEvent.event) {
            case SEND_APPLY_IN_GROUP_OK:
//                ToastUtil.showShortToast(getApplicationContext(),
//                        getString(R.string.join_group_send_success), Gravity.BOTTOM);
                if(imService != null) {
                    GroupEntity groupEntity = imService.getGroupManager()
                            .findGroupById(applyInGroupEvent.bean.groupId);
                    if(groupEntity != null) {
                        //打开ChatActivity
                        startActivityNew(GroupJoinInActivity.this, ChatActivity.class,
                                Constants.SESSION_ID_KEY, groupEntity.getSessionKey());
                        finish(GroupJoinInActivity.this);
                    }
                }
                break;
            case SEND_APPLY_IN_GROUP_FAILED:
                ToastUtil.showShortToast(getApplicationContext(),
                        getString(R.string.join_group_send_failed), Gravity.BOTTOM);
                break;
        }
    }





    public void showProgressBar() {
        progressbar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        progressbar.setVisibility(View.GONE);
    }


    public void sendGetGroupOutline2BServer(String groupId, String inviteCode) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId,inviteCode", groupId, inviteCode);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETGROUPOUTLINE;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject jsonRoot= (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    if(status == 0) {
                        final String groupName = JSONUtils.getString(jsonRoot, "groupName");
                        final long userCount = JSONUtils.getLong(jsonRoot, "userCount", 1);
                        String[] jsonUserNos = JSONUtils.getStringArray(jsonRoot, "topUserNos", null);
                        final List<String> userNoList = new ArrayList<>();

                        if(jsonUserNos != null) {
                            for(String userNo : jsonUserNos) {
                                userNoList.add(HttpConstants.getPortraitUrl()+userNo);
                            }
                        }
                        GroupJoinInActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_name.setText(groupName);
                                tv_num.setText("(共"+userCount+"人)");
                                setGroupAvatar(userNoList);
                                hideProgressBar();
                            }
                        });
                    } else {
                        final String desc = JSONUtils.getString(jsonRoot, "desc");
                        GroupJoinInActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMsgDialog(getResValue(desc));
                                hideProgressBar();
                                tv_num.setVisibility(View.GONE);
                                tv_name.setVisibility(View.GONE);
                                iv_head.setVisibility(View.GONE);
                                tv_join_group.setVisibility(View.GONE);
                                lay_info.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                GroupJoinInActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgressBar();
                        ToastUtil.showShortToast(getApplicationContext(),
                                getString(R.string.system_service_error), Gravity.BOTTOM);
                    }
                });
            }
        }, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            hideProgressBar();
            ToastUtil.showShortToast(getApplicationContext(),
                    getString(R.string.system_service_error), Gravity.BOTTOM);
        } catch (JSONException e) {
            logger.error(e);
            hideProgressBar();
            ToastUtil.showShortToast(getApplicationContext(),
                    getString(R.string.system_service_error), Gravity.BOTTOM);
        }
    }

    public void sendGetGroupInfo2BServer(final String groupId) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId", groupId);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETGROUPINFO;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    if(status == 0) {
                        try {
                            JSONObject jsonGroup = jsonRoot.getJSONObject("group");
                            final String name = JSONUtils.getString(jsonGroup, "name");
                            sendGroupUsers2BServer(groupId);
                            GroupJoinInActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_name.setText(name);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            hideProgressBar();
                            ToastUtil.showShortToast(getApplicationContext(),
                                    getString(R.string.system_service_error), Gravity.BOTTOM);
                        }

                    } else {
                        String desc = JSONUtils.getString(jsonRoot, "desc");
                        showMsgDialog(getResValue(desc));
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                GroupJoinInActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgressBar();
                        ToastUtil.showShortToast(getApplicationContext(),
                                getString(R.string.system_service_error), Gravity.BOTTOM);
                    }
                });

            }
        }, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            hideProgressBar();
            ToastUtil.showShortToast(getApplicationContext(),
                    getString(R.string.system_service_error), Gravity.BOTTOM);
        } catch (JSONException e) {
            logger.error(e);
            hideProgressBar();
            ToastUtil.showShortToast(getApplicationContext(),
                    getString(R.string.system_service_error), Gravity.BOTTOM);
        }
    }

    public void sendGroupUsers2BServer(String groupId) {
        Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId", groupId);
        CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETGROUPUSERS;
        JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                httpReqParam.url(), new HttpCallBack4OK() {
            @Override
            public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                if(obj != null && obj instanceof JSONObject) {
                    JSONObject jsonRoot = (JSONObject)obj;
                    int status = JSONUtils.getInt(jsonRoot, "status", -1);
                    if(status == 0) {
                        try {
                            final JSONArray jsonArray = jsonRoot.getJSONArray("users");
                            final int length = jsonArray.length();
                            final List<String> userNoList = new ArrayList<>();
                            for (int i = 0; i <jsonArray.length() ; i++) {
                                if(i == 9) {
                                    break;
                                }
                                JSONObject jsonUser = (JSONObject) jsonArray.get(i);
                                String userNo = JSONUtils.getString(jsonUser, "userNo");
                                userNoList.add(HttpConstants.getPortraitUrl()+userNo);
                            }

                            GroupJoinInActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_num.setText("(共"+length+"人)");
                                    setGroupAvatar(userNoList);
                                    hideProgressBar();
                                }
                            });
                        } catch (JSONException e) {
                            hideProgressBar();
                            ToastUtil.showShortToast(getApplicationContext(),
                                    getString(R.string.system_service_error), Gravity.BOTTOM);
                        }
                    } else {
                        String desc = JSONUtils.getString(jsonRoot, "desc");
                        showMsgDialog(getResValue(desc));
                    }
                }
            }

            @Override
            public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                GroupJoinInActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgressBar();
                        ToastUtil.showShortToast(getApplicationContext(),
                                getString(R.string.system_service_error), Gravity.BOTTOM);
                    }
                });
            }
        }, valueMap, JSONObject.class);
        try {
            client.sendGet4OK();
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            hideProgressBar();
            ToastUtil.showShortToast(getApplicationContext(),
                    getString(R.string.system_service_error), Gravity.BOTTOM);
        } catch (JSONException e) {
            logger.error(e);
            hideProgressBar();
            ToastUtil.showShortToast(getApplicationContext(),
                    getString(R.string.system_service_error), Gravity.BOTTOM);
        }
    }


    /**
     * 设置群头像
     * @param avatarUrlList
     */
    private void setGroupAvatar(List<String> avatarUrlList){
        try {
            if (null == avatarUrlList) {
                return;
            }
            iv_head.setAvatarUrlAppend(Constants.AVATAR_APPEND_32);
            iv_head.setChildCorner(3);
            if (null != avatarUrlList) {
                iv_head.setAvatarUrls(new ArrayList<String>(avatarUrlList));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
