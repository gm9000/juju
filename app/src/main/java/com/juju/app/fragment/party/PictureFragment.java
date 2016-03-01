package com.juju.app.fragment.party;

import com.juju.app.R;
import com.juju.app.activity.party.PartyActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.ui.base.BaseFragment;

/**
 * 项目名称：juju
 * 类描述：群聊—Fragment
 * 创建人：gm
 * 日期：2016/2/18 15:09
 * 版本：V1.0.0
 *
 */
@CreateFragmentUI(viewId = R.layout.fragment_picture)
public class PictureFragment extends BaseFragment{


    private PartyActivity parentActivity;



    @Override
    public void setOnListener() {
    }


    /**
     * 刷新页面
     */
    public void refresh() {

    }

    @Override
    protected void findViews() {
        super.findViews();
        parentActivity = (PartyActivity) getActivity();
    }


}
