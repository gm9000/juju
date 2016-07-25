package com.juju.app.activity.party;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.adapters.VideoProgramListAadpter;
import com.juju.app.entity.Party;
import com.juju.app.entity.VideoProgram;
import com.juju.app.enums.DisplayAnimation;
import com.juju.app.event.notify.LiveNotifyEvent;
import com.juju.app.event.notify.PartyNotifyEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.IMServiceConnector;
import com.juju.app.service.im.manager.IMContactManager;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.db.Selector;
import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ContentView(R.layout.activity_party_live)
public class PartyLiveActivity extends BaseActivity implements View.OnClickListener, VideoProgramListAadpter.Callback {

    private static final String TAG = "PartyLiveActivity";

    private static final int SEIZE_PLAY = 0x01;
    private static final int SEIZE_UPLOAD = 0x02;

    @ViewInject(R.id.layout_party_bar)
    private RelativeLayout topLayout;

    @ViewInject(R.id.img_right)
    private ImageView img_right;


    @ViewInject(R.id.txt_title)
    private TextView txt_title;

    @ViewInject(R.id.img_back)
    private ImageView img_back;

    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    @ViewInject(R.id.listLiveView)
    private ListView listView;

    @ViewInject(R.id.img_live_start)
    private ImageView imgLiveStart;


    private List<VideoProgram> videoProgramList;
    private  VideoProgramListAadpter programListAadpter;

    private Party party;
    private String partyId;

    private int lastVisibleItemPosition=0;
    private boolean hasLive = false;

