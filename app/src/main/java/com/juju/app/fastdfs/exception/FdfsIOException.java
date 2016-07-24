package com.juju.app.fastdfs.exception;

/**
 * 项目名称：juju
 * 类描述： 非fastdfs本身的错误码抛出的异常，而是java客户端向服务端发送命令、
 * 文件或从服务端读取结果、下载文件时发生io异常
 * 创建人：gm
 * 日期：2016/7/22 10:19
 * 版本：V1.0.0
 */
public class FdfsIOException extends FdfsException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * @param cause
	 */
	public FdfsIOException(Throwable cause) {
		super("客户端连接服务端出现了io异常" , cause);
	}

	/**
	 * @param cause
	 */
	public FdfsIOException(String messge, Throwable cause) {
		super("客户端连接服务端出现了io异常:" +  messge , cause);
	}
	
}
