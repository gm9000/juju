package com.juju.app.activity.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.juju.app.R;
import com.juju.app.adapter.ChatAdapter;
import com.juju.app.adapter.album.AlbumHelper;
import com.juju.app.adapter.album.ImageBucket;
import com.juju.app.adapter.album.ImageItem;
import com.juju.app.annotation.CreateUI;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.AudioMessage;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.ImageMessage;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.entity.chat.TextMessage;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.MessageEvent;
import com.juju.app.event.PriorityEvent;
import com.juju.app.event.SelectEvent;
import com.juju.app.event.SmallMediaEvent;
import com.juju.app.event.notify.ApplyInGroupEvent;
import com.juju.app.event.notify.ExitGroupEvent;
import com.juju.app.event.notify.InviteInGroupEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.event.notify.MasterTransferEvent;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.event.notify.RemoveGroupEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.HandlerConstant;
import com.juju.app.golobal.IntentConstant;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.audio.AudioPlayerHandler;
import com.juju.app.service.im.audio.AudioRecordHandler;
import com.juju.app.service.im.manager.IMGroupManager;
import com.juju.app.tools.Emoparser;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.CommonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.SystemConfigSp;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.EmoGridView;
import com.juju.app.view.MGProgressbar;
import com.juju.app.view.groupchat.YayaEmoGridView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.apache.commons.lang.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
@CreateUI(showTopView = true)
public class ChatActivity extends BaseActivity implements CreateUIHelper,
        PullToRefreshBase.OnRefreshListener2<ListView>,
        TextWatcher,
        View.OnTouchListener,
        SensorEventListener {
    private Logger logger = Logger.getLogger(ChatActivity.class);
    private InputMethodManager inputManager = null;
//    private Dialog soundVolumeDialog = null;

    private MGProgressbar progressbar = null;
    private static SensorManager sensorManager = null;
    private static  Sensor sensor = null;
    private static ChatAdapter adapter = null;
    private IMService imService;

    @ViewInject(R.id.show_add_photo_btn)
    private ImageView addPhotoBtn;
    @ViewInject(R.id.show_emo_btn)
    private ImageView addEmoBtn;
    @ViewInject(R.id.emo_gridview)
    private EmoGridView emoGridView;

    //消息输入框
    @ViewInject(R.id.message_text)
    private EditText messageEdt;

    //发送消息按钮
    @ViewInject(R.id.send_message_btn)
    private TextView sendBtn;

    // 列表控件(开源PTR)
    @ViewInject(R.id.message_list)
    private PullToRefreshListView lvPTR;

    @ViewInject(R.id.record_voice_btn)
    private Button recordAudioBtn;
    @ViewInject(R.id.voice_btn)
    private ImageView audioInputImg;
    @ViewInject(R.id.show_keyboard_btn)
    private ImageView keyboardInputImg;
    @ViewInject(R.id.add_others_panel)
    private View addOthersPanelView;
//    @ViewInject(R.id.take_photo_btn)
//    private View takePhotoBtn;
//    @ViewInject(R.id.take_camera_btn)
//    private View takeCameraBtn;
//    @ViewInject(R.id.take_small_video)
//    private View takeSmallVideoBtn;
    @ViewInject(R.id.emo_layout)
    private LinearLayout emoLayout;
    @ViewInject(R.id.tt_new_msg_tip)
    private TextView textView_new_msg_tip;



    //声音相关组件
    @ViewInject(R.id.sound_volume_img)
    private ImageView soundVolumeImg;

    @ViewInject(R.id.sound_volume_bk)
    private LinearLayout soundVolumeLayout;





    //键盘布局相关参数
    int rootBottom = Integer.MIN_VALUE, keyboardHeight = 0;
    private SwitchInputMethodReceiver receiver;
    private String currentInputMethod;


    private UserEntity loginUser;

    //测试使用
    private PeerEntity peerEntity;

    // 当前的session
    private String currentSessionKey;

    private int historyTimes = 0;

    private static Handler uiHandler = null; //处理语音

    private UserInfoBean userInfoBean;

    private AlbumHelper albumHelper = null;
    private List<ImageBucket> albumList = null;

    private String takePhotoSavePath = "";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(receiver);
        imServiceConnector.disconnect(this);
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK != resultCode)
            return;
        switch (requestCode) {
            case Constants.CAMERA_WITH_DATA:
                handleTakePhotoData(data);
                break;
            case Constants.ALBUM_BACK_DATA:
                logger.d("pic#ALBUM_BACK_DATA");
                setIntent(data);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void loadData() {
        userInfoBean = AppContext.getUserInfoBean();
        EventBus.getDefault().register(this);
        initHandler();
        imServiceConnector.connect(this);
        currentSessionKey =  ChatActivity.this.getIntent().getStringExtra(Constants.SESSION_ID_KEY);
        UserInfoBean userInfoBean = AppContext.getUserInfoBean();

        loginUser = new UserEntity();
        loginUser.setGender(1);
        loginUser.setPeerId(userInfoBean.getUserNo() + "@"+userInfoBean.getmServiceName());
        String[] sessionKeyArr = currentSessionKey.split("_");
        if(sessionKeyArr.length > 1) {
            //测试使用
            peerEntity = new PeerEntity() {
                @Override
                public int getType() {
                    return DBConstant.SESSION_TYPE_GROUP;
                }
            };
            peerEntity.setPeerId(sessionKeyArr[1]);
        }


    }

    @Override
    public void initView() {
        initTitleView();
        initSoftInputMethod();
        initEmo();
        initAlbumHelper();
        initAudioSensor();
        initViews();

    }

    /**
     * 绑定事件
     */
    @Override
    protected void setOnListener() {
        super.setOnListener();
        messageEdt.addTextChangedListener(this);
        recordAudioBtn.setOnTouchListener(this);

    }


    //"十字"按键
    @Event(R.id.show_add_photo_btn)
    private void onClick4AddPhotoBtn(View view) {
        recordAudioBtn.setVisibility(View.GONE);
        keyboardInputImg.setVisibility(View.GONE);
        messageEdt.setVisibility(View.VISIBLE);
        audioInputImg.setVisibility(View.VISIBLE);
        addEmoBtn.setVisibility(View.VISIBLE);

        if (keyboardHeight != 0) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        if (addOthersPanelView.getVisibility() == View.VISIBLE) {
            if (!messageEdt.hasFocus()) {
                messageEdt.requestFocus();
            }
            inputManager.toggleSoftInputFromWindow(messageEdt.getWindowToken(), 1, 0);
            if (keyboardHeight == 0) {
                addOthersPanelView.setVisibility(View.GONE);
            }
        } else if (addOthersPanelView.getVisibility() == View.GONE) {
            addOthersPanelView.setVisibility(View.VISIBLE);
            inputManager.hideSoftInputFromWindow(messageEdt.getWindowToken(), 0);
        }
        if (null != emoLayout
                && emoLayout.getVisibility() == View.VISIBLE) {
            emoLayout.setVisibility(View.GONE);
        }

        scrollToBottomListItem();
    }

    //"表情"按键
    @Event(R.id.show_emo_btn)
    private void onClick4AddEmoBtn(View view) {
        recordAudioBtn.setVisibility(View.GONE);
        keyboardInputImg.setVisibility(View.GONE);
        messageEdt.setVisibility(View.VISIBLE);
        audioInputImg.setVisibility(View.VISIBLE);
        addEmoBtn.setVisibility(View.VISIBLE);

        if (keyboardHeight != 0) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        if (emoLayout.getVisibility() == View.VISIBLE) {
            if (!messageEdt.hasFocus()) {
                messageEdt.requestFocus();
            }
            inputManager.toggleSoftInputFromWindow(messageEdt.getWindowToken(), 1, 0);
            if (keyboardHeight == 0) {
                emoLayout.setVisibility(View.GONE);
            }
        } else if (emoLayout.getVisibility() == View.GONE) {
            emoLayout.setVisibility(View.VISIBLE);
//            yayaEmoGridView.setVisibility(View.VISIBLE);
//            emoRadioGroup.check(R.id.tab1);
            emoGridView.setVisibility(View.VISIBLE);
            inputManager.hideSoftInputFromWindow(messageEdt.getWindowToken(), 0);
        }
        if (addOthersPanelView.getVisibility() == View.VISIBLE) {
            addOthersPanelView.setVisibility(View.GONE);
        }
    }

    //"发送"按键
    @Event(R.id.send_message_btn)
    private void onClick4SendBtn(View view) {
        logger.d("chat_activity#send btn clicked");
        String content = messageEdt.getText().toString();
        logger.d("chat_activity#chat content:%s", content);
        if (content.trim().equals("")) {
            Toast.makeText(ChatActivity.this,
                    getResources().getString(R.string.message_null), Toast.LENGTH_LONG).show();
            return;
        }
        if(peerEntity != null) {
            //构造消息实体
            MessageEntity textMessage = TextMessage.buildForSend(content, loginUser, peerEntity);
            imService.getMessageManager().sendText(textMessage);
            messageEdt.setText("");
            pushList(textMessage);
        } else {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.chat_is_null, 0);

        }
        scrollToBottomListItem();
    }


    @Event(R.id.tt_new_msg_tip)
    private void onClick4NewMsgTip(View view) {
        scrollToBottomListItem();
        textView_new_msg_tip.setVisibility(View.GONE);
    }



    /**
     * @Description 初始化表情数据（表情）
     */
