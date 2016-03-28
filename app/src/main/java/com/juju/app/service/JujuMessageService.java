package com.juju.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class JujuMessageService extends Service {

    private MediaProcessCallback callback;

    public JujuMessageService() {
    }

    public void setCallback(MediaProcessCallback mpCallback){
        callback = mpCallback;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
