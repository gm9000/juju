package com.juju.app.fastdfs.exception;

/**
 * 项目名称：juju
 * 类描述：非fastdfs本身的错误码抛出的异常，取服务端连接取不到时抛出的异常
 * 创建人：gm
 * 日期：2016/7/22 10:13
 * 版本：V1.0.0
 */
public class FdfsUnavailableException extends FdfsException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * @param message
	 */
	public FdfsUnavailableException(String message) {
		super("无法获取服务端连接资源：" + message);
	}
	
	/**
	 * @param message
	 */
	public FdfsUnavailableException(String message, Throwable t) {
		super("无法获取服务端连接资源：" + message, t);
	}

}
