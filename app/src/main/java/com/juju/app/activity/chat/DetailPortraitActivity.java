package com.juju.app.activity.chat;

import android.content.Intent;
import android.os.Handler;

import com.juju.app.R;
import com.juju.app.annotation.CreateUI;
import com.juju.app.annotation.SystemColor;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.ui.base.BaseActivity;
import com.juju.app.ui.base.CreateUIHelper;
import com.juju.app.utils.Logger;
import com.juju.app.view.ZoomableImageView;

import org.xutils.view.annotation.ContentView;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/5/30 20:17
 * 版本：V1.0.0
 */
@ContentView(R.layout.activity_detail_portrait)
@CreateUI(showTopView = false)
@SystemColor(isApply = false)
public class DetailPortraitActivity extends BaseActivity implements CreateUIHelper {

    private Logger logger = Logger.getLogger(DetailPortraitActivity.class);


    public static String imageUri = "";

    @Override
    public void loadData() {

    }

    @Override
    public void initView() {
//        setTopTitle(R.string.search_original_image);
//        showTopLeftAll(R.string.page_user_detail, 0);
        Intent intent = getIntent();
        if (intent == null) {
            logger.e("detailPortrait#displayimage#null intent");
            return;
        }

        String resUri = intent.getStringExtra(Constants.KEY_AVATAR_URL);
        imageUri = resUri;
        logger.d("detailPortrait#displayimage#resUri:%s", resUri);

        boolean isContactAvatar = intent.getBooleanExtra(Constants.KEY_IS_IMAGE_CONTACT_AVATAR, false);
        logger.d("displayimage#isContactAvatar:%s", isContactAvatar);

        final ZoomableImageView portraitView = (ZoomableImageView) findViewById(R.id.detail_portrait);


        if (portraitView == null) {
            logger.e("detailPortrait#displayimage#portraitView is null");
            return;
        }

        logger.d("detailPortrait#displayimage#going to load the detail portrait");


        if (isContactAvatar) {
            IMUIHelper.setEntityImageViewAvatarNoDefaultPortrait(portraitView, resUri, DBConstant.SESSION_TYPE_SINGLE, 0);
        } else {
            IMUIHelper.displayImageNoOptions(portraitView, resUri, -1, 0);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                portraitView.setFinishActivity(new finishActivity() {
                    @Override
                    public void finish() {
                        if(DetailPortraitActivity.this!=null)
                        {
                            DetailPortraitActivity.this.finish();
                            overridePendingTransition(
                                    R.anim.tt_stay, R.anim.tt_image_exit);
                        }
                    }
                });
            }
        },500);
    }


    public interface finishActivity{
        public void finish();
    }
}
