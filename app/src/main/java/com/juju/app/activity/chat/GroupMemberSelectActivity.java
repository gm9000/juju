package com.juju.app.activity.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.adapter.GroupSelectAdapter;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.Logger;
import com.juju.app.view.SearchEditText;
import com.juju.app.view.SortSideBar;

import org.xutils.view.annotation.ContentView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


@ContentView(R.layout.activity_group_member_select)
@CreateUI(showTopView = true)
public class GroupMemberSelectActivity extends BaseActivity implements CreateUIHelper, SortSideBar.OnTouchingLetterChangedListener {

    protected static Logger logger = Logger.getLogger(GroupMemberSelectActivity.class);


    private IMService imService;

    private SortSideBar sortSideBar;
    private TextView dialog;
    private SearchEditText searchEditText;


    private String curSessionKey;
    private PeerEntity peerEntity;

    private GroupSelectAdapter adapter;

    private ListView contactListView;

    private UserInfoBean userInfoBean;


    IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onIMServiceConnected() {
            logger.d("groupselmgr#onIMServiceConnected");

            imService = imServiceConnector.getIMService();
            Intent intent = getIntent();
            curSessionKey = intent.getStringExtra(Constants.SESSION_ID_KEY);
            peerEntity = imService.getSessionManager().findPeerEntity(curSessionKey);
            /**已经处于选中状态的list*/
            Set<String> alreadyList = getAlreadyCheckList();
            initContactList(alreadyList);
        }

        @Override
        public void onServiceDisconnected() {}
    };



    @Override
    public void loadData() {
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
    }

    @Override
    public void initView() {
        initRes();

        imServiceConnector.connect(GroupMemberSelectActivity.this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        imServiceConnector.disconnect(GroupMemberSelectActivity.this);
        super.onDestroy();
    }

    /**
     * 获取列表中 默认选中成员列表
     * @return
     */
    private Set<String> getAlreadyCheckList(){
        Set<String> alreadyListSet = new HashSet<>();
        if(peerEntity == null){
            Toast.makeText(GroupMemberSelectActivity.this, getString(R.string.error_group_info), Toast.LENGTH_SHORT).show();
            finish(GroupMemberSelectActivity.this);
            logger.e("[fatal error,groupInfo is null,cause by SESSION_TYPE_GROUP]");
            //return Collections.emptySet();
        }
        switch (peerEntity.getType()){
            case DBConstant.SESSION_TYPE_GROUP:{
                GroupEntity entity = (GroupEntity) peerEntity;
                alreadyListSet.addAll(entity.getlistGroupMemberIds());
            }break;

            case DBConstant.SESSION_TYPE_SINGLE:{
                String loginId = userInfoBean.getJujuNo();
                alreadyListSet.add(loginId);
                alreadyListSet.add(peerEntity.getPeerId());
            }break;
        }
        return alreadyListSet;
    }

    private void initContactList(final Set<String> alreadyList) {
        // 根据拼音排序
        adapter = new GroupSelectAdapter(GroupMemberSelectActivity.this, imService);
        contactListView.setAdapter(adapter);

        contactListView.setOnItemClickListener(adapter);
        contactListView.setOnItemLongClickListener(adapter);

        List<User> contactList = imService.getContactManager().getContactSortedList();
        adapter.setAllUserList(contactList);
        adapter.setAlreadyListSet(alreadyList);
    }

    @Override
    public void onTouchingLetterChanged(String s) {
        int position = adapter.getPositionForSection(s.charAt(0));
        if (position != -1) {
            contactListView.setSelection(position);
        }
    }

    /**
     * @Description 初始化资源
     */
    private void initRes() {
        // 设置标题栏
        setTopTitle(getString(R.string.choose_contact));
        setTopRightText(R.string.confirm);
//        setTopLeftText(R.string.cancel);
        showTopLeftAll(R.string.cancel, 0);

        topRightTitleTxt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                logger.d("tempgroup#on 'save' btn clicked");

                if(adapter.getCheckListSet().size()<=0){
                    Toast.makeText(GroupMemberSelectActivity.this,
                            getString(R.string.select_group_member_empty), Toast.LENGTH_SHORT).show();
                    return;
                }

                Set<String> checkListSet =  adapter.getCheckListSet();
                IMGroupManager groupMgr = imService.getGroupManager();
                //从个人过来的，创建群，默认自己是加入的，对方的sessionId也是加入的
                //自己与自己对话，也能创建群的，这个时候要判断，群组成员一定要大于2个
                int sessionType = peerEntity.getType();
                if (sessionType == DBConstant.SESSION_TYPE_SINGLE) {
                    String loginId = userInfoBean.getJujuNo();
                    logger.d("tempgroup#loginId:%d", loginId);
                    checkListSet.add(loginId);
                    checkListSet.add(peerEntity.getPeerId());
                    logger.d("tempgroup#memberList size:%d", checkListSet.size());
                    ShowDialogForTempGroupname(groupMgr, checkListSet);
                } else if (sessionType == DBConstant.SESSION_TYPE_GROUP) {
//                    showProgressBar();
                    loading();
                    imService.getGroupManager().reqAddGroupMember(peerEntity.getPeerId(),checkListSet);
                }
            }


            private void ShowDialogForTempGroupname(final IMGroupManager groupMgr,final Set<String> memberList) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(GroupMemberSelectActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog));

                LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View dialog_view = inflater.inflate(R.layout.tt_custom_dialog, null);
                final EditText editText = (EditText)dialog_view.findViewById(R.id.dialog_edit_content);
                TextView textText = (TextView)dialog_view.findViewById(R.id.dialog_title);
                textText.setText(R.string.create_temp_group_dialog_title);
                builder.setView(dialog_view);

                builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tempGroupName = editText.getText().toString();
                        tempGroupName = tempGroupName.trim();
//                        showProgressBar();
//                        groupMgr.reqCreateTempGroup(tempGroupName,memberList);
                    }
                });
                builder.setNegativeButton(getString(R.string.negative), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        InputMethodManager inputManager =
                                (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(editText.getWindowToken(),0);
                    }
                });
                final AlertDialog alertDialog = builder.create();

                /**只有输入框中有值的时候,确定按钮才可以按下*/
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if(TextUtils.isEmpty(s.toString().trim())){
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }else{
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    }
                });

                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                /**对话框弹出的时候，下面的键盘也要跟上来*/
                Timer timer = new Timer();
                timer.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        InputMethodManager inputManager =
                                (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                        inputManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }, 100);
            }
        });

        sortSideBar = (SortSideBar) findViewById(R.id.sidrbar);
        sortSideBar.setOnTouchingLetterChangedListener(this);

        dialog = (TextView) findViewById(R.id.dialog);
        sortSideBar.setTextView(dialog);

        contactListView = (ListView) findViewById(R.id.all_contact_list);
        contactListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //如果存在软键盘，关闭掉
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                //txtName is a reference of an EditText Field
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        searchEditText = (SearchEditText) findViewById(R.id.filter_edit);
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String key = s.toString();
                if(TextUtils.isEmpty(key)){
                    adapter.recover();
                    sortSideBar.setVisibility(View.VISIBLE);
                }else{
                    sortSideBar.setVisibility(View.INVISIBLE);
                    adapter.onSearch(key);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
