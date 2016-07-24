package com.juju.app.activity.chat;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.entity.User;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.view.groupchat.IMBaseImageView;

import org.xutils.view.annotation.ContentView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@ContentView(R.layout.activity_user_info)
@CreateUI(showTopView = true)
public class UserInfoActivity extends BaseActivity implements CreateUIHelper {


    protected static Logger logger = Logger.getLogger(UserInfoActivity.class);


    private IMService imService;
    private User currentUser;
    private String currentUserId;

    private IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onIMServiceConnected() {
            logger.d("detail#onIMServiceConnected");

            imService = imServiceConnector.getIMService();
            if (imService == null) {
                logger.e("detail#imService is null");
                return;
            }

            currentUserId = getIntent().getStringExtra(Constants.KEY_PEERID);
            if(StringUtils.isBlank(currentUserId)){
                logger.e("detail#intent params error!!");
                return;
            }
            currentUser = imService.getContactManager().findContact(currentUserId);
            if(currentUser != null) {
                initBaseProfile();
                initDetailProfile();
            }
            ArrayList<String> userIds = new ArrayList<String>(1);
            //just single type
            userIds.add(currentUserId);

            //TODO 是否需要从后台查询一次？
        }
        @Override
        public void onServiceDisconnected() {}
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imServiceConnector.connect(UserInfoActivity.this);
    }

    @Override
    protected void onDestroy() {
        imServiceConnector.disconnect(UserInfoActivity.this);
        super.onDestroy();
    }

    private void initBaseProfile() {
        logger.d("detail#initBaseProfile");
        IMBaseImageView portraitImageView = (IMBaseImageView) findViewById(R.id.user_portrait);

        setTextViewContent(R.id.nickName, currentUser.getNickName());
//        setTextViewContent(R.id.userName, currentUser.getNickName());
        //头像设置
        portraitImageView.setDefaultImageRes(R.mipmap.tt_default_user_portrait_corner);
        portraitImageView.setCorner(8);
        portraitImageView.setImageResource(R.mipmap.tt_default_user_portrait_corner);
        portraitImageView.setImageUrl(currentUser.getAvatar());

        portraitImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> value = new HashMap<>();
                value.put(Constants.KEY_AVATAR_URL, currentUser.getAvatar());
                value.put(Constants.KEY_IS_IMAGE_CONTACT_AVATAR, true);
                ActivityUtil.startActivityNew(UserInfoActivity.this, DetailPortraitActivity.class, value);
            }
        });

        // 设置界面信息
//        Button chatBtn = (Button) findViewById(R.id.chat_btn);
//        if (currentUserId == imService.getLoginManager().getLoginId()) {
//            chatBtn.setVisibility(View.GONE);
//        }else{
//            chatBtn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View arg0) {
//                    IMUIHelper.openChatActivity(getActivity(),currentUser.getSessionKey());
//                    getActivity().finish();
//                }
//            });
//
//        }
    }

    private void initDetailProfile() {
        logger.d("detail#initDetailProfile");
//        hideProgressBar();
//        DepartmentEntity deptEntity = imService.getContactManager().findDepartment(currentUser.getDepartmentId());
//        setTextViewContent(R.id.department,deptEntity.getDepartName());
//        setTextViewContent(R.id.telno, currentUser.getPhone());

        completeLoading();
        setTextViewContent(R.id.tv_useNo, currentUser.getUserNo());
        setTextViewContent(R.id.tv_email, currentUser.getEmail());
        setTextViewContent(R.id.tv_phone, currentUser.getUserPhone());
        View phoneView = findViewById(R.id.phoneArea);
//        View emailView = findViewById(R.id.emailArea);
//        IMUIHelper.setViewTouchHightlighted(phoneView);
//        IMUIHelper.setViewTouchHightlighted(emailView);

//        emailView.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view) {
//                if (currentUserId == IMLoginManager.instance().getLoginId())
//                    return;
//                IMUIHelper.showCustomDialog(getActivity(),View.GONE,String.format(getString(R.string.confirm_send_email),currentUser.getEmail()),new IMUIHelper.dialogCallback() {
//                    @Override
//                    public void callback() {
//                        Intent data=new Intent(Intent.ACTION_SENDTO);
//                        data.setData(Uri.parse("mailto:" + currentUser.getEmail()));
//                        data.putExtra(Intent.EXTRA_SUBJECT, "");
//                        data.putExtra(Intent.EXTRA_TEXT, "");
//                        startActivity(data);
//                    }
//                });
//            }
//        });

//        phoneView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (currentUserId == IMLoginManager.instance().getLoginId())
//                    return;
//                IMUIHelper.showCustomDialog(getActivity(),View.GONE,String.format(getString(R.string.confirm_dial),currentUser.getPhone()),new IMUIHelper.dialogCallback() {
//                    @Override
//                    public void callback() {
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                IMUIHelper.callPhone(getActivity(), currentUser.getPhone());
//                            }
//                        },0);
//                    }
//                });
//            }
//        });

        setSex(currentUser.getGender());
    }

    private void setTextViewContent(int id, String content) {
        TextView textView = (TextView) findViewById(id);
        if (textView == null) {
            return;
        }

        textView.setText(content);
    }

    private void setSex(int sex) {
        TextView sexTextView = (TextView) findViewById(R.id.tv_sex);
        if (sexTextView == null) {
            return;
        }
//        int textColor = Color.rgb(255, 138, 168); //xiaoxian
        String text = getString(R.string.female);

        if (sex == DBConstant.SEX_MAILE) {
//            textColor = Color.rgb(144, 203, 1);
            text = getString(R.string.male);
        }

        sexTextView.setVisibility(View.VISIBLE);
        sexTextView.setText(text);
//        sexTextView.setTextColor(textColor);
    }

    @Override
    public void loadData() {

    }

    @Override
    public void initView() {
        showTopLeftAll(0, 0);
        setTopTitle(R.string.page_user_detail);
        loading();
    }
}