//    private void initEmoData() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Emoparser.getInstance(ChatActivity.this);
//            }
//        }).start();
//    }

//    /**
//     * @Description 向消息列表适配器中添加一条消息
//     * @param msgInfo
//     */
//    public static void addItem(MessageInfo msgInfo) {
//        Logger logger = Logger.getLogger(ChatActivity.class);
//        logger.d("chat#addItem msgInfo:%s", msgInfo);
//        adapter.addItem(msgInfo);
//        //数据变更通知
//        adapter.notifyDataSetChanged();
//    }



    @Event(R.id.take_photo_btn)
    private void onClick4BtnTakePhoto(View view) {
        if (albumList.size() < 1) {
            Toast.makeText(ChatActivity.this,
                    getResources().getString(R.string.not_found_album), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        // 选择图片的时候要将session的整个回话 传过来
        Intent intent = new Intent(ChatActivity.this, PickPhotoActivity.class);
        intent.putExtra(IntentConstant.KEY_SESSION_KEY, currentSessionKey);
        startActivityForResult(intent, Constants.ALBUM_BACK_DATA);

        ChatActivity.this.overridePendingTransition(R.anim.tt_album_enter, R.anim.tt_stay);
        //addOthersPanelView.setVisibility(View.GONE);
        messageEdt.clearFocus();//切记清除焦点
        scrollToBottomListItem();
    }

    @Event(R.id.take_camera_btn)
    private void onClick4BtnTakCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoSavePath = CommonUtil.getImageSavePath(String.valueOf(System.currentTimeMillis())
                + ".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(takePhotoSavePath)));
        startActivityForResult(intent, Constants.CAMERA_WITH_DATA);
        addOthersPanelView.setVisibility(View.GONE);
        scrollToBottomListItem();
    }

    @Event(R.id.take_small_video)
    private void onClick4BtnTakeSmallVideo(View view) {
        startActivityNew(ChatActivity.this, SmallMediaRecorderActivity.class);
        scrollToBottomListItem();
    }

    @Event(R.id.show_keyboard_btn)
    private void onClick4BtnShowKeyboard(View view) {
        recordAudioBtn.setVisibility(View.GONE);
        keyboardInputImg.setVisibility(View.GONE);
        messageEdt.setVisibility(View.VISIBLE);
        audioInputImg.setVisibility(View.VISIBLE);
        addEmoBtn.setVisibility(View.VISIBLE);
    }

    @Event(R.id.voice_btn)
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



    private String audioSavePath = null;
    private AudioRecordHandler audioRecorderInstance = null;
    private Thread audioRecorderThread = null;



    /**
     * @Description 滑动到列表底部
     */
    private void scrollToBottomListItem() {
        ListView lv = lvPTR.getRefreshableView();
        if (lv != null) {
            lv.setSelection(adapter.getCount() + 1);
        }
        textView_new_msg_tip.setVisibility(View.GONE);
    }

    /**
     * @Description 初始化音量对话框
     */
    private void initSoundVolumeDlg() {
//        soundVolumeDialog = new Dialog(this, R.style.SoundVolumeStyle);
//        soundVolumeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        soundVolumeDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        soundVolumeDialog.setContentView(R.layout.tt_sound_volume_dialog);
//        soundVolumeDialog.setCanceledOnTouchOutside(true);
//        soundVolumeImg = (ImageView) soundVolumeDialog.findViewById(R.id.sound_volume_img);
//        soundVolumeLayout = (LinearLayout) soundVolumeDialog.findViewById(R.id.sound_volume_bk);
    }


    private void initSoftInputMethod() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        receiver = new SwitchInputMethodReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.INPUT_METHOD_CHANGED");
        registerReceiver(receiver, filter);

        SystemConfigSp.instance().init(this);
        currentInputMethod = Settings.Secure.getString(ChatActivity.this.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        keyboardHeight = SystemConfigSp.instance().getIntConfig(currentInputMethod);
    }



    /**
     * 初始化群聊信息
     */
    private void initTitleView() {
        //显示返回菜单
//        img_back.setVisibility(View.VISIBLE);
//        txt_left.setText(R.string.group_chat);
//        txt_left.setVisibility(View.VISIBLE);

        showTopLeftAll(R.string.group_chat, 0);
        topRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityNew(ChatActivity.this, GroupManagerActivity.class,
                        Constants.SESSION_ID_KEY, currentSessionKey);
            }
        });

        String[] sessionKeyArr = currentSessionKey.split("_");
        if(sessionKeyArr.length > 1) {
            GroupEntity groupEntity = IMGroupManager.instance().getGroupMap().get(sessionKeyArr[1]);
//            txt_title.setText(groupEntity.getMainName());
            setTopTitle(groupEntity.getMainName());
        }
