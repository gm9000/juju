package com.juju.app.fastdfs.service.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.juju.app.fastdfs.service.IFdfsFileInputStreamHandler;


public class ByteArrayFdfsFileInputStreamHandler implements
		IFdfsFileInputStreamHandler<byte[]> {

	@Override
	public byte[] deal(InputStream ins) throws IOException {
		return IOUtils.toByteArray(ins);
	}

}
