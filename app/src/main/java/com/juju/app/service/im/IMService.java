package com.juju.app.service.im;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.juju.app.service.im.manager.IMLoginManager;
import com.juju.app.service.im.manager.IMRecentSessionManager;

public class IMService extends Service {

    private final String TAG = getClass().getSimpleName();

    private IMServiceBinder binder = new IMServiceBinder();


    public IMService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind" + this.toString());
        return binder;
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate" + this.toString());
        Log.d(TAG, "threadId" + Thread.currentThread().getId());
        super.onCreate();
        startForeground((int) System.currentTimeMillis(), new Notification());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand" + this.toString());
        Context ctx = getApplicationContext();
        IMLoginManager.instance().onStartIMManager(ctx, this);
        Log.d(TAG, "XMPP建立连接完成");
        //服务kill掉后能重启
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy" + this.toString());
        super.onDestroy();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind" + this.toString());
        return super.onUnbind(intent);
    }


    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind" + this.toString());
        super.onRebind(intent);
    }

    public IMLoginManager getLoginManager() {
        Log.d(TAG, "getLoginManager");
        return IMLoginManager.instance();
    }



    public class IMServiceBinder extends Binder {
        public IMService getService() {
            return IMService.this;
        }
    }
}
