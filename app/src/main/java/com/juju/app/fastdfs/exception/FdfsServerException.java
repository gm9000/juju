package com.juju.app.fastdfs.exception;

import com.juju.app.fastdfs.file.ErrorCodeConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * 项目名称：juju
 * 类描述：fastdfs服务端返回的错误码构成的异常
 * 创建人：gm
 * 日期：2016/7/22 10:20
 * 版本：V1.0.0
 */
public class FdfsServerException extends FdfsException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Map<Integer, String> CODE_MESSAGE_MAPPING;
	static {
		Map<Integer, String> mapping = new HashMap<Integer, String>();
		mapping.put((int) ErrorCodeConstants.ERR_NO_ENOENT, "猜测：找不到节点或文件");
		mapping.put((int) ErrorCodeConstants.ERR_NO_EIO, "猜测：服务端发生io异常");
		mapping.put((int) ErrorCodeConstants.ERR_NO_EINVAL, "猜测：无效的参数");
		mapping.put((int) ErrorCodeConstants.ERR_NO_EBUSY, "猜测：服务端忙");
		mapping.put((int) ErrorCodeConstants.ERR_NO_ENOSPC, "猜测：没有足够的存储空间");
		mapping.put((int) ErrorCodeConstants.ERR_NO_CONNREFUSED, "猜测：服务端拒绝连接");
		mapping.put((int) ErrorCodeConstants.ERR_NO_EALREADY, "猜测：文件已经存在？");
		CODE_MESSAGE_MAPPING = Collections.unmodifiableMap(mapping);
	}

	private int errorCode;

	/**
	 * 
	 */
	private FdfsServerException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public static FdfsServerException byCode(int errorCode) {
		String message = CODE_MESSAGE_MAPPING.get(errorCode);
		if (message == null) {
			message = "未知错误";
		}
		message = "错误码：" + errorCode + "，错误信息：" + message;

		return new FdfsServerException(errorCode, message);
	}

	/**
	 * @return the errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

}
