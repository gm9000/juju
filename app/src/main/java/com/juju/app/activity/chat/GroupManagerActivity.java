package com.juju.app.activity.chat;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.adapter.GroupManagerAdapter;
import com.juju.app.adapter.SingleCheckAdapter;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.event.notify.ExitGroupEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.event.notify.MasterTransferEvent;
import com.juju.app.event.notify.RemoveGroupEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IntentConstant;
import com.juju.app.helper.CheckboxConfigHelper;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.sp.ConfigurationSp;
import com.juju.app.service.notify.ExitGroupNotify;
import com.juju.app.service.notify.MasterTransferNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.view.dialog.WarnTipDialog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.apache.http.message.BasicNameValuePair;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ContentView(R.layout.activity_group_manager)
@CreateUI(showTopView = true)
public class GroupManagerActivity extends BaseActivity {

    protected static Logger logger = Logger.getLogger(GroupManagerActivity.class);

    @ViewInject(R.id.group_manager_grid)
    private GridView gridView;

    @ViewInject(R.id.quit_group)
    private TextView quit_group;

    @ViewInject(R.id.transfer_group)
    private View transfer_group;


//    @ViewInject(R.id.tv_invite_code)
//    private TextView tv_invite_code;


    private GroupManagerAdapter adapter;


    @ViewInject(R.id.NotificationNoDisturbCheckbox)
    private CheckBox noDisturbCheckbox;


    @ViewInject(R.id.NotificationTopMessageCheckbox)
    private CheckBox topSessionCheckBox;

    @ViewInject(R.id.group_manager_title)
    private TextView groupNameView;

    @ViewInject(R.id.progress_bar)
    private ProgressBar progressbar;



    /**需要的状态参数*/
    private IMService imService;
    private String curSessionKey;
    private PeerEntity peerEntity;

    private UserInfoBean userInfoBean;

    CheckboxConfigHelper checkBoxConfiger = new CheckboxConfigHelper();



    private IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onServiceDisconnected() {
        }

        @Override
        public void onIMServiceConnected() {
            logger.d("groupmgr#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            if(imService == null){
                Toast.makeText(GroupManagerActivity.this,
                        getResources().getString(R.string.im_service_disconnected),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            checkBoxConfiger.init(imService.getConfigSp());
            userInfoBean = BaseApplication.getInstance().getUserInfoBean();
            initViews();
            initAdapter();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        imServiceConnector.connect(GroupManagerActivity.this);
    }

    @Override
    protected void onDestroy() {
        imServiceConnector.disconnect(GroupManagerActivity.this);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }



    private void initViews() {
        showTopLeftAll(0, 0);
        if (null == imService) {
            logger.e("groupmgr#init failed,cause by imService or curView is null");
            return;
        }

        curSessionKey =  GroupManagerActivity.this.getIntent().getStringExtra(Constants.SESSION_ID_KEY);
        if (TextUtils.isEmpty(curSessionKey)) {
            logger.e("groupmgr#getSessionInfoFromIntent failed");
            return;
        }
        peerEntity = imService.getSessionManager().findPeerEntity(curSessionKey);
        if(peerEntity == null){
            logger.e("groupmgr#findPeerEntity failed,sessionKey:%s",curSessionKey);
            return;
        }
        String ownerId = null;
        switch (peerEntity.getType()){
            case DBConstant.SESSION_TYPE_GROUP:{
                GroupEntity groupEntity = (GroupEntity) peerEntity;
                ownerId = groupEntity.getMasterId();
                setTopTitle(getString(R.string.chat_detail)+"("+groupEntity.getUserCnt()+")");
                // 群组名称的展示
                groupNameView.setText(groupEntity.getMainName());
//                tv_invite_code.setText(groupEntity.getInviteCode() == null ? ""
//                        : groupEntity.getInviteCode());
            }break;
        }

        String userNo = userInfoBean.getUserNo();
        if(ownerId.equals(userNo)) {
            transfer_group.setVisibility(View.VISIBLE);
        }
        // 初始化配置checkBox
        initCheckbox();
    }

    private void initAdapter(){
        logger.d("groupmgr#initAdapter");
        gridView.setSelector(new ColorDrawable(Color.TRANSPARENT));// 去掉点击时的黄色背影
        gridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true));

        adapter = new GroupManagerAdapter(GroupManagerActivity.this, imService, peerEntity);
        gridView.setAdapter(adapter);


    }

    //群组二维码
    @Event(value = R.id.re_qr)
    private void onClick4Qr(View view) {
        startActivityNew(GroupManagerActivity.this, GroupQrActivity.class, Constants.GROUP_ID_KEY,
                peerEntity.getId());
    }

