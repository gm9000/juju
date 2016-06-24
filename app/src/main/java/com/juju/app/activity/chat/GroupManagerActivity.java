package com.juju.app.activity.chat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.adapter.GroupManagerAdapter;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IntentConstant;
import com.juju.app.helper.CheckboxConfigHelper;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.sp.ConfigurationSp;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.apache.http.message.BasicNameValuePair;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;


@ContentView(R.layout.activity_group_manager)
@CreateUI(showTopView = true)
public class GroupManagerActivity extends BaseActivity {

    protected static Logger logger = Logger.getLogger(GroupManagerActivity.class);


    @ViewInject(R.id.group_manager_grid)
    private GridView gridView;

    @ViewInject(R.id.quit_group)
    private TextView quit_group;

    @ViewInject(R.id.transfer_group)
    private TextView transfer_group;


    @ViewInject(R.id.tv_invite_code)
    private TextView tv_invite_code;



    private GroupManagerAdapter adapter;


    @ViewInject(R.id.NotificationNoDisturbCheckbox)
    private CheckBox noDisturbCheckbox;


    @ViewInject(R.id.NotificationTopMessageCheckbox)
    private CheckBox topSessionCheckBox;

    @ViewInject(R.id.group_manager_title)
    private TextView groupNameView;

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
        imServiceConnector.connect(GroupManagerActivity.this);
    }

    @Override
    protected void onDestroy() {
        imServiceConnector.disconnect(GroupManagerActivity.this);
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
                ownerId = groupEntity.getCreatorId();
                setTopTitle(getString(R.string.chat_detail)+"("+groupEntity.getUserCnt()+")");
                // 群组名称的展示
                groupNameView.setText(groupEntity.getMainName());
                tv_invite_code.setText(groupEntity.getInviteCode() == null ? ""
                        : groupEntity.getInviteCode());
            }break;
        }

        String loginId = userInfoBean.getJujuNo();
        if(ownerId.equals(loginId)) {
            transfer_group.setVisibility(View.VISIBLE);
        } else {
            quit_group.setVisibility(View.VISIBLE);
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
        String groupName = groupNameView.getText().toString();
        List<BasicNameValuePair> valuePairs = new ArrayList<>();
        BasicNameValuePair peerIdValue = new BasicNameValuePair(Constants.GROUP_ID_KEY,
                peerEntity.getPeerId());
        valuePairs.add(peerIdValue);
        ActivityUtil.startActivity(GroupManagerActivity.this, GroupQrActivity.class,
                valuePairs.toArray(new BasicNameValuePair[]{}));

    }

    private void initCheckbox() {
//        checkBoxConfiger.initCheckBox(noDisturbCheckbox, curSessionKey,
//                ConfigurationSp.CfgDimension.NOTIFICATION);
        String groupId = curSessionKey.split("_")[1];
        checkBoxConfiger.initForbiddenCheckBox(noDisturbCheckbox, curSessionKey, groupId);
        checkBoxConfiger.initTopCheckBox(topSessionCheckBox, curSessionKey);
    }



}
