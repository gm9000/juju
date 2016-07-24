package com.juju.app.fastdfs.exception;

/**
 * 非fastdfs本身的错误码抛出的异常，socket连不上时抛出的异常
 * 
 * @author yuqihuang
 * 
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
