package com.juju.app.db;

/**
 * 项目名称：juju
 * 类描述：数据库工厂类
 * 创建人：gm
 * 日期：2016/2/17 11:50
 * 版本：V1.0.0
 */
public class DaoFactory {

    private static DaoFactory mInstance = null;

    public static DaoFactory getInstance() {
        if (mInstance == null) {
            synchronized (DaoFactory.class) {
                if (mInstance == null) {
                    mInstance = new DaoFactory();
                }
            }
        }
        return mInstance;
    }


}
