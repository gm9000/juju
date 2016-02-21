package com.juju.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 项目名称：juju
 * 类描述：Activity调用onCreate()方法 调用此注解
 * 创建人：gm
 * 日期：2016/2/19 17:00
 * 版本：V1.0.0
    */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CreateUI {

        boolean isLoadData() default true;

        boolean isInitView() default true;
}
