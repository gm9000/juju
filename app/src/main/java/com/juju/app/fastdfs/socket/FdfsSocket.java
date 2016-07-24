package com.juju.app.fastdfs.socket;

import com.juju.app.fastdfs.exception.FdfsConnectException;
import com.juju.app.fastdfs.file.BytesUtil;
import com.juju.app.fastdfs.file.CmdConstants;
import com.juju.app.fastdfs.file.OtherConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;



public class FdfsSocket extends Socket {

	public static FdfsSocket create(InetSocketAddress address,
			int soTimeout, int connectTimeout) {
		try {
			FdfsSocket socket = new FdfsSocket();
			socket.setSoTimeout(soTimeout);
			socket.connect(address, connectTimeout);
			return socket;
		} catch (IOException e) {
			throw new FdfsConnectException("can't create socket", e);
		}
	}

	@Override
	public synchronized void close() throws IOException {
		try {
			byte[] header = new byte[OtherConstants.FDFS_PROTO_PKG_LEN_SIZE + 2];
			Arrays.fill(header, (byte) 0);

			byte[] hex_len = BytesUtil.long2buff(0);
			System.arraycopy(hex_len, 0, header, 0, hex_len.length);
			header[OtherConstants.PROTO_HEADER_CMD_INDEX] = CmdConstants.FDFS_PROTO_CMD_QUIT;
			header[OtherConstants.PROTO_HEADER_STATUS_INDEX] = (byte) 0;
			this.getOutputStream().write(header);
		} catch (IOException ignore) {

		}

		super.close();
	}

	protected boolean check() {

		try {
			byte[] header = new byte[OtherConstants.FDFS_PROTO_PKG_LEN_SIZE + 2];
			Arrays.fill(header, (byte) 0);

			byte[] hex_len = BytesUtil.long2buff(0);
			System.arraycopy(hex_len, 0, header, 0, hex_len.length);
			header[OtherConstants.PROTO_HEADER_CMD_INDEX] = CmdConstants.FDFS_PROTO_CMD_ACTIVE_TEST;
			header[OtherConstants.PROTO_HEADER_STATUS_INDEX] = (byte) 0;

			this.getOutputStream().write(header);

			if (this.getInputStream().read(header) != header.length) {
				return false;
			}

			return header[OtherConstants.PROTO_HEADER_STATUS_INDEX] == 0 ? true
					: false;
		} catch (IOException e) {
			return false;
		}

	}

}
