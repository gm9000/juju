package com.juju.app.fastdfs.exception;

/**
 * 封装fastdfs的异常，使用非受检异常
 * 
 * @author yuqih
 * 
 */
public abstract class FdfsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected FdfsException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	protected FdfsException(String message, Throwable cause) {
		super(message, cause);
	}

}
