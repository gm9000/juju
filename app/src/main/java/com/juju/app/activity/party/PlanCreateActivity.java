package com.juju.app.activity.party;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.PlanTypeAdapter;
import com.juju.app.entity.Plan;
import com.juju.app.fastdfs.StorePath;
import com.juju.app.fastdfs.callback.ProgressCallback;
import com.juju.app.fastdfs.exception.FdfsIOException;
import com.juju.app.fastdfs.service.impl.StorageClientService;
import com.juju.app.golobal.Constants;
import com.juju.app.helper.PhotoHelper;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ImageLoaderUtil;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.RectCopperImageView;
import com.juju.app.view.imagezoom.utils.DecodeUtils;
import com.juju.app.view.wheel.dialog.SelectDateTimeDialog;
import com.rey.material.app.BottomSheetDialog;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@ContentView(R.layout.activity_plan_create)
public class PlanCreateActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "PlanCreateActivity";

    private static final int CMD_REQ_LOCATION = 1;
    private static final int PHOTO_REQUEST_TAKEPHOTO = 0x02;// 拍照
    private static final int PHOTO_REQUEST_GALLERY = 0x03;// 从相册中选择

    @ViewInject(R.id.txt_title)
    private TextView txt_title;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.txt_left)
    private TextView txt_left;
    @ViewInject(R.id.img_right)
    private ImageView img_right;
    @ViewInject(R.id.txt_right)
    private TextView txt_right;

    @ViewInject(R.id.layout_plan)
    private LinearLayout layout_plan;
    @ViewInject(R.id.layout_cover)
    private RelativeLayout layoutCover;
    @ViewInject(R.id.img_cover)
    private ImageView imgCover;

    @ViewInject(R.id.layout_plan_type)
    private LinearLayout layoutPlanType;
    @ViewInject(R.id.txt_plan_type)
    private TextView txtPlanType;

    @ViewInject(R.id.layout_time)
    private LinearLayout layoutTime;
    @ViewInject(R.id.txt_time)
    private TextView txt_time;
    @ViewInject(R.id.txt_location)
    private EditText txt_location;
    @ViewInject(R.id.txt_plan_description)
    private EditText txt_planDescription;


    @ViewInject(R.id.layout_picker)
    private RelativeLayout layoutPicker;
    @ViewInject(R.id.cover_picker)
    private RectCopperImageView coverPicker;
    @ViewInject(R.id.txt_upload_process)
    private TextView txtUploadProcess;
    @ViewInject(R.id.menu_layout)
    private RelativeLayout menuLayout;
    @ViewInject(R.id.cancel)
    private TextView txt_cancel;
    @ViewInject(R.id.confirm)
    private TextView txt_confirm;

    private Bitmap copperBitmap;


    private double latitude = 0;
    private double longitude = 0;

    @ViewInject(R.id.img_select_location)
    private ImageView img_selectLocation;

    @ViewInject(R.id.btn_add_plan)
    private Button btnAddPlan;

    private InputMethodManager inputManager = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private BottomSheetDialog msgDialog;

    private int index = -1;
    private Plan plan;
    private String coverUrl;

    private TextView txtCapture;
    private TextView txtPhoto;
    private TextView txtDefault;
    private TextView txtCancel;

    private int coverWidth;
    private int coverHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        initParam();
        initView();
        initListeners();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }

    private void initListeners() {
        layoutPlanType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePlanType();
            }
        });
    }


    private void initView() {
        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);

        txt_title.getRootView().setBackgroundColor(getResources().getColor(R.color.background));
        txt_title.setText(R.string.party_plan);
        img_right.setVisibility(View.GONE);
        txt_right.setVisibility(View.GONE);

        if(plan != null){
            txtPlanType.setText(getResValue(plan.getType()));
            txtPlanType.setTag(Plan.Type.valueOf(plan.getType()));
            coverUrl = plan.getCoverUrl();
            if(coverUrl!=null && coverUrl.startsWith("http:")){
                ImageLoaderUtil.getImageLoaderInstance().displayImage(coverUrl, imgCover, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
            }else {
                imgCover.setImageResource(getResValue(plan.getType().toLowerCase(), "mipmap"));
            }
            txt_time.setText(dateFormat.format(plan.getStartTime()));
            txt_location.setText(plan.getAddress());
            txt_planDescription.setText(plan.getDesc());
            latitude = plan.getLatitude();
            longitude = plan.getLongitude();
        }

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        coverWidth = (int) (Math.min(metrics.widthPixels, metrics.heightPixels) / 0.55);
        coverHeight = coverWidth;
    }


    public void initParam() {
        Intent intent = getIntent();
        String indexStr = intent.getStringExtra("index");
        if(!StringUtils.empty(indexStr)){
            index = Integer.parseInt(indexStr);
            String planStr = intent.getStringExtra("planStr");
            plan = JacksonUtil.turnString2Obj(planStr,Plan.class);
        }
    }

    @Event(R.id.txt_left)
    private void goCancel(View view){
        ActivityUtil.finish(this);
    }
    @Event(value=R.id.img_back)
    private void goBack(View view){
        ActivityUtil.finish(this);
    }


    @Event(value=R.id.layout_time)
    private void showSelectTimeDialog(View view) {
        SelectDateTimeDialog mSelectDateTimeDialog = new SelectDateTimeDialog(this);
        mSelectDateTimeDialog.setCanceledOnTouchOutside(true);
        mSelectDateTimeDialog.setOnClickListener(new SelectDateTimeDialog.OnClickListener() {
            @Override
            public boolean onSure(int mYear, int mMonth, int mDay, int hour, int minute) {
                Date date = new Date();
                date.setYear(mYear - 1900);
                date.setMonth(mMonth);
                date.setDate(mDay);
                date.setHours(hour);
                date.setMinutes(minute);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                txt_time.setText(dateFormat.format(date));
                return false;
            }

            @Override
            public boolean onCancel() {
                return false;
            }
        });
        if(txt_time.getText()!=null && !txt_time.getText().toString().equals("")) {

            mSelectDateTimeDialog.showTimeStr(txt_time.getText().toString());
        }else{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 09:00");
            String dateString = dateFormat.format(new Date().getTime()+24*3600*1000);
            mSelectDateTimeDialog.showTimeStr(dateString);
        }
    }


    @Event(value=R.id.img_select_location)
    private void selectLocation(View view){
        Intent intent = new Intent(this,PlanLocationActivity.class);
        intent.putExtra(Constants.EDIT_MODE,true);
        intent.putExtra(Constants.ADDRESS,txt_location.getText().toString());
        intent.putExtra(Constants.LONGITUDE, longitude);
        intent.putExtra(Constants.LATITUDE, latitude);
        startActivityForResult(intent, CMD_REQ_LOCATION);
    }



    @Event(R.id.btn_add_plan)
    private void addPlan(View view){

        if(StringUtils.empty(txtPlanType.getText().toString())){
            ToastUtil.showShortToast(this, "请选择活动类型", 1);
            return;
        }
        if(txt_time.getText().toString().equals("") || txt_location.getText().toString().equals("")) {
            ToastUtil.showShortToast(this, "时间和地址必须设定", 1);
            return;
        }

        if(plan==null){
            plan = new Plan();
        }
        try {
            plan.setStartTime(dateFormat.parse(txt_time.getText().toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        plan.setAddress(txt_location.getText().toString());
        plan.setDesc(txt_planDescription.getText().toString());
        plan.setLatitude(latitude);
        plan.setLongitude(longitude);
        plan.setType(((Plan.Type) txtPlanType.getTag()).name());

        if(coverUrl != null){
            plan.setCoverUrl(coverUrl);
        }else{
            plan.setCoverUrl(plan.getType());
        }

        Intent intent = getIntent();
        intent.putExtra("planJson", JacksonUtil.turnObj2String(plan));
        intent.putExtra("index",index);
        setResult(RESULT_OK,intent);
        ActivityUtil.finish(this);

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch(parent.getId()) {
            case R.id.grid_view_plan_type:
                Plan.Type planType = Plan.Type.values()[position];
                txtPlanType.setText(getResValue(planType.name()));
                txtPlanType.setTag(planType);
                if(coverUrl == null) {
                    imgCover.setImageResource(getResValue(planType.name().toLowerCase(), "mipmap"));
                }else{
                    ImageLoaderUtil.getImageLoaderInstance().displayImage(coverUrl, imgCover, ImageLoaderUtil.DISPLAY_IMAGE_OPTIONS);
                }
                msgDialog.dismiss();
                break;
        }

    }

    @Event(R.id.layout_cover)
    private void showPicMenu(View view) {

        msgDialog = new BottomSheetDialog(this);
        msgDialog.setCanceledOnTouchOutside(true);
        msgDialog.contentView(R.layout.layout_bottom_menu)
                .inDuration(300);
        txtCapture = (TextView)msgDialog.findViewById(R.id.btn_menu1);
        txtCapture.setText(R.string.take_camera_btn_text);
        txtCapture.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SdCardPath")
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // 指定调用相机拍照后照片的储存路径
                File cacheDir = new File(Constants.BASE_PATH);
                if(!cacheDir.exists()){
                    cacheDir.mkdir();
                }
                File imageFile = new File(Constants.PLAN_COVER_CACHE);
                if (!imageFile.exists()) {
                    try {
                        imageFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                startActivityForResult(intent, PHOTO_REQUEST_TAKEPHOTO);
                msgDialog.dismiss();
            }
        });

        txtPhoto = (TextView)msgDialog.findViewById(R.id.btn_menu2);
        txtPhoto.setText(R.string.album);
        txtPhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, PHOTO_REQUEST_GALLERY);
                msgDialog.dismiss();
            }
        });

        if(coverUrl!=null && coverUrl.startsWith("http:")) {
            txtDefault = (TextView) msgDialog.findViewById(R.id.btn_menu3);
            txtDefault.setText(R.string.to_default);
            txtDefault.setVisibility(View.VISIBLE);
            txtDefault.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    coverUrl = null;
                    if (StringUtils.empty(txtPlanType.getText())) {
                        imgCover.setImageDrawable(null);
                    } else {
                        Plan.Type planType = (Plan.Type) txtPlanType.getTag();
                        imgCover.setImageResource(getResValue(planType.name().toLowerCase(), "mipmap"));
                    }
                    msgDialog.dismiss();
                }
            });
        }

        txtCancel = (TextView)msgDialog.findViewById(R.id.txt_cancel);
        txtCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                msgDialog.dismiss();
            }
        });

        msgDialog.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CMD_REQ_LOCATION:
                    latitude = data.getDoubleExtra(Constants.LATITUDE,0f);
                    longitude = data.getDoubleExtra(Constants.LONGITUDE, 0f);
                    String address = data.getStringExtra(Constants.ADDRESS);
                    txt_location.setText(address);
                    break;
                case PHOTO_REQUEST_TAKEPHOTO:
                    txtUploadProcess.setText("");
                    layoutPicker.setVisibility(View.VISIBLE);
                    menuLayout.setVisibility(View.VISIBLE);
                    Bitmap bitmap = DecodeUtils.decode(this, Uri.parse(Constants.PLAN_COVER_CACHE), coverWidth, coverHeight);
                    coverPicker.setImageBitmap(bitmap);

                    break;

                case PHOTO_REQUEST_GALLERY:
                    txtUploadProcess.setText("");
                    layoutPicker.setVisibility(View.VISIBLE);
                    menuLayout.setVisibility(View.VISIBLE);
                    if (data != null) {
                        Bitmap bitmap1 = DecodeUtils.decode(this, data.getData(), coverWidth, coverHeight);
                        coverPicker.setImageBitmap(bitmap1);
                    }
                    break;

            }
            super.onActivityResult(requestCode, resultCode, data);

        }else{
        }
    }


    @Event(R.id.confirm)
    private void uploadHead(View view){

        txt_confirm.setClickable(false);
        txt_cancel.setClickable(false);
        copperBitmap = null;
        if(!coverPicker.isFillRect()){
            ToastUtil.showShortToast(this,"图片需充满矩形区域！",1);
            txt_confirm.setClickable(true);
            txt_cancel.setClickable(true);
            return;
        }
        txtUploadProcess.setVisibility(View.VISIBLE);
        txtUploadProcess.setText(R.string.uploading);
        copperBitmap = coverPicker.getCroppedImage();
        if(copperBitmap.getWidth()>360){
            copperBitmap = Bitmap.createScaledBitmap(copperBitmap,360,360*copperBitmap.getHeight()/copperBitmap.getWidth(),true);
        }

        if(copperBitmap!=null) {
            new Thread(){
                @Override
                public void run() {
                    uploadPlanCover();
                }

            }.start();
        }else{
            txt_confirm.setClickable(true);
            txt_cancel.setClickable(true);
        }
    }

    @Event(R.id.cancel)
    private void cancelUpload(View view){
        layoutPicker.setVisibility(View.GONE);
        menuLayout.setVisibility(View.GONE);
    }

    private void uploadPlanCover(){
        byte[] bytes = PhotoHelper.getBytes(copperBitmap);
        String uuid = UUID.randomUUID().toString();
        StorageClientService.instance().uploadFile(uuid, "group1", new ByteArrayInputStream(bytes), bytes.length,
                "jpg", new ProgressCallback<StorePath>() {

                    @Override
                    public void updateProgress(String id, long total, long current) {
                        double d = (double)current/total;
                        DecimalFormat formatVal =new DecimalFormat("已上传## %");
                        final String percent = formatVal.format(d);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtUploadProcess.setText(percent);
                            }
                        });

                    }

                    @Override
                    public void sendError(String id, FdfsIOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txt_confirm.setClickable(true);
                                txt_cancel.setClickable(true);
                                txtUploadProcess.setText(R.string.upload_fail);
                            }
                        });

                    }

                    @Override
                    public void recvError(String id, FdfsIOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txt_confirm.setClickable(true);
                                txt_cancel.setClickable(true);
                                txtUploadProcess.setText(R.string.upload_fail);
                            }
                        });
                    }

                    @Override
                    public void complete(String id, StorePath storePath) {
                        coverUrl = storePath.getUrl();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtUploadProcess.setText(R.string.uploaded);
                                imgCover.setImageBitmap(copperBitmap);
                                txt_confirm.setClickable(true);
                                txt_cancel.setClickable(true);
                                layoutPicker.setVisibility(View.GONE);
                                menuLayout.setVisibility(View.GONE);

                            }
                        });


                    }
                });
    }


    private void choosePlanType() {

        msgDialog = new BottomSheetDialog(this);
        msgDialog.setCanceledOnTouchOutside(true);
        msgDialog.contentView(R.layout.layout_plan_type)
                .inDuration(300);
        TextView txtTitle = (TextView)msgDialog.findViewById(R.id.txt_title);
        txtTitle.setText(R.string.plan_type);
        GridView planTypeGridView = (GridView)msgDialog.findViewById(R.id.grid_view_plan_type);
        PlanTypeAdapter planTypeAdapter = new PlanTypeAdapter(this);
        planTypeGridView.setAdapter(planTypeAdapter);
        planTypeGridView.setOnItemClickListener(this);
        msgDialog.show();

    }

}