    @Event(value = R.id.transfer_group)
    private void onClick4Transfer(View view) {
        String itemListData =  buildItemList(peerEntity.getPeerId());
        String groupId = peerEntity.getId();
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.SINGLE_CHECK_LIST_DATA, itemListData);
        valueMap.put(Constants.SINGLE_CHECK_TARGET_ID, groupId);
        startActivityNew(GroupManagerActivity.this, GroupTransferActivity.class,
                valueMap);
    }

    @Event(value = R.id.quit_group)
    private void onClick4QuitGroup(View view) {
        String message = "确定退出"+peerEntity.getMainName()+"吗？";
        WarnTipDialog tipdialog = new WarnTipDialog(GroupManagerActivity.this, message);
        tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                User user = imService.getContactManager().findContact(userInfoBean.getUserNo());
                final GroupEntity groupEntity = imService.getGroupManager().findGroupById(peerEntity.getId());
                if(groupEntity != null && groupEntity.getUserCnt() == 1) {
                    //删除群组
                    Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("groupId", groupEntity.getId());
                    CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.DELETEGROUP;
                    JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                            httpReqParam.url(), new HttpCallBack4OK() {
                        @Override
                        public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                            if(obj != null && obj instanceof JSONObject) {
                                JSONObject jsonRoot = (JSONObject)obj;
                                int status = JSONUtils.getInt(jsonRoot, "status", -1);
                                String desc = JSONUtils.getString(jsonRoot, "desc");
                                if(status == 0) {
                                    imService.getGroupManager().changeGroup4Trigger(groupEntity.getId(), DBConstant.GROUP_MODIFY_TYPE_DEL);
                                    GroupManagerActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToastUtil.TextIntToast(getApplicationContext(), R.string.exit_group_send_success, 0);
                                        }
                                    });
                                    finish(GroupManagerActivity.this);
                                } else {
                                    GroupManagerActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToastUtil.TextIntToast(getApplicationContext(), R.string.exit_group_send_failed, 0);
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                            GroupManagerActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtil.TextIntToast(getApplicationContext(), R.string.exit_group_send_failed, 0);
                                }
                            });
                        }
                    }, valueMap, JSONObject.class);

                    try {
                        client.sendPost4OK();
                    } catch (UnsupportedEncodingException e) {
                        logger.error(e);
                    } catch (JSONException e) {
                        logger.error(e);
                    }

                } else {
                    //主动退出
                    ExitGroupEvent.ExitGroupBean exitGroupBean = ExitGroupEvent.ExitGroupBean
                            .valueOf(peerEntity.getId(), userInfoBean.getUserNo(), userInfoBean.getNickName(), 1);
                    ExitGroupNotify.instance().executeCommand4Send(exitGroupBean);
                }
            }
        });
        tipdialog.show();
    }





    private void initCheckbox() {
//        checkBoxConfiger.initCheckBox(noDisturbCheckbox, curSessionKey,
//                ConfigurationSp.CfgDimension.NOTIFICATION);
        String groupId = curSessionKey.split("_")[1];
        checkBoxConfiger.initForbiddenCheckBox(noDisturbCheckbox, curSessionKey, groupId);
        checkBoxConfiger.initTopCheckBox(topSessionCheckBox, curSessionKey);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4InviteUser(InviteUserEvent inviteUserEvent) {
        switch (inviteUserEvent.event) {
            case INVITE_USER_OK:
                User userEntity =  imService.getContactManager()
                        .findContact(inviteUserEvent.bean.userNo);
                if(userEntity != null) {
                    adapter.add(userEntity);
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4InviteRemoveGroup(RemoveGroupEvent inviteUserEvent) {
        switch (inviteUserEvent.event) {
            case SEND_REMOVE_GROUP_OK:
                User userEntity =  imService.getContactManager()
                        .findContact(inviteUserEvent.bean.userNo);
                if(userEntity != null) {
                    adapter.remove(userEntity);
                }
                hideProgressBar();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4MasterTransfer(MasterTransferEvent masterTransferEvent) {
        switch (masterTransferEvent.event) {
            case SEND_MASTER_TRANSFER_OK:
                GroupEntity groupEntity = imService.getGroupManager()
                        .findGroupById(masterTransferEvent.bean.groupId);
                if(groupEntity != null) {
                    adapter.refreshGroupData(groupEntity);
                    if(!userInfoBean.getUserNo().equals(masterTransferEvent.bean.masterNo)) {
                        transfer_group.setVisibility(View.GONE);
                    }
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4ExitGroup(ExitGroupEvent exitGroupEvent) {
        switch (exitGroupEvent.event) {
            case SEND_EXIT_GROUP_OK:
                finish(GroupManagerActivity.this);
                break;
        }
    }



    private String buildItemList(String peerId) {
        String itemListData = null;
        GroupEntity groupEntity = imService.getGroupManager().findGroup(peerId);
        if(groupEntity != null && StringUtils.isNotBlank(groupEntity.getUserList())) {
            String[] userNoArr = groupEntity.getUserList().split(",");
            List<SingleCheckAdapter.ItemBean> itemBeanList = new ArrayList<>();
            for(String userNo : userNoArr) {
                if(userInfoBean.getUserNo().equals(userNo)){
                    continue;
                } else {
                    User user = imService.getContactManager().findContact(userNo);
                    if(user != null) {
                        SingleCheckAdapter.ItemBean itemBean = SingleCheckAdapter
                                .ItemBean.build4UserEntity(user);
                        itemBeanList.add(itemBean);
                    }
                }
            }
            if(itemBeanList.size() >0) {
                itemListData = JacksonUtil.turnObj2String(itemBeanList);
            }
        }
        return itemListData;
    }



    public void showProgressBar() {
        progressbar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        progressbar.setVisibility(View.GONE);
    }

}
