package com.juju.app.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.juju.app.R;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.golobal.Constants;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ThreadPoolUtil;
import com.juju.app.view.groupchat.IMGroupAvatar;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@ContentView(R.layout.fragment_qr)
@CreateFragmentUI(viewId = R.layout.fragment_qr)
public class QrFragment extends BaseFragment implements CreateUIHelper {

    private Logger logger = Logger.getLogger(QrFragment.class);


    private IMService imService;

    private String groupId;

    private String groupName;

    @ViewInject(R.id.tvname)
    private TextView tvname;

    @ViewInject(R.id.img_code)
    private ImageView img_code;

    @ViewInject(R.id.iv_head)
    private IMGroupAvatar iv_head;

    @ViewInject(R.id.lin_all)
    private LinearLayout lin_all;

    @ViewInject(R.id.progress_bar)
    private ProgressBar progressbar;

    private Handler uiHandler = new Handler();

    Bitmap bitmap;


    private IMServiceConnector imServiceConnector = new IMServiceConnector(){
        @Override
        public void onServiceDisconnected() {
        }

        @Override
        public void onIMServiceConnected() {
            logger.d("groupmgr#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            if(imService == null){
                Toast.makeText(QrFragment.this.getActivity(),
                        getResources().getString(R.string.im_service_disconnected),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            initViews();
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imServiceConnector.connect(getContext());
    }

    @Override
    public void onDestroy() {
        imServiceConnector.disconnect(getContext());
        bitmap = null;
        super.onDestroy();
    }

    @Override
    public void loadData() {
        showProgressBar();
//        groupName = getActivity().getIntent().getStringExtra(Constants.GROUP_NAME_KEY);
//        isGroup = "0".equals( getActivity().getIntent().getStringExtra(Constants.QR_VIEW_TYPE));
        groupId = getActivity().getIntent().getStringExtra(Constants.GROUP_ID_KEY);

    }

    @Override
    public void initView() {

    }

    private void initViews() {
        final GroupEntity groupEntity = imService.getGroupManager().findGroupById(groupId);
        groupName = groupEntity.getMainName();
        Set<String> set = groupEntity.getlistGroupMemberIds();
        List<String> avatarUrlList = new ArrayList<String>();
        for(String userNo : set){
            User entity = IMContactManager.instance().findContact(userNo);
            if(entity != null){
                avatarUrlList.add(entity.getAvatar());

            }
            if(avatarUrlList.size() >= 9){
                break;
            }
        }
        //构造群组图像
        setGroupAvatar(avatarUrlList);
        tvname.setText(groupName == null ? "" : groupName);

        //使用hander.post 解决lin_all获取宽高问题
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                final int width = lin_all.getWidth();
                final int height = width;
                //获取二维码
                JSONObject jsonObject = new JSONObject();
                String content = null;
                if(StringUtils.isBlank(groupEntity.getQrCode())) {
                    content = HttpConstants.APP_DOWN_URL;
                } else {
                    String token = "";
                    try {
                        jsonObject.put("code", groupEntity.getInviteCode());
                        jsonObject.put("groupId", groupEntity.getId());
                        token = jsonObject.toString();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    content = groupEntity.getQrCode()+"?token="+token;
                }
                bitmap = generateQRCode(content, width, height);
                hideProgressBar();
                img_code.setImageBitmap(bitmap);
            }
        });
    }

    private Bitmap generateQRCode(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            logger.d("generateQRCode -> width:%d,height:%d", lin_all.getWidth(), lin_all.getWidth());
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE,
                    width, height);
            return bitMatrix2Bitmap(matrix);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap bitMatrix2Bitmap(BitMatrix matrix) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        int[] rawData = new int[w * h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int color = Color.WHITE;
                if (matrix.get(i, j)) {
                    color = Color.BLACK;
                }
                rawData[i + (j * w)] = color;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        bitmap.setPixels(rawData, 0, w, 0, 0, w, h);
        return bitmap;
    }

    /**
     * 设置群头像
     * @param avatarUrlList
     */
    private void setGroupAvatar(List<String> avatarUrlList){
        try {
            if (null == avatarUrlList) {
                return;
            }
            iv_head.setAvatarUrlAppend(Constants.AVATAR_APPEND_32);
            iv_head.setChildCorner(3);
            if (null != avatarUrlList) {
                iv_head.setAvatarUrls(new ArrayList<String>(avatarUrlList));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public void showProgressBar() {
        progressbar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        progressbar.setVisibility(View.GONE);
    }


}
