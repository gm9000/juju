package com.juju.app.fastdfs;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * 向tracker请求上传、下载文件或其他文件请求时，tracker返回的文件storage节点的信息
 * 
 * @author yuqih
 *
 */
public class StorageClient {

	private final InetSocketAddress inetSocketAddress;
	private final Charset charset;
	private final byte storeIndex;

	/**
	 * @param inetSocketAddress
	 * @param charset
	 * @param storeIndex
	 */
	public StorageClient(InetSocketAddress inetSocketAddress, Charset charset,
			byte storeIndex) {
		super();
		this.inetSocketAddress = inetSocketAddress;
		this.charset = charset;
		this.storeIndex = storeIndex;
	}

	/**
	 * @return the inetSocketAddress
	 */
	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}

	/**
	 * @return the charset
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * @return the storeIndex
	 */
	public byte getStoreIndex() {
		return storeIndex;
	}

}
