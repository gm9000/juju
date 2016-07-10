package com.juju.app.activity.chat;

import android.app.Fragment;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.juju.app.R;
import com.juju.app.adapter.SingleCheckAdapter;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.event.notify.MasterTransferEvent;
import com.juju.app.fragment.SingleCheckFragment;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.notify.MasterTransferNotify;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.view.dialog.WarnTipDialog;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

@ContentView(R.layout.activity_group_transfer)
@CreateUI(showTopView = true)
public class GroupTransferActivity extends BaseActivity implements CreateUIHelper {

    private Logger logger = Logger.getLogger(GroupTransferActivity.class);

    private SingleCheckFragment singleCheckFragment;

    private UserInfoBean userInfoBean;

    private String groupId;

    private IMService imService;



    /**
     * IMServiceConnector
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("main_activity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            imService.getLoginManager().reConnect();
        }

        @Override
        public void onServiceDisconnected() {

        }
    };

    @Override
    public void loadData() {
        imServiceConnector.connect(GroupTransferActivity.this);
        userInfoBean = AppContext.getUserInfoBean();
        groupId = getIntent().getStringExtra(Constants.SINGLE_CHECK_TARGET_ID);
    }

    @Override
    public void initView() {
        singleCheckFragment = new SingleCheckFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, singleCheckFragment)
                .show(singleCheckFragment).commit();
        setTopTitle(R.string.select_new_group_manager);
        showTopLeftAll(R.string.top_left_back, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imServiceConnector.disconnect(GroupTransferActivity.this);
    }

    @Override
    protected void setOnListener() {
        super.setOnListener();
        singleCheckFragment.setOnClickListener(new SingleCheckAdapter.ListenerInfo.OnClickListener() {
            @Override
            public void onItemClick(final SingleCheckAdapter.ItemBean itemBean) {
                logger.d("GroupTransferActivity#onItemClick ->itemBean.mainName:%s",
                        itemBean.getMainName());

                String message = "确定选择"+itemBean.getMainName()+"为新群主，您将自动放弃群主身份";
                WarnTipDialog tipdialog = new WarnTipDialog(GroupTransferActivity.this, message);
                tipdialog.setBtnOkLinstener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        User user = imService.getContactManager().findContact(userInfoBean.getUserNo());
                        MasterTransferEvent.MasterTransferBean masterTransferBean = MasterTransferEvent
                                .MasterTransferBean.valueOf(groupId, userInfoBean.getUserNo(),
                                        user.getNickName(), itemBean.getId(), itemBean.getMainName());
                        MasterTransferNotify.instance().executeCommand4Send(masterTransferBean);
                        finish(GroupTransferActivity.this);
                    }
                });
                tipdialog.show();
            }
        });
    }
}
