package com.juju.app.event;


import com.juju.app.adapter.album.ImageItem;

import java.util.List;

/**
 * 项目名称：juju
 * 类描述：照片选择事件
 * 创建人：gm   
 * 日期：2016/7/21 16:31
 * 版本：V1.0.0
 */
public class SelectEvent {
    private List<ImageItem> list;
    public SelectEvent(List<ImageItem> list){
        this.list = list;
    }

    public List<ImageItem> getList() {
        return list;
    }

    public void setList(List<ImageItem> list) {
        this.list = list;
    }
}