    private IMService imService;
    /**
     * IMServiceConnector
     */
    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("UploadVieoActivity#onIMServiceConnected");
            imService = imServiceConnector.getIMService();
            initParam();
            initView();
            loadLiveProgram();
            wrapProgramList();
            addClickListener();
        }

        @Override
        public void onServiceDisconnected() {
        }
    };

    public IMContactManager getIMContactManager(){
        return imService.getContactManager();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        imServiceConnector.connect(this);
    }

    @Override
    protected  void onResume(){
        super.onResume();
        //  TODO 这里处理处理上有问题， 需要针对特定聚会进行局部刷新
        if(videoProgramList!=null && JujuDbUtils.needRefresh(VideoProgram.class)){
            Selector selector = null;
            try {
                selector = JujuDbUtils.getInstance().selector(VideoProgram.class).where("party_id","=",partyId);
                selector.orderBy("status").orderBy("local_id", true);
                videoProgramList = selector.findAll();
                if(videoProgramList == null){
                    videoProgramList = new ArrayList<VideoProgram>();
                }
                if(videoProgramList.size()>0){
                    hasLive = (videoProgramList.get(0).getStatus()==0);
                }
                programListAadpter.setVideoProgramList(videoProgramList);
                programListAadpter.notifyDataSetChanged();
                JujuDbUtils.closeRefresh(VideoProgram.class);
            } catch (DbException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        imServiceConnector.disconnect(this);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void initParam() {
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);

        try {
            party = JujuDbUtils.getInstance().selector(Party.class).where("id", "=", partyId).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }

        if(party.isNew()){
            party.setNew(false);
            JujuDbUtils.saveOrUpdate(party);
        }
    }

    private void addClickListener() {
        img_back.setOnClickListener(this);
        img_right.setOnClickListener(this);
        txt_left.setOnClickListener(this);


        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
                switch (scrollState) {
                    // 当不滚动时
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:// 当屏幕停止滚动时
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:// 当屏幕滚动时
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:// 当用户由于之前划动屏幕并抬起手指，屏幕产生惯性滑动时
                        break;
                }
            }

            /**
             * firstVisibleItem：当前能看见的第一个列表项ID（从0开始）
             * visibleItemCount：当前能看见的列表项个数（小半个也算） totalItemCount：列表项共数
             */
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                    if (firstVisibleItem > lastVisibleItemPosition) {// 上滑
                        if(imgLiveStart.getVisibility()==View.VISIBLE) {
                            imgLiveStart.startAnimation(DisplayAnimation.DOWN_HIDDEN);
                            topLayout.startAnimation(DisplayAnimation.UP_HIDDEN);
                            imgLiveStart.setVisibility(View.GONE);
                            topLayout.setVisibility(View.GONE);
                        }
                    } else if (firstVisibleItem < lastVisibleItemPosition) {// 下滑
                        if(imgLiveStart.getVisibility()==View.GONE) {

                            imgLiveStart.startAnimation(DisplayAnimation.UP_SHOW);
                            topLayout.startAnimation(DisplayAnimation.DOWN_SHOW);
                            imgLiveStart.setVisibility(View.VISIBLE);
                            topLayout.setVisibility(View.VISIBLE);
                        }
                    } else {
                        return;
                    }
                    lastVisibleItemPosition = firstVisibleItem;
            }
        });
    }

    private void initView() {

        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.top_left_back);
        txt_title.setText(R.string.live);
        img_right.setImageResource(R.mipmap.location_white);

        topLayout.getBackground().setAlpha(200);

        if(party.getStatus() == 2){
            imgLiveStart.setVisibility(View.GONE);
        }

        View emptyView = getLayoutInflater().inflate(R.layout.layout_empty, null);
        ((ViewGroup)listView.getParent()).addView(emptyView,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listView.setEmptyView(emptyView);
    }


    private void loadLiveProgram(){
        Selector selector = null;
        try {
            selector = JujuDbUtils.getInstance().selector(VideoProgram.class).where("party_id","=",partyId);
            selector.orderBy("status").orderBy("local_id", true);
            videoProgramList = selector.findAll();
            if(videoProgramList == null){
                videoProgramList = new ArrayList<VideoProgram>();
            }
            if(videoProgramList.size()>0){
                hasLive = (videoProgramList.get(0).getStatus()==0);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    private void wrapProgramList(){
        programListAadpter = new VideoProgramListAadpter(this, videoProgramList);
        programListAadpter.setCallback(this);
        listView.setAdapter(programListAadpter);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_right:
                Map<String,String> param = new HashMap<String,String>();
                param.put(Constants.GROUP_ID,party.getGroupId());
                param.put(Constants.PARTY_ID,partyId);
                ActivityUtil.startActivityNew(this,PartyLocationActivity.class,param);
                break;
            case R.id.img_back:
                ActivityUtil.finish(this);
                break;
            case R.id.txt_left:
                ActivityUtil.finish(this);
                break;
        }

    }

    @Event(value = R.id.img_live_start, type = View.OnClickListener.class)
    private void startLive(View view){
        if(hasLive){
            ToastUtil.showLongToast(this,getString(R.string.live_limit));
            return;
        }
        Map<String,String> param = new HashMap<String,String>();
        param.put(Constants.GROUP_ID,party.getGroupId());
        param.put(Constants.PARTY_ID,partyId);
        ActivityUtil.startActivityForResultNew(this,UploadVideoActivity.class,SEIZE_PLAY,param);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SEIZE_PLAY:
                    seizePlayLive(data.getStringExtra(Constants.LIVE_ID),data.getStringExtra(Constants.VIDEO_URL));
                    break;
                case SEIZE_UPLOAD:
                    seizeUploadLive(data.getStringExtra(Constants.LIVE_ID));
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private  void seizePlayLive(String liveId,String videoUrl){
        Map<String,Object> paramMap = new HashMap<String,Object>();
        paramMap.put(Constants.GROUP_ID,party.getGroupId());
        paramMap.put(Constants.LIVE_ID,liveId);
        paramMap.put(Constants.VIDEO_URL,videoUrl +"?requestId="+ UUID.randomUUID().toString());

        ActivityUtil.startActivityForResultNew(this, PlayVideoActivity.class,SEIZE_UPLOAD,paramMap);
    }

    private  void seizeUploadLive(String liveId){
        Map<String,String> param = new HashMap<String,String>();
        param.put(Constants.GROUP_ID,party.getGroupId());
        param.put(Constants.PARTY_ID,partyId);
        param.put(Constants.LIVE_ID,liveId);
        ActivityUtil.startActivityForResultNew(this,UploadVideoActivity.class,SEIZE_PLAY,param);
    }


    @Override
    public void playVideo(VideoProgram videoProgram) {
        //  TODO 获取视频请求的URL参数，传入播放界面

        Map<String,Object> paramMap = new HashMap<String,Object>();
        paramMap.put(Constants.GROUP_ID,party.getGroupId());
        paramMap.put(Constants.LIVE_ID,videoProgram.getId());
        paramMap.put(Constants.VIDEO_URL,videoProgram.getVideoUrl()+"?requestId="+ UUID.randomUUID().toString());

        ActivityUtil.startActivityForResultNew(this, PlayVideoActivity.class,SEIZE_UPLOAD, paramMap);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4LiveNotifyEvent(LiveNotifyEvent event){
        switch (event.event){
            case LIVE_START_OK:
                onResume();
                break;
            case LIVE_STOP_OK:
                onResume();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4PartyNotifyEvent(PartyNotifyEvent event){
        Log.d(TAG,"PartyNotifyEvent:"+event);
        switch (event.event){
            case PARTY_END_OK:
                if(event.bean.getPartyId().equals(partyId)){
                    ToastUtil.showShortToast(this,getString(R.string.party_finish),1);
                    imgLiveStart.setVisibility(View.GONE);
                }
                break;
        }
    }
}
