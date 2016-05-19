package com.juju.app.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.activity.SearchActivity;
import com.juju.app.annotation.CreateFragmentUI;
import com.juju.app.utils.Logger;
import com.juju.app.view.SearchEditText;
import com.juju.libs.tools.ScreenTools;

import org.xutils.view.annotation.ContentView;
import org.xutils.x;


/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/5/19 09:54
 * 版本：V1.0.0
 */
public abstract class TitleBaseFragment extends BaseFragment {


    protected ViewGroup topContentView;


    protected ImageView topLeftBtn;

    protected ImageView topRightBtn;

    protected TextView topTitleTxt;

    protected TextView topLetTitleTxt;

    protected TextView topRightTitleTxt;

    protected ViewGroup topBar;

    protected SearchEditText topSearchEdt;

    protected FrameLayout searchFrameLayout;


    protected float x1, y1, x2, y2 = 0;
    protected static Logger logger = Logger.getLogger(TitleBaseFragment.class);


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        topContentView = (ViewGroup) LayoutInflater
                .from(getActivity()).inflate(R.layout.tt_fragment_base, null);
        topBar = (ViewGroup) topContentView.findViewById(R.id.topbar);

        //左边区域
        topLetTitleTxt = (TextView) topContentView.findViewById(R.id.txt_left);
        topLeftBtn = (ImageView) topContentView.findViewById(R.id.img_back);

        //右边区域
        topRightTitleTxt = (TextView) topContentView.findViewById(R.id.txt_right);
        topRightBtn = (ImageView) topContentView.findViewById(R.id.img_right);

        //标题
        topTitleTxt = (TextView) topContentView.findViewById(R.id.txt_title);

        //搜索框
        topSearchEdt = (SearchEditText) topContentView.findViewById(R.id.chat_title_search);

        //搜索FrameLayout
        searchFrameLayout = (FrameLayout)topContentView.findViewById(R.id.searchbar);

        initView();
    }


    //重写父类方法
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

    }

    //重写父类方法
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vg,
                             Bundle bundle) {
        final ContentView contentView = this.getClass().
                getAnnotation(ContentView.class);
        if(contentView != null && contentView.value() >0) {
            if(rootView == null) {
                rootView = inflater.inflate(contentView.value(), topContentView, true);
                x.view().inject(this, rootView);
                findViews();
                setOnListener();
                final CreateFragmentUI createFragmentUI = this.getClass().
                        getAnnotation(CreateFragmentUI.class);
                if(this instanceof CreateUIHelper) {
                    CreateUIHelper uiHelper = (CreateUIHelper) this;
                    if(createFragmentUI.isLoadData()) {
                        uiHelper.loadData();
                    }
                    if(createFragmentUI.isInitView()) {
                        uiHelper.initView();
                    }
                }
            }
//            initView();
        }
        return rootView;
    }

    private void initView() {
        topLeftBtn.setVisibility(View.GONE);
        topLetTitleTxt.setVisibility(View.GONE);
        topRightBtn.setVisibility(View.GONE);
        topRightTitleTxt.setVisibility(View.GONE);
        topTitleTxt.setVisibility(View.GONE);
        topSearchEdt.setVisibility(View.GONE);
    }

    protected void setTopTitleBold(String title) {
        if (title == null) {
            return;
        }
        if (title.length() > 12) {
            title = title.substring(0, 11) + "...";
        }
        // 设置字体为加粗
        TextPaint paint =  topTitleTxt.getPaint();
        paint.setFakeBoldText(true);

        topTitleTxt.setText(title);
        topTitleTxt.setVisibility(View.VISIBLE);

    }

    protected void setTopTitle(String title) {
        if (title == null) {
            return;
        }
        if (title.length() > 12) {
            title = title.substring(0, 11) + "...";
        }
        topTitleTxt.setText(title);
        topTitleTxt.setVisibility(View.VISIBLE);
    }

    protected void hideTopTitle() {
        topTitleTxt.setVisibility(View.GONE);
    }


    protected void setTopLeftButton(int resID) {
        if (resID < 0) {
            return;
        } else if (resID == 0) {
            resID = R.mipmap.icon_back;
        }
        topLeftBtn.setImageResource(resID);
        topLeftBtn.setVisibility(View.VISIBLE);
    }

    protected void setTopLeftButtonPadding(int l,int t,int r,int b) {
        topLeftBtn.setPadding(l, t, r, b);
    }

    protected void hideTopLeftButton() {
        topLeftBtn.setVisibility(View.GONE);
    }

    protected void setTopLeftText(int resId) {
        if (resId < 0) {
            return;
        } else if (resId == 0) {
            resId = R.string.top_left_back;
        }
        topLetTitleTxt.setText(resId);
        topLetTitleTxt.setVisibility(View.VISIBLE);
    }

    protected void setTopRightText(int resId) {
        if (resId < 0) {
            return;
        } else if (resId == 0) {
            resId = R.string.save;
        }
        topRightTitleTxt.setText(resId);
        topRightTitleTxt.setVisibility(View.VISIBLE);
    }

    protected void setTopRightButton(int resID) {
        if (resID < 0) {
            return;
        } else if (resID == 0) {
            resID = R.mipmap.icon_add;
        }
        topRightBtn.setImageResource(resID);
        topRightBtn.setVisibility(View.VISIBLE);
    }

    protected void hideTopRightButton() {

        topRightBtn.setVisibility(View.GONE);
    }

    @Deprecated
    protected void setTopBar(int resID) {
        if (resID <= 0) {
            return;
        }
        topBar.setBackgroundResource(resID);
    }

    protected void showTopSearchBar() {
        topSearchEdt.setVisibility(View.VISIBLE);
    }

    protected void hideTopSearchBar() {
        topSearchEdt.setVisibility(View.GONE);
    }


    /**
     * 显示左边区域
     * @param textId
     * @param btnId
     */
    protected void showLeftAll(int textId, int btnId) {
        setTopLeftText(textId);
        setTopLeftButton(btnId);
    }

    /**
     * 隐藏左右区域
     *
     */
    protected void hideLeftAll() {
        hideTopLeftButton();
        topLetTitleTxt.setVisibility(View.GONE);
    }

    /**
     * 显示右边区域
     * @param textId
     * @param btnId
     */
    protected void showRightAll(int textId, int btnId) {
        setTopRightText(textId);
        setTopRightButton(btnId);
    }

    /**
     * 隐藏右边区域
     *
     */
    protected void hideRightAll() {
        hideTopRightButton();
        topRightTitleTxt.setVisibility(View.GONE);
    }


    protected void showSearchAll() {
        showTopSearchBar();
    }

    protected void hideSearchAll() {
        hideTopSearchBar();
    }





    /**
     * 显示搜索searchFrameLayout
     */
    protected void showSearchFrameLayout(){
        searchFrameLayout.setVisibility(View.VISIBLE);
        /**还是以前的页面，没有看psd是否改过*/
        searchFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showSearchView();
            }
        });
        //下面的历史代码
        //tryHandleSearchAction(action);
    }



    protected abstract void initHandler();

    @Override
    public void onActivityCreated(Bundle bundle) {
        logger.d("Fragment onActivityCreate:" + getClass().getName());
        super.onActivityCreated(bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void initSearch() {
//        setTopRightButton(R.mipmap.tt_top_search);
//        topRightBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                showSearchView();
//            }
//        });
    }

    public void showSearchView() {
        startActivity(new Intent(getActivity(), SearchActivity.class));
    }

    protected void onSearchDataReady() {
        initSearch();
    }
}
