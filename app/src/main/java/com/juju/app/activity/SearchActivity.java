package com.juju.app.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.juju.app.R;
import com.juju.app.annotation.SystemColor;
import com.juju.app.ui.base.BaseActivity;

import org.xutils.view.annotation.ContentView;


@ContentView(R.layout.tt_fragment_activity_search)
//@SystemColor(colorValue = R.color.white)
public class SearchActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
