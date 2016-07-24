package com.juju.app.fastdfs.socket;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import third.rewrite.fastdfs.exception.FdfsConnectException;

class FdfsSocketFactory extends BasePooledObjectFactory<PooledFdfsSocket> {

	private FdfsSocketPool pool;

	private InetSocketAddress socketAddress;
	private int soTimeout;
	private int connectTimeout;

	/**
	 * @param pool
	 * @param socketAddress
	 * @param soTimeout
	 * @param connectTimeout
	 */
	public FdfsSocketFactory(InetSocketAddress socketAddress, int soTimeout,
			int connectTimeout) {
		super();
		// this.pool = pool;
		this.socketAddress = socketAddress;
		this.soTimeout = soTimeout;
		this.connectTimeout = connectTimeout;
	}

	@Override
	public PooledFdfsSocket create() throws Exception {
		try {
			PooledFdfsSocket socket = new PooledFdfsSocket(pool);
			socket.setSoTimeout(soTimeout);
			socket.connect(socketAddress, connectTimeout);
			return socket;
		} catch (IOException e) {
			throw new FdfsConnectException("can't create socket", e);
		}
	}

	@Override
	public PooledObject<PooledFdfsSocket> wrap(PooledFdfsSocket obj) {
		return new DefaultPooledObject<PooledFdfsSocket>(obj);
	}

	/**
	 * No-op.
	 * 
	 * @param obj
	 *            ignored
	 */
	@Override
	public void destroyObject(PooledObject<PooledFdfsSocket> socket)
			throws Exception {
		socket.getObject().destroy();
	}

	@Override
	public boolean validateObject(PooledObject<PooledFdfsSocket> socket) {
		if (socket.getObject().isNeedDestroy()) {
			return false;
		}
		return socket.getObject().check();
	}

	/**
	 * @param pool
	 *            the pool to set
	 */
	protected void setPool(FdfsSocketPool pool) {
		this.pool = pool;
	}

}
