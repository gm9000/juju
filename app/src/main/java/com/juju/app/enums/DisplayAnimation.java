package com.juju.app.enums;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

/**
 * Created by JanzLee on 2016/7/8 0008.
 */
public class DisplayAnimation {

    public static final TranslateAnimation UP_SHOW = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            1.2f, Animation.RELATIVE_TO_SELF, 0.0f);

    public static final TranslateAnimation UP_HIDDEN = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, -1.2f);

    public static final TranslateAnimation DOWN_SHOW = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            -1.2f, Animation.RELATIVE_TO_SELF, 0.0f);
    public static final TranslateAnimation DOWN_HIDDEN = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 1.2f);

    static{
        UP_SHOW.setDuration(600);
        UP_HIDDEN.setDuration(600);
        DOWN_SHOW.setDuration(600);
        DOWN_HIDDEN.setDuration(600);
    }
}
