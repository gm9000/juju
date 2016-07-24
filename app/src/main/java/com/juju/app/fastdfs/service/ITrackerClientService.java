package com.juju.app.fastdfs.service;

import com.juju.app.fastdfs.StorageClient;

/**
 * 项目名称：juju
 * 类描述：Tracker模块接口
 * 创建人：gm
 * 日期：2016/7/22 09:50
 * 版本：V1.0.0
 */
public interface ITrackerClientService {

    StorageClient getStoreStorage(String groupName);

    StorageClient getFetchStorage(String groupName, String path);

}