//        img_right.setImageResource(R.mipmap.icon_groupinfo);
        setTopRightButton(R.mipmap.icon_groupinfo);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        logger.d("onTouchonTouchonTouch-> event:%s", event.toString());
        int id = v.getId();
        scrollToBottomListItem();
        if (id == R.id.record_voice_btn) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (AudioPlayerHandler.getInstance().isPlaying())
                    AudioPlayerHandler.getInstance().stopPlayer();
                y1 = event.getY();
                recordAudioBtn.setBackgroundResource(R.drawable.tt_pannel_btn_voiceforward_pressed);
                recordAudioBtn.setText(ChatActivity.this.getResources().getString(
                        R.string.release_to_send_voice));

//                soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_01);
//                soundVolumeImg.setVisibility(View.VISIBLE);
//                soundVolumeLayout.setBackgroundResource(R.mipmap.tt_sound_volume_default_bk);
//                soundVolumeDialog.show();

//                ChatActivity.this.showMsgDialog(R.string.join_group_send_failed);


                soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_01);
                soundVolumeImg.setVisibility(View.VISIBLE);
                soundVolumeLayout.setBackgroundResource(R.mipmap.tt_sound_volume_default_bk);
                soundVolumeLayout.setVisibility(View.VISIBLE);

                audioSavePath = CommonUtil
                        .getAudioSavePath(userInfoBean.getUserNo());

                // 这个callback很蛋疼，发送消息从MotionEvent.ACTION_UP 判断
                audioRecorderInstance = new AudioRecordHandler(audioSavePath);
                logger.d("onTouch4RecordVoiceBtn -> audioSavePath:%s", audioSavePath);
                audioRecorderThread = new Thread(audioRecorderInstance);
                audioRecorderInstance.setRecording(true);
                logger.d("message_activity#audio#audio record thread starts");
                audioRecorderThread.start();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                y2 = event.getY();
                if (y1 - y2 > 180) {
                    soundVolumeImg.setVisibility(View.GONE);
                    soundVolumeLayout.setBackgroundResource(R.mipmap.tt_sound_volume_cancel_bk);
                } else {
                    soundVolumeImg.setVisibility(View.VISIBLE);
                    soundVolumeLayout.setBackgroundResource(R.mipmap.tt_sound_volume_default_bk);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                y2 = event.getY();
                if (audioRecorderInstance.isRecording()) {
                    audioRecorderInstance.setRecording(false);
                }

//                if (soundVolumeDialog.isShowing()) {
//                    soundVolumeDialog.dismiss();
//                }

                if(soundVolumeLayout.getVisibility() == View.VISIBLE) {
                    soundVolumeLayout.setVisibility(View.GONE);
                }

                recordAudioBtn.setBackgroundResource(R.drawable.tt_pannel_btn_voiceforward_normal);
                recordAudioBtn.setText(ChatActivity.this.getResources().getString(
                        R.string.tip_for_voice_forward));
                if (y1 - y2 <= 180) {
                    if (audioRecorderInstance.getRecordTime() >= 0.5) {
                        if (audioRecorderInstance.getRecordTime() < Constants.MAX_SOUND_RECORD_TIME) {
                            Message msg = uiHandler.obtainMessage();
                            msg.what = HandlerConstant.HANDLER_RECORD_FINISHED;
                            msg.obj = audioRecorderInstance.getRecordTime();
                            uiHandler.sendMessage(msg);
                        }
                    } else {
                        soundVolumeImg.setVisibility(View.GONE);
                        soundVolumeLayout
                                .setBackgroundResource(R.mipmap.tt_sound_volume_short_tip_bk);
//                        soundVolumeDialog.show();
//                        if(soundVolumeLayout.getVisibility() == View.VISIBLE) {
//
//                        }
                        soundVolumeLayout.setVisibility(View.VISIBLE);

                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            public void run() {
//                                if (soundVolumeDialog.isShowing())
//                                    soundVolumeDialog.dismiss();
                                if(soundVolumeLayout.getVisibility() == View.VISIBLE) {
                                    soundVolumeLayout.setVisibility(View.GONE);
                                }
                                this.cancel();
                            }
                        }, 700);
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                y2 = event.getY();
                if (audioRecorderInstance.isRecording()) {
                    audioRecorderInstance.setRecording(false);
                }
//                if (soundVolumeDialog.isShowing()) {
//                    soundVolumeDialog.dismiss();
//                }
                if(soundVolumeLayout.getVisibility() == View.VISIBLE) {
                    soundVolumeLayout.setVisibility(View.GONE);
                }
                recordAudioBtn.setBackgroundResource(R.drawable.tt_pannel_btn_voiceforward_normal);
                recordAudioBtn.setText(ChatActivity.this.getResources().getString(
                        R.string.tip_for_voice_forward));
                if (y1 - y2 <= 180) {
                    if (audioRecorderInstance.getRecordTime() >= 0.5) {
                        if (audioRecorderInstance.getRecordTime() < Constants.MAX_SOUND_RECORD_TIME) {
                            Message msg = uiHandler.obtainMessage();
                            msg.what = HandlerConstant.HANDLER_RECORD_FINISHED;
                            msg.obj = audioRecorderInstance.getRecordTime();
                            uiHandler.sendMessage(msg);
                        }
                    } else {
                        soundVolumeImg.setVisibility(View.GONE);
                        soundVolumeLayout
                                .setBackgroundResource(R.mipmap.tt_sound_volume_short_tip_bk);
                        soundVolumeLayout.setVisibility(View.VISIBLE);
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            public void run() {
//                                if (soundVolumeDialog.isShowing())
//                                    soundVolumeDialog.dismiss();
                                if(soundVolumeLayout.getVisibility() == View.VISIBLE) {
                                    soundVolumeLayout.setVisibility(View.GONE);
                                }
                                this.cancel();
                            }
                        }, 700);
                    }
                }
            }
        }
        return false;
    }



    private class SwitchInputMethodReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.INPUT_METHOD_CHANGED")) {
                currentInputMethod = Settings.Secure.getString(ChatActivity.this.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
                SystemConfigSp.instance().setStrConfig(SystemConfigSp.SysCfgDimension.DEFAULTINPUTMETHOD, currentInputMethod);
                int height =  SystemConfigSp.instance().getIntConfig(currentInputMethod);
                if(keyboardHeight!=height)
                {
                    keyboardHeight = height;
                    addOthersPanelView.setVisibility(View.GONE);
                    emoLayout.setVisibility(View.GONE);
                    ChatActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    messageEdt.requestFocus();
                    if(keyboardHeight!=0 && addOthersPanelView.getLayoutParams().height!=keyboardHeight)
                    {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) addOthersPanelView.getLayoutParams();
                        params.height = keyboardHeight;
                    }
                    if(keyboardHeight!=0 && emoLayout.getLayoutParams().height!=keyboardHeight)
                    {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emoLayout.getLayoutParams();
                        params.height = keyboardHeight;
                    }
                }
                else
                {
                    addOthersPanelView.setVisibility(View.VISIBLE);
                    emoLayout.setVisibility(View.VISIBLE);
                    ChatActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                    messageEdt.requestFocus();
                }
            }
        }
    }

    private void initEmo() {
        Emoparser.getInstance(ChatActivity.this);
//        IMApplication.gifRunning = true;
    }

    /**
     * @Description 初始化数据（相册,表情,数据库相关）
     */
    private void initAlbumHelper() {
        albumHelper = AlbumHelper.getHelper(ChatActivity.this);
        albumList = albumHelper.getImagesBucketList(false);
    }

    /**
     * @Description 初始化AudioManager，用于访问控制音量和钤声模式
     */
    private void initAudioSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * @Description 初始化界面控件
     * 有点庞大 todo
     */
    private void initViews() {
        // 列表控件(开源PTR)
        lvPTR.getRefreshableView().addHeaderView(LayoutInflater.from(this).inflate(R.layout.tt_messagelist_header,lvPTR.getRefreshableView(), false));
        Drawable loadingDrawable = getResources().getDrawable(R.drawable.pull_to_refresh_indicator);
        final int indicatorWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29,
                getResources().getDisplayMetrics());
        loadingDrawable.setBounds(new Rect(0, indicatorWidth, 0, indicatorWidth));
        lvPTR.getLoadingLayoutProxy().setLoadingDrawable(loadingDrawable);
        lvPTR.getRefreshableView().setCacheColorHint(Color.WHITE);
        lvPTR.getRefreshableView().setSelector(new ColorDrawable(Color.WHITE));
        lvPTR.getRefreshableView().setOnTouchListener(lvPTROnTouchListener);
        adapter = new ChatAdapter(this);
        adapter.setImService(null, loginUser);
        lvPTR.setAdapter(adapter);
        lvPTR.setOnRefreshListener(this);
        lvPTR.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true) {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        if (view.getLastVisiblePosition() == (view.getCount() - 1)) {
                            textView_new_msg_tip.setVisibility(View.GONE);
                        }
                        break;
                }
            }
        });
