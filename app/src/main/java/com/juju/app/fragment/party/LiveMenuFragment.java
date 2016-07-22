package com.juju.app.fragment.party;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.party.PlayVideoActivity;
import com.juju.app.activity.party.UploadVideoActivity;
import com.juju.app.adapters.DiscussListAdapter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.StringUtils;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

@SuppressLint("ValidFragment")
@ContentView(R.layout.fragment_live_menu)
@CreateFragmentUI(viewId = R.layout.fragment_live_menu)
public class LiveMenuFragment extends BaseFragment implements CreateUIHelper{

    private static final String TAG = "LiveMenuFragment";
    private UploadVideoActivity uploadActivity;
    private PlayVideoActivity playActivity;

    @ViewInject(R.id.live_discuss_listView)
    private ListView discussListView;
    @ViewInject(R.id.txt_discuss)
    private EditText txtDiscuss;
    @ViewInject(R.id.btn_send)
    private Button btnSend;
//    @ViewInject(R.id.img_share)
//    private ImageView imgShare;

//    private BottomSheetDialog shareDialog;
    private TextView txtWeixin;
    private TextView txtPyq;
    private TextView txtWeibo;
    private TextView txtCopy;
    private TextView txtCancel;

    private DiscussListAdapter discussAdapter;
    private String hlsUrl;
    private int index;
    private boolean isUpload;


    public LiveMenuFragment(BaseActivity activity, boolean upload, int index, String hlsUrl){
        super();
        if(upload){
            this.uploadActivity = (UploadVideoActivity)activity;
        }else{
            this.playActivity = (PlayVideoActivity)activity;
        }
        this.isUpload = upload;
        this.index = index;
        this.hlsUrl = hlsUrl;

    }


    @Override
    protected void findViews() {
    }

    @Override
    public void loadData() {

    }

    @Override
    public void initView() {
        switch (index){
            case 0:
                discussListView.setVisibility(View.GONE);
                txtDiscuss.setVisibility(View.GONE);
                btnSend.setVisibility(View.GONE);
//                imgShare.setVisibility(View.GONE);
                break;
            case 1:
                discussListView.setAdapter(discussAdapter);
                break;
        }

    }

    @Override
    public void setOnListener() {
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();

    }

    public void setDiscussAdapter(DiscussListAdapter discussAdapter) {
        this.discussAdapter = discussAdapter;
    }


//    @Event(R.id.img_share)
//    private void showDialog(View view) {
//        shareDialog = new BottomSheetDialog(isUpload?uploadActivity:playActivity);
//        shareDialog.contentView(R.layout.layout_video_share)
//                .inDuration(300);
//        txtWeixin = (TextView) shareDialog.findViewById(R.id.txt_weixin);
//        txtPyq = (TextView) shareDialog.findViewById(R.id.txt_pyq);
//        txtWeibo = (TextView) shareDialog.findViewById(R.id.txt_weibo);
//        txtCopy = (TextView) shareDialog.findViewById(R.id.txt_copy);
//        txtCancel = (TextView) shareDialog.findViewById(R.id.txt_cancel);
//        txtWeixin.setOnClickListener(this);
//        txtPyq.setOnClickListener(this);
//        txtWeibo.setOnClickListener(this);
//        txtCopy.setOnClickListener(this);
//        txtCancel.setOnClickListener(this);
//        shareDialog.show();
//    }
//
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.txt_weixin:
//                ToastUtil.showShortToast(isUpload?uploadActivity:playActivity, "weixin", 1);
//                shareDialog.dismiss();
//                break;
//            case R.id.txt_pyq:
//                ToastUtil.showShortToast(isUpload?uploadActivity:playActivity, "pyq", 1);
//                shareDialog.dismiss();
//                break;
//            case R.id.txt_weibo:
//                ToastUtil.showShortToast(isUpload?uploadActivity:playActivity, "weibo", 1);
//                shareDialog.dismiss();
//                break;
//            case R.id.txt_copy:
//                ClipboardManager cm = (ClipboardManager) (isUpload?uploadActivity:playActivity).getSystemService((isUpload?uploadActivity:playActivity).CLIPBOARD_SERVICE);
//                cm.setText(hlsUrl);
//                ToastUtil.showShortToast(isUpload?uploadActivity:playActivity, "视频地址已经复制到剪切板", 1);
//                shareDialog.dismiss();
//                break;
//            case R.id.txt_cancel:
//                shareDialog.dismiss();
//                break;
//        }
//    }

    @Event(R.id.btn_send)
    private void addDiscuss(View view) {

        if (StringUtils.empty(txtDiscuss.getText())) {
            return;
        }
        if(isUpload) {
            uploadActivity.sendDiscuss(txtDiscuss.getText().toString());
        }else{
            playActivity.sendDiscuss(txtDiscuss.getText().toString());
        }

    }

    public void updateDiscuss(boolean clearFlag) {
        discussListView.setSelection(discussAdapter.getCount());
        if(clearFlag){
            txtDiscuss.setText("");
        }

    }
}
