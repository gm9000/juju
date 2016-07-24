package com.juju.app.fastdfs.socket;

import com.juju.app.fastdfs.callback.ProgressCallback;

import java.io.IOException;
import java.io.InputStream;


public class FdfsInputStream extends InputStream {

	private static final long loadingUpdateMaxTimeSpan = 300; // 300ms

	private final InputStream ins;
	private final long size;
	private long remainByteSize;
	private ProgressCallback callback;
	private String uuid;
	long lastUpdateTime = System.currentTimeMillis();


	public FdfsInputStream(InputStream ins, long size) {
		this.ins = ins;
		this.size = size;
		remainByteSize = size;
	}
	
	public FdfsInputStream(InputStream ins, long size, String uuid, ProgressCallback callback) {
		this.ins = ins;
		this.size = size;
		remainByteSize = size;
		this.callback = callback;
		this.uuid = uuid;
		lastUpdateTime = System.currentTimeMillis();
	}

	@Override
	public int read() throws IOException {
		return ins.read();
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		if (remainByteSize == 0) {
			return -1;
		}
		int byteSize = ins.read(b, off, len);
		if (remainByteSize < byteSize) {
			throw new IOException("协议长度" + size + "与实际长度不符");
		}
		remainByteSize -= byteSize;
		
		if(callback != null) {
			long currTime = System.currentTimeMillis();
	           if (currTime - lastUpdateTime >= loadingUpdateMaxTimeSpan) {
	               lastUpdateTime = currTime;
	               callback.updateProgress(uuid, size, size-remainByteSize);
	           }
			 if(remainByteSize == 0) {
				 callback.complete(uuid, this);
			 }
		}
		return byteSize;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	/**
	 * 是否已完成读取
	 * 
	 * @return
	 */
	public boolean isReadCompleted() {
		return remainByteSize == 0;
	}

}
