package com.juju.app.fastdfs.service;

import com.juju.app.fastdfs.StorePath;
import com.juju.app.fastdfs.callback.ProgressCallback;

import java.io.InputStream;

/**
 * 项目名称：juju
 * 类描述：Storage模块接口
 * 创建人：gm
 * 日期：2016/7/22 09:41
 * 版本：V1.0.0
 */
public interface IStorageClientService {

    /**
     * 上传不可修改的文件, 回调上传进度
     * @param ins
     * @param size
     * @param ext
     * @return
     */
    public StorePath uploadFile(String uuid, String groupName, InputStream ins, long size,
                                String ext, ProgressCallback progressCallback);


    /**
     * 下载整个文件+回调下载进度
     *
     *
     * @param groupName
     * @param path
     * @param handling
     * @return
     */
    public <T> T downloadFile(String uuid, String groupName, String path,
                       IFdfsFileInputStreamHandler<T> handling, ProgressCallback callback);


    /**
     * 下载文件片段+回调下载进度
     *
     *
     * @param groupName
     * @param path
     * @param offset
     * @param size
     * @param handling
     * @return
     */
    <T> T downloadFile(String uuid, String groupName, String path, long offset, long size,
                       IFdfsFileInputStreamHandler<T> handling, ProgressCallback callback);

    String getNginxUrl(String groupName, String path);

}
