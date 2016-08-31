package com.juju.app.service.im;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;


import com.juju.app.entity.chat.ImageMessage;
import com.juju.app.event.MessageEvent;
import com.juju.app.fastdfs.StorePath;
import com.juju.app.fastdfs.callback.ProgressCallback;
import com.juju.app.fastdfs.exception.FdfsIOException;
import com.juju.app.fastdfs.service.impl.StorageClientService;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.helper.PhotoHelper;
import com.juju.app.utils.FileUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.SystemConfigSp;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;


/**
 * 项目名称：juju
 * 类描述：异步图片服务
 * 创建人：gm
 * 日期：2016/7/22 16:18
 * 版本：V1.0.0
 */
public class LoadImageService extends IntentService {

    private static Logger logger = Logger.getLogger(LoadImageService.class);

    public LoadImageService(){
        super("LoadImageService");
    }

    public LoadImageService(String name) {
        super(name);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(android.content.Intent)}.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final ImageMessage messageInfo = (ImageMessage)intent.getSerializableExtra(Constants.UPLOAD_IMAGE_INTENT_PARAMS);
            final String result = null;
            Bitmap bitmap;
            try {
                File file= new File(messageInfo.getPath());
                bitmap = PhotoHelper.revitionImage(messageInfo.getPath());
                if (null != bitmap) {
                    byte[] bytes = PhotoHelper.getBytes(bitmap);
                    String uuid = UUID.randomUUID().toString();
                    StorageClientService.instance().uploadFile(uuid, GlobalVariable.GROUPNAME1, new ByteArrayInputStream(bytes), bytes.length,
                            FileUtil.getExtensionName(messageInfo.getPath()).toLowerCase(), new ProgressCallback<StorePath>() {

                                @Override
                                public void updateProgress(String id, long total, long current) {
                                    logger.d("上传进度：%d", current);
                                    double d = (double)current/total;
                                    logger.d("上传百分百："+(d*100));
                                    EventBus.getDefault().post(new MessageEvent(MessageEvent.Event
                                            .IMAGE_UPLOAD_PROGRESSING, messageInfo, Double.valueOf(d*100).intValue()));
                                }

                                @Override
                                public void sendError(String id, FdfsIOException e) {
                                    logger.e("uploadFile#sendError is error");
                                    e.printStackTrace();
                                    EventBus.getDefault().post(new MessageEvent(MessageEvent.Event
                                            .IMAGE_UPLOAD_FAILD, messageInfo));
                                }

                                @Override
                                public void recvError(String id, FdfsIOException e) {
                                    logger.e("uploadFile#recvError is error");
                                    e.printStackTrace();
                                    EventBus.getDefault().post(new MessageEvent(MessageEvent.Event.IMAGE_UPLOAD_FAILD
                                            ,messageInfo));
                                }

                                @Override
                                public void complete(String id, StorePath storePath) {
                                    System.out.println("上传完成");
                                    logger.i("upload image succcess,url is %s",storePath.getUrl());
                                    String imageUrl = storePath.getUrl();
                                    messageInfo.setUrl(imageUrl);
                                    EventBus.getDefault().post(new MessageEvent(MessageEvent.Event
                                            .IMAGE_UPLOAD_SUCCESS, messageInfo));
                                }
                            });
                }
            } catch (IOException e) {
                logger.e(e.getMessage());
            }
    }
}
