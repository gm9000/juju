package com.juju.app.activity.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
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
import com.juju.app.biz.DaoSupport;
import com.juju.app.biz.impl.UserDaoImpl;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.event.UserInfoEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.golobal.CommandActionConstant;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.https.HttpCallBack4OK;
import com.juju.app.https.JlmHttpClient;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.HttpReqParamUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ThreadPoolUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.utils.json.JSONUtils;
import com.juju.app.utils.pinyin.PinYinUtil;
import com.juju.app.view.SearchEditText;
import com.juju.app.view.SortSideBar;

import org.apache.commons.lang.time.DateUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.view.annotation.ContentView;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@ContentView(R.layout.activity_group_member_select)
@CreateUI(showTopView = true)
public class GroupMemberSelectActivity extends BaseActivity implements CreateUIHelper,
        SortSideBar.OnTouchingLetterChangedListener {

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

    private DaoSupport userDao;


    IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onIMServiceConnected() {
            loading();
            logger.d("groupselmgr#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            Intent intent = getIntent();
            curSessionKey = intent.getStringExtra(Constants.SESSION_ID_KEY);
            peerEntity = imService.getSessionManager().findPeerEntity(curSessionKey);
            initUser4Phones();
            /**已经处于选中状态的list*/
            Set<String> alreadyList = getAlreadyCheckList();
            initContactList(alreadyList);
            completeLoading(0);
        }

        @Override
        public void onServiceDisconnected() {}
    };



    @Override
    public void loadData() {
        userDao = new UserDaoImpl(getApplicationContext());
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
//        logger.d("phones -> %s", phones);
    }

    @Override
    public void initView() {
        EventBus.getDefault().register(this);
        imServiceConnector.connect(GroupMemberSelectActivity.this);
        initRes();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
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
                String loginId = userInfoBean.getUserNo();
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
        notifyAdapter(alreadyList);
    }

    private void notifyAdapter(Set<String> alreadyList) {
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
                    String loginId = userInfoBean.getUserNo();
                    logger.d("tempgroup#loginId:%d", loginId);
                    checkListSet.add(loginId);
                    checkListSet.add(peerEntity.getPeerId());
                    logger.d("tempgroup#memberList size:%d", checkListSet.size());
                    ShowDialogForTempGroupname(groupMgr, checkListSet);
                } else if (sessionType == DBConstant.SESSION_TYPE_GROUP) {
                    loading();
                    imService.getGroupManager().reqAddGroupMember(peerEntity.getPeerId(),
                            peerEntity.getId(),  checkListSet);
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

    private void initUser4Phones() {
        List<String> phoneSet = getContactPhoneSet();
        List<String> oldPhoneSet = new ArrayList<>();
        int length = 100;

        if(phoneSet.size() >0) {
            Map<String, User> userMap = imService.getContactManager().getUserMap();
            Set<Map.Entry<String, User>> entrySet = userMap.entrySet();
            for(Map.Entry<String, User> entry : entrySet) {
                User user = entry.getValue();
//                if(StringUtils.isMobileNO(user.getUserPhone())) {
//                    oldPhoneSet.add(user.getUserPhone());
//                }
                oldPhoneSet.add(user.getUserPhone());
            }
            phoneSet.removeAll(oldPhoneSet);
            if(phoneSet.size() >0) {
                int total = phoneSet.size();
                int pageNum = phoneSet.size() / length;
                int mod = phoneSet.size() % length;
                if(mod != 0) {
                    pageNum = pageNum + 1;
                }
                CountDownLatch countDownLatch = new CountDownLatch(pageNum);
                for (int i = 1; i <= pageNum; i++) {
                    List<String> newPhoneSet = new ArrayList<>();
                    int beginIndex = (pageNum - 1) * length;
                    int endIndex = 0;
                    if(total < pageNum * length) {
                        endIndex = total;
                    } else {
                        endIndex = (pageNum * length);
                    }
                    newPhoneSet = phoneSet.subList(beginIndex, endIndex);
                    if(newPhoneSet.size() >0) {
                        GetExistUsersTask existUsersTask = new GetExistUsersTask(countDownLatch, newPhoneSet);
                        ThreadPoolUtil.instance().executeImTask(existUsersTask);
                    } else {
                        countDownLatch.countDown();
                    }
                }

                try {
                    countDownLatch.await(10, TimeUnit.SECONDS);
                    //更新列表
//                    Set<String> alreadyList = getAlreadyCheckList();
//                    notifyAdapter(alreadyList);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4JoinGroup(InviteUserEvent event) {
        switch (event.event) {
            case INVITE_USER_OK:
                completeLoading(0);
                ToastUtil.TextIntToast(getApplicationContext(), R.string.invite_user_send_success, 3);
                finish(GroupMemberSelectActivity.this);
                break;
            case INVITE_USER_FAILED:
                completeLoading(0);
                ToastUtil.TextIntToast(getApplicationContext(), R.string.invite_user_send_failed, 3);
                break;
        }
    }

//    public void onEventMainThread(UserInfoEvent event) {
//        switch (event) {
//            case USER_INFO_UPDATE:
//            case USER_INFO_OK:
//
//                break;
//        }
//    }

    /**
     * 获取联系人手机号
     * @return
     */
    private List<String> getContactPhoneSet() {
        List<String> phoneList = new ArrayList<>();
        // 获得所有的联系人
        Cursor cur = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        // 循环遍历
        if (cur.moveToFirst()) {
            int idColumn = cur.getColumnIndex(ContactsContract.Contacts._ID);
            int displayNameColumn = cur
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            do {
                // 获得联系人的ID号
                String contactId = cur.getString(idColumn);
                // 获得联系人姓名
                String disPlayName = cur.getString(displayNameColumn);
                // 查看该联系人有多少个电话号码。如果没有这返回值为0
                int phoneCount = cur
                        .getInt(cur
                                .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (phoneCount > 0) {
                    // 获得联系人的电话号码
                    Cursor phones = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                    + " = " + contactId, null, null);
                    if (phones.moveToFirst()) {
                        do { // 遍历所有的电话号码
                            String phoneNumber = phones
                                    .getString(phones
                                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).trim();
                            if (phoneNumber.startsWith("+186")) {
                                phoneNumber = phoneNumber.substring(4);
                            }
//                            if (StringUtils.isMobileNO(phoneNumber)) {
//                                phoneSet.add(phoneNumber);
//                            }
                            phoneNumber = phoneNumber.replaceAll(" ", "");
                            phoneList.add(phoneNumber);
                        } while (phones.moveToNext());
                    }
                }
            } while (cur.moveToNext());
        }
        return phoneList;
    }

    /**
     * 批量获取JLM用户列表
     */
    class GetExistUsersTask implements Runnable {

        private CountDownLatch countDownLatch;
        private List<String> phoneSet;

        public GetExistUsersTask(CountDownLatch countDownLatch, List<String>  phoneSet) {
            this.countDownLatch = countDownLatch;
            this.phoneSet = phoneSet;
        }

        @Override
        public void run() {
            Map<String, Object> valueMap = HttpReqParamUtil.instance().buildMap("phones", phoneSet);
            CommandActionConstant.HttpReqParam httpReqParam = CommandActionConstant.HttpReqParam.GETEXISTUSERS;
            JlmHttpClient<Map<String, Object>> client = new JlmHttpClient<>(httpReqParam.code(),
                    httpReqParam.url(), new HttpCallBack4OK() {
                @Override
                public void onSuccess4OK(Object obj, int accessId, Object inputParameter) {
                    if(obj instanceof JSONObject) {
                        handlerGetExistUsers4BServer((JSONObject)obj);
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void onFailure4OK(Exception e, int accessId, Object inputParameter) {
                    countDownLatch.countDown();
                }
            }, valueMap, JSONObject.class);
            try {
                client.sendPost4OK();
            } catch (UnsupportedEncodingException e) {
                logger.error(e);
                countDownLatch.countDown();
            } catch (JSONException e) {
                logger.error(e);
                countDownLatch.countDown();
            }
        }
    }

    //处理获取用户详情响应
    private void handlerGetExistUsers4BServer(JSONObject jsonObject) {
        int status = JSONUtils.getInt(jsonObject, "status", -1);
        if(status == 0) {
            JSONArray jsonUsers;
            try {
                jsonUsers = jsonObject.getJSONArray("users");
                if(jsonUsers != null && jsonUsers.length() >0) {
                    for (int i = 0; i < jsonUsers.length(); i++) {
                        JSONObject jsonUser = jsonUsers.getJSONObject(i);
                        String nickName = JSONUtils.getString(jsonUser, "nickName");
                        String userPhone = JSONUtils.getString(jsonUser, "userPhone");
                        String birthday = JSONUtils.getString(jsonUser, "birthday", "");
                        int gender = JSONUtils.getInt(jsonUser, "gender", 1);
                        String createTime = JSONUtils.getString(jsonUser, "createTime");
                        String userNo = JSONUtils.getString(jsonUser, "userNo");
                        Date birthdayDate = null;
                        Date createTimeDate = null;
                        if(StringUtils.isNotBlank(birthday)
                                && !"null".equals(birthday)) {
                            birthdayDate = DateUtils.parseDate(birthday,
                                    new String[] {"yyyy-MM-dd HH:mm:ss"});
                        }
                        if(StringUtils.isNotBlank(createTime) && !"null".equals(createTime)) {
                            createTimeDate = DateUtils.parseDate(createTime,
                                    new String[] {"yyyy-MM-dd HH:mm:ss"});
                        }
                        User user = User.buildForCreate(userNo, userPhone, null, gender, nickName,
                                birthdayDate, createTimeDate, HttpConstants.getPortraitUrl()+userNo);
                        userDao.replaceInto(user);
                        imService.getContactManager().getUserMap().put(userNo, user);
                        PinYinUtil.getPinYin(user.getNickName(), user.getPinyinElement());
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            logger.e("GETEXISTUSERS is faild");
        }
    }
}
