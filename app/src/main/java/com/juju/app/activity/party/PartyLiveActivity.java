package com.juju.app.activity.party;

import android.os.Bundle;
import android.view.View;
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
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ScreenUtil;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
        initView();
        loadLiveProgram();
        wrapProgramList();
        addClickListener();

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

    }


    private void loadLiveProgram(){
        videoProgramList = new ArrayList<VideoProgram>();
        VideoProgram v1 = new VideoProgram();
        v1.setCreatorName("聚龙小子");
        v1.setStatus(0);
        v1.setStartTime("2015-12-12 09:00:00");
        v1.setEndTime("2015-12-12 12:00:00");
        v1.setVideoUrl("rtmp://219.143.237.232:1935/juju/12345");

        VideoProgram v2 = new VideoProgram();
        v2.setCreatorName("金牛之女");
        v2.setStatus(1);
        v2.setStartTime("2015-12-12 09:20:00");
        v2.setEndTime("2015-12-12 10:00:00");
        v2.setVideoUrl("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");


        VideoProgram v3 = new VideoProgram();
        v3.setCreatorName("摩羯之子");
        v3.setStatus(1);
        v3.setStartTime("2015-12-12 15:00:00");
        v3.setEndTime("2015-12-12 15:10:00");
        v3.setVideoUrl("rtmp://219.143.237.232:1935/juju/123456");

        VideoProgram v4 = new VideoProgram();
        v4.setCreatorName("摩羯之子4");
        v4.setStatus(1);
        v4.setStartTime("2015-12-12 15:00:00");
        v4.setEndTime("2015-12-12 15:10:00");
        v4.setVideoUrl("rtmp://219.143.237.232:1935/juju/123456");


        VideoProgram v5 = new VideoProgram();
        v5.setCreatorName("摩羯之子5");
        v5.setStatus(1);
        v5.setStartTime("2015-12-12 15:00:00");
        v5.setEndTime("2015-12-12 15:10:00");
        v5.setVideoUrl("rtmp://219.143.237.232:1935/juju/123456");


        VideoProgram v6 = new VideoProgram();
        v6.setCreatorName("摩羯之子6");
        v6.setStatus(1);
        v6.setStartTime("2015-12-12 15:00:00");
        v6.setEndTime("2015-12-12 15:10:00");
        v6.setVideoUrl("rtmp://219.143.237.232:1935/juju/123456");
        videoProgramList.add(v1);
        videoProgramList.add(v2);
        videoProgramList.add(v3);
        videoProgramList.add(v4);
        videoProgramList.add(v5);
        videoProgramList.add(v6);

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
        ActivityUtil.startActivity4UPAndNew(this,UploadVideoActivity.class);
    }

    @Override
    public void playVideo(VideoProgram videoProgram) {
        //  TODO 获取视频请求的URL参数，传入播放界面
//        BasicNameValuePair nvPair = new BasicNameValuePair("videoUrl", videoProgram.getVideoUrl()+"?requestId="+ UUID.randomUUID().toString());
        ActivityUtil.startActivity4UPAndNew(this, PlayVideoActivity.class, "videoUrl",
                videoProgram.getVideoUrl()+"?requestId="+ UUID.randomUUID().toString());
    }
}
