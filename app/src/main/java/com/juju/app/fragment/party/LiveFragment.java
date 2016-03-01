package com.juju.app.fragment.party;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.juju.app.R;
import com.juju.app.activity.party.PartyActivity;
import com.juju.app.activity.party.PlayVideoActivity;
import com.juju.app.adapters.VideoProgramListAdpter;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.entity.VideoProgram;
import com.juju.app.ui.base.BaseFragment;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.NetWorkUtil;
import com.juju.app.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：群聊—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:09
 * 版本：V1.0.0
 */
@CreateFragmentUI(viewId = R.layout.fragment_live)
public class LiveFragment extends BaseFragment implements CreateUIHelper,
        View.OnClickListener, AdapterView.OnItemClickListener {


    private PartyActivity parentActivity;

    private ListView listView;
    private List<VideoProgram> videoProgramList;


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_error_item:
                NetWorkUtil.openSetNetWork(getActivity());
                break;
            default:
                break;
        }
    }

    @Override
    public void setOnListener() {
        listView.setOnItemClickListener(this);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        VideoProgram videoProgram = (VideoProgram) listView.getItemAtPosition(position);
        ToastUtil.showShortToast(getActivity(), videoProgram.getCreatorName() + "的直播节目", 1);
        videoProgram.getVideoUrl();
        ActivityUtil.startActivity(getActivity(), PlayVideoActivity.class);
    }


    /**
     * 刷新页面
     */
    public void refresh() {

    }

    @Override
    protected void findViews() {
        super.findViews();
        listView = (ListView) findViewById(R.id.listLiveView);
        parentActivity = (PartyActivity) getActivity();
    }

    @Override
    public void loadData() {
        initTestData();
    }

    @Override
    public void initView() {
        VideoProgramListAdpter adpter = new VideoProgramListAdpter(getActivity(), videoProgramList);
        listView.setAdapter(adpter);

    }

    /**
     * 测试数据
     */
    private void initTestData() {
        videoProgramList = new ArrayList<VideoProgram>();
        VideoProgram v1 = new VideoProgram();
        v1.setCreatorName("聚龙小子");
        v1.setStatus(0);
        v1.setStartTime("2015-12-12 09:00:00");
        v1.setEndTime("2015-12-12 12:00:00");

        VideoProgram v2 = new VideoProgram();
        v2.setCreatorName("金牛之女");
        v2.setStatus(1);
        v2.setStartTime("2015-12-12 09:20:00");
        v2.setEndTime("2015-12-12 10:00:00");

        VideoProgram v3 = new VideoProgram();
        v3.setCreatorName("摩羯之子");
        v3.setStatus(1);
        v3.setStartTime("2015-12-12 15:00:00");
        v3.setEndTime("2015-12-12 15:10:00");

        videoProgramList.add(v1);
        videoProgramList.add(v2);
        videoProgramList.add(v3);
    }
}
