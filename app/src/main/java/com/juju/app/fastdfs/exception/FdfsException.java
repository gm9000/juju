package com.juju.app.fastdfs.exception;

/**
 * 项目名称：juju
 * 类描述：封装fastdfs的异常，使用非受检异常
 * 创建人：gm
 * 日期：2016/7/22 10:14
 * 版本：V1.0.0
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
