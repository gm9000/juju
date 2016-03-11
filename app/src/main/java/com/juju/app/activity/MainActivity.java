package com.juju.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.fragment.GroupChatFragment;
import com.juju.app.fragment.GroupPartyFragment;
import com.juju.app.fragment.MeFragment;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.view.dialog.titlemenu.ActionItem;
import com.juju.app.view.dialog.titlemenu.TitlePopup;
import com.juju.app.view.dialog.titlemenu.TitlePopup.OnItemOnClickListener;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import org.apache.http.message.BasicNameValuePair;

@ContentView(R.layout.activity_main)
@CreateUI
public class MainActivity extends BaseActivity implements CreateUIHelper {

    @ViewInject(R.id.img_right)
    private ImageView img_right;

    @ViewInject(R.id.txt_title)
    private TextView txt_title;

    @ViewInject(R.id.layout_bar)
    private RelativeLayout layout_bar;

    private GroupChatFragment groupChatFragment;
    private GroupPartyFragment groupPartyFragment;
    private MeFragment meFragment;
    private TitlePopup titlePopup;


    private Fragment[] fragments;
    private ImageView[] imagebuttons;
    private TextView[] textviews;
    private int index;
    private int currentTabIndex;// 当前fragment的index

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void loadData() {

    }

    @Override
    public void initView() {
        initTabView();
        initPopWindow();
    }

    /**
     * 初始化TabView
     */
    private void initTabView() {
        groupChatFragment = new GroupChatFragment();
        groupPartyFragment = new GroupPartyFragment();
        meFragment = new MeFragment();
        fragments = new Fragment[] { groupChatFragment, groupPartyFragment,
                meFragment };
        imagebuttons = new ImageView[3];
        imagebuttons[0] = (ImageView) findViewById(R.id.ib_group_chat);
        imagebuttons[1] = (ImageView) findViewById(R.id.ib_group_party);
        imagebuttons[2] = (ImageView) findViewById(R.id.ib_profile);
        imagebuttons[0].setSelected(true);

        textviews = new TextView[3];
        textviews[0] = (TextView) findViewById(R.id.tv_group_chat);
        textviews[1] = (TextView) findViewById(R.id.tv_group_party);
        textviews[2] = (TextView) findViewById(R.id.tv_profile);
        textviews[0].setTextColor(0xFF45C01A);

        // 添加显示第一个fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, groupChatFragment)
                .add(R.id.fragment_container, groupPartyFragment)
                .add(R.id.fragment_container, meFragment)
                .hide(groupPartyFragment).hide(meFragment).show(groupChatFragment).commit();
    }

    public void onTabClicked(View view) {
        img_right.setVisibility(View.GONE);
        switch (view.getId()) {
            case R.id.re_group_chat:
                img_right.setVisibility(View.VISIBLE);
                index = 0;
                if (groupChatFragment != null) {
                    groupChatFragment.refresh();
                }
                txt_title.setText(R.string.group_chat);
                img_right.setImageResource(R.mipmap.icon_add);
                break;
            case R.id.re_group_party:
                index = 1;
                txt_title.setText(R.string.group_party);
                img_right.setVisibility(View.VISIBLE);
                img_right.setImageResource(R.mipmap.icon_titleaddfriend);
                break;
            case R.id.re_profile:
                index = 2;
                txt_title.setText(R.string.me);
                break;
        }
        if (currentTabIndex != index) {
            FragmentTransaction trx = getSupportFragmentManager()
                    .beginTransaction();
            trx.hide(fragments[currentTabIndex]);
            if (!fragments[index].isAdded()) {
                trx.add(R.id.fragment_container, fragments[index]);
            }
            trx.show(fragments[index]).commit();
        }
        imagebuttons[currentTabIndex].setSelected(false);
        // 把当前tab设为选中状态
        imagebuttons[index].setSelected(true);
        textviews[currentTabIndex].setTextColor(0xFF999999);
        textviews[index].setTextColor(0xFF45C01A);
        currentTabIndex = index;
    }


    private OnItemOnClickListener onitemClick = new OnItemOnClickListener() {

        @Override
        public void onItemClick(ActionItem item, int position) {
            switch (position) {
                case 0:// 创建群
                    break;
                case 1:// 扫一扫加群
                    break;
                case 2:// 邀请码加群
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
        titlePopup.addAction(new ActionItem(this, R.string.menu_group,
                R.mipmap.icon_menu_group));
        titlePopup.addAction(new ActionItem(this, R.string.menu_qrcode,
                R.mipmap.icon_menu_qrcode));
        titlePopup.addAction(new ActionItem(this, R.string.menu_invitecode,
                R.mipmap.icon_menu_invitecode));

    }


    @OnClick(R.id.img_right)
    public void onClick4ImgRight(View v) {
        titlePopup.show(layout_bar);
    }


}
