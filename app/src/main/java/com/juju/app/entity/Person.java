package com.juju.app.entity;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * 项目名称：juju
 * 类描述：
 * 创建人：gm
 * 日期：2016/5/8 00:49
 * 版本：V1.0.0
 */
@Table(name = "person")
public class Person extends Model {

    @Column(name = "name")
    private String name;

    @Column(name = "age")
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
