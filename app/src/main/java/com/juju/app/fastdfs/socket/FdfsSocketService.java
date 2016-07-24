package com.juju.app.fastdfs.socket;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FdfsSocketService {

	private final Log logger = LogFactory.getLog(getClass());

	protected static final int DEFAULT_CONNECT_TIMEOUT = 5 * 1000;
	protected static final int DEFAULT_NETWORK_TIMEOUT = 30 * 1000;
	
	private static final IBorrowSockectErrorPolicy Default_BorrowSockectErrorPolicy = new BorrowSockectErrorThrowPolicy();

	private int connectTimeout; // 单位：毫秒, 连接超时时间，应用与socket connect方法的参数
	private int soTimeout; // 单位：毫秒，读取超时时间,对应so_timeout
	private FdfsPoolConfig poolConfig;
	private IBorrowSockectErrorPolicy  borrowSockectErrorPolicy;

	private final Map<InetSocketAddress, FdfsSocketPool> poolMapping = new ConcurrentHashMap<InetSocketAddress, FdfsSocketPool>();

	public void init() {
		if (connectTimeout <= 0) {
			connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		}

		if (soTimeout <= 0) {
			soTimeout = DEFAULT_NETWORK_TIMEOUT;
		}
		
		if (borrowSockectErrorPolicy == null) {
			borrowSockectErrorPolicy = Default_BorrowSockectErrorPolicy; 
		}

	}

	public FdfsSocket getSocket(InetSocketAddress address) {
		if (poolConfig == null) {
			return FdfsSocket.create(address, soTimeout, connectTimeout);
		}

		FdfsSocketPool pool;
		synchronized (this) {
			pool = poolMapping.get(address);
			if (pool == null) {
				FdfsSocketFactory factory = new FdfsSocketFactory(address,
						soTimeout, connectTimeout);
				pool = new FdfsSocketPool(factory, poolConfig);
				poolMapping.put(address, pool);
			}
		}
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			return borrowSockectErrorPolicy.handleWhenErrorOccur(pool, address, e);
		}
	}

	public void destroy() {

		synchronized (this) {
			for (FdfsSocketPool pool : poolMapping.values()) {
				try {
					pool.close();
					logger.debug("pool current size :" + pool.getNumActive()  + "-" + pool.getNumIdle());
				} catch (Exception e) {
					logger.warn("destory pool error", e);
				}
			}
			poolMapping.clear();
		}
	}

	/**
	 * @param connectTimeout
	 *            the connectTimeout to set
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout * 1000;
	}

	/**
	 * @param soTimeout
	 *            the soTimeout to set
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout * 1000;
	}

	/**
	 * @param poolConfig
	 *            the poolConfig to set
	 */
	public void setPoolConfig(FdfsPoolConfig poolConfig) {
		this.poolConfig = poolConfig;
	}

	/**
	 * @param borrowSockectErrorPolicy the borrowSockectErrorPolicy to set
	 */
	public void setBorrowSockectErrorPolicy(IBorrowSockectErrorPolicy borrowSockectErrorPolicy) {
		this.borrowSockectErrorPolicy = borrowSockectErrorPolicy;
	}

}
