package com.juju.app.service.im.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.enums.ConnectionState;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.service.im.XMPPServiceCallback;
import com.juju.app.service.im.data.MessageProvider;



import com.juju.app.service.im.data.MessageProvider.ChatConstants;
import com.juju.app.service.im.tls.TLSMode;
import com.juju.app.ui.base.BaseApplication;


import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.PingFailedListener;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;


/**
 * 项目名称：juju
 * 类描述：XMPP协议消息服务通信实现类
 * 创建人：gm
 * 日期：2016/3/22 10:04
 * 版本：V1.0.0
 */
public class XMPPServiceImpl implements
        SocketService, StanzaListener, ConnectionListener, PingFailedListener {

    private final String TAG = getClass().getName();

    // 客户端名称和类型。主要是向服务器登记，有点类似QQ显示iphone或者Android手机在线的功能
    public static final String XMPP_IDENTITY_NAME = "XMPP";// 客户端名称
    public static final String XMPP_IDENTITY_TYPE = "phone";// 客户端类型
    private static final int PACKET_TIMEOUT = 30000;// 超时时间



    private XMPPServiceCallback mServiceCallBack;

    static {
        registerSmackProviders();
    }

    //基础信息配置
    static void registerSmackProviders() {

    }

    static File capsCacheDir = null; ///< this is used to cache if we already initialized EntityCapsCache

    private ConnectionState mRequestedState = ConnectionState.OFFLINE;
    private ConnectionState mState = ConnectionState.OFFLINE;
    private String mLastError;


    private AlarmManager mAlarmManager;
    private String mPingID;
    private long mPingTimestamp;

    private static final String PING_ALARM = "com.juju.app.PING_ALARM";
    private static final String PONG_TIMEOUT_ALARM = "com.juju.app.PONG_TIMEOUT_ALARM";
    private Intent mPingAlarmIntent = new Intent(PING_ALARM);
    private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);

    private PendingIntent mPingAlarmPendIntent;
    private PendingIntent mPongTimeoutAlarmPendIntent;

    private final ContentResolver mContentResolver;
    private Service mService;



    private final String serverName;
    private final String token;
    private final String resource;
    private final boolean saslEnabled;
    private final TLSMode tlsMode;
    private boolean started;
    XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();

    private AbstractXMPPConnection xmppConnection;
    private final AcceptAll ACCEPT_ALL = new AcceptAll();

    private MultiUserChat multiUserChat;




    public XMPPServiceImpl(ContentResolver contentResolver, Service service) {
        this.mContentResolver = contentResolver;
        this.mService = service;

        //初始化配置信息
        serverName = BaseApplication.getInstance().getUserInfoBean().getmServiceName();
        token = "";
        resource = "XMPP";
        saslEnabled = false;
        tlsMode = TLSMode.legacy;

    }

    private void createConnection(boolean useSRVLookup)
            throws XMPPException, IOException, SmackException {
        if (useSRVLookup) {
            builder.setServiceName(serverName);
        } else {
            String host = BaseApplication.getInstance().getUserInfoBean().getmHost();
            int port = BaseApplication.getInstance().getUserInfoBean().getmPort();
            builder.setHost(host);
            builder.setPort(port);
            builder.setServiceName(serverName);
        }
        onReady(builder);
    }

    private void onReady(XMPPTCPConnectionConfiguration.Builder builder)
            throws IOException, XMPPException, SmackException {
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        // 不加这行会报错，因为没有证书
        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
//        builder.setCompressionEnabled(compression);
//        builder.setSendPresence(false);
//        builder.setUsernameAndPassword(userInfoBean.getmAccount(), userInfoBean.getmPassword());
        try {
            TLSUtils.acceptAllCertificates(builder);
            TLSUtils.disableHostnameVerificationForTlsCertificicates(builder);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        xmppConnection = new XMPPTCPConnection(builder.build());
        xmppConnection.addAsyncStanzaListener(this, ACCEPT_ALL);
        xmppConnection.addConnectionListener(this);

        // by default Smack disconnects in case of parsing errors
        xmppConnection.setParsingExceptionCallback(new ExceptionLoggingCallback());

        final Roster roster = Roster.getInstanceFor(xmppConnection);
//        roster.addRosterListener(rosterListener);
//        roster.addRosterLoadedListener(rosterListener);
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        org.jivesoftware.smackx.ping.PingManager.getInstanceFor(xmppConnection).
                registerPingFailedListener(this);

    }



    /**
     * 用户登陆消息服务器
     *
     * @return
     */
    @Override
    public boolean login() throws IOException, XMPPException, SmackException {
        createConnection(false);
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        String userName = userInfoBean.getmAccount();
        String password = userInfoBean.getmPassword();
        String serviceName = userInfoBean.getmServiceName();
        xmppConnection.connect();
        xmppConnection.login(userName, password, serviceName);
        boolean isOk = xmppConnection.isAuthenticated();
        return isOk;
    }

    /**
     * 注销登陆
     *
     * @return 是否成功
     */
    @Override
    public boolean logout() {

        return false;
    }

    /**
     * 是否已经连接上服务器
     *
     * @return
     */
    @Override
    public boolean isAuthenticated() {
        if (xmppConnection != null) {
            return (xmppConnection.isConnected() && xmppConnection
                    .isAuthenticated());
        }
        return false;
    }

    /**
     * 向服务器发送心跳包，保持长连接 通过一个闹钟控制，定时发送，
     */
    @Override
    public void sendServerPing() {

    }

    /**
     * 发送消息
     *
     * @param user
     * @param message
     */
    @Override
    public void sendMessage(String user, String message)
            throws SmackException.NotConnectedException {
        final Message newMessage = new Message(user, Message.Type.groupchat);
        newMessage.setBody(message);

        if (isAuthenticated()) {
//            addChatMessageToDB(ChatConstants.OUTGOING, user, message, ChatConstants.DS_SENT_OR_READ,
//                    System.currentTimeMillis(), newMessage.getPacketID());
            xmppConnection.sendPacket(newMessage);
        } else {
            // send offline -> store to DB
//            addChatMessageToDB(ChatConstants.OUTGOING, user, message, ChatConstants.DS_NEW,
//                    System.currentTimeMillis(), newMessage.getPacketID());
        }
    }

    /**
     * 注册所有监听
     */
    @Override
    public void registerAllListener() {

    }

    /**
     * @param newPassword 新密码
     * @return
     */
    @Override
    public String changePassword(String newPassword) {
        return null;
    }

    /**
     * 注册回调方法
     *
     * @param callBack
     */
    @Override
    public void registerCallback(XMPPServiceCallback callBack) {

    }

    /**
     * 注销回调方法
     */
    @Override
    public void unRegisterCallback() {

    }

    /**
     * 加入聊天室
     */
    @Override
    public void joinChatRoom() throws JUJUXMPPException, XMPPException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(xmppConnection);
        String jid = BaseApplication.getInstance().getUserInfoBean().getmRoomName()+
                "@"+BaseApplication.getInstance().getUserInfoBean().getmMucServiceName()+".juju";
        Log.d(TAG, "chatRoom:"+jid);
        multiUserChat = multiUserChatManager.getMultiUserChat(jid);
        multiUserChat.join(BaseApplication.getInstance().getUserInfoBean().getUserName());
//        multiUserChat.sendMessage("哈哈");
    }



