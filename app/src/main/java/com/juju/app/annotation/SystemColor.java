package com.juju.app.annotation;

import com.juju.app.R;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 项目名称：juju
 * 类描述：系统色彩
 * 创建人：gm
 * 日期：2016/5/11 09:47
 * 版本：V1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SystemColor {

    boolean isApply() default true;

    int colorValue() default R.color.blue;

    int titleColorValue() default R.color.blue;
}