//        textView_new_msg_tip.setOnClickListener(this);

        // 界面底部输入框布局
        RelativeLayout.LayoutParams messageEdtParam = (RelativeLayout.LayoutParams) messageEdt.getLayoutParams();
        messageEdtParam.addRule(RelativeLayout.LEFT_OF, R.id.show_emo_btn);
        messageEdtParam.addRule(RelativeLayout.RIGHT_OF, R.id.voice_btn);

//        messageEdt.setOnFocusChangeListener(msgEditOnFocusChangeListener);
//        messageEdt.setOnClickListener(this);
//        messageEdt.addTextChangedListener(this);
//        addPhotoBtn.setOnClickListener(this);
//        addEmoBtn.setOnClickListener(this);
//        keyboardInputImg.setOnClickListener(this);
//        audioInputImg.setOnClickListener(this);
//        recordAudioBtn.setOnTouchListener(this);
//        sendBtn.setOnClickListener(this);
        initSoundVolumeDlg();

        //OTHER_PANEL_VIEW
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) addOthersPanelView.getLayoutParams();
        if (keyboardHeight > 0) {
            params.height = keyboardHeight;
            addOthersPanelView.setLayoutParams(params);
        }
//        takePhotoBtn.setOnClickListener(this);
//        takeCameraBtn.setOnClickListener(this);

        //EMO_LAYOUT
        RelativeLayout.LayoutParams paramEmoLayout = (RelativeLayout.LayoutParams) emoLayout.getLayoutParams();
        if (keyboardHeight > 0) {
            paramEmoLayout.height = keyboardHeight;
            emoLayout.setLayoutParams(paramEmoLayout);
        }
        emoGridView.setOnEmoGridViewItemClick(onEmoGridViewItemClick);
        emoGridView.setAdapter();
//        yayaEmoGridView.setOnEmoGridViewItemClick(yayaOnEmoGridViewItemClick);
//        yayaEmoGridView.setAdapter();
//        emoRadioGroup.setOnCheckedChangeListener(emoOnCheckedChangeListener);


        //LOADING
        View view = LayoutInflater.from(ChatActivity.this)
                .inflate(R.layout.tt_progress_ly, null);
        progressbar = (MGProgressbar) view.findViewById(R.id.tt_progress);
        RelativeLayout.LayoutParams pgParms = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        pgParms.bottomMargin = 50;
        addContentView(view, pgParms);

        //ROOT_LAYOUT_LISTENER
