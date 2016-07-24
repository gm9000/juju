package com.juju.app.fastdfs.service.impl;

import com.juju.app.fastdfs.StorageClient;
import com.juju.app.fastdfs.exception.FdfsConnectException;
import com.juju.app.fastdfs.exception.FdfsUnavailableException;
import com.juju.app.fastdfs.file.handler.ICmdProtoHandler;
import com.juju.app.fastdfs.file.handler.TrackerGetFetchStorageHandler;
import com.juju.app.fastdfs.file.handler.TrackerGetStoreStorageHandler;
import com.juju.app.fastdfs.service.ITrackerClientService;
import com.juju.app.fastdfs.socket.FdfsSocket;
import com.juju.app.fastdfs.socket.FdfsSocketService;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.GlobalVariable;
import com.juju.app.utils.StringUtils;
import org.apache.commons.io.IOUtils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * 项目名称：juju
 * 类描述：Tracker模块业务实现
 * 创建人：gm
 * 日期：2016/7/22 09:53
 * 版本：V1.0.0
 */
public class TrackerClientService implements ITrackerClientService {

    private static final String DEFAULT_CHARSET_NAME = "ISO8859-1";

    private String charsetName = DEFAULT_CHARSET_NAME;
    private String[] trackerServerValues;
    private FdfsSocketService fdfsSocketService;

    private Object lock = new Object();
    private Charset charset;
    private final CircularList<TrackerAddressHolder> trackerAddresses =
            new CircularList<TrackerAddressHolder>();
    private int availableCount;



    private volatile static TrackerClientService inst;


    public static ITrackerClientService instance() {
        if(inst == null) {
            synchronized (TrackerClientService.class) {
                if (inst == null) {
                    inst = new TrackerClientService();
                    inst.setTrackerServerValues(new String[]{GlobalVariable.TRACKER_SERVER1});
                    inst.setFdfsSocketService(FdfsSocketService.instance());
                    inst.init();
                }
            }
        }
        return inst;
    }



    public void init() {

        charset = Charset.forName(charsetName);

        String[] parts;

        Set<InetSocketAddress> trackerAddressSet = new HashSet<InetSocketAddress>();
        for (String trackerServersValue : trackerServerValues) {
            if (StringUtils.isBlank(trackerServersValue)) {
                continue;
            }
            parts = StringUtils.split(trackerServersValue, ":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "the value of item \"tracker_server\" is invalid, the correct format is host:port");
            }
            InetSocketAddress address = new InetSocketAddress(parts[0].trim(),
                    Integer.parseInt(parts[1].trim()));
            trackerAddressSet.add(address);
        }
        availableCount = trackerAddressSet.size();
        if (availableCount == 0) {
            throw new IllegalArgumentException(
                    "item \"tracker_server\"  not found");
        }

        for (InetSocketAddress address : trackerAddressSet) {
            trackerAddresses.add(new TrackerAddressHolder(address));
        }

    }

    /**
     * 一个ip取不到就取下一个ip的连接，直到所有的ip都取过一遍还没取到报异常
     *
     * @return
     */
    private FdfsSocket getTrackerSocket() {

        InetSocketAddress trackerAddresse;
        FdfsSocket socket = null;
        TrackerAddressHolder holder;
        for (int i = 0; i < trackerAddresses.size(); i++) {
            holder = trackerAddresses.next();
            // list中不是所有的的都被标记成不可用并且当前被标记成不可用时间小于10分钟的情况下，直接跳到下一个
            if (availableCount != 0
                    && !holder.available
                    && (System.currentTimeMillis() - holder.lastUnavailableTime) < 10 * 60 * 1000) {
                continue;
            }

            trackerAddresse = holder.address;
            try {
                socket = fdfsSocketService.getSocket(trackerAddresse);
                holder.setState(true);
            } catch (FdfsConnectException e) {
                holder.setState(false);
                holder.lastUnavailableTime = System.currentTimeMillis();
            } catch (Exception ignore) {

            }
            if (socket != null) {
                return socket;
            }
        }

        throw new FdfsUnavailableException("找不到可用的tracker");

    }

    private <T> T process(Socket socket, ICmdProtoHandler<T> handler) {
        try {
            return handler.handle();
        } finally {
            IOUtils.closeQuietly(socket);
        }
    }



    @Override
    public StorageClient getStoreStorage(String groupName) {
        FdfsSocket socket = getTrackerSocket();
        ICmdProtoHandler<StorageClient> handler = new TrackerGetStoreStorageHandler(
                socket, groupName, charset);
        return process(socket, handler);
    }

    @Override
    public StorageClient getFetchStorage(String groupName, String path) {
        FdfsSocket socket = getTrackerSocket();
        ICmdProtoHandler<StorageClient> handler = new TrackerGetFetchStorageHandler(
                socket, false, groupName, path, charset);
        return process(socket, handler);

    }


    /**
     * @param charsetName
     *            the charsetName to set
     */
    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    /**
     * @param trackerServerValues
     *            the trackerServerValues to set
     */
    public void setTrackerServerValues(String[] trackerServerValues) {
        this.trackerServerValues = trackerServerValues;
    }

    /**
     * @param fdfsSocketService the fdfsSocketService to set
     */
    public void setFdfsSocketService(FdfsSocketService fdfsSocketService) {
        this.fdfsSocketService = fdfsSocketService;
    }

    private class TrackerAddressHolder {

        private InetSocketAddress address;
        private boolean available;
        private long lastUnavailableTime;

        /**
         * @param address
         */
        private TrackerAddressHolder(InetSocketAddress address) {
            super();
            this.address = address;
            this.available = true;
        }

        private void setState(boolean available) {

            synchronized (lock) {
                if (this.available != available) {
                    this.available = available;
                    if (available) {
                        TrackerClientService.this.availableCount++;
                    } else {
                        TrackerClientService.this.availableCount--;
                    }
                }
            }

        }
    }

}
