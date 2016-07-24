package com.juju.app.fastdfs.callback;

import com.juju.app.fastdfs.StorePath;
import com.juju.app.fastdfs.exception.FdfsIOException;

/**
 * 项目名称：juju
 * 类描述：文件上传、下载进度更新回调接口
 * 创建人：gm
 * 日期：2016/7/22 09:43
 * 版本：V1.0.0
 */
public interface ProgressCallback<T> {

    void updateProgress(String id, long total, long current);

    void sendError(String id, FdfsIOException e);

    void recvError(String id, FdfsIOException e);

    void complete(String id, T t);

}