//        baseRoot.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    /**
     * 单条信息
     * @param msg
     */
    private void pushList(MessageEntity msg) {
        logger.d("chat#pushList msgInfo:%s", msg);
        adapter.addItem(msg);
    }

    /**
     * 多条信息
     * @param entityList
     */
    public void pushList(List<MessageEntity> entityList) {
        logger.d("chat#pushList list:%d", entityList.size());
        adapter.loadHistoryList(entityList);
    }

    private View.OnTouchListener lvPTROnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                messageEdt.clearFocus();
                if (emoLayout.getVisibility() == View.VISIBLE) {
                    emoLayout.setVisibility(View.GONE);
                }

                if (addOthersPanelView.getVisibility() == View.VISIBLE) {
                    addOthersPanelView.setVisibility(View.GONE);
                }
                inputManager.hideSoftInputFromWindow(messageEdt.getWindowToken(), 0);
            }
            return false;
        }
    };

    private View.OnFocusChangeListener msgEditOnFocusChangeListener = new android.view.View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                if (keyboardHeight == 0) {
                    addOthersPanelView.setVisibility(View.GONE);
                    emoLayout.setVisibility(View.GONE);
                } else {
                    ChatActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                    if (addOthersPanelView.getVisibility() == View.GONE) {
                        addOthersPanelView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    };

    private EmoGridView.OnEmoGridViewItemClick onEmoGridViewItemClick = new EmoGridView.OnEmoGridViewItemClick() {
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
                String pharse = Emoparser.getInstance(ChatActivity.this).getIdPhraseMap()
                        .get(resId);
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
    };



    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Rect r = new Rect();
//            baseRoot.getGlobalVisibleRect(r);
            // 进入Activity时会布局，第一次调用onGlobalLayout，先记录开始软键盘没有弹出时底部的位置
            if (rootBottom == Integer.MIN_VALUE) {
                rootBottom = r.bottom;
                return;
            }
            // adjustResize，软键盘弹出后高度会变小
            if (r.bottom < rootBottom) {
                //按照键盘高度设置表情框和发送图片按钮框的高度
                keyboardHeight = rootBottom - r.bottom;
                SystemConfigSp.instance().init(ChatActivity.this);
                SystemConfigSp.instance().setIntConfig(currentInputMethod, keyboardHeight);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) addOthersPanelView.getLayoutParams();
                params.height = keyboardHeight;
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) emoLayout.getLayoutParams();
                params1.height = keyboardHeight;
            }
        }
    };


    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() > 0) {
            sendBtn.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) messageEdt
                    .getLayoutParams();
            param.addRule(RelativeLayout.LEFT_OF, R.id.show_emo_btn);
            addPhotoBtn.setVisibility(View.GONE);
        } else {
            addPhotoBtn.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) messageEdt
                    .getLayoutParams();
            param.addRule(RelativeLayout.LEFT_OF, R.id.show_emo_btn);
            sendBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

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
    public void onPullDownToRefresh(final PullToRefreshBase<ListView> refreshView) {
        // 获取消息
        refreshView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ListView mlist = lvPTR.getRefreshableView();
                int preSum = mlist.getCount();
                MessageEntity messageEntity = adapter.getTopMsgEntity();
                if (messageEntity != null) {
                    List<MessageEntity> historyMsgInfo = imService.getMessageManager()
                            .loadHistoryMsg(messageEntity, historyTimes);
                    if (historyMsgInfo.size() > 0) {
                        historyTimes++;
                        adapter.loadHistoryList(historyMsgInfo);
                    }
                }

                int afterSum = mlist.getCount();
                mlist.setSelection(afterSum - preSum);
                /**展示位置为这次消息的最末尾*/
                //mlist.setSelection(size);
                // 展示顶部
//                if (!(mlist).isStackFromBottom()) {
//                    mlist.setStackFromBottom(true);
//                }
//                mlist.setStackFromBottom(false);
                refreshView.onRefreshComplete();
            }
        }, 200);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {

    }


    @Subscribe(threadMode = ThreadMode.MAIN, priority = Constants.MESSAGE_EVENTBUS_PRIORITY)
    public void onEventMain4MessageEvent(MessageEvent event) {
        MessageEvent.Event type = event.getEvent();
        MessageEntity entity = event.getMessageEntity();
        switch (type) {
            case ACK_SEND_MESSAGE_OK: {
                onMsgAck(event.getMessageEntity());
            }
            break;
            case ACK_SEND_MESSAGE_FAILURE:
                // 失败情况下新添提醒
                ToastUtil.TextIntToast(getApplicationContext(), R.string.message_send_failed, 0);
            case ACK_SEND_MESSAGE_TIME_OUT: {
//                onMsgUnAckTimeoutOrFailure(event.getMessageEntity());
            }
            break;

            case HANDLER_IMAGE_UPLOAD_FAILD: {
                logger.d("pic#onUploadImageFaild");
                ImageMessage imageMessage = (ImageMessage) event.getMessageEntity();
                adapter.updateItemState(imageMessage);
                ToastUtil.TextIntToast(getApplicationContext(), R.string.message_send_failed, 0);
            }
            break;
            case HANDLER_IMAGE_UPLOAD_SUCCESS: {
                ImageMessage imageMessage = (ImageMessage) event.getMessageEntity();
                adapter.updateItemState(imageMessage);
            }
            break;

            case HISTORY_MSG_OBTAIN: {
                if (historyTimes == 1) {
                    adapter.clearItem();
                    reqHistoryMsg();
                }
            }
            break;
        }
    }

    /**
     * 背景: 1.EventBus的cancelEventDelivery的只能在postThread中运行，而且没有办法绕过这一点
     * 2. onEvent(A a)  onEventMainThread(A a) 这个两个是没有办法共存的
     * 解决: 抽离出那些需要优先级的event，在onEvent通过handler调用主线程，
     * 然后cancelEventDelivery
     * <p/>
     * todo  need find good solution
     */
    @Subscribe(threadMode = ThreadMode.POSTING, priority = Constants.MESSAGE_EVENTBUS_PRIORITY)
    public void onEvent4PriorityEvent(PriorityEvent event) {
        switch (event.event) {
            case MSG_RECEIVED_MESSAGE: {
                MessageEntity entity = (MessageEntity) event.object;
                /**正式当前的会话*/
                if (currentSessionKey.equals(entity.getSessionKey())) {
                    Message message = Message.obtain();
                    message.what = HandlerConstant.MSG_RECEIVED_MESSAGE;
                    message.obj = entity;
                    uiHandler.sendMessage(message);
                    //取消事件传递（阻止IMService监听到此事件）
                    EventBus.getDefault().cancelEventDelivery(event);
                }
            }
            break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4InviteUser(InviteUserEvent event) {
        switch (event.event) {
            case INVITE_USER_OK:
                InviteUserEvent.InviteUserBean inviteUserBean  = event.bean;
                if(currentSessionKey.indexOf(inviteUserBean.groupId) >= 0) {
                    User fromUser = imService.getContactManager().findContact(userInfoBean.getUserNo());
                    GroupEntity groupEntity = imService.getGroupManager().findGroupById(inviteUserBean.groupId);
                    //需考虑批量邀请
                    String content = "你邀请"+inviteUserBean.nickName+"加入了群聊";
                    TextMessage textMessage = TextMessage.buildForSend2Notify(inviteUserBean.replyId,
                            inviteUserBean.replyTime, content, fromUser.getPeerId(), groupEntity.getPeerId());
                    onMsgRecv(textMessage);

                    MessageEntity messageEntity = textMessage.clone();
                    imService.getMessageManager().replaceInto(messageEntity);
                }
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4InviteInGroup(InviteInGroupEvent event) {
        switch (event.event) {
            case RECV_INVITE_IN_GROUP_OK:
                InviteInGroupEvent.InviteInGroupBean inviteInGroupBean  = event.bean;
                if(currentSessionKey.indexOf(inviteInGroupBean.groupId) >= 0) {
                    if(userInfoBean.getUserNo().equals(inviteInGroupBean.invitorNo)) {
                        break;
                    } else {
                        User fromUser = imService.getContactManager().findContact(inviteInGroupBean.invitorNo);
                        GroupEntity groupEntity = imService.getGroupManager().findGroupById(inviteInGroupBean.groupId);
                        //需考虑批量邀请
                        String content = fromUser.getNickName()+"邀请"+inviteInGroupBean.nickName+"加入了群聊";
                        TextMessage textMessage = TextMessage.buildForSend2Notify(inviteInGroupBean.replyId,
                                inviteInGroupBean.replyTime, content, fromUser.getPeerId(), groupEntity.getPeerId());
                        onMsgRecv(textMessage);

                        MessageEntity messageEntity = textMessage.clone();
                        imService.getMessageManager().replaceInto(messageEntity);
                    }
                }
                break;
        }
    }


    //TODO 暂时这样处理
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4GroupEvent(GroupEvent event) {
        switch (event.getChangeType()) {
            case DBConstant.GROUP_MODIFY_TYPE_DEL:
                String id = event.getChangeList().get(0);
                if(currentSessionKey.indexOf(id) >= 0) {
                    finish(ChatActivity.this);
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4ApplyInGroupEvent(ApplyInGroupEvent event){
        logger.d("ChatActivity#ApplyInGroupEvent# -> %s", event);
        ApplyInGroupEvent.ApplyInGroupBean applyInGroupBean  = event.bean;
        if(currentSessionKey.indexOf(applyInGroupBean.groupId) >= 0) {
            switch (event.event){
                case RECV_APPLY_IN_GROUP_OK:
                    if(userInfoBean.getUserNo().equals(applyInGroupBean.userNo)) {
                        break;
                    } else {
                        User fromUser = imService.getContactManager().findContact(applyInGroupBean.userNo);
                        GroupEntity groupEntity = imService.getGroupManager().findGroupById(applyInGroupBean.groupId);
                        //需考虑批量邀请
                        String content = fromUser.getNickName()+"扫码加入了群聊";
                        TextMessage textMessage = TextMessage.buildForSend2Notify(applyInGroupBean.replyId,
                                applyInGroupBean.replyTime, content,fromUser.getPeerId(), groupEntity.getPeerId());
                        onMsgRecv(textMessage);

                        MessageEntity messageEntity = textMessage.clone();
                        imService.getMessageManager().replaceInto(messageEntity);
                    }
                    break;
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4ExitGroupEvent(ExitGroupEvent event){
        logger.d("ChatActivity#ExitGroupEvent# -> %s", event);
        ExitGroupEvent.ExitGroupBean exitGroupBean  = event.bean;
        if(currentSessionKey.indexOf(exitGroupBean.groupId) >= 0) {
            GroupEntity groupEntity = imService.getGroupManager().findGroupById(exitGroupBean.groupId);
            String groupPeerId = exitGroupBean.groupId+"@"+userInfoBean.getmMucServiceName()
                    +"."+userInfoBean.getmServiceName();
            User fromUser = null;
            String content = null;
            switch (event.event){
//                case SEND_EXIT_GROUP_OK:
//                    fromUser = imService.getContactManager().findContact(groupEntity.getMasterId());
//                    content = "你将"+exitGroupBean.nickname+"移除群聊";
//                    TextMessage textMessage4Send = TextMessage.buildForSend2Notify(exitGroupBean.replyId,
//                            exitGroupBean.replyTime, content, fromUser.getPeerId(), groupPeerId);
//                    onMsgRecv(textMessage4Send);
//                    MessageEntity messageEntity4Send = textMessage4Send.clone();
//                    imService.getMessageManager().replaceInto(messageEntity4Send);
//                    break;
                case RECV_EXIT_GROUP_OK:
                    if(userInfoBean.getUserNo().equals(exitGroupBean.userNo)) {
                        //是否需要提醒
                        finish(ChatActivity.this);
                        break;
                    } else {
                        //管理员移除
                        if(exitGroupBean.flag == 0) {
                            fromUser = imService.getContactManager().findContact(groupEntity.getMasterId());
                            User targetUser = imService.getContactManager().findContact(exitGroupBean.userNo);
                            content = targetUser.getNickName()+"被管理员移出群聊";
                        }
                        //用户主动退群
                        else if (exitGroupBean.flag == 1) {
                            fromUser = imService.getContactManager().findContact(exitGroupBean.userNo);
                            content = fromUser.getNickName()+"退出群聊";
                        }
//                    GroupEntity groupEntity = imService.getGroupManager().findGroupById(exitGroupBean.groupId);

                        //需考虑批量邀请
                        TextMessage textMessage = TextMessage.buildForSend2Notify(exitGroupBean.replyId,
                                exitGroupBean.replyTime, content, fromUser.getPeerId(), groupPeerId);
                        onMsgRecv(textMessage);

                        MessageEntity messageEntity = textMessage.clone();
                        imService.getMessageManager().replaceInto(messageEntity);
                    }
                    break;
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4RemoveGroupEvent(RemoveGroupEvent event){
        logger.d("ChatActivity#RemoveGroupEvent# -> %s", event);
        RemoveGroupEvent.RemoveGroupBean removeGroupBean  = event.bean;
        if(currentSessionKey.indexOf(removeGroupBean.groupId) >= 0) {
            String toPeerId = removeGroupBean.userNo+"@"+userInfoBean.getmServiceName();
            User fromUser = null;
            User toUser = null;
            String content = null;
            switch (event.event){
                case SEND_REMOVE_GROUP_OK:
                    fromUser = imService.getContactManager().findContact(userInfoBean.getUserNo());
                    toUser = imService.getContactManager().findContact(removeGroupBean.userNo);
                    content = "你将"+toUser.getNickName()+"移除群聊";
                    TextMessage textMessage4Send = TextMessage.buildForSend2Notify(removeGroupBean.replyId,
                            removeGroupBean.replyTime, content, fromUser.getPeerId(), toPeerId);
                    onMsgRecv(textMessage4Send);
                    MessageEntity messageEntity4Send = textMessage4Send.clone();
                    imService.getMessageManager().replaceInto(messageEntity4Send);
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4RemoveGroupEvent(MasterTransferEvent event){
        logger.d("ChatActivity#RemoveGroupEvent# -> %s", event);
        MasterTransferEvent.MasterTransferBean masterTransferBean  = event.bean;
        if(currentSessionKey.indexOf(masterTransferBean.groupId) >= 0) {
            String toPeerId = masterTransferBean.groupId+"@"+userInfoBean.getmMucServiceName()+"."
                    +userInfoBean.getmServiceName();
            User fromUser = null;
            String content = null;
            switch (event.event){
                case SEND_MASTER_TRANSFER_OK:
                    fromUser = imService.getContactManager().findContact(userInfoBean.getUserNo());
                    content = "你将管理员转让给"+masterTransferBean.masterNickName;
                    TextMessage textMessage4Send = TextMessage.buildForSend2Notify(masterTransferBean.replyId,
                            masterTransferBean.replyTime, content, fromUser.getPeerId(), toPeerId);
                    onMsgRecv(textMessage4Send);
                    MessageEntity messageEntity4Send = textMessage4Send.clone();
                    imService.getMessageManager().replaceInto(messageEntity4Send);
                    break;
                case RECV_MASTER_TRANSFER_OK:
                    fromUser = imService.getContactManager().findContact(masterTransferBean.userNo);
                    if(masterTransferBean.masterNo.equals(userInfoBean.getUserNo())) {
                        content = masterTransferBean.nickname+"将管理员转让给你";
                    } else {
                        content = masterTransferBean.nickname+"将管理员转让给"+masterTransferBean.masterNickName;
                    }
                    TextMessage textMessage4Recv = TextMessage.buildForSend2Notify(masterTransferBean.replyId,
                            masterTransferBean.replyTime, content, fromUser.getPeerId(), toPeerId);
                    onMsgRecv(textMessage4Recv);
                    MessageEntity messageEntity4Recv = textMessage4Recv.clone();
                    imService.getMessageManager().replaceInto(messageEntity4Recv);
                    break;
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4PartyNotifyEvent(PartyNotifyEvent event){
        logger.d("ChatActivity#PartyNotifyEvent# -> %s", event);
        PartyNotifyEvent.PartyNotifyBean partyNotifyBean  = event.bean;
        if(currentSessionKey.indexOf(partyNotifyBean.getGroupId()) >= 0) {
            String toPeerId = partyNotifyBean.getGroupId()+"@"+userInfoBean.getmMucServiceName()+"."
                    +userInfoBean.getmServiceName();
            User fromUser = null;
            String content = null;
            switch (event.event){
                case PARTY_RECRUIT_OK:
                    fromUser = imService.getContactManager().findContact(partyNotifyBean.getUserNo());
                    if(partyNotifyBean.getUserNo().equals(userInfoBean.getUserNo())) {
                        content = "我发起了聚会“"+partyNotifyBean.getPartyName()+"”";
                    } else {
                        content = partyNotifyBean.getNickName()+"发起了聚会“"+partyNotifyBean.getPartyName()+"”";
                    }
                    TextMessage textMessage4Recv = TextMessage.buildForSend2Notify(partyNotifyBean.replyId,
                            partyNotifyBean.replyTime, content, fromUser.getPeerId(), toPeerId);
                    onMsgRecv(textMessage4Recv);
//                    MessageEntity messageEntity4Recv = textMessage4Recv.clone();
//                    imService.getMessageManager().replaceInto(messageEntity4Recv);
                    break;
                case PARTY_CANCEL_OK:
                    fromUser = imService.getContactManager().findContact(partyNotifyBean.getUserNo());
                    if(partyNotifyBean.getUserNo().equals(userInfoBean.getUserNo())) {
                        content = "我取消了聚会“"+partyNotifyBean.getPartyName()+"”";
                    } else {
                        content = partyNotifyBean.getNickName()+"取消了聚会“"+partyNotifyBean.getPartyName()+"”";
                    }
                    TextMessage partyCancelMsg4Recv = TextMessage.buildForSend2Notify(partyNotifyBean.replyId,
                            partyNotifyBean.replyTime, content, fromUser.getPeerId(), toPeerId);
                    onMsgRecv(partyCancelMsg4Recv);
                    break;

                case PARTY_CONFIRM_OK:
                    fromUser = imService.getContactManager().findContact(partyNotifyBean.getUserNo());
                    if(partyNotifyBean.getUserNo().equals(userInfoBean.getUserNo())) {
                        content = "我启动了聚会“"+partyNotifyBean.getPartyName()+"”";
                    } else {
                        content = partyNotifyBean.getNickName()+"启动了聚会“"+partyNotifyBean.getPartyName()+"”";
                    }
                    TextMessage partyConfirmMsg4Recv = TextMessage.buildForSend2Notify(partyNotifyBean.replyId,
                            partyNotifyBean.replyTime, content, fromUser.getPeerId(), toPeerId);
                    onMsgRecv(partyConfirmMsg4Recv);
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4SelectPhoto(SelectEvent event) {
        List<ImageItem> itemList = event.getList();
        if (itemList != null || itemList.size() > 0)
            handleImagePickData(itemList);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4SmallMedia(SmallMediaEvent event) {
            handleSmallMedia(event);
    }


    private void handleSmallMedia(SmallMediaEvent event) {

        //TODO 发送视频
//        imService.getMessageManager().sendMsgImages(listMsg);
    }


    private void handleImagePickData(List<ImageItem> list) {
        ArrayList<ImageMessage> listMsg = new ArrayList<>();
        ArrayList<ImageItem> itemList = (ArrayList<ImageItem>) list;
        for (ImageItem item : itemList) {
            ImageMessage imageMessage = ImageMessage.buildForSend(item, loginUser, peerEntity);
            listMsg.add(imageMessage);
            pushList(imageMessage);
        }
        //TODO 发送图片
        imService.getMessageManager().sendMsgImages(listMsg);
    }


    /**
     * [备注] DB保存，与session的更新manager已经做了
     *
     * @param messageEntity
     */
    private void onMsgAck(MessageEntity messageEntity) {
        logger.d("message_activity#onMsgAck");
        int msgId = messageEntity.getMsgId();
        logger.d("chat#onMsgAck, msgId:%d", msgId);

        /**到底采用哪种ID呐??*/
//        long localId = messageEntity.getId();
        adapter.updateItemState(messageEntity);
    }


    // 肯定是在当前的session内
    private void onMsgRecv(MessageEntity entity) {
        logger.d("chat_activity#onMsgRecv");
//        imService.getUnReadMsgManager().ackReadMsg(entity);
        logger.d("chat#start pushList");
        pushList(entity);
        ListView lv = lvPTR.getRefreshableView();
        if (lv != null) {
            //提示新消息
            if (lv.getLastVisiblePosition() < adapter.getCount()) {
                textView_new_msg_tip.setVisibility(View.VISIBLE);
            } else {
                scrollToBottomListItem();
            }
        }
    }

    protected void initHandler() {
        uiHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case HandlerConstant.MSG_RECEIVED_MESSAGE:
                        MessageEntity entity = (MessageEntity) msg.obj;
                        onMsgRecv(entity);
                        imService.getUnReadMsgManager().onNotifyRead(entity);
                        break;

                    case HandlerConstant.HANDLER_RECORD_FINISHED:
                        onRecordVoiceEnd((Float) msg.obj);
                        break;

                    // 录音结束
                    case HandlerConstant.HANDLER_STOP_PLAY:
                        // 其他地方处理了
                        //adapter.stopVoicePlayAnim((String) msg.obj);
                        break;

                    case HandlerConstant.RECEIVE_MAX_VOLUME:
                        onReceiveMaxVolume((Integer) msg.obj);
                        break;

                    case HandlerConstant.RECORD_AUDIO_TOO_LONG:
                        doFinishRecordAudio();
                        break;

                    default:
                        break;
                }
            }
        };
    }


    private void initData() {
        historyTimes = 0;
        adapter.clearItem();
        ImageMessage.clearImageMessageList();
        // 头像、历史消息加载、取消通知
        setTitleByUser();
        reqHistoryMsg();
        adapter.setImService(imService, loginUser);
        //清除未读消息
        imService.getUnReadMsgManager().readUnreadSession(currentSessionKey);
        imService.getNotificationManager().cancelSessionNotifications(currentSessionKey);
    }

    /**
     * 设定聊天名称
     * 1. 如果是user类型， 点击触发UserProfile
     * 2. 如果是群组，检测自己是不是还在群中
     */
    private void setTitleByUser() {
        setTitle(peerEntity.getMainName());
        int peerType = peerEntity.getType();
//        switch (peerType) {
//            case DBConstant.SESSION_TYPE_GROUP: {
//                GroupEntity group = (GroupEntity) peerEntity;
//                Set<Integer> memberLists = group.getlistGroupMemberIds();
//                if (!memberLists.contains(loginUser.getPeerId())) {
//                    Toast.makeText(MessageActivity.this, R.string.no_group_member, Toast.LENGTH_SHORT).show();
//                }
//            }
//            break;
//            case DBConstant.SESSION_TYPE_SINGLE: {
//                topTitleTxt.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        IMUIHelper.openUserProfileActivity(MessageActivity.this, peerEntity.getPeerId());
//                    }
//                });
//            }
//            break;
//        }
    }

    /**
     * 1.初始化请求历史消息
     * 2.本地消息不全，也会触发
     */
    private void reqHistoryMsg() {
        historyTimes++;
        //拉取历史信息
        List<MessageEntity> msgList = imService.getMessageManager().
                loadHistoryMsg(historyTimes, currentSessionKey, peerEntity);
        pushList(msgList);
        scrollToBottomListItem();
    }


    /**
     * ImService
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("chat_activity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            initData();
        }

        @Override
        public void onServiceDisconnected() {

        }
    };



    /**
     * @param audioLen
     * @Description 录音结束后处理录音数据
     */
    private void onRecordVoiceEnd(float audioLen) {
        logger.d("message_activity#chat#audio#onRecordVoiceEnd audioLen:%f", audioLen);
        AudioMessage audioMessage = AudioMessage.buildForSend(audioLen, audioSavePath, loginUser, peerEntity);
        imService.getMessageManager().sendMsgAudio(audioMessage);
        pushList(audioMessage);
    }

    /**
     * @param voiceValue
     * @Description 根据分贝值设置录音时的音量动画
     */
    private void onReceiveMaxVolume(int voiceValue) {
        if (voiceValue < 200.0) {
            soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_01);
        } else if (voiceValue > 200.0 && voiceValue < 600) {
            soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_02);
        } else if (voiceValue > 600.0 && voiceValue < 1200) {
            soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_03);
        } else if (voiceValue > 1200.0 && voiceValue < 2400) {
            soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_04);
        } else if (voiceValue > 2400.0 && voiceValue < 10000) {
            soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_05);
        } else if (voiceValue > 10000.0 && voiceValue < 28000.0) {
            soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_06);
        } else if (voiceValue > 28000.0) {
            soundVolumeImg.setImageResource(R.mipmap.tt_sound_volume_07);
        }
    }

    /**
     * @Description 录音超时(60s)，发消息调用该方法
     */
    public void doFinishRecordAudio() {
        try {
            if (audioRecorderInstance.isRecording()) {
                audioRecorderInstance.setRecording(false);
            }
//            if (soundVolumeDialog.isShowing()) {
//                soundVolumeDialog.dismiss();
//            }
            if(soundVolumeLayout.getVisibility() == View.VISIBLE) {
                soundVolumeLayout.setVisibility(View.GONE);
            }

            recordAudioBtn.setBackgroundResource(R.drawable.tt_pannel_btn_voiceforward_normal);

            audioRecorderInstance.setRecordTime(Constants.MAX_SOUND_RECORD_TIME);
            onRecordVoiceEnd(Constants.MAX_SOUND_RECORD_TIME);
        } catch (Exception e) {
        }
    }


    /**
     * @param data
     * @Description 处理拍照后的数据
     * 应该是从某个 activity回来的
     */
    private void handleTakePhotoData(Intent data) {
        ImageMessage imageMessage = ImageMessage.buildForSend(takePhotoSavePath, loginUser, peerEntity);
        List<ImageMessage> sendList = new ArrayList<>(1);
        sendList.add(imageMessage);
        imService.getMessageManager().sendMsgImages(sendList);
        // 格式有些问题
        pushList(imageMessage);
        messageEdt.clearFocus();//消除焦点
    }

    public static Handler getUiHandler() {
        return uiHandler;
    }


}
