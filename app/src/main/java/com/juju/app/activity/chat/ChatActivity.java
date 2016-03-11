package com.juju.app.activity.chat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.juju.app.R;
import com.juju.app.adapter.ChatAdapter;
import com.juju.app.annotation.CreateUI;
import com.juju.app.entity.MessageInfo;
import com.juju.app.golobal.Constants;
import com.juju.app.helper.MessageHelper;
import com.juju.app.tools.Emoparser;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.EmoGridView;
import com.juju.app.view.MGProgressbar;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;
import com.lidroid.xutils.view.annotation.event.OnFocusChange;
import com.lidroid.xutils.view.annotation.event.OnTouch;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 项目名称：juju
 * 类描述：聊天--Activity
 * 创建人：gm
 * 日期：2016/2/25 11:52
 * 版本：V1.0.0
 */
@ContentView(R.layout.activity_chat)
@CreateUI
public class ChatActivity extends BaseActivity implements CreateUIHelper,
        PullToRefreshBase.OnRefreshListener2<ListView>,
        TextWatcher,
        SensorEventListener {

    private Logger logger = Logger.getLogger(ChatActivity.class);


    //    private Logger logger = Logger.getLogger(getClass());
    private static Handler uiHandler = null;// 处理界面消息
    private static Handler msgHandler = null;// 处理协议消息




    private ImageView soundVolumeImg = null;
    private LinearLayout soundVolumeLayout = null;
//    private static MessageAdapter adapter = null;



    private String audioSavePath = null;
    private InputMethodManager inputManager = null;
    private boolean textChanged = false;
//    private AudioRecordHandler audioRecorderInstance = null;
    private Thread audioRecorderThread = null;
    private Dialog soundVolumeDialog = null;
    private View unreadMessageNotifyView = null;


//    private AlbumHelper albumHelper = null;
//    private static List<ImageBucket> albumList = null;
    MGProgressbar progressbar = null;
    private boolean audioReday = false;
    static private AudioManager audioManager = null;
    static private SensorManager sensorManager = null;
    static private Sensor sensor = null;
//    static private int audioPlayMode = SysConstant.AUDIO_PLAY_MODE_NORMAL;
    private String takePhotoSavePath = "";
    // 避免用户信息与商品详情的重复请求
    public static boolean requestingGoodsDetail = false;
    public static boolean requestingUserInfo = false;
//    private IMServiceHelper imServiceHelper = new IMServiceHelper();
//    private IMService imService;
//    private IMSession session = new IMSession(imServiceHelper);
    private int MSG_CNT_PER_PAGE = 15;
    private int firstHistoryMsgTime = -1;
    private boolean imServiceConnectionEnabled = false;

    public static final int REQUEST_CODE_COPY_AND_PASTE = 11;
    public static final String COPY_IMAGE = "EASEMOBIMG";

    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_right)
    private ImageView img_right;
    @ViewInject(R.id.show_add_photo_btn)
    private ImageView addPhotoBtn;
    @ViewInject(R.id.show_emo_btn)
    private ImageView addEmoBtn;
    @ViewInject(R.id.emo_gridview)
    private EmoGridView emoGridView;
    @ViewInject(R.id.message_text)
    private EditText messageEdt;
    @ViewInject(R.id.send_message_btn)
    private TextView sendBtn;
    @ViewInject(R.id.message_list)
    private PullToRefreshListView lvPTR; // 列表控件(开源PTR)
    @ViewInject(R.id.record_voice_btn)
    private Button recordAudioBtn;
    @ViewInject(R.id.voice_btn)
    private ImageView audioInputImg;
    @ViewInject(R.id.show_keyboard_btn)
    private ImageView keyboardInputImg;
    @ViewInject(R.id.add_others_panel)
    private View addOthersPanelView;
    @ViewInject(R.id.take_photo_btn)
    private View takePhotoBtn;
    @ViewInject(R.id.take_camera_btn)
    private View takeCameraBtn;


    private String sessionId;
    private String groupName;

    private static ChatAdapter adapter = null;


    /**
     * 初始化群聊信息
     */
    private void initGroupInfo() {
//        img_right.setImageResource(R.mipmap.icon_groupinfo);
//        chatType = getIntent().getIntExtra(Constants.TYPE, CHATTYPE_SINGLE);
    }




    @Override
    public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {

    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {

    }



    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
//        scrollToBottomListItem();
        if (s.length() > 0) {
            String strMsg = messageEdt.getText().toString();
            CharSequence emoCharSeq = Emoparser.getInstance(ChatActivity.this).emoCharsequence(strMsg);
            if (!textChanged) {
                textChanged = true;
                messageEdt.setText(emoCharSeq);
                Editable edtable = messageEdt.getText();
                int position = edtable.length();
                Selection.setSelection(edtable, position);
            } else {
                textChanged = false;
            }
            sendBtn.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) messageEdt.getLayoutParams();
            param.addRule(RelativeLayout.LEFT_OF, R.id.send_message_btn);
            addPhotoBtn.setVisibility(View.GONE);
        } else {
            addPhotoBtn.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) messageEdt.getLayoutParams();
            param.addRule(RelativeLayout.LEFT_OF, R.id.show_add_photo_btn);
            sendBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }


    @Override
    public void loadData() {
        sessionId = ChatActivity.this.getIntent().getStringExtra(Constants.SESSION_ID_KEY);
        groupName = ChatActivity.this.getIntent().getStringExtra(Constants.GROUP_NAME_KEY);
        initEmoData();
    }

    @Override
    public void initView() {
        //显示返回菜单
        img_back.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.group_chat);
        txt_left.setVisibility(View.VISIBLE);
        txt_title.setText(groupName);
        img_right.setImageResource(R.mipmap.icon_groupinfo);

        // 未读消息提示
        unreadMessageNotifyView = new View(this);
        unreadMessageNotifyView.setBackgroundResource(R.mipmap.tt_unread_message_notify_bg);
        final int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, width);
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        lp.topMargin = width - 4;
        lp.rightMargin = width - 5;
        unreadMessageNotifyView.setLayoutParams(lp);
        unreadMessageNotifyView.setVisibility(View.GONE);

        // 输入对象
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // 表情
        emoGridView.setOnEmoGridViewItemClick(new EmoGridView.OnEmoGridViewItemClick() {
            @Override
            public void onItemClick(int facesPos, int viewIndex) {
                int deleteId = (++viewIndex) * (Constants.pageSize - 1);
                if (deleteId > Emoparser.getInstance(ChatActivity.this).getResIdList().length) {
                    deleteId = Emoparser.getInstance(ChatActivity.this).getResIdList().length;
                }
                if (deleteId == facesPos) {
                    String msgContent = messageEdt.getText().toString();
                    if (msgContent.isEmpty())
                        return;
                    if (msgContent.contains("["))
                        msgContent = msgContent.substring(0, msgContent.lastIndexOf("["));
                    messageEdt.setText(msgContent);
                } else {
                    int resId = Emoparser.getInstance(ChatActivity.this).getResIdList()[facesPos];
                    String pharse = Emoparser.getInstance(ChatActivity.this).getIdPhraseMap().get(resId);
                    int startIndex = messageEdt.getSelectionStart();
                    Editable edit = messageEdt.getEditableText();
                    if (startIndex < 0 || startIndex >= edit.length()) {
                        if (null != pharse) {
                            edit.append(pharse);
                        }
                    } else {
                        if (null != pharse) {
                            edit.insert(startIndex, pharse);
                        }
                    }
                }
                Editable edtable = messageEdt.getText();
                int position = edtable.length();
                Selection.setSelection(edtable, position);
            }
        });
        emoGridView.setAdapter();

        Drawable loadingDrawable = getResources().getDrawable(R.drawable.pull_to_refresh_indicator);
        final int indicatorWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29, getResources().getDisplayMetrics());
        loadingDrawable.setBounds(new Rect(0, indicatorWidth, 0, indicatorWidth));
        lvPTR.getLoadingLayoutProxy().setLoadingDrawable(loadingDrawable);

        lvPTR.getRefreshableView().setCacheColorHint(Color.WHITE);
        lvPTR.getRefreshableView().setSelector(new ColorDrawable(Color.WHITE));
        lvPTR.getRefreshableView().setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (emoGridView.getVisibility() == View.VISIBLE) {
                        emoGridView.setVisibility(View.GONE);
                    }

                    if (addOthersPanelView.getVisibility() == View.VISIBLE) {
                        addOthersPanelView.setVisibility(View.GONE);
                    }
                    inputManager.hideSoftInputFromWindow(messageEdt.getWindowToken(), 0);
                }
                return false;
            }
        });
        lvPTR.getRefreshableView().addHeaderView(LayoutInflater.from(this).
                inflate(R.layout.tt_messagelist_header, lvPTR.getRefreshableView(), false));
        adapter = new ChatAdapter(this);
