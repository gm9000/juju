package com.juju.app.fastdfs.socket;

import com.juju.app.fastdfs.exception.FdfsConnectException;

import java.net.InetSocketAddress;


public class BorrowSockectErrorCreateNotPooledPolicy implements
		IBorrowSockectErrorPolicy {

	private int soTimeout;
	private int connectTimeout;

	public void init() {
		if (connectTimeout <= 0) {
			connectTimeout = FdfsSocketService.DEFAULT_CONNECT_TIMEOUT;
		}

		if (soTimeout <= 0) {
			soTimeout = FdfsSocketService.DEFAULT_NETWORK_TIMEOUT;
		}

	}

	@Override
	public FdfsSocket handleWhenErrorOccur(FdfsSocketPool pool,
			InetSocketAddress address, Exception ex) {
		Throwable e = ex; 
		int i = 0;
		while (e != null && i < 5) {
			if (e instanceof FdfsConnectException) {
				throw (FdfsConnectException)e;
			}
			e = e.getCause();
			i++;
		}
		
		return FdfsSocket.create(address, soTimeout, connectTimeout);
	}

	/**
	 * @param soTimeout
	 *            the soTimeout to set
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @param connectTimeout
	 *            the connectTimeout to set
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
