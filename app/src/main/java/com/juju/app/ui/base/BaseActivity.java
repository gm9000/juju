package com.juju.app.ui.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

//import com.baidu.mapapi.SDKInitializer;
import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.annotation.SystemColor;
import com.juju.app.golobal.AppContext;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.TipsToastUtil;
import com.juju.app.view.CustomDialog;
import com.juju.app.view.SearchEditText;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.greenrobot.eventbus.EventBus;
import org.xutils.view.annotation.ContentView;
import org.xutils.x;

/**
 * 项目名称：juju
 * 类描述：Activity父类
 * 创建人：gm
 * 日期：2016/2/17 10:34
 * 版本：V1.0.0
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static TipsToastUtil tipsToast;

    protected Context context = null;

    private RelativeLayout loading_layout;

    private TextView txt_status_message;

    protected ViewGroup topContentView;

    protected ImageView topLeftBtn;

    protected ImageView topRightBtn;

    protected TextView topTitleTxt;

    protected TextView topLetTitleTxt;

    protected TextView topRightTitleTxt;

    protected ViewGroup topBar;

    protected SearchEditText topSearchEdt;

    protected FrameLayout searchFrameLayout;

    protected float x1, y1, x2, y2 = 0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        final CreateUI createUI = this.getClass().getAnnotation(CreateUI.class);
        if(createUI != null && createUI.showTopView()) {
            //初始化Top
            initTopView();
        }
        if(topContentView != null) {
            final ContentView contentView = this.getClass().getAnnotation(ContentView.class);
            LayoutInflater.from(this).inflate(contentView.value(), topContentView, true);
            x.view().inject(this, topContentView);
            setContentView(topContentView);

        } else {
            x.view().inject(this);
        }
        setSystemColor();
        //初始化数据
        if(this instanceof CreateUIHelper) {
            CreateUIHelper uiHelper = (CreateUIHelper) this;
            if(createUI != null) {
                if(createUI.isLoadData()) {
                    uiHelper.loadData();
                }
                if(createUI.isInitView()) {
                    uiHelper.initView();
                }
            }
        }
        setOnListener();
        initPublicViews();
        AppContext.getActivities().add(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }


    protected Context getContext() {
        return context;
    }


    protected void startService() {
        //开启服务
    }

    protected void stopService() {
        //关闭服务
    }


    /**
     * 消息TIP
     * @param iconResId
     * @param tips
     */
    protected void showTips(int iconResId, String tips) {
        if (tipsToast != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                tipsToast.cancel();
            }
        } else {
            tipsToast = TipsToastUtil.makeText(getApplication().getBaseContext(),
                    tips, TipsToastUtil.LENGTH_SHORT);
        }
        tipsToast.show();
        tipsToast.setIcon(iconResId);
        tipsToast.setText(tips);
    }

    /**
     * 消息弹出框
     * @param text
     */
    protected void showMsgDialog(int text) {
        CustomDialog.Builder builder = new CustomDialog.Builder(
                BaseActivity.this);
        builder.setMessage(text);
        builder.setNegativeButton("确定",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int paramInt) {
                        dialog.dismiss();
                        completeLoading();
                    }
                });
        builder.create().show();
    }

    /**
     * 消息弹出框
     * @param text
     */
    protected void showMsgDialog4Main(final int text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CustomDialog.Builder builder = new CustomDialog.Builder(
                        BaseActivity.this);
                builder.setMessage(text);
                builder.setNegativeButton("确定",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int paramInt) {
                                dialog.dismiss();
                                completeLoading();
                            }
                        });
                builder.create().show();
            }
        });
    }

//    /**
//     * 打开Activity
//     *
//     * @param activity
//     * @param cls
//     * @param name
//     */
//    @Deprecated
//    protected void startActivity(Activity activity, Class<?> cls,
//                                 BasicNameValuePair... name) {
//        ActivityUtil.startActivity(activity, cls, name);
//    }

    /**
     * parameter参数长度不能大于2，支持两种格式
     * 例子：1: key,value 2: Map (参数个数超过两个使用map)
     * @param context
     * @param cls
     * @param parameter
     */
    protected void startActivityNew(Context context, Class<?> cls,
                                 Object... parameter) {
        ActivityUtil.startActivityNew(context, cls, parameter);
    }




