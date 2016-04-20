package com.juju.app.entity.base;

import com.lidroid.xutils.db.annotation.Column;
import com.lidroid.xutils.db.annotation.Id;

import java.io.Serializable;

/**
 * 项目名称：juju
 * 类描述：实体类--基类
 * 创建人：gm
 * 日期：2016/2/17 11:56
 * 版本：V1.0.0
 */
public abstract class BaseEntity implements Serializable {

    @Id(column = "localId")
    private long localId;

    @Column(column = "id")
    private String id;

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
