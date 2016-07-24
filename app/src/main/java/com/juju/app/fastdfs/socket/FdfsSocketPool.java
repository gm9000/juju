package com.juju.app.fastdfs.socket;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

class FdfsSocketPool extends GenericObjectPool<PooledFdfsSocket> {

	/**
	 * @param factory
	 * @param config
	 */
	public FdfsSocketPool(FdfsSocketFactory factory, GenericObjectPoolConfig config) {
		super(factory, config);
		factory.setPool(this);
	}

}
