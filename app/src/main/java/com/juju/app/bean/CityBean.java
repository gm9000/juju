package com.juju.app.bean;

import android.text.TextUtils;

import com.juju.app.entity.chat.SearchElement;
import com.juju.app.utils.pinyin.PinYinUtil;

/**
 * Created by Administrator on 2016/8/4 0004.
 */
public class CityBean {
    private String city;
    private SearchElement searchElement = new SearchElement();

    private String sectionName;
    private PinYinUtil.PinYinElement pinyinElement = new PinYinUtil.PinYinElement();

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public SearchElement getSearchElement() {
        return searchElement;
    }

    public void setSearchElement(SearchElement searchElement) {
        this.searchElement = searchElement;
    }

    public PinYinUtil.PinYinElement getPinyinElement() {
        return pinyinElement;
    }

    public String getSectionName() {
        sectionName = "";
        if (TextUtils.isEmpty(pinyinElement.pinyin)) {
            return sectionName;
        }
        sectionName = pinyinElement.pinyin.substring(0, 1);
        return sectionName;
    }
}
