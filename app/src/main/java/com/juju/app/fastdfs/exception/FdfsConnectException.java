package com.juju.app.fastdfs.exception;

/**
 * 项目名称：juju
 * 类描述：非fastdfs本身的错误码抛出的异常，socket连不上时抛出的异常
 * 创建人：gm
 * 日期：2016/7/22 10:13
 * 版本：V1.0.0
 */
public class FdfsConnectException extends FdfsUnavailableException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 */
	public FdfsConnectException(String message, Throwable t) {
		super(message, t);
	}

}
