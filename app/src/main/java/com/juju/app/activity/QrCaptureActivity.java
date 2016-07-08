package com.juju.app.activity;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.juju.app.R;
import com.juju.app.activity.chat.GroupJoinInActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.annotation.SystemColor;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ToastUtil;
import com.juju.app.zxing.ViewfinderView;
import com.juju.app.zxing.camera.CameraManager;
import com.juju.app.zxing.decoding.InactivityTimer;
import com.juju.app.zxing.decoding.QrCaptureActivityHandler;

import org.apache.commons.lang.math.NumberUtils;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * 扫一扫
 */
@ContentView(R.layout.activity_qr_capture)
@CreateUI(showTopView = false)
@SystemColor(isApply = true)
public class QrCaptureActivity extends BaseActivity implements SurfaceHolder.Callback, CreateUIHelper {

    private Logger logger = Logger.getLogger(QrCaptureActivity.class);

    private QrCaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;

    private TextView mTitle;
    private ImageView mGoHome;
    private boolean isNoCute = true;


    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    @ViewInject(R.id.txt_title)
    private TextView txt_title;

    @ViewInject(R.id.img_back)
    private View img_back;

    @ViewInject(R.id.img_right)
    private View img_right;





    @Event(value = R.id.txt_left)
    private void onClickTxtLeft(View view) {
        finish(QrCaptureActivity.this);
    }

    @Event(value = R.id.img_back)
    private void onClickImaBack(View view) {
        finish(QrCaptureActivity.this);
    }

    /**
     * 加载数据
     */
    @Override
    public void loadData() {

    }

    /**
     * 初始化组件
     */
    @Override
    public void initView() {
        txt_left.setVisibility(View.VISIBLE);
        img_back.setVisibility(View.VISIBLE);
        img_right.setVisibility(View.GONE);
        txt_title.setText(R.string.menu_qrcode);

        CameraManager.init(getApplication());
        initControl();
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getBoolean("isNoCute")) {
            isNoCute = true;
        } else {
            isNoCute = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isNoCute) {
                finish(QrCaptureActivity.this);
            } else {
//				startActivity(CaptureActivity.this, LoginActivity.class);
                finish(QrCaptureActivity.this);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initControl() {
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
//        mTitle = (TextView) findViewById(R.id.common_title_msg);
//        mTitle.setText("扫一扫");
//        mGoHome = (ImageView) findViewById(R.id.img_back);
//        mGoHome.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish(QrCaptureActivity.this);
//            }
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    public void onDestroy() {
        if (inactivityTimer != null)
            inactivityTimer.shutdown();
        super.onDestroy();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new QrCaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    /**
     * 扫描正确后的震动声音,如果感觉apk大了,可以删除
     */
    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    public void handleDecode(com.google.zxing.Result result, Bitmap barcode) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        final String resultString = result.getText();
        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("qr_scan_result", resultString);
        resultIntent.putExtras(bundle);
        this.setResult(RESULT_OK, resultIntent);
        if (!isNoCute) {
            if (TextUtils.isEmpty(resultString)) {
                ToastUtil.TextIntToast(getApplicationContext(), R.string.qr_code_error, 0);
                return;
            } else {
                if (resultString.indexOf("?token=") >=0
                        && resultString.indexOf("juju") >=0) {
                    String content = resultString.substring(resultString.indexOf("?token="), resultString.length());
                    String jsonStr = content.split("=")[1];
                    GroupQrBean groupQrBean = JacksonUtil.turnString2Obj(jsonStr, GroupQrBean.class);
                    logger.d("handleDecode#扫描成功 -> groupId:%s,code:%s", groupQrBean.groupId, groupQrBean.code);
                    Map<String, String> valueMap = new HashMap<>();
                    valueMap.put("code", groupQrBean.code);
                    valueMap.put("groupId", groupQrBean.groupId);
                    startActivityNew(QrCaptureActivity.this, GroupJoinInActivity.class, valueMap);
                } else if (resultString.startsWith("http://")
                        || resultString.startsWith("https://")) {
                    Uri uri = Uri.parse(resultString);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } else {
                    ToastUtil.showLongToast(this, "扫描结果为：" + result);
                }
            }
        }
        finish(QrCaptureActivity.this);
    }

    public static class GroupQrBean {
        public String groupId;
        public String code;

        public GroupQrBean() {

        }

        public GroupQrBean(String groupId, String code) {
            this.groupId = groupId;
            this.code = code;
        }
    }

}
