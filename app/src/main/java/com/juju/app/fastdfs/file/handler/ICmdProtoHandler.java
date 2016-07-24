package com.juju.app.fastdfs.file.handler;


import com.juju.app.fastdfs.callback.ProgressCallback;

public interface ICmdProtoHandler<T> {

	/**
	 * 处理
	 *
	 */
	T handle();

	/**
	 * 处理并回调
	 * 
	 */
	T handle(String uuid, ProgressCallback progressCallback);

}
