package com.juju.app.activity.user;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.chat.ChatActivity;
import com.juju.app.annotation.CreateUI;
import com.juju.app.golobal.Constants;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.CustomDialog;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;


@ContentView(R.layout.activity_regist)
@CreateUI(isLoadData = true, isInitView = true)
public class RegistActivity extends BaseActivity implements CreateUIHelper {

    /**
     *******************************************Activity常量***************************************
     */
    private static final int PHOTO_REQUEST_TAKEPHOTO = 1;// 拍照
    private static final int PHOTO_REQUEST_GALLERY = 2;// 从相册中选择
    private static final int PHOTO_REQUEST_CUT = 3;// 结果

    /**
     *******************************************Activity组件***************************************
     */
    @ViewInject(R.id.img_back)
    private ImageView img_back;

    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    @ViewInject(R.id.txt_title)
    private TextView txt_title;

    @ViewInject(R.id.img_right)
    private ImageView img_right;

    @ViewInject(R.id.img_photo)
    private ImageView img_photo;

    @ViewInject(R.id.img_hide)
    private ImageView img_hide;

    @ViewInject(R.id.img_show)
    private ImageView img_show;

    @ViewInject(R.id.et_password)
    private EditText et_password;

    @ViewInject(R.id.et_usernick)
    private EditText et_usernick;

    @ViewInject(R.id.et_usertel)
    private EditText et_usertel;

    @ViewInject(R.id.btn_register)
    private Button btn_register;


    /**
     *******************************************全局属性******************************************
     */
    private String imageName;
    private String nickName;
    private String phone;
    private String password;
    private String photoAddr;




    /**
     *******************************************公共函数******************************************
     */
    @Override
    public void loadData() {
        setListeners();
    }

    @Override
    public void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.btn_register);
        txt_left.setVisibility(View.VISIBLE);
        txt_title.setVisibility(View.GONE);
        img_right.setVisibility(View.GONE);
    }

    @SuppressLint("SdCardPath")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PHOTO_REQUEST_TAKEPHOTO:
                    startPhotoZoom(
                            Uri.fromFile(new File("/sdcard/juju/", imageName)),
                            480);
                    break;

                case PHOTO_REQUEST_GALLERY:
                    if (data != null)
                        startPhotoZoom(data.getData(), 480);
                    break;

                case PHOTO_REQUEST_CUT:
//                BitmapFactory.Options options = new BitmapFactory.Options();
//
//                /**
//                 * 最关键在此，把options.inJustDecodeBounds = true;
//                 * 这里再decodeFile()，返回的bitmap为空
//                 * ，但此时调用options.outHeight时，已经包含了图片的高了
//                 */
//                options.inJustDecodeBounds = true;
                    Bitmap bitmap = BitmapFactory.decodeFile("/sdcard/juju/"
                            + imageName);
                    img_photo.setImageBitmap(bitmap);
                    break;

            }
            super.onActivityResult(requestCode, resultCode, data);

        }
    }


    /**
     *******************************************事件函数******************************************
     */
    //结束注册
    @Event(R.id.img_back)
    private void onClickImgBack(ImageView view) {
        ActivityUtil.finish(RegistActivity.this);
    }

    //结束注册
    @Event(R.id.txt_left)
    private void onClickTxtLeft(TextView view) {
        ActivityUtil.finish(RegistActivity.this);
    }

    //点击图片拍照
    @Event(R.id.img_photo)
    private void onClickImgPhoto(View view) {
        showCamera();
    }

    //明文显示密码
    @Event(R.id.img_hide)
    private void onClickImgHide(View view) {
        img_hide.setVisibility(View.GONE);
        img_show.setVisibility(View.VISIBLE);
        et_password
                .setTransformationMethod(HideReturnsTransformationMethod
                        .getInstance());
        // 切换后将EditText光标置于末尾
        CharSequence charSequence = et_password.getText();
        if (charSequence instanceof Spannable) {
            Spannable spanText = (Spannable) charSequence;
            Selection.setSelection(spanText, charSequence.length());
        }
    }

    //密文显示密码
    @Event(R.id.img_show)
    private void onClickImgShow(View view) {
        img_show.setVisibility(View.GONE);
        img_hide.setVisibility(View.VISIBLE);
        et_password
                .setTransformationMethod(PasswordTransformationMethod
                        .getInstance());
        // 切换后将EditText光标置于末尾
        CharSequence charSequence = et_password.getText();
        if (charSequence instanceof Spannable) {
            Spannable spanText = (Spannable) charSequence;
            Selection.setSelection(spanText, charSequence.length());
        }
    }

    @Event(R.id.btn_register)
    private void onClickBtnRegister(View view) {
        boolean bool = validateRegist();
        if(bool) {
            phone = et_usertel.getText().toString();
            nickName = et_usernick.getText().toString();
            password = et_password.getText().toString();
            showMobileDialog();
        }
    }



    /**
     *******************************************回调函数******************************************
     */


    /**
     *******************************************私有函数******************************************
     */

    private void setListeners() {
        // 监听多个输入框
        et_usernick.addTextChangedListener(new TextChange());
        et_usertel.addTextChangedListener(new TextChange());
        et_password.addTextChangedListener(new TextChange());
    }

    // 拍照部分
    private void showCamera() {
        final AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.show();
        Window window = dlg.getWindow();
        // *** 主要就是在这里实现这种效果的.
        // 设置窗口的内容页面,shrew_exit_dialog.xml文件中定义view内容
        window.setContentView(R.layout.alertdialog);
        // 为确认按钮添加事件,执行退出应用操作
        TextView tv_paizhao = (TextView) window.findViewById(R.id.tv_content1);
        tv_paizhao.setText(R.string.take_camera_btn_text);
        tv_paizhao.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SdCardPath")
            public void onClick(View v) {
                imageName = getNowTime() + ".png";
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // 指定调用相机拍照后照片的储存路径
                intent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(new File("/sdcard/juju/", imageName)));
                startActivityForResult(intent, PHOTO_REQUEST_TAKEPHOTO);
                dlg.cancel();
            }
        });
        TextView tv_xiangce = (TextView) window.findViewById(R.id.tv_content2);
        tv_xiangce.setText(R.string.album);
        tv_xiangce.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getNowTime();
                imageName = getNowTime() + ".png";
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, PHOTO_REQUEST_GALLERY);
                dlg.cancel();
            }
        });

    }

    private void showMobileDialog() {
        CustomDialog.Builder builder = new CustomDialog.Builder(RegistActivity.this);
        builder.setMessage((String) getText(R.string.login_mobile_confirm) + phone);
        builder.setPositiveButton(R.string.confirm,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        jumpNext();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(R.string.negative,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    @SuppressLint("SimpleDateFormat")
    private String getNowTime() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmssSS");
        return dateFormat.format(date);
    }

    @SuppressLint("SdCardPath")
    private void startPhotoZoom(Uri uri1, int size) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri1, "image/*");
        // crop为true是设置在开启的intent中设置显示的view可以剪裁
        intent.putExtra("crop", "true");

        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);

        // outputX,outputY 是剪裁图片的宽高
        intent.putExtra("outputX", size);
        intent.putExtra("outputY", size);
        intent.putExtra("return-data", false);

        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File("/sdcard/juju/", imageName)));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, PHOTO_REQUEST_CUT);
    }

    /**
     * 验证登陆
     * @return
     */
    private boolean validateRegist() {
        String phone = et_usertel.getText().toString();
        String pwd = et_password.getText().toString();
        if(!StringUtils.isMobileNO(phone)) {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.regist_phone_error, 0);
            et_usertel.requestFocus();
            return false;
        }
        if(pwd.length() < 6) {
            ToastUtil.TextIntToast(getApplicationContext(), R.string.regist_pwd_length_error, 0);
            et_password.requestFocus();
            return false;
        }
        return true;
    }

    //跳转到下一步（短信认证）
    private void jumpNext() {
        //封装数据，发送给下一步
//        List<BasicNameValuePair> valuePairs = new ArrayList<BasicNameValuePair>();
//        BasicNameValuePair phoneValue = new BasicNameValuePair(Constants.PHONE,
//                phone);
//        BasicNameValuePair nickNameValue = new BasicNameValuePair(Constants.NICKNAME,
//                nickName);
//        BasicNameValuePair passwordValue = new BasicNameValuePair(Constants.PASSWORD,
//                password);
//        valuePairs.add(phoneValue);
//        valuePairs.add(nickNameValue);
//        valuePairs.add(passwordValue);
        Map<String, Object> valueMap = new HashMap<String,Object>();
        valueMap.put(Constants.PHONE, phone);
        valueMap.put(Constants.NICKNAME, nickName);
        valueMap.put(Constants.PASSWORD, password);
        startActivityNew(RegistActivity.this, RegistNext1Activity.class, valueMap);
    }

    /**
     *******************************************类部类******************************************
     */

    // EditText监听器
    class TextChange implements TextWatcher {

        @Override
        public void afterTextChanged(Editable arg0) {

        }

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {

        }

        @Override
        public void onTextChanged(CharSequence cs, int start, int before,
                                  int count) {

            boolean sign1 = et_usernick.getText().length() > 0;
            boolean sign2 = et_usertel.getText().length() > 0;
            boolean sign3 = et_password.getText().length() > 0;

            if (sign1 & sign2 & sign3) {
                btn_register.setTextColor(0xFFFFFFFF);
                btn_register.setEnabled(true);
            }
            // 在layout文件中，对Button的text属性应预先设置默认值，否则刚打开程序的时候Button是无显示的
            else {
                btn_register.setTextColor(0xFFD0EFC6);
                btn_register.setEnabled(false);
            }
        }
    }
}
