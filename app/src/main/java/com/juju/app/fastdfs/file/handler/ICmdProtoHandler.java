package com.juju.app.fastdfs.file.handler;

import third.rewrite.fastdfs.callback.ProgressCallback;

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
	T handle(ProgressCallback progressCallback);

}
