package com.juju.app.entity.base;


import org.xutils.db.annotation.Column;

import java.io.Serializable;

/**
 * 项目名称：juju
 * 类描述：实体类--基类
 * 创建人：gm
 * 日期：2016/2/17 11:56
 * 版本：V1.0.0
 */
public abstract class BaseEntity implements Serializable {

    @Column(name = "local_id", isId = true)
    protected long localId;

    @Column(name = "id")
    protected String id;

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
