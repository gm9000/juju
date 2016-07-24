package com.juju.app.fastdfs.file.handler;

import com.juju.app.fastdfs.callback.ProgressCallback;
import com.juju.app.fastdfs.exception.FdfsIOException;
import com.juju.app.fastdfs.exception.FdfsServerException;
import com.juju.app.fastdfs.file.BytesUtil;
import com.juju.app.fastdfs.file.CmdConstants;
import com.juju.app.fastdfs.file.ErrorCodeConstants;
import com.juju.app.fastdfs.file.OtherConstants;
import com.juju.app.fastdfs.socket.PooledFdfsSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * 项目名称：juju
 * 类描述：fastdfs处理基础类
 * 创建人：gm   
 * 日期：2016/7/22 10:17
 * 版本：V1.0.0
 */
abstract class AbstractHandler<T> implements ICmdProtoHandler<T> {

	protected final Socket socket;
	protected byte errorCode;
	protected long contentLength;
	protected T result;

	/**
	 * @param socket
	 */
	protected AbstractHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	/**
	 * 处理
	 *
	 */
	@Override
	public T handle() {
		try {
			send(socket.getOutputStream());
		} catch (IOException e) {
			if (socket instanceof PooledFdfsSocket) {
				((PooledFdfsSocket) socket).setNeedDestroy(true);
			}
			throw new FdfsIOException(
					"socket io exception occured while sending cmd", e);
		}
		try {
			receive(socket.getInputStream());
		} catch (IOException e) {
			if (socket instanceof PooledFdfsSocket) {
				((PooledFdfsSocket) socket).setNeedDestroy(true);
			}
			throw new FdfsIOException(
					"socket io exception occured while receive content", e);
		}

		if (errorCode == ErrorCodeConstants.SUCCESS) {
			return result;
		}
		throw FdfsServerException.byCode(errorCode);
	}

	/**
	 * 处理
	 * 
	 */
	@Override
	public T handle(String uuid, ProgressCallback callback) {
		try {
			send(socket.getOutputStream(), uuid, callback);
		} catch (IOException e) {
			if (socket instanceof PooledFdfsSocket) {
				((PooledFdfsSocket) socket).setNeedDestroy(true);
			}
			FdfsIOException fdfsIOException = new FdfsIOException(
					"socket io exception occured while sending cmd", e);
			callback.sendError(uuid, fdfsIOException);
			return null;
		}
		try {
			receive(socket.getInputStream(), uuid, callback, socket.getInetAddress().getHostAddress());
		} catch (IOException e) {
			if (socket instanceof PooledFdfsSocket) {
				((PooledFdfsSocket) socket).setNeedDestroy(true);
			}
			FdfsIOException fdfsIOException = new FdfsIOException(
					"socket io exception occured while receive content", e);
			callback.recvError(uuid, fdfsIOException);
			return null;
		}

		if (errorCode == ErrorCodeConstants.SUCCESS) {
			return result;
		}
		throw FdfsServerException.byCode(errorCode);

	}

	protected abstract void send(OutputStream ous) throws IOException;

	protected abstract void send(OutputStream ous, String uuid, ProgressCallback callback) throws IOException;

	protected abstract void receive(InputStream ins) throws IOException;

	protected abstract void receive(InputStream ins, String uuid, ProgressCallback callback, String recvHost) throws IOException;


	protected byte[] packHeader(byte cmd, long contentLength) {
		byte[] header;
		byte[] hex_len;

		header = new byte[OtherConstants.FDFS_PROTO_PKG_LEN_SIZE + 2];
		Arrays.fill(header, (byte) 0);

		hex_len = BytesUtil.long2buff(contentLength);
		System.arraycopy(hex_len, 0, header, 0, hex_len.length);
		header[OtherConstants.PROTO_HEADER_CMD_INDEX] = cmd;
		header[OtherConstants.PROTO_HEADER_STATUS_INDEX] = (byte) 0;
		return header;
	}

	protected void receiveHeader(InputStream ins) throws IOException {

		byte[] header = new byte[OtherConstants.FDFS_PROTO_PKG_LEN_SIZE + 2];
		int bytes;
		if ((bytes = ins.read(header)) != header.length) {
			throw new IOException("recv package size " + bytes + " != "
					+ header.length);
		}

		if (header[OtherConstants.PROTO_HEADER_CMD_INDEX] != CmdConstants.FDFS_PROTO_CMD_RESP) {
			throw new IOException("recv cmd: "
					+ header[OtherConstants.PROTO_HEADER_CMD_INDEX]
					+ " is not correct, expect cmd: "
					+ CmdConstants.FDFS_PROTO_CMD_RESP);
		}

		if (header[OtherConstants.PROTO_HEADER_STATUS_INDEX] != 0) {
			errorCode = header[OtherConstants.PROTO_HEADER_STATUS_INDEX];
			return;
		}

		long contentLength = BytesUtil.buff2long(header, 0);
		if (contentLength < 0) {
			throw new IOException("recv body length: " + contentLength
					+ " < 0!");
		}

		errorCode = (byte) 0;
		this.contentLength = contentLength;
	}

	protected void sendFileContent(InputStream ins, long size, OutputStream ous)
			throws IOException {
		long remainBytes = size;
		byte[] buff = new byte[256 * 1024];
		int bytes;
//		int curLength = 0;
//		double scbl;
		while (remainBytes > 0) {
			if ((bytes = ins
					.read(buff, 0, remainBytes > buff.length ? buff.length
							: (int) remainBytes)) < 0) {
				throw new IOException(
						"the end of the stream has been reached. not match the expected size ");
			}
//			curLength += bytes;
//			scbl = (double)curLength/size;
//		    int bl = (int)(scbl*100);
//			System.out.println("已上传："+curLength+"上传比例:"+bl);
			ous.write(buff, 0, bytes);
			remainBytes -= bytes;
		}
	}

}
