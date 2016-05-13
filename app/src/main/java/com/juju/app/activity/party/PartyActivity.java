package com.juju.app.activity.party;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.entity.Plan;
import com.juju.app.fragment.party.LiveFragment;
import com.juju.app.fragment.party.LocationFragment;
import com.juju.app.fragment.party.PictureFragment;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.utils.ActivityUtil;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.dialog.titlemenu.ActionItem;
import com.juju.app.view.dialog.titlemenu.TitlePopup;
import com.juju.app.view.dialog.titlemenu.TitlePopup.OnItemOnClickListener;

import org.xutils.ex.DbException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;


@ContentView(R.layout.activity_party)
public class PartyActivity extends AppCompatActivity implements View.OnClickListener {

    @ViewInject(R.id.img_right)
    private ImageView img_right;

    @ViewInject(R.id.txt_title)
    private TextView txt_title;

    @ViewInject(R.id.img_back)
    private ImageView img_back;

    @ViewInject(R.id.txt_left)
    private TextView txt_left;

    private LocationFragment locationFragment;
    private LiveFragment liveFragment;
    private PictureFragment pictureFragment;
    private TitlePopup titlePopup;


    private Fragment[] fragments;
    private ImageView[] imagebuttons;
    private TextView[] textviews;
    private int index;
    private int currentTabIndex;// 当前fragment的index

    private String partyId;
    private Plan plan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);
        initParam();
        initTabView();
        initPopWindow();
        addClickListener();

    }

    private void initParam() {
        partyId = getIntent().getStringExtra(Constants.PARTY_ID);
        try {
            plan = JujuDbUtils.getInstance(this).selector(Plan.class).where("status", "=", 1).and("partyId", "=", partyId).findFirst();
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    private void addClickListener() {
        img_back.setOnClickListener(this);
        img_right.setOnClickListener(this);
        txt_left.setOnClickListener(this);
    }

    /**
     * 初始化TabView
     */
    private void initTabView() {
        locationFragment = new LocationFragment(plan);
        liveFragment = new LiveFragment();
        pictureFragment = new PictureFragment();
        fragments = new Fragment[]{locationFragment, liveFragment,
                pictureFragment};
        imagebuttons = new ImageView[3];
        imagebuttons[0] = (ImageView) findViewById(R.id.menu_location_icon);
        imagebuttons[1] = (ImageView) findViewById(R.id.menu_live_icon);
        imagebuttons[2] = (ImageView) findViewById(R.id.menu_picture_icon);

        textviews = new TextView[3];
        textviews[0] = (TextView) findViewById(R.id.menu_location_txt);
        textviews[1] = (TextView) findViewById(R.id.menu_live_txt);
        textviews[2] = (TextView) findViewById(R.id.menu_picture_txt);

        //  默认选中直播页签
        currentTabIndex = 1;
        imagebuttons[1].setSelected(true);
        textviews[1].setTextColor(0xFF0082E3);
        img_right.setVisibility(View.VISIBLE);
        img_right.setImageResource(R.mipmap.live_add);
        txt_title.setText(R.string.live);

        // 添加显示第一个fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.party_fragment_container, locationFragment)
                .add(R.id.party_fragment_container, liveFragment)
                .add(R.id.party_fragment_container, pictureFragment)
                .show(liveFragment).hide(pictureFragment).hide(locationFragment).commit();


        img_back.setVisibility(View.VISIBLE);
        txt_left.setVisibility(View.VISIBLE);
        txt_left.setText(R.string.group_party);

    }

    public void onTabClicked(View view) {
        img_right.setVisibility(View.GONE);
        switch (view.getId()) {
            case R.id.menu_locaiont:
                index = 0;
                if (locationFragment != null) {
                    locationFragment.refresh();
                }
                txt_title.setText(R.string.location);
                break;
            case R.id.menu_live:
                index = 1;
                txt_title.setText(R.string.live);

                img_right.setVisibility(View.VISIBLE);
                img_right.setImageResource(R.mipmap.live_add);

                break;
            case R.id.menu_picture:
                index = 2;
                txt_title.setText(R.string.picture);
                break;
        }
        if (currentTabIndex != index) {
            FragmentTransaction trx = getSupportFragmentManager()
                    .beginTransaction();
            trx.hide(fragments[currentTabIndex]);
            if (!fragments[index].isAdded()) {
                trx.add(R.id.party_fragment_container, fragments[index]);
            }
            trx.show(fragments[index]).commit();
        }
        imagebuttons[currentTabIndex].setSelected(false);
        // 把当前tab设为选中状态
        imagebuttons[index].setSelected(true);
        textviews[currentTabIndex].setTextColor(0xFF8E8E93);
        textviews[index].setTextColor(0xFF0082E3);
        currentTabIndex = index;
    }

    private OnItemOnClickListener onitemClick = new OnItemOnClickListener() {

        @Override
        public void onItemClick(ActionItem item, int position) {
            switch (position) {
                case 0:// 发起直播
                    break;
                default:
                    break;
            }
        }
    };

    private void initPopWindow() {
        // 实例化标题栏弹窗
        titlePopup = new TitlePopup(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titlePopup.setItemOnClickListener(onitemClick);
        // 给标题栏弹窗添加子类
        titlePopup.addAction(new ActionItem(this, R.string.menu_add_live,
                R.mipmap.icon_menu_group));

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_right:
                //  单击“发起直播”选项
                if(currentTabIndex == 1) {
                    //  初始化 发起直播节目的参数
                    ActivityUtil.startActivity(PartyActivity.this,UploadVideoActivity.class);
                }
                //  单击“照片菜单”项
                if(currentTabIndex == 2){
                    ToastUtil.showShortToast(PartyActivity.this, "照片菜单", 1);
                }
                break;
            case R.id.img_back:
                ActivityUtil.finish(this);
                break;
            case R.id.txt_left:
                ActivityUtil.finish(this);
                break;
        }

    }
}
