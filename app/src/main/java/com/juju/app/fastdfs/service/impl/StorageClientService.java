package com.juju.app.fastdfs.service.impl;

import com.juju.app.fastdfs.StorageClient;
import com.juju.app.fastdfs.StorePath;
import com.juju.app.fastdfs.callback.ProgressCallback;
import com.juju.app.fastdfs.exception.FdfsIOException;
import com.juju.app.fastdfs.file.handler.ICmdProtoHandler;
import com.juju.app.fastdfs.file.handler.StorageDownloadHandler;
import com.juju.app.fastdfs.file.handler.StorageUploadHandler;
import com.juju.app.fastdfs.service.IFdfsFileInputStreamHandler;
import com.juju.app.fastdfs.service.IStorageClientService;
import com.juju.app.fastdfs.service.ITrackerClientService;
import com.juju.app.fastdfs.socket.FdfsInputStream;
import com.juju.app.fastdfs.socket.FdfsSocket;
import com.juju.app.fastdfs.socket.FdfsSocketService;
import com.juju.app.fastdfs.socket.PooledFdfsSocket;
import com.juju.app.utils.Logger;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * 项目名称：juju
 * 类描述：Storage模块业务实现 (是否需要放在IMService,管理其生命周期)
 * 创建人：gm
 * 日期：2016/7/22 09:48
 * 版本：V1.0.0
 */
public class StorageClientService implements IStorageClientService {

    //Nginx默认端口
    public final static int DEFAULT_NGINX_PORT = 80;

    private Logger logger = Logger.getLogger(StorageClientService.class);

    private volatile static StorageClientService inst;

    public static IStorageClientService instance() {
        if(inst == null) {
            synchronized (StorageClientService.class) {
                if (inst == null) {
                    inst = new StorageClientService();
                }
            }
        }
        return inst;
    }


    @Override
    public StorePath uploadFile(String uuid, String groupName, InputStream ins, long size,
                                String ext, ProgressCallback progressCallback) {
        StorageClient storageClient = TrackerClientService.instance()
                .getStoreStorage(groupName);

        FdfsSocket socket = FdfsSocketService.instance().getSocket(storageClient
                .getInetSocketAddress());
        ICmdProtoHandler<StorePath> handler = new StorageUploadHandler(socket,
                false, ins, size, storageClient.getStoreIndex(), ext,
                storageClient.getCharset());
        return process(uuid, socket, handler, progressCallback);
    }

    @Override
    public <T> T downloadFile(String uuid, String groupName, String path,
                              IFdfsFileInputStreamHandler<T> handling, ProgressCallback callback) {
        long offset = 0;
        long size = 0;
        return downloadFile(uuid, groupName, path, offset, size, handling, callback);
    }

    @Override
    public <T> T downloadFile(String uuid, String groupName, String path, long offset, long size,
                              IFdfsFileInputStreamHandler<T> handling, ProgressCallback callback) {
        StorageClient storageClient = TrackerClientService.instance().getFetchStorage(
                groupName, path);

        FdfsSocket socket = FdfsSocketService.instance().getSocket(storageClient
                .getInetSocketAddress());
        ICmdProtoHandler<FdfsInputStream> handler = new StorageDownloadHandler(
                socket, groupName, path, offset, size,
                storageClient.getCharset());

        try {
            FdfsInputStream fdfsInputStream = handler.handle(uuid, callback);
            T result = handling.deal(fdfsInputStream);

            if (!fdfsInputStream.isReadCompleted()
                    && socket instanceof PooledFdfsSocket) {
                ((PooledFdfsSocket) socket).setNeedDestroy(true);
            }
            IOUtils.closeQuietly(fdfsInputStream);
            return result;
        } catch (IOException e) {
            if (socket instanceof PooledFdfsSocket) {
                ((PooledFdfsSocket) socket).setNeedDestroy(true);
            }
            throw new FdfsIOException(e);
        } finally {
            IOUtils.closeQuietly(socket);
        }
    }

    @Override
    public String getNginxUrl(String groupName, String path) {
        StorageClient storageClient = TrackerClientService.instance().getFetchStorage(
                groupName, path);
        InetSocketAddress inetSockAddr = storageClient.getInetSocketAddress();
        String fileUrl = "http://" + inetSockAddr.getAddress().getHostAddress()+"/";
        return fileUrl;
    }

    private <T> T process(String uuid, FdfsSocket socket, ICmdProtoHandler<T> handler,
                          ProgressCallback progressCallback) {
        try {
            return handler.handle(uuid, progressCallback);
        } finally {
            IOUtils.closeQuietly(socket);
        }
    }
}
