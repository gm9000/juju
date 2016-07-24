package com.juju.app.fastdfs.socket;

import com.juju.app.utils.Logger;

import java.io.IOException;

import org.apache.commons.pool2.impl.GenericObjectPool;

public class PooledFdfsSocket extends FdfsSocket {

	private Logger logger = Logger.getLogger(PooledFdfsSocket.class);

	private GenericObjectPool<PooledFdfsSocket> pool;
	private boolean needDestroy;

	/**
	 * @param pool
	 */
	public PooledFdfsSocket(GenericObjectPool<PooledFdfsSocket> pool) {
		super();
		this.pool = pool;
	}

	/**
	 * 当客户端关闭连接的时候状态设置为true(空闲）
	 */
	@Override
	public synchronized void close() throws IOException {
		if (needDestroy) {
			try {
				pool.invalidateObject(this);
			} catch (Exception ignore) {
				logger.error(ignore);
			}
		} else {
			try {
				pool.returnObject(this);
			} catch (Exception ignore) {
				logger.error(ignore);
			}
		}
	}

	protected synchronized void destroy() throws IOException {
		super.close();
	}

	/**
	 * @return
	 */
	protected boolean isNeedDestroy() {
		return needDestroy;
	}

	/**
	 * @param needDestroy
	 */
	public void setNeedDestroy(boolean needDestroy) {
		this.needDestroy = needDestroy;
	}

}
