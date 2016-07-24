package com.juju.app.fastdfs.socket;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class PooledFdfsSocket extends FdfsSocket {
	
	private final Log logger = LogFactory.getLog(getClass());

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
				logger.warn("error occurs when invalidate socket in pool", ignore);
			}
		} else {
			try {
				pool.returnObject(this);
			} catch (Exception ignore) {
				logger.warn("error occurs when return socket to pool", ignore);
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