//    // called at the end of a state transition
//    private synchronized void updateConnectionState(ConnectionState new_state) {
//        if (new_state == ConnectionState.ONLINE || new_state == ConnectionState.CONNECTING)
//            mLastError = null;
//        Log.d(TAG, "updateConnectionState: " + mState + " -> " + new_state + " (" + mLastError + ")");
//        if (new_state == mState)
//            return;
//        mState = new_state;
//        if (mServiceCallBack != null)
//            mServiceCallBack.connectionStateChanged();
//    }


    private void registerMessageListener() {
        // do not register multiple packet listeners
//        if (mPacketListener != null)
//            mXMPPConnection.removePacketListener(mPacketListener);
//
//        PacketTypeFilter filter = new PacketTypeFilter(Message.class);
//
//        mPacketListener = new PacketListener() {
//            public void processPacket(Packet packet) {
//                try {
//                    if (packet instanceof Message) {
//                        Message msg = (Message) packet;
//                        String fromJID = getBareJID(msg.getFrom());
//                        int direction = ChatConstants.INCOMING;
//                        Carbon cc = CarbonManager.getCarbon(msg);
//                        // extract timestamp
//                        long ts;
//                        DelayInfo timestamp = (DelayInfo)msg.getExtension("delay", "urn:xmpp:delay");
//                        if (timestamp == null)
//                            timestamp = (DelayInfo)msg.getExtension("x", "jabber:x:delay");
//                        if (cc != null) // Carbon timestamp overrides packet timestamp
//                            timestamp = cc.getForwarded().getDelayInfo();
//                        if (timestamp != null)
//                            ts = timestamp.getStamp().getTime();
//                        else
//                            ts = System.currentTimeMillis();
//
//                        // try to extract a carbon
//                        if (cc != null) {
//                            Log.d(TAG, "carbon: " + cc.toXML());
//                            msg = (Message)cc.getForwarded().getForwardedPacket();
//
//                            // outgoing carbon: fromJID is actually chat peer's JID
//                            if (cc.getDirection() == Carbon.Direction.sent) {
//                                fromJID = getBareJID(msg.getTo());
//                                direction = ChatConstants.OUTGOING;
//                            } else {
//                                fromJID = getBareJID(msg.getFrom());
//
//                                // hook off carbonated delivery receipts
//                                DeliveryReceipt dr = (DeliveryReceipt)msg.getExtension(
//                                        DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE);
//                                if (dr != null) {
//                                    Log.d(TAG, "got CC'ed delivery receipt for " + dr.getId());
//                                    changeMessageDeliveryStatus(dr.getId(), ChatConstants.DS_ACKED);
//                                }
//                            }
//                        }
//
//                        String chatMessage = msg.getBody();
//
//                        // display error inline
//                        if (msg.getType() == Message.Type.error) {
//                            if (changeMessageDeliveryStatus(msg.getPacketID(), ChatConstants.DS_FAILED))
//                                mServiceCallBack.messageError(fromJID, msg.getError().toString(), (cc != null));
//                            return; // we do not want to add errors as "incoming messages"
//                        }
//
//                        // ignore empty messages
//                        if (chatMessage == null) {
//                            Log.d(TAG, "empty message.");
//                            return;
//                        }
//                        // carbons are old. all others are new
//                        int is_new = (cc == null) ? ChatConstants.DS_NEW : ChatConstants.DS_SENT_OR_READ;
//                        if (msg.getType() == Message.Type.error)
//                            is_new = ChatConstants.DS_FAILED;
//
//                        addChatMessageToDB(direction, fromJID, chatMessage, is_new, ts, msg.getPacketID());
//                        if (direction == ChatConstants.INCOMING)
//                            mServiceCallBack.newMessage(fromJID, chatMessage, (cc != null));
//                    }
//                } catch (Exception e) {
//                    // SMACK silently discards exceptions dropped from processPacket :(
//                    Log.e(TAG, "failed to process packet:");
//                    e.printStackTrace();
//                }
//            }
//        };
//        mXMPPConnection.addPacketListener(mPacketListener, filter);
    }

    private void registerPresenceListener() {
        // do not register multiple packet listeners
//        if (mPresenceListener != null)
//            mXMPPConnection.removePacketListener(mPresenceListener);
//
//        mPresenceListener = new PacketListener() {
//            public void processPacket(Packet packet) {
//                try {
//                    Presence p = (Presence) packet;
//                    switch (p.getType()) {
//                        case subscribe:
//                            handleIncomingSubscribe(p);
//                            break;
//                        case unsubscribe:
//                            break;
//                    }
//                } catch (Exception e) {
//                    // SMACK silently discards exceptions dropped from processPacket :(
//                    Log.e(TAG, "failed to process presence:");
//                    e.printStackTrace();
//                }
//            }
//        };
//
//        mXMPPConnection.addPacketListener(mPresenceListener, new PacketTypeFilter(Presence.class));
    }

    private void registerPongListener() {
        // reset ping expectation on new connection
        mPingID = null;

//        if (mPongListener != null)
//            mXMPPConnection.removePacketListener(mPongListener);
//
//        mPongListener = new PacketListener() {
//
//            @Override
//            public void processPacket(Packet packet) {
//                if (packet == null) return;
//
//                gotServerPong(packet.getPacketID());
//            }
//
//        };
//
//        mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(IQ.class));
//        mPingAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPingAlarmIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
//                System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mPingAlarmPendIntent);
    }

    private String getBareJID(String from) {
        String[] res = from.split("/");
        return res[0].toLowerCase();
    }

    public boolean changeMessageDeliveryStatus(String packetID, int new_status) {
        ContentValues cv = new ContentValues();
        cv.put(ChatConstants.DELIVERY_STATUS, new_status);
        Uri rowuri = Uri.parse("content://" + MessageProvider.AUTHORITY + "/"
                + MessageProvider.TABLE_NAME);
        return mContentResolver.update(rowuri, cv,
                ChatConstants.PACKET_ID + " = ? AND " +
                        ChatConstants.DELIVERY_STATUS + " != " + ChatConstants.DS_ACKED + " AND " +
                        ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
                new String[] { packetID }) > 0;
    }

    private void addChatMessageToDB(int direction, String JID,
                                    String message, int delivery_status, long ts, String packetID) {
        ContentValues values = new ContentValues();

        values.put(ChatConstants.DIRECTION, direction);
        values.put(ChatConstants.JID, JID);
        values.put(ChatConstants.MESSAGE, message);
        values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
        values.put(ChatConstants.DATE, ts);
        values.put(ChatConstants.PACKET_ID, packetID);

        mContentResolver.insert(MessageProvider.CONTENT_URI, values);
    }




    /**
     *******************************************接口实现方法******************************************
     */
    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        if(packet instanceof Message) {
            Message message = (Message) packet;
            Log.d(TAG, "APP接收消息"+message.getBody());
        }
    }

    /**
     * Notification that the connection has been successfully connected to the remote endpoint (e.g. the XMPP server).
     * <p>
     * Note that the connection is likely not yet authenticated and therefore only limited operations like registering
     * an account may be possible.
     * </p>
     *
     * @param connection the XMPPConnection which successfully connected to its endpoint.
     */
    @Override
    public void connected(XMPPConnection connection) {
        Log.d(TAG, "connected()：连接成功");
    }

    /**
     * Notification that the connection has been authenticated.
     *
     * @param connection the XMPPConnection which successfully authenticated.
     * @param resumed    true if a previous XMPP session's stream was resumed.
     */
    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        Log.d(TAG, "authenticated()：校验成功");
    }

    /**
     * Notification that the connection was closed normally.
     */
    @Override
    public void connectionClosed() {
        Log.d(TAG, "connectionClosed()：连接断开");
    }

    /**
     * Notification that the connection was closed due to an exception. When
     * abruptly disconnected it is possible for the connection to try reconnecting
     * to the server.
     *
     * @param e the exception.
     */
    @Override
    public void connectionClosedOnError(Exception e) {
        Log.e(TAG, "connectionClosedOnError()：连接断开异常", e);

        Log.d(TAG, "connectionClosedOnError()：连接断开异常");

    }

    /**
     * The connection has reconnected successfully to the server. Connections will
     * reconnect to the server when the previous socket connection was abruptly closed.
     */
    @Override
    public void reconnectionSuccessful() {
        Log.d(TAG, "reconnectionSuccessful()：重连成功");
    }

    /**
     * The connection will retry to reconnect in the specified number of seconds.
     * <p>
     * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()} returns true, i.e.
     * only when the reconnection manager is enabled for the connection.
     * </p>
     *
     * @param seconds remaining seconds before attempting a reconnection.
     */
    @Override
    public void reconnectingIn(int seconds) {
    }

    /**
     * An attempt to connect to the server has failed. The connection will keep trying reconnecting to the server in a
     * moment.
     * <p>
     * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()} returns true, i.e.
     * only when the reconnection manager is enabled for the connection.
     * </p>
     *
     * @param e the exception that caused the reconnection to fail.
     */
    @Override
    public void reconnectionFailed(Exception e) {
        Log.d(TAG, "reconnectionFailed()：重连失败");

    }

    /**
     * Called when the server ping fails.
     */
    @Override
    public void pingFailed() {
        Log.d(TAG, "pingFailed()：ping失败");

    }


    /**
     *******************************************内部类******************************************
     */
    //消息过滤器
    static class AcceptAll implements StanzaFilter {
        @Override
        public boolean accept(Stanza packet) {
            return true;
        }
    }

}
