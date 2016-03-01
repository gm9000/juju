package com.juju.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.ClipboardManager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;

/**
 * 项目名称：juju
 * 类描述：聊天--Activity
 * 创建人：gm
 * 日期：2016/2/25 11:52
 * 版本：V1.0.0
 */
@ContentView(R.layout.activity_chat)
@CreateUI(isLoadData = true, isInitView = true)
public class ChatActivity extends BaseActivity implements CreateUIHelper {


    public static final int REQUEST_CODE_COPY_AND_PASTE = 11;
    public static final String COPY_IMAGE = "EASEMOBIMG";

    private InputMethodManager manager;
    private ClipboardManager clipboard;
    private PowerManager.WakeLock wakeLock;


    @ViewInject(R.id.btn_more)
    private Button btn_more;

    @ViewInject(R.id.more)
    private LinearLayout layout_more;

    @ViewInject(R.id.img_right)
    private ImageView img_right;

    @ViewInject(R.id.ll_btn_container)
    private LinearLayout layout_btn_container;

    @ViewInject(R.id.ll_face_container)
    private LinearLayout layout_face_container;

    @ViewInject(R.id.iv_emoticons_normal)
    private ImageView img_emoticons_normal;

    @ViewInject(R.id.iv_emoticons_checked)
    private ImageView img_emoticons_checked;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void loadData() {

    }

    @Override
    public void initView() {
        manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "demo");
        initGroupInfo();

    }

    /**
     * 显示或隐藏图标按钮页
     * @param view
     */
    public void more(View view) {
        if (layout_more.getVisibility() == View.GONE) {
            System.out.println("more gone");
            hideKeyboard();
            layout_more.setVisibility(View.VISIBLE);
            layout_btn_container.setVisibility(View.VISIBLE);
            layout_face_container.setVisibility(View.GONE);
        } else {
            if (layout_face_container.getVisibility() == View.VISIBLE) {
                layout_face_container.setVisibility(View.GONE);
                layout_btn_container.setVisibility(View.VISIBLE);
                img_emoticons_normal.setVisibility(View.VISIBLE);
                img_emoticons_checked.setVisibility(View.INVISIBLE);
            } else {
                layout_more.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 隐藏软键盘
     */
    private void hideKeyboard() {
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null)
                manager.hideSoftInputFromWindow(getCurrentFocus()
                        .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 初始化群聊信息
     */
    private void initGroupInfo() {
        img_right.setImageResource(R.mipmap.icon_groupinfo);
    }


}