//    /**
//     * 打开Activity，带返回值
//     * @param activity
//     * @param cls
//     * @param requestCode
//     * @param name
//     */
//    protected void startActivityForResult(Activity activity, Class<?> cls, int requestCode,
//                                          BasicNameValuePair... name) {
//        ActivityUtil.startActivityForResult(activity, cls, requestCode, name);
//    }

    /**
     * 打开Activity，带返回值
     * @param activity
     * @param cls
     * @param requestCode
     * @param obj
     */
    protected void startActivityForResultNew(Activity activity, Class<?> cls, int requestCode,
                                          Object... obj) {
        ActivityUtil.startActivityForResultNew(activity, cls, requestCode, obj);
    }

    /**
     * 关闭 Activity
     *
     * @param activity
     */
    protected void finish(Activity activity) {
        ActivityUtil.finish(activity);
    }


    /**
     * 开始加载
     * @param objs  第一个参数：是否显示文本（boolean）
     *              第二个参数：文本内容
     */
    public void loading(Object... objs){
        if(loading_layout != null) {
            if(objs == null) {
                _loading();
            } else if (objs.length == 1
                    && objs[0] instanceof Boolean) {
                _loading((Boolean)objs[0]);
            } else if (objs.length == 2
                    && objs[0] instanceof Boolean
                    && objs[1] instanceof Integer) {
                _loading((Boolean)objs[0], (Integer)objs[1]);
            } else {
                _loading();
            }
        }
    }

    /**
     * 完成加载
     */
    public void completeLoading(Integer... objs){
        if(objs != null && objs.length >0) {
            if(objs[0] > 0) {
                txt_status_message.setText(objs[0]);
            }
        } else {
//            txt_status_message.setText(R.string.completed);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(loading_layout != null) {
            loading_layout.setVisibility(View.GONE);
        }
    }



    //仿IOS系统加载
    KProgressHUD loadingUI;
    protected void loadingCommon(Object... msg) {
        if(loadingUI == null) {
            loadingUI =  KProgressHUD.create(this)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setCancellable(true)
                    .setAnimationSpeed(1)
                    .setDimAmount(0.5f);
            if(msg == null
                    || msg.length == 0) {
                loadingUI.setLabel(getContext().getResources()
                        .getText(R.string.common_loading).toString());
            } else {
               if(msg[0] instanceof Integer) {
                   loadingUI.setLabel(getContext().getResources()
                           .getText((Integer) msg[0]).toString());
               } else if (msg[0] instanceof String) {
                   loadingUI.setLabel((String)msg[0]);
               } else {
                   loadingUI.setLabel(getContext().getResources()
                           .getText(R.string.common_loading).toString());
               }
            }
        }
        loadingUI.show();
    }


    protected void loadingCommon4Main(final Object... msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(loadingUI == null) {
                    loadingUI =  KProgressHUD.create(BaseActivity.this)
                            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                            .setCancellable(true)
                            .setAnimationSpeed(1)
                            .setDimAmount(0.5f);
                    if(msg == null
                            || msg.length == 0) {
                        loadingUI.setLabel(getContext().getResources()
                                .getText(R.string.common_loading).toString());
                    } else {
                        if(msg[0] instanceof Integer) {
                            loadingUI.setLabel(getContext().getResources()
                                    .getText((Integer) msg[0]).toString());
                        } else if (msg[0] instanceof String) {
                            loadingUI.setLabel((String)msg[0]);
                        } else {
                            loadingUI.setLabel(getContext().getResources()
                                    .getText(R.string.common_loading).toString());
                        }
                    }
                }
                loadingUI.show();
            }
        });
    }

    protected void completeLoadingCommon() {
        if(loadingUI != null) {
            loadingUI.dismiss();
        }
    }

    protected void completeLoadingCommon4Main() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(loadingUI != null) {
                    loadingUI.dismiss();
                }
            }
        });
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(resultCode == Constants.REQUEST_CODE) {
//            completeLoading();
//        }
//    }




    /**
     *******************************************私有函数部分，可封装******************************
     */
    private void initPublicViews() {
        loading_layout = (RelativeLayout)findViewById(R.id.loading_layout);
        txt_status_message = (TextView)findViewById(R.id.txt_status_message);
    }

    private void _loading(){
        if(loading_layout != null) {
            loading_layout.setVisibility(View.VISIBLE);
            if(txt_status_message != null) {
                txt_status_message.setVisibility(View.VISIBLE);
            }
        }
    }

    private void _loading(boolean isShowMsg){
        if(loading_layout != null) {
            loading_layout.setVisibility(View.VISIBLE);
            if(txt_status_message != null && isShowMsg) {
                txt_status_message.setVisibility(View.VISIBLE);
            }
        }
    }

    private void _loading(boolean isShowMsg, int msg){
        if(loading_layout != null) {
            loading_layout.setVisibility(View.VISIBLE);
            if(txt_status_message != null && isShowMsg) {
                if(msg != 0) {
                    txt_status_message.setText(msg);
                }
                txt_status_message.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setListeners() {

    }


    /**
     * 绑定事件
     */
    protected void setOnListener() {

    }

    public int getResValue(String name, String... type) {
        int resId = 0;
        if(type == null || type.length == 0) {
            resId = getResources().getIdentifier(name, "string", "com.juju.app");
        } else {
            resId = getResources().getIdentifier(name, type[0], "com.juju.app");
        }
        if(resId == 0) resId = R.string.system_service_error;
        return resId;
    }

    public String getDrawablePath(String name, String... type) {
        int resId = getResources().getIdentifier(name, type[0], "com.juju.app");
        String drawablePath = getResources().getResourceTypeName(resId) + "://" + resId;
//                + getResources().getResourceEntryName(resId);
        return drawablePath;
    }

    @SuppressLint("NewApi")
    private void setSystemColor() {
        final SystemColor createUI = this.getClass().getAnnotation(SystemColor.class);
        int color = R.color.blue;
        if(createUI != null) {
            //设置状态栏、导航栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if(createUI.isApply()) {
                    color = createUI.colorValue();
                    setStatusBarAndNavigationBar(true, true, color);
                } else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                }
            }
            //设置标题栏
            if(topBar != null) {
                topBar.setBackgroundColor(getResources().getColor(createUI.titleColorValue()));
            }
        } else {
            setStatusBarAndNavigationBar(true, true, color);
        }



    }

    @SuppressLint("NewApi")
    private void setStatusBarAndNavigationBar(boolean isSetStatusBar,
                                              boolean isSetNavigationBar, int color) {
        if(!(isSetStatusBar && isSetNavigationBar))
            return;

        if(isSetStatusBar) {
            //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        if(isSetNavigationBar) {
            //透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        if(isSetStatusBar) {
            tintManager.setStatusBarTintEnabled(true);
            //此处可以重新指定状态栏颜色
            tintManager.setStatusBarTintResource(color);
        }
        if(isSetNavigationBar) {
            tintManager.setNavigationBarTintEnabled(true);
            //此处可以重新指定导航栏颜色
            tintManager.setNavigationBarTintResource(color);
        }
    }

    //发送消息，消息发布者，UI需监听
    protected void triggerEvent(Object paramObject)
    {
        EventBus.getDefault().post(paramObject);
    }

    //发送消息，消息发布者，UI需监听
    protected void triggerEvent4Sticky(Object paramObject)
    {
        EventBus.getDefault().postSticky(paramObject);
    }


    //加粗显示标题
    protected void setTopTitleBold(String title) {
        if (title == null) {
            return;
        }
        if (title.length() > 12) {
            title = title.substring(0, 11) + "...";
        }
        // 设置字体为加粗
        TextPaint paint =  topTitleTxt.getPaint();
        paint.setFakeBoldText(true);

        topTitleTxt.setText(title);
        topTitleTxt.setVisibility(View.VISIBLE);

    }

    //显示标题
    protected void setTopTitle(Object resId) {
        String title = null;
        if(resId instanceof Integer) {
            title = getContext().getResources().getText((Integer) resId).toString();
        } else if (resId instanceof String) {
            title = (String)resId;
        }
        if (title == null) {
            return;
        }
        if (title.length() > 12) {
            title = title.substring(0, 11) + "...";
        }
        topTitleTxt.setText(title);
        topTitleTxt.setVisibility(View.VISIBLE);
    }

    //隐藏标题
    protected void hideTopTitle() {
        topTitleTxt.setVisibility(View.GONE);
    }


    //显示左边按钮
    protected void setTopLeftButton(int resID) {
        if (resID < 0) {
            return;
        } else if (resID == 0) {
            resID = R.mipmap.icon_back;
        }
        topLeftBtn.setImageResource(resID);
        topLeftBtn.setVisibility(View.VISIBLE);
    }

    //设置左边按钮padding
    protected void setTopLeftButtonPadding(int l,int t,int r,int b) {
        topLeftBtn.setPadding(l, t, r, b);
    }

    //隐藏左边按钮
    protected void hideTopLeftButton() {
        topLeftBtn.setVisibility(View.GONE);
    }

    //显示左边文字
    protected void setTopLeftText(int resId) {
        if (resId < 0) {
            return;
        } else if (resId == 0) {
            resId = R.string.top_left_back;
        }
        topLetTitleTxt.setText(resId);
        topLetTitleTxt.setVisibility(View.VISIBLE);
    }

    //显示右边文字
    protected void setTopRightText(int resId) {
        if (resId < 0) {
            return;
        } else if (resId == 0) {
            resId = R.string.save;
        }
        topRightTitleTxt.setText(resId);
        topRightTitleTxt.setVisibility(View.VISIBLE);
    }

    //显示右边按钮
    protected void setTopRightButton(int resID) {
        if (resID < 0) {
            return;
        } else if (resID == 0) {
            resID = R.mipmap.icon_add;
        }
        topRightBtn.setImageResource(resID);
        topRightBtn.setVisibility(View.VISIBLE);
    }

    //隐藏右边按钮
    protected void hideTopRightButton() {
        topRightBtn.setVisibility(View.GONE);
    }

    //设置背景色
    @Deprecated
    protected void setTopBar(int resID) {
        if (resID <= 0) {
            return;
        }
        topBar.setBackgroundResource(resID);
    }


    //显示搜索框
    protected void showTopSearchBar() {
        topSearchEdt.setVisibility(View.VISIBLE);
    }

    //隐藏搜索框
    protected void hideTopSearchBar() {
        topSearchEdt.setVisibility(View.GONE);
    }


    /**
     * 显示左边区域
     * @param textId
     * @param btnId
     */
    protected void showTopLeftAll(int textId, int btnId) {
        setTopLeftText(textId);
        setTopLeftButton(btnId);
    }

    /**
     * 隐藏左边区域
     *
     */
    protected void hideTopLeftAll() {
        hideTopLeftButton();
        topLetTitleTxt.setVisibility(View.GONE);
    }

    /**
     * 显示右边区域
     * @param textId
     * @param btnId
     */
    protected void showTopRightAll(int textId, int btnId) {
        setTopRightText(textId);
        setTopRightButton(btnId);
    }

    /**
     * 隐藏右边区域
     *
     */
    protected void hideTopRightAll() {
        hideTopRightButton();
        topRightTitleTxt.setVisibility(View.GONE);

    }


    private void initTopView() {
        topContentView = (ViewGroup) LayoutInflater
                .from(this).inflate(R.layout.tt_fragment_base, null);
        topBar = (ViewGroup) topContentView.findViewById(R.id.layout_bar);

        //左边区域
        topLetTitleTxt = (TextView) topContentView.findViewById(R.id.txt_left);
        topLeftBtn = (ImageView) topContentView.findViewById(R.id.img_back);

        topLetTitleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               finish(BaseActivity.this);
            }
        });


        topLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(BaseActivity.this);
            }
        });

        //右边区域
        topRightTitleTxt = (TextView) topContentView.findViewById(R.id.txt_right);
        topRightBtn = (ImageView) topContentView.findViewById(R.id.img_right);

        //标题
        topTitleTxt = (TextView) topContentView.findViewById(R.id.txt_title);

        //搜索框
        topSearchEdt = (SearchEditText) topContentView.findViewById(R.id.chat_title_search);

        //搜索FrameLayout
        searchFrameLayout = (FrameLayout)topContentView.findViewById(R.id.searchbar);

        topLeftBtn.setVisibility(View.GONE);
        topLetTitleTxt.setVisibility(View.GONE);
        topRightBtn.setVisibility(View.GONE);
        topRightTitleTxt.setVisibility(View.GONE);
        topTitleTxt.setVisibility(View.GONE);
        topSearchEdt.setVisibility(View.GONE);
    }



}
