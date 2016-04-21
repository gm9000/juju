package com.juju.app.entity.base;

import com.lidroid.xutils.db.annotation.Column;
import com.lidroid.xutils.db.annotation.Id;
import com.lidroid.xutils.db.annotation.NoAutoIncrement;

import java.io.Serializable;

/**
 * 项目名称：juju
 * 类描述：实体类--基类
 * 创建人：gm
 * 日期：2016/2/17 11:56
 * 版本：V1.0.0
 */
public abstract class BaseEntity implements Serializable {

    @Id
    @Column(column = "id")
    protected Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
