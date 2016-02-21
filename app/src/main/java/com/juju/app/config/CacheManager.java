package com.juju.app.config;

import android.content.Context;

import com.juju.app.utils.FileUtil;

import java.io.File;

/**
 * 项目名称：juju
 * 类描述：App缓存管理
 * 创建人：gm
 * 日期：2016/2/17 11:43
 * 版本：V1.0.0
 */
public class CacheManager {

    /**
     * 获取外部缓存目录
     *
     * @param context
     * @return
     */
    public static String getExternalCachePath(Context context) {
        return FileUtil.getExternalCacheDir(context);
    }

    /**
     * 获取博客收藏数据库目录
     *
     * @param context
     * @return
     */
    public static String getBloggerCollectDbPath(Context context) {
        return getExternalCachePath(context) + File.separator + "BlogCollect";
    }

    /**
     * 获取博主数据库目录
     *
     * @param context
     * @return
     */
    public static String getBloggerDbPath(Context context) {
        return getExternalCachePath(context) + File.separator + "Blogger";
    }

    /**
     * 获取博客列表数据库目录
     *
     * @param context
     * @return
     */
    public static String getBlogListDbPath(Context context) {
        return getExternalCachePath(context) + File.separator + "BlogList";
    }

    /**
     * 获取博客内容数据库目录
     *
     * @param context
     * @return
     */
    public static String getBlogContentDbPath(Context context) {
        return getExternalCachePath(context) + File.separator + "BlogContent";
    }

    /**
     * 获取评论数据库目录
     *
     * @param context
     * @return
     */
    public static String getBlogCommentDbPath(Context context) {
        return getExternalCachePath(context) + File.separator + "CommentList";
    }

    /**
     * 获取某频道博主数据库目录
     *
     * @param context
     * @return
     */
    public static String getChannelBloggerDbPath(Context context) {
        return getExternalCachePath(context) + File.separator + "ChannelBlogger";
    }

    /**
     * 获取WebView缓存目录
     *
     * @return
     */
    public static String getAppCachePath(Context context) {
        return getExternalCachePath(context) + File.separator + "App" + File.separator + "Cache";
    }

    /**
     * 获取WebView数据库目录
     *
     * @param context
     * @return
     */
    public static String getAppDatabasePath(Context context) {
        return getExternalCachePath(context) + File.separator + "App" + File.separator + "DataBase";
    }
}
