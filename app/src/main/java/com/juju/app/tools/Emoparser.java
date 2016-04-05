
package com.juju.app.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;


import com.juju.app.R;
import com.juju.app.utils.CommonUtil;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressLint("UseSparseArrays")
public class Emoparser {
    private Context context;
    private String[] emoList;
    private Pattern mPattern;
    private static HashMap<String, Integer> phraseIdMap;
    private static HashMap<Integer, String> idPhraseMap;
    private static Emoparser instance = null;
    private final int DEFAULT_SMILEY_TEXTS = R.array.default_emo_phrase;
    private final int[] DEFAULT_EMO_RES_IDS = {
            R.mipmap.tt_e0, R.mipmap.tt_e1,
            R.mipmap.tt_e2, R.mipmap.tt_e3, R.mipmap.tt_e4, R.mipmap.tt_e5,
            R.mipmap.tt_e6, R.mipmap.tt_e7, R.mipmap.tt_e8, R.mipmap.tt_e9,
            R.mipmap.tt_e10, R.mipmap.tt_e11, R.mipmap.tt_e12, R.mipmap.tt_e13,
            R.mipmap.tt_e14, R.mipmap.tt_e15, R.mipmap.tt_e16, R.mipmap.tt_e17,
            R.mipmap.tt_e18, R.mipmap.tt_e19, R.mipmap.tt_e20, R.mipmap.tt_e21,
            R.mipmap.tt_e22, R.mipmap.tt_e23, R.mipmap.tt_e24, R.mipmap.tt_e25,
            R.mipmap.tt_e26, R.mipmap.tt_e27, R.mipmap.tt_e28, R.mipmap.tt_e29,
            R.mipmap.tt_e30, R.mipmap.tt_e31, R.mipmap.tt_e32, R.mipmap.tt_e33,
            R.mipmap.tt_e34, R.mipmap.tt_e35, R.mipmap.tt_e36, R.mipmap.tt_e37,
            R.mipmap.tt_e38, R.mipmap.tt_e39, R.mipmap.tt_e40, R.mipmap.tt_e41,
            R.mipmap.tt_e42, R.mipmap.tt_e43, R.mipmap.tt_e44, R.mipmap.tt_e45
    };

    public int[] getResIdList() {
        return DEFAULT_EMO_RES_IDS;
    }

    public static synchronized Emoparser getInstance(Context cxt) {
        if (null == instance && null != cxt) {
            instance = new Emoparser(cxt);
        }
        return instance;
    }

    private Emoparser(Context cxt) {
        context = cxt;
        emoList = context.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
        buildMap();
        mPattern = buildPattern();
    }

    private void buildMap() {
        if (DEFAULT_EMO_RES_IDS.length != emoList.length) {
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        phraseIdMap = new HashMap<String, Integer>(emoList.length);
        idPhraseMap = new HashMap<Integer, String>(emoList.length);
        for (int i = 0; i < emoList.length; i++) {
            phraseIdMap.put(emoList[i], DEFAULT_EMO_RES_IDS[i]);
            idPhraseMap.put(DEFAULT_EMO_RES_IDS[i], emoList[i]);
        }
    }

    public HashMap<String, Integer> getPhraseIdMap() {
        return phraseIdMap;
    }

    public HashMap<Integer, String> getIdPhraseMap() {
        return idPhraseMap;
    }

    private Pattern buildPattern() {
        StringBuilder patternString = new StringBuilder(emoList.length * 3);
        patternString.append('(');
        for (String s : emoList) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        patternString.replace(patternString.length() - 1,
                patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }

    public CharSequence emoCharsequence(CharSequence text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        Matcher matcher = mPattern.matcher(text);
        while (matcher.find()) {
            int resId = phraseIdMap.get(matcher.group());
            Drawable drawable = context.getResources().getDrawable(resId);
            int size = (int) (CommonUtil.getElementSzie(context) * 0.8);
            drawable.setBounds(0, 0, size, size);
            ImageSpan imageSpan = new ImageSpan(drawable,
                    ImageSpan.ALIGN_BOTTOM);
            builder.setSpan(imageSpan, matcher.start(), matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
}
