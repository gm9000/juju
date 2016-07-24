package com.juju.app.fastdfs.socket;

import java.net.InetSocketAddress;

public interface IBorrowSockectErrorPolicy {
	
	FdfsSocket handleWhenErrorOccur(FdfsSocketPool pool, InetSocketAddress address, Exception ex);

}
