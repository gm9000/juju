package com.juju.app.service;

public interface MediaProcessCallback {
    void onConnectStatus(int status);
    void onBitRate(int bitRate);
}
