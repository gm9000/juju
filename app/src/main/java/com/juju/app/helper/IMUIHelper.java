package com.juju.app.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.juju.app.R;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.SearchElement;
import com.juju.app.event.LoginEvent;
import com.juju.app.utils.pinyin.PinYinUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

public class IMUIHelper {






    // 文字高亮显示
    public static void setTextHilighted(TextView textView, String text,SearchElement searchElement) {
        textView.setText(text);
        if (textView == null
                || TextUtils.isEmpty(text)
                || searchElement ==null) {
            return;
        }

        int startIndex = searchElement.startIndex;
        int endIndex = searchElement.endIndex;
        if (startIndex < 0 || endIndex > text.length()) {
            return;
        }
        // 开始高亮处理
        int color =  Color.rgb(69, 192, 26);
        textView.setText(text, BufferType.SPANNABLE);
        Spannable span = (Spannable) textView.getText();
        span.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    public static boolean handleContactSearch(String key, User contact) {
        if (TextUtils.isEmpty(key) || contact == null) {
            return false;
        }

        contact.getSearchElement().reset();

        return handleTokenFirstCharsSearch(key, contact.getPinyinElement(), contact.getSearchElement())
                || handleTokenPinyinFullSearch(key, contact.getPinyinElement(), contact.getSearchElement())
                || handleNameSearch(contact.getNickName(), key, contact.getSearchElement());
        // 原先是 contact.name 代表花名的意思嘛??
    }

    public static boolean handleTokenFirstCharsSearch(String key, PinYinUtil.PinYinElement pinYinElement, SearchElement searchElement) {
        return handleNameSearch(pinYinElement.tokenFirstChars, key.toUpperCase(), searchElement);
    }

    public static boolean handleNameSearch(String name, String key,
                                           SearchElement searchElement) {
        int index = name.indexOf(key);
        if (index == -1) {
            return false;
        }

        searchElement.startIndex = index;
        searchElement.endIndex = index + key.length();

        return true;
    }

    public static boolean handleTokenPinyinFullSearch(String key, PinYinUtil.PinYinElement pinYinElement, SearchElement searchElement) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }

        String searchKey = key.toUpperCase();

        //onLoginOut the old search result
        searchElement.reset();

        int tokenCnt = pinYinElement.tokenPinyinList.size();
        int startIndex = -1;
        int endIndex = -1;

        for (int i = 0; i < tokenCnt; ++i) {
            String tokenPinyin = pinYinElement.tokenPinyinList.get(i);

            int tokenPinyinSize = tokenPinyin.length();
            int searchKeySize = searchKey.length();

            int keyCnt = Math.min(searchKeySize, tokenPinyinSize);
            String keyPart = searchKey.substring(0, keyCnt);

            if (tokenPinyin.startsWith(keyPart)) {

                if (startIndex == -1) {
                    startIndex = i;
                }

                endIndex = i + 1;
            } else {
                continue;
            }

            if (searchKeySize <= tokenPinyinSize) {
                searchKey = "";
                break;
            }

            searchKey = searchKey.substring(keyCnt, searchKeySize);
        }

        if (!searchKey.isEmpty()) {
            return false;
        }

        if (startIndex >= 0 && endIndex > 0) {
            searchElement.startIndex = startIndex;
            searchElement.endIndex = endIndex;

            return true;
        }

        return false;
    }

    public static boolean handleGroupSearch(String key, GroupEntity group) {
        if (TextUtils.isEmpty(key) || group == null) {
            return false;
        }
        group.getSearchElement().reset();

        return handleTokenFirstCharsSearch(key, group.getPinyinElement(), group.getSearchElement())
                || handleTokenPinyinFullSearch(key, group.getPinyinElement(), group.getSearchElement())
                || handleNameSearch(group.getMainName(), key, group.getSearchElement());
    }

}