//        adapter.setSession(session);
//        adapter.setIMServiceHelper(imServiceHelper);
        lvPTR.setAdapter(adapter);
        lvPTR.setOnRefreshListener(this);

        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) messageEdt.getLayoutParams();
        param.addRule(RelativeLayout.LEFT_OF, R.id.show_add_photo_btn);
        param.addRule(RelativeLayout.RIGHT_OF, R.id.show_emo_btn);

        initSoundVolumeDlg();

//        View takePhotoBtn = findViewById(R.id.take_photo_btn);
//        View takeCameraBtn = findViewById(R.id.take_camera_btn);

        // 初始化滚动条(注意放到最后)
        View view = LayoutInflater.from(ChatActivity.this).inflate(R.layout.tt_progress_ly, null);
        progressbar = (MGProgressbar) view.findViewById(R.id.tt_progress);
        RelativeLayout.LayoutParams pgParms = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        pgParms.bottomMargin = 50;
        addContentView(view, pgParms);

        // 绑定各控件事件监听对象
        messageEdt.addTextChangedListener(this);

    }

    //结束群聊
    @OnClick(R.id.img_back)
    private void goBack(ImageView view) {
        ActivityUtil.finish(ChatActivity.this);
    }

    //结束群聊
    @OnClick(R.id.txt_left)
    private void goBack1(TextView view) {
        ActivityUtil.finish(ChatActivity.this);
    }

    @OnClick(R.id.show_add_photo_btn)
    private void onClick4AddPhotoBtn(View view) {
        if (addOthersPanelView.getVisibility() == View.VISIBLE) {
            addOthersPanelView.setVisibility(View.GONE);
        } else if (addOthersPanelView.getVisibility() == View.GONE) {
            addOthersPanelView.setVisibility(View.VISIBLE);
            inputManager.hideSoftInputFromWindow(messageEdt.getWindowToken(), 0);
        }
        if (null != emoGridView
                && emoGridView.getVisibility() == View.VISIBLE) {
            emoGridView.setVisibility(View.GONE);
        }
        scrollToBottomListItem();
    }

    @OnClick(R.id.show_emo_btn)
    private void onClick4AddEmoBtn(View view) {
        inputManager.hideSoftInputFromWindow(messageEdt.getWindowToken(), 0);
        if (emoGridView.getVisibility() == View.GONE) {
            emoGridView.setVisibility(View.VISIBLE);
        } else if (emoGridView.getVisibility() == View.VISIBLE) {
            emoGridView.setVisibility(View.GONE);
        }

        if (addOthersPanelView.getVisibility() == View.VISIBLE) {
            addOthersPanelView.setVisibility(View.GONE);
        }
        scrollToBottomListItem();
    }

    @OnClick(R.id.send_message_btn)
    private void onClick4SendBtn(View view) {
        logger.d("chatactivity#send btn clicked");
        String content = messageEdt.getText().toString();
        logger.d("chatactivity#chat content:%s", content);
        if (content.trim().equals("")) {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.message_null, 0);
            return;
        }
        MessageInfo msg = MessageHelper.obtainTextMessage("1", content);
        if (msg != null) {
            addItem(msg);
//            if (session != null) {
//                session.sendText(session.getType(), msg);
//            }
            messageEdt.setText("");
        }
        scrollToBottomListItem();
    }

    /**
     * @Description 初始化表情数据（表情）
     */
    private void initEmoData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Emoparser.getInstance(ChatActivity.this);
            }
        }).start();
    }

    /**
     * @Description 向消息列表适配器中添加一条消息
     * @param msgInfo
     */
    public static void addItem(MessageInfo msgInfo) {
        Logger logger = Logger.getLogger(ChatActivity.class);
        logger.d("chat#addItem msgInfo:%s", msgInfo);
        adapter.addItem(msgInfo);
        //数据变更通知
        adapter.notifyDataSetChanged();
    }


    @OnClick(R.id.message_text)
    private void onClick4TxtMessage(View view) {
        if (addOthersPanelView.getVisibility() == View.VISIBLE) {
            addOthersPanelView.setVisibility(View.GONE);
        }
        emoGridView.setVisibility(View.GONE);
    }

    @OnClick(R.id.take_photo_btn)
    private void onClick4BtnTakePhoto(View view) {
//        if (albumList.size() < 1) {
//            Toast.makeText(ChatActivity.this, getResources().getString(R.string.not_found_album), Toast.LENGTH_LONG).show();
//            return;
//        }
//        Intent intent = new Intent(ChatActivity.this, PickPhotoActivity.class);
//        intent.putExtra(SysConstant.EXTRA_CHAT_USER_ID, session.getSessionId());
//        startActivityForResult(intent, SysConstant.ALBUM_BACK_DATA);
//        MessageActivity.this.overridePendingTransition(R.anim.tt_album_enter, R.anim.tt_stay);
//        addOthersPanelView.setVisibility(View.GONE);
        scrollToBottomListItem();
    }

    @OnClick(R.id.take_camera_btn)
    private void onClick4BtnTakCamera(View view) {
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        takePhotoSavePath = CommonUtil.getImageSavePath(String.valueOf(System.currentTimeMillis())
//                + ".jpg");
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(takePhotoSavePath)));
//        startActivityForResult(intent, SysConstant.CAMERA_WITH_DATA);
//        addOthersPanelView.setVisibility(View.GONE);
        scrollToBottomListItem();
    }

    @OnClick(R.id.show_keyboard_btn)
    private void onClick4BtnShowKeyboard(View view) {
        recordAudioBtn.setVisibility(View.GONE);
        keyboardInputImg.setVisibility(View.GONE);
        messageEdt.setVisibility(View.VISIBLE);
        audioInputImg.setVisibility(View.VISIBLE);
        addEmoBtn.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.voice_btn)
    private void onClick4BtnVoice(View view) {
        inputManager.hideSoftInputFromWindow(messageEdt.getWindowToken(), 0);
        messageEdt.setVisibility(View.GONE);
        audioInputImg.setVisibility(View.GONE);
        recordAudioBtn.setVisibility(View.VISIBLE);
        keyboardInputImg.setVisibility(View.VISIBLE);
        emoGridView.setVisibility(View.GONE);
        addOthersPanelView.setVisibility(View.GONE);
        addEmoBtn.setVisibility(View.GONE);
        messageEdt.setText("");
    }



    @OnFocusChange(R.id.message_text)
    private void onFocusChange4TxtMessage(View v, boolean hasFocus) {
        if (hasFocus) {
            scrollToBottomListItem();
            if (emoGridView.getVisibility() == View.VISIBLE) {
                emoGridView.setVisibility(View.GONE);
            }
            if (addOthersPanelView.getVisibility() == View.VISIBLE) {
                addOthersPanelView.setVisibility(View.GONE);
            }
        }
    }


    @OnTouch(R.id.record_voice_btn)
    private void onTouch4TxtMessage(View v, MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            if (AudioPlayerHandler.getInstance().isPlaying())
//                AudioPlayerHandler.getInstance().stopPlayer();
//            y1 = event.getY();
//            recordAudioBtn.setBackgroundResource(R.drawable.tt_pannel_btn_voiceforward_pressed);
//            recordAudioBtn.setText(MessageActivity.this.getResources().getString(R.string.release_to_send_voice));
//
//            soundVolumeImg.setImageResource(R.drawable.tt_sound_volume_01);
//            soundVolumeImg.setVisibility(View.VISIBLE);
//            soundVolumeLayout.setBackgroundResource(R.drawable.tt_sound_volume_default_bk);
//            soundVolumeDialog.show();
//            audioSavePath = CommonUtil.getAudioSavePath(CacheHub.getInstance().getLoginUserId());
//            audioRecorderInstance = new AudioRecordHandler(audioSavePath, new TaskCallback() {
//
//                @Override
//                public void callback(Object result) {
//                    logger.d("messageactivity#audio#in callback");
//                    if (audioReday) {
//                        if (msgHandler != null) {
//                            logger.d("messageactivity#audio#send record finish message");
//
//                            Message msg = uiHandler.obtainMessage();
//                            msg.what = HandlerConstant.HANDLER_RECORD_FINISHED;
//                            msg.obj = audioRecorderInstance.getRecordTime();
//                            uiHandler.sendMessage(msg);
//                        }
//                    }
//                }
//            });
//            audioRecorderThread = new Thread(audioRecorderInstance);
//            audioReday = false;
//            audioRecorderInstance.setRecording(true);
//            logger.d("messageactivity#audio#audio record thread starts");
//            audioRecorderThread.start();
//        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
//            y2 = event.getY();
//            if (y1 - y2 > 50) {
//                soundVolumeImg.setVisibility(View.GONE);
//                soundVolumeLayout.setBackgroundResource(R.drawable.tt_sound_volume_cancel_bk);
//            } else {
//                soundVolumeImg.setVisibility(View.VISIBLE);
//                soundVolumeLayout.setBackgroundResource(R.drawable.tt_sound_volume_default_bk);
//            }
//        } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            // if (!StateManager.getInstance().isOnline()) {
//            // Toast.makeText(
//            // MessageActivity.this,
//            // getResources().getString(
//            // R.string.disconnected_by_server),
//            // Toast.LENGTH_LONG).show();
//            // if (soundVolumeDialog.isShowing()) {
//            // soundVolumeDialog.dismiss();
//            // }
//            // return false;
//            // }
//            if (audioRecorderInstance.isRecording()) {
//                audioRecorderInstance.setRecording(false);
//            }
//            if (soundVolumeDialog.isShowing()) {
//                soundVolumeDialog.dismiss();
//            }
//            recordAudioBtn.setBackgroundResource(R.drawable.tt_pannel_btn_voiceforward_normal);
//            recordAudioBtn.setText(MessageActivity.this.getResources().getString(R.string.tip_for_voice_forward));
//            if (y1 - y2 <= 50) {
//                if (audioRecorderInstance.getRecordTime() >= 0.5) {
//                    if (audioRecorderInstance.getRecordTime() < SysConstant.MAX_SOUND_RECORD_TIME) {
//                        audioReday = true;
//                    }
//                } else {
//                    soundVolumeImg.setVisibility(View.GONE);
//                    soundVolumeLayout.setBackgroundResource(R.drawable.tt_sound_volume_short_tip_bk);
//                    soundVolumeDialog.show();
//                    Timer timer = new Timer();
//                    timer.schedule(new TimerTask() {
//                        public void run() {
//                            if (soundVolumeDialog.isShowing())
//                                soundVolumeDialog.dismiss();
//                            this.cancel();
//                        }
//                    }, 700);
//                }
//            }
//        }
    }



    /**
     * @Description 滑动到列表底部
     */
    private void scrollToBottomListItem() {
        ListView lv = lvPTR.getRefreshableView();
        if (lv != null) {
            lv.setSelection(adapter.getCount() + 1);
        }
    }

    /**
     * @Description 初始化音量对话框
     */
    private void initSoundVolumeDlg() {
        soundVolumeDialog = new Dialog(this, R.style.SoundVolumeStyle);
        soundVolumeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        soundVolumeDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        soundVolumeDialog.setContentView(R.layout.tt_sound_volume_dialog);
        soundVolumeDialog.setCanceledOnTouchOutside(true);
        soundVolumeImg = (ImageView) soundVolumeDialog.findViewById(R.id.sound_volume_img);
        soundVolumeLayout = (LinearLayout) soundVolumeDialog.findViewById(R.id.sound_volume_bk);
    }
}
