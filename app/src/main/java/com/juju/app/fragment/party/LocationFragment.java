package com.juju.app.fragment.party;

import com.juju.app.R;
import com.juju.app.activity.party.PartyActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.ui.base.BaseFragment;

@CreateFragmentUI(viewId = R.layout.fragment_location)
public class LocationFragment extends BaseFragment{


    private PartyActivity parentActivity;


    @Override
    protected void findViews() {
        super.findViews();
        parentActivity = (PartyActivity) getActivity();
    }

    /**
     * 刷新页面
     */
    public void refresh() {

    }


}
