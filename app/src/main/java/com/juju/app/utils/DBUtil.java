package com.juju.app.utils;

import android.content.Context;

import com.facebook.stetho.InspectorModulesProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.inspector.database.DatabaseFilesProvider;
import com.facebook.stetho.inspector.database.SqliteDatabaseDriver;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.juju.app.config.CacheManager;
import com.juju.app.golobal.DBConstant;
import com.juju.app.ui.base.BaseApplication;

import org.xutils.DbManager;
import org.xutils.db.table.TableEntity;
import org.xutils.ex.DbException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：本地数据库辅助信息
 * 创建人：gm
 * 日期：2016/6/14 14:12
 * 版本：V1.0.0
 */
public class DBUtil {

    private final String TABLE_MESSAGE = "message";
    private final String TABLE_OTHER_MESSAGE = "other_message";
    private final String TABLE_INVITE = "invite";



    private volatile static DBUtil dbUtil = null;

    private Context context = null;

    private String userNO;

    private DbManager.DaoConfig daoConfig;

    private DBUtil(){

    }

    public static DBUtil instance(){
        if (dbUtil == null) {
            synchronized (DBUtil.class) {
                if (dbUtil == null) {
                    dbUtil = new DBUtil();
                }
            }
        }
        return dbUtil;
    }

    public DbManager.DaoConfig getDaoConfig() {
        return daoConfig;
    }

    public void initDBHelp(Context context, String userNO) {
        if(context == null || StringUtils.isBlank(userNO)) {
            throw new IllegalArgumentException("db parameter is null");
        }

        if(!userNO.equals(this.userNO)) {
            this.context = context;
            this.userNO = userNO;
            initDBConfig(context, userNO);
//            initStetho4Debug(context, userNO);
        }
    }

    private void initDBConfig(Context context, String userNO) {
        String dbDir =  CacheManager.getAppDatabasePath(context);
        File file = new File(dbDir);
        if(!file.isDirectory()) {
            file.mkdirs();
        }
        daoConfig = new DbManager.DaoConfig()
                .setDbDir(file)
                .setDbName("jlm_"+userNO)
                //监听table表创建事件
                .setTableCreateListener(new DbManager.TableCreateListener() {
                    @Override
                    public void onTableCreated(DbManager db, TableEntity<?> table) {
                        handlerTableCreated(db, table);
                    }
                });
    }


    private void handlerTableCreated(DbManager db, TableEntity<?> table) {
        executeDDL(db, table);
    }

    //执行DDL
    private void executeDDL(DbManager db, TableEntity<?> table) {
        try {
            if(TABLE_MESSAGE.equalsIgnoreCase(table.getName())) {
                db.execNonQuery("CREATE INDEX index_message_created ON message(created)");
                db.execNonQuery("CREATE UNIQUE INDEX index_message_session_key_msg_id " +
                        "on message(session_key, msg_id)");
            } else if (TABLE_OTHER_MESSAGE.equals(table.getName())) {
                db.execNonQuery("CREATE INDEX index_other_message_created " +
                        "ON other_message(created)");
            } else if (TABLE_INVITE.equals(table.getName())) {
                db.execNonQuery( "CREATE UNIQUE INDEX index_invite_id  " +
                        "ON invite(id)");
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

//    private void initStetho4Debug(Context context, final String userNO) {
//        if(context instanceof BaseApplication) {
//            final BaseApplication baseApplication = (BaseApplication)context;
//            Stetho.initialize(Stetho.newInitializerBuilder(context)
//                    .enableWebKitInspector(new InspectorModulesProvider() {
//                        @Override
//                        public Iterable<ChromeDevtoolsDomain> get() {
//                            return new Stetho.DefaultInspectorModulesBuilder(baseApplication).provideDatabaseDriver(
//                                    new SqliteDatabaseDriver(baseApplication, new DatabaseFilesProvider() {
//                                        @Override
//                                        public List<File> getDatabaseFiles() {
//                                            List<File> files = new ArrayList<File>();
//                                            files.add(baseApplication.getDatabasePath("jlm_"+userNO));
//                                            return files;
//                                        }
//                                    })
//                            ).finish();
//                        }
//                    }).build());
//
//        }
//    }
}
