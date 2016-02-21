package com.juju.app.entity.base;

import com.lidroid.xutils.db.annotation.Column;

import java.io.Serializable;

/**
 * 项目名称：juju
 * 类描述：实体类--基类
 * 创建人：gm
 * 日期：2016/2/17 11:56
 * 版本：V1.0.0
 */
public abstract class BaseEntity implements Serializable {

    @Column(column = "id")
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
