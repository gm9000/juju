package com.juju.app.service.im.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.biz.DaoSupport;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.PeerEntity;
import com.juju.app.entity.chat.SessionEntity;
import com.juju.app.entity.chat.TextMessage;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.enums.ConnectionState;
import com.juju.app.event.ChatMessageEvent;
import com.juju.app.event.LoginEvent;
import com.juju.app.event.NotifyMessageEvent;
import com.juju.app.event.PriorityEvent;
import com.juju.app.event.SmackSocketEvent;
import com.juju.app.exceptions.JUJUXMPPException;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.media.rtmp.packets.RtmpHeader;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.callback.FixListenerQueue;
import com.juju.app.service.im.callback.ListenerQueue;
import com.juju.app.service.im.callback.XMPPServiceCallbackImpl;


import com.juju.app.service.im.iq.RedisResIQ;
import com.juju.app.service.im.manager.IMSessionManager;
import com.juju.app.service.im.manager.IMUnreadMsgManager;
import com.juju.app.service.im.provider.RedisPacketExtensionProvider;
import com.juju.app.service.im.tls.TLSMode;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ThreadPoolUtil;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.parsing.ExceptionLoggingCallback;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilderFactory;


/**
 * 项目名称：juju
 * 类描述：XMPP协议消息服务通信实现类
 * 创建人：gm
 * 日期：2016/3/22 10:04
 * 版本：V1.0.0
 */
public class XMPPServiceImpl implements
        SocketService, StanzaListener, ConnectionListener, PingFailedListener {

    private Logger logger = Logger.getLogger(XMPPServiceImpl.class);

    private Service mService;
    private final String serverName;
    XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();

    private AbstractXMPPConnection xmppConnection;
    private final AcceptAll ACCEPT_ALL = new AcceptAll();

    private UserInfoBean userInfoBean = null;
    private DaoSupport messageDao;

    private IMSessionManager sessionManager = IMSessionManager.instance();

    //callback 队列
    private ListenerQueue listenerQueue = ListenerQueue.instance();

    private FixListenerQueue fixListenerQueue = FixListenerQueue.instance();

    public XMPPServiceImpl(ContentResolver contentResolver, Service service,DaoSupport messageDao) {
//        this.mContentResolver = contentResolver;
        this.mService = service;
        //初始化配置信息
        //        token = "";
//        resource = "XMPP";
//        saslEnabled = false;
//        tlsMode = TLSMode.legacy;
        serverName = BaseApplication.getInstance().getUserInfoBean().getmServiceName();
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        this.messageDao = messageDao;
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
        builder.setSendPresence(true);
        onReady(builder);
    }

    private void onReady(XMPPTCPConnectionConfiguration.Builder builder)
            throws IOException, XMPPException, SmackException {
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        // 不加这行会报错，因为没有证书
        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        if(Constants.IS_APP_MODEL) {
            builder.setDebuggerEnabled(true);
        }
        builder.setCompressionEnabled(false);
        builder.setSendPresence(true);

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
        xmppConnection.setFromMode(XMPPConnection.FromMode.UNCHANGED);


        final Roster roster = Roster.getInstanceFor(xmppConnection);
//        roster.addRosterListener(rosterListener);
//        roster.addRosterLoadedListener(rosterListener);
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        org.jivesoftware.smackx.ping.PingManager.getInstanceFor(xmppConnection).
                registerPingFailedListener(this);

        //监听消息回复
        ProviderManager.addExtensionProvider(ReplayMessageTime.NAME,
                ReplayMessageTime.NAME_SPACE, new ReplayMessageTimeProvider());

        //监听redis相关回复
        ProviderManager.addIQProvider(RedisPacketExtensionProvider.NAME,
                RedisPacketExtensionProvider.NAMESPACE,
                new RedisPacketExtensionProvider());


        MsgTypeProvider msgTypeProvider = new MsgTypeProvider();
        ProviderManager.addExtensionProvider(ElementNameType.msgType.name(),
                IMBaseDefine.NameSpaceType.MESSAGE.value(), msgTypeProvider);

        NotifyTypeProvider notifyTypeProvider = new NotifyTypeProvider();
        ProviderManager.addExtensionProvider(ElementNameType.notifyType.name(),
                IMBaseDefine.NameSpaceType.NOTIFY.value(), notifyTypeProvider);


        doReconnection();
    }



    /**
     * 用户登陆消息服务器
     *
     * @return
     */
    @Override
    public boolean login() throws IOException, XMPPException, SmackException {
        //TODO 与消息服务连接是否可以提到登陆之前 ？
        if(xmppConnection == null) {
            createConnection(false);
            xmppConnection.connect();
        }
        UserInfoBean userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        String userName = userInfoBean.getmAccount();
        String password = userInfoBean.getmPassword();
        String serviceName = userInfoBean.getmServiceName();
        xmppConnection.login(userName, password, serviceName);

        boolean isOk = xmppConnection.isAuthenticated();
        if(isOk)
            sendPing();

        logger.d("login -> isOk:%b", isOk);
        logger.d("login -> XMPPServiceImpl:%s", this.toString());
        return isOk;
    }

    /**
     * 注销登陆
     *
     * @return 是否成功
     */
    @Override
    public boolean logout() {
        if(xmppConnection != null && xmppConnection.isConnected()) {
            xmppConnection.disconnect();
            return true;
        }
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
     * @param to
     * @param message
     * @param uuid
     * @param callback
     */
    @Override
    public void sendMessage(String to, String message, String uuid,
                            XMPPServiceCallbackImpl callback) {
        Message newMessage = new Message(to, Message.Type.groupchat);
        newMessage.setStanzaId(uuid);
        newMessage.setBody(message);
        if (isAuthenticated()) {
            if(callback.getType() == 0) {
                sendStanzaAndBindListener(newMessage, uuid, callback);
            } else {
                sendStanzaAndBindFixListener(newMessage, uuid, callback);
            }
        }
    }

    /**
     * 计算消息总数
     *
     * @param to
     * @param minTime
     * @param maxTime
     * @param uuid
     * @param listener
     * @return
     */
    @Override
    public void countMessage(final String to, final String minTime, final String maxTime,
                             String uuid, XMPPServiceCallbackImpl listener) {
        IQ iq = new IQ("request", "com:jlm:iq:redis") {
            @Override
            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
                    IQChildElementXmlStringBuilder xml) {
                xml.attribute("key", "2_"+to);
                xml.attribute("command", "zcount");
                xml.attribute("min", minTime);
                if(StringUtils.isNotBlank(maxTime)) {
                    xml.attribute("max", maxTime);
                }
                xml.append(">");
                return xml;
            }
        };
        iq.setStanzaId(uuid);
        if (isAuthenticated()) {
            if(listener.getType() == 0) {
                sendStanzaAndBindListener(iq, uuid, listener);
            } else {
                sendStanzaAndBindFixListener(iq, uuid, listener);
            }
        }
    }

    /**
     * 计算消息总数
     *
     * @param to
     * @param minTime
     * @param maxTime
     * @param uuid
     * @param offset
     * @param length
     * @param listener
     * @return
     */
    @Override
    public void findHisMessages(final String command, final String to, final String minTime, final String maxTime,
                                final String uuid,  final int offset,
                                final int length,  XMPPServiceCallbackImpl listener) {
        IQ iq = new IQ("request", "com:jlm:iq:redis") {
            @Override
            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
                    IQChildElementXmlStringBuilder xml) {
                xml.attribute("key", "2_"+to);
//                xml.attribute("command", "zrangebyscore");
                xml.attribute("command", command);
                xml.attribute("min", minTime);
                if(StringUtils.isNotBlank(maxTime)) {
                    xml.attribute("max", maxTime);
                }
                xml.attribute("offset", offset);
                xml.attribute("length", length);
                xml.append(">");
                return xml;
            }
        };
        iq.setStanzaId(uuid);
        if(listener.getType() == 0) {
            sendStanzaAndBindListener(iq, uuid, listener);
        } else {
            sendStanzaAndBindFixListener(iq, uuid, listener);
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

//    /**
//     * 注册回调方法
//     *
//     * @param callBack
//     */
//    @Override
//    public void registerCallback(XMPPServiceCallback callBack) {
//        this.mServiceCallBack = callBack;
//    }
//
//    /**
//     * 注销回调方法
//     */
//    @Override
//    public void unRegisterCallback() {
//        this.mServiceCallBack = null;
//    }

    /**
     * 加入聊天室
     */
    @Override
    public void joinChatRoom(String chatRoom, long lastUpdateTime) throws JUJUXMPPException, XMPPException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        logger.d("joinChatRoom -> xmppConnection:%s", xmppConnection.toString());
        logger.d("login -> XMPPServiceImpl:%s", this.toString());
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(xmppConnection);
        logger.d("joinChatRoom -> chatRoom:%s", chatRoom);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(chatRoom);
        //1：群组存在 2：还没有加入
        if(multiUserChat != null
                && !multiUserChat.isJoined()) {
            logger.d("joinChatRoom#chatRoom will be join " +
                    "-> chatRoom:%s", chatRoom);
            String nickName = userInfoBean.getmAccount();
            String password = userInfoBean.getmPassword();
            DiscussionHistory history = new DiscussionHistory();
            history.setSince(new Date());
            history.setMaxStanzas(0);
            try {
                multiUserChat.join(nickName, password);
            } catch (SmackException e) {
                e.printStackTrace();
            }
            logger.d("joinChatRoom#chatRoom is joined OK " +
                    "-> chatRoom:%s", chatRoom);
        } else {
            //设置在线状态
            multiUserChat.changeAvailabilityStatus("", Presence.Mode.available);
            logger.d("joinChatRoom#chatRoom is joined or chatRoom " +
                    "is not exist -> chatRoom:%s", chatRoom);
        }
    }

    @Override
    public void leaveChatRoom(String peerId) throws SmackException.NotConnectedException {
        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(xmppConnection);
        logger.d("leaveChatRoom -> chatRoomId:%s", peerId);
        MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(peerId);
        if(multiUserChat != null) {
            multiUserChat.leave();
        }
    }

    @Override
    public boolean createChatRoom(String groupId, String groupName, String groupDesc,
                                  String mucServiceName, String serviceName) {
        boolean bool = false;
        //可以考虑用注解实现权限检查
        if (isAuthenticated()) {
            if(StringUtils.isBlank(mucServiceName))
                mucServiceName = "conference";
            try{
                MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(xmppConnection);
                String jid = groupId+"@"+mucServiceName+"."+serviceName;
                MultiUserChat muc = multiUserChatManager.getMultiUserChat(jid);
                muc.create(xmppConnection.getUser());
                // 获得聊天室的配置表单
                Form form = muc.getConfigurationForm();
                // 根据原始表单创建一个要提交的新表单。
                Form submitForm = form.createAnswerForm();
                // 向要提交的表单添加默认答复
                List<FormField> fileds = form.getFields();
                for(FormField f : fileds) {
                    if (!FormField.Type.hidden.equals(f.getType()) && f.getVariable() != null) {
                        if("muc#roomconfig_persistentroom".equals(f.getVariable())) {
                            //是否持久化房间
                            submitForm.setAnswer("muc#roomconfig_persistentroom", true);
                        } else if ("muc#roomconfig_publicroom".equals(f.getVariable())) {
                            //是否列出目录中的房间
                            submitForm.setAnswer("muc#roomconfig_publicroom", false);
                        } else if ("muc#roomconfig_roomname".equals(f.getVariable()))  {
                            //设置房间名称
                            submitForm.setAnswer("muc#roomconfig_roomname", groupName);
                        } else if ("muc#roomconfig_roomdesc".equals(f.getVariable())) {
                            submitForm.setAnswer("muc#roomconfig_roomdesc", groupDesc);
                        } else {
                            submitForm.setDefaultAnswer(f.getVariable());
                        }
                    }
                }
                muc.sendConfigurationForm(submitForm);
                bool = muc.isJoined();
            }  catch (SmackException.NoResponseException e) {
                logger.error(e);
            } catch (XMPPException.XMPPErrorException e) {
                logger.error(e);
            } catch (SmackException.NotConnectedException e) {
                logger.error(e);
            } catch (SmackException e) {
                logger.error(e);
            }
        }
        return bool;
    }

    @Override
    public void reConnect() {
        ThreadPoolUtil.instance().executeImTask(new Runnable() {
            @Override
            public void run() {
                if(xmppConnection != null
                        && !xmppConnection.isConnected()) {
                    try {
                        xmppConnection.connect();
                    } catch (SmackException e) {
                        logger.error(e);
                    } catch (IOException e) {
                        logger.error(e);
                    } catch (XMPPException e) {
                        logger.error(e);
                    }
                }

                //加入聊天室
                Map<String, GroupEntity> groupEntityMap = ((IMService)mService)
                        .getGroupManager().getGroupMap();
                if(groupEntityMap != null && groupEntityMap.size() >0) {
                    ((IMService)mService).getGroupManager().joinChatRooms(groupEntityMap.values());
                }
            }
        });
    }

    @Override
    public void notifyMessage(String peerId, String message, Message.Type type,
                              IMBaseDefine.NotifyType notifyType, String uuid, boolean isSaveMsg,
                              XMPPServiceCallbackImpl callback, Object... reqEntity) {
        if (isAuthenticated()) {
            Message newMessage = new Message(peerId, type);
            //需要调整
            newMessage.setFrom(userInfoBean.getJujuNo()+"@juju");
            newMessage.setStanzaId(uuid);
            newMessage.setBody(message);
            ExtensionElement extensionElement = new NotifyExtensionElement(notifyType,
                    IMBaseDefine.NameSpaceType.NOTIFY);
            newMessage.addExtension(extensionElement);
            if(isSaveMsg) {
                //保存到本地数据库(可以考虑拦截方式)
                ((IMService)mService).getOtherManager().saveOtherMessage(newMessage, notifyType,
                        uuid, reqEntity);
            }
            if(callback.getType() == 0) {
                sendStanzaAndBindListener(newMessage, uuid, callback);
            } else {
                sendStanzaAndBindFixListener(newMessage, uuid, callback);
            }
            logger.d("notifyMessage ->  message:%s -> peerId:%s -> notifyType:%s -> uuid:%s",
                    message, peerId, notifyType.code(), uuid);
        }
    }

    @Override
    public boolean createAccount(String userNo, String password) {
        try {
            createConnection(false);
            xmppConnection.connect();
            AccountManager accountManager = AccountManager.getInstance(xmppConnection);
            accountManager.createAccount(userNo, password, new HashMap<String, String>());
            return true;
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送心跳包
     */
    @Override
    public void sendPing() {
        PingManager pingManager = PingManager.getInstanceFor(xmppConnection);
        try {
            //频率
            pingManager.setPingInterval(30);
            pingManager.pingMyServer();
//            pingManager.registerPingFailedListener();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }


    /**
     *******************************************接口实现方法******************************************
     */
    @Override
    public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
        if(packet instanceof Message) {
            handlerMessage(packet);
        } else if (packet instanceof RedisResIQ) {
            handlerRedisResIQ(packet);
        }
    }


    @Override
    public void connected(XMPPConnection connection) {
        logger.d("connected()：连接成功");
    }


    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        logger.d("authenticated()：校验成功");
    }


    @Override
    public void connectionClosed() {
        logger.d("connectionClosed()：关闭连接");
    }


    @Override
    public void connectionClosedOnError(Exception e) {
        logger.d("connectionClosedOnError()：连接断开");
        if (e instanceof XMPPException) {
            XMPPException.StreamErrorException xe = (XMPPException.StreamErrorException) e;
            final StreamError streamError = xe.getStreamError();
            switch (streamError.getCondition()) {
                //登陆冲突
                case conflict:
                    ((IMService)mService).getLoginManager().setKickout(true);
                    break;
            }
        }
        if(mService != null && mService instanceof IMService) {
            ((IMService)mService).getLoginManager()
                    .handlerSocketEvent(SmackSocketEvent.MSG_SERVER_DISCONNECTED);
        }
    }


    @Override
    public void reconnectionSuccessful() {
        logger.d("reconnectionSuccessful()：重连成功");
        if(!xmppConnection.isConnected()) {
            try {
                xmppConnection.connect();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }
        if(mService != null && mService instanceof IMService) {
            //重连后需要处理加入聊天室
            ((IMService)mService).getLoginManager().handlerLoginEvent(LoginEvent
                    .LOCAL_LOGIN_MSG_SERVICE);
        }
//        reConnect();
    }


    @Override
    public void reconnectingIn(int seconds) {
    }


    @Override
    public void reconnectionFailed(Exception e) {
        logger.d("reconnectionFailed()：重连失败");
    }

    /**
     * Called when the server ping fails.
     */
    @Override
    public void pingFailed() {
        logger.d("pingFailed()：ping失败");
    }



    private String getBareJID(String from) {
        String[] res = from.split("/");
        return res[0].toLowerCase();
    }

    /**
     *******************************************内部类******************************************
     */
    //消息过滤器
    static class AcceptAll implements StanzaFilter {
        @Override
        public boolean accept(Stanza packet) {
            UserInfoBean bean = BaseApplication.getInstance().getUserInfoBean();
            //针对自己发出消息的回复 需要放过
//            if(packet instanceof Message && ((Message)packet).
//                    getExtension(ReplayMessageTime.NAME, ReplayMessageTime.NAME_SPACE) != null) {
//                return true;
//            }
            if(bean != null
                    && StringUtils.isNotBlank(bean.getmAccount())
                    && StringUtils.isNotBlank(packet.getFrom())
                    && packet.getFrom().indexOf(bean.getmAccount()) >= 0) {
                return false;
            }
            return true;
        }
    }


    //发送消息，消息发布者，UI需监听
    private void triggerEvent(Object paramObject)
    {
        EventBus.getDefault().postSticky(paramObject);
    }



    class ReplayMessageTimeProvider extends
            ExtensionElementProvider<ExtensionElement> {

        @Override
        public ExtensionElement parse(XmlPullParser parser, int initialDepth)
                throws XmlPullParserException, IOException, SmackException {

            boolean done = false;
            String id = null;
            String time = null;

            while (!done) {
                int eventType = parser.next();
                String name = parser.getName();
                // XML Tab标签
                if (eventType == XmlPullParser.START_TAG) {
                    if (name.equals("id")) {
                        id = parser.nextText();
                    }
                    if (name.equals("time")) {
                        time = parser.nextText();
                    }
                }
                if (eventType == XmlPullParser.END_TAG) {
                    if (name.equals("replay")) {
                        done = true;
                    }
                }
            }
            ReplayMessageTime messageTime = new ReplayMessageTime(id, time);
            return messageTime;
        }

    }


    public class ReplayMessageTime implements ExtensionElement {

        public static final String NAME = "replay";
        public static final String NAME_SPACE = "com:jlm:replay";

        private String id;
        private String time;

        public ReplayMessageTime(String id, String time) {
            this.id = id;
            this.time = time;
        }

        public String getElementName() {
            return NAME;
        }

        /**
         * 构造XML
         * @return
         */
        public CharSequence toXML() {
            StringBuffer sb = new StringBuffer();
            sb.append("<replay").append(" xmlns=\"").append(NAME_SPACE)
                    .append("\">");
            sb.append("<id>").append(id).append("</id>");
            sb.append("<time>").append(time).append("</time>");
            sb.append("</replay>");
            return sb.toString();
        }

        public String getNamespace() {
            return NAME_SPACE;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }

    /**
     * 发送数据包，绑定监听
     * @param packet
     * @param uuid
     * @param listener
     */
    private void sendStanzaAndBindListener(Stanza packet, String uuid,
                                           XMPPServiceCallbackImpl listener) {
        if(listener != null) {
            listenerQueue.push(uuid, listener);
        }
        try {
            xmppConnection.sendStanza(packet);
        } catch (SmackException.NotConnectedException e) {
            //发送消息失败
            if(listener != null) {
                listener.onFailed();
            }
            listenerQueue.pop(uuid);
            logger.error(e);
        }
    }

    /**
     * 发送数据包，绑定监听 (适合一个请求 多条响应)
     * @param packet
     * @param uuid
     * @param listener
     */
    private void sendStanzaAndBindFixListener(Stanza packet, String uuid,
                                           XMPPServiceCallbackImpl listener) {
        if(listener != null) {
            fixListenerQueue.push(uuid, listener);
        }
        try {
            xmppConnection.sendStanza(packet);
        } catch (SmackException.NotConnectedException e) {
            //发送消息失败
            if(listener != null) {
                listener.onFailed();
            }
            fixListenerQueue.remove(uuid);
//            Log.e(TAG, "数据包发送异常", e);
            logger.error(e);
        }
    }


    //TODO 是否考虑放在子线程
    private void handlerMessage(Stanza stanza) {
        Message message = (Message) stanza;
        logger.d("handlerMessage -> message:%s", message.toString());
        MsgExtensionElement msgExtensionElement = message.getExtension(ElementNameType.msgType.name(),
                IMBaseDefine.NameSpaceType.MESSAGE.value());
        NotifyExtensionElement notifyExtensionElement = message.getExtension(ElementNameType.notifyType.name(),
                IMBaseDefine.NameSpaceType.NOTIFY.value());
        if(msgExtensionElement != null) {
            handlerChatMsg(message, msgExtensionElement.msgType);
        } else if (notifyExtensionElement != null) {
            handlerNotifyMsg(message, notifyExtensionElement.notifyType);
        } else {
            handlerChatMsg(message, null);
        }
    }

    /**
     * 处理聊天消息
     */
    private void handlerChatMsg(Message message, IMBaseDefine.MsgType msgType) {
        if(msgType == null ) {
            handlerMsg4NormalMessage(message);
        } else {
            switch (msgType) {
                //文本消息
                case MSG_TEXT:
                    handlerMsg4NormalMessage(message);
                    break;

            }
        }
    }

    /**
     * 处理通知消息
     */
    private void handlerNotifyMsg(Message message, IMBaseDefine.NotifyType notifyType) {
        logger.d("handlerNotify -> type:%s", notifyType.code());
        switch (notifyType) {
            //邀请相关
//            case INVITE_USER:
//                triggerNotifyMsg(message, notifyType);
//                break;
            default:
                triggerNotifyMsg(message, notifyType);
                break;
        }
    }

    /**
     * 处理普通消息
     * @param message
     */
    private void handlerMsg4NormalMessage(Message message) {
        //接收群聊消息
        if(message.getExtension(ReplayMessageTime.NAME, ReplayMessageTime.NAME_SPACE) == null) {
            String[] fromArr = message.getFrom().split("/");
            if(fromArr != null && fromArr.length >= 2) {
                String peerId = fromArr[0];
                String fromId = fromArr[1];
                if(fromId.indexOf("@") >= 0) {
                    fromId = fromId.substring(0, fromId.indexOf("@"));
                }
                UserEntity userEntity = new UserEntity();
                userEntity.setPeerId(fromId);
                PeerEntity peerEntity = new PeerEntity() {
                    @Override
                    public int getType() {
                        return DBConstant.SESSION_TYPE_GROUP;
                    }
                };
                peerEntity.setPeerId(peerId);
                MessageEntity textMessage = TextMessage.buildForReceive(message,
                        userEntity, peerEntity);

                //将消息保存到本地数据库
                MessageEntity dbMessage = textMessage.clone();
                messageDao.saveOrUpdate(dbMessage);

                SessionEntity cacheSessionEntity = sessionManager.getSessionMap()
                        .get(textMessage.getSessionKey());

                /**
                 * 1:最新接收的消息时间不能小于缓存消息的创建时间
                 * 2:时间由服务器生成
                 */
                if(cacheSessionEntity == null
                        || cacheSessionEntity.getCreated() < textMessage.getCreated()) {
                    //更新缓存，触发通知（更新群组列表UI）
                    sessionManager.updateSession(dbMessage, true);
                }

                /**
                 *  发送已读确认由上层的activity处理 特殊处理
                 *  1. 未读计数、 通知、session页面
                 *  2. 当前会话
                 * */
                PriorityEvent  notifyEvent = new PriorityEvent();
                notifyEvent.event = PriorityEvent.Event.MSG_RECEIVED_MESSAGE;
                notifyEvent.object = textMessage;
                triggerEvent(notifyEvent);
            }
        } else {
            //针对自己请求消息的回复，确认是否发送到服务器
            ReplayMessageTime messageTime = message
                    .getExtension(ReplayMessageTime.NAME, ReplayMessageTime.NAME_SPACE);
            String uuid = messageTime.getId();
            XMPPServiceCallbackImpl listener = listenerQueue.pop(uuid);
            if(listener != null) {
                listener.onSuccess(messageTime);
            }
        }
    }


    /**
     * 触发聊天消息相关事件
     * @param message
     * @param msgType
     */
    private void triggerChatMsg(Message message, IMBaseDefine.MsgType msgType) {
        //是否需要处理其他业务
        ChatMessageEvent event = new ChatMessageEvent();
        event.msgType = msgType;
        event.message = message;
        triggerEvent(event);
    }

    /**
     * 触发通知消息相关事件（IMOtherManager进行监听）
     * @param message
     * @param notifyType
     */
    private void triggerNotifyMsg(Message message, IMBaseDefine.NotifyType notifyType) {
        //是否需要处理其他业务
        NotifyMessageEvent event = new NotifyMessageEvent();
        event.notifyType = notifyType;
        event.message = message;
        triggerEvent(event);
    }


    private void handlerRedisResIQ(Stanza stanza) {
        RedisResIQ redisResIQ = (RedisResIQ)stanza;
        String uuid = stanza.getStanzaId();
        XMPPServiceCallbackImpl listener = listenerQueue.pop(uuid);
        if(listener != null){
            listener.onSuccess(redisResIQ);
        } else {
            XMPPServiceCallbackImpl fixListener = fixListenerQueue.get(uuid);
            if(fixListener != null) {
                fixListener.onSuccess(redisResIQ);
            }
        }

//        Log.d(TAG, redisResIQ.getStanzaId()+"接收IQ消息:"+redisResIQ.getContent());
        logger.d("handlerRedisResIQ -> message:%s接收IQ消息%s",
                redisResIQ.getStanzaId(), redisResIQ.getContent());
    }

    /**
     * 处理断线重连
     */
    private void doReconnection() {
        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(xmppConnection);
        reconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY);
        //重连时间10秒
        reconnectionManager.setFixedDelay(10);
        reconnectionManager.enableAutomaticReconnection();
    }



    /**
     * 消息相关扩展元素
     */
    static class MsgExtensionElement implements ExtensionElement {

        public IMBaseDefine.MsgType msgType;
        public IMBaseDefine.NameSpaceType nameSpaceType;

        public MsgExtensionElement(IMBaseDefine.MsgType msgType,
                                   IMBaseDefine.NameSpaceType nameSpaceType) {
            this.msgType = msgType;
            this.nameSpaceType = nameSpaceType;
        }


        @Override
        public String getNamespace() {
            return nameSpaceType.value();
        }

        @Override
        public String getElementName() {
            return ElementNameType.msgType.name();
        }

        @Override
        public CharSequence toXML() {
            StringBuilder sbf = new StringBuilder();
            sbf.append("<"+getElementName()+" xmlns='"+getNamespace()+"'>")
                    .append("<code>")
                    .append(msgType.code())
                    .append("</code>")
                    .append("</"+getElementName()+">");
            return sbf.toString();
        }
    }

    /**
     * 通知相关扩展元素
     */
    static class NotifyExtensionElement implements ExtensionElement {

        public IMBaseDefine.NotifyType notifyType;
        public IMBaseDefine.NameSpaceType nameSpaceType;

        public NotifyExtensionElement(IMBaseDefine.NotifyType notifyType,
                                       IMBaseDefine.NameSpaceType nameSpaceType) {
            this.notifyType = notifyType;
            this.nameSpaceType = nameSpaceType;
        }


        @Override
        public String getNamespace() {
            return nameSpaceType.value();
        }

        @Override
        public String getElementName() {
            return ElementNameType.notifyType.name();
        }

        @Override
        public CharSequence toXML() {
            StringBuilder sbf = new StringBuilder();
            sbf.append("<"+getElementName()+" xmlns='"+getNamespace()+"'>")
                    .append("<code>")
                    .append(notifyType.code())
                    .append("</code>")
                    .append("</"+getElementName()+">");
            return sbf.toString();
        }
    }


    //自定义元素类型（可扩展）
    enum ElementNameType {
        msgType, notifyType
    }


    static class MsgTypeProvider extends
            ExtensionElementProvider<ExtensionElement> {

        @Override
        public ExtensionElement parse(XmlPullParser parser, int initialDepth)
                throws XmlPullParserException, IOException, SmackException {
            boolean done = false;
            IMBaseDefine.MsgType msgType = null;

            while (!done) {
                int eventType = parser.next();
                String name = parser.getName();
                // XML Tab标签
                if (eventType == XmlPullParser.START_TAG) {
                    if (name.equals("code")) {
                        String codeValue = parser.nextText();
                        msgType = IMBaseDefine.MsgType.getInstanceByCode(codeValue);
                    }
                }
                if (eventType == XmlPullParser.END_TAG) {
                    if (name.equals("msgType")) {
                        done = true;
                    }
                }
            }
            MsgExtensionElement element = new MsgExtensionElement(msgType,
                    IMBaseDefine.NameSpaceType.MESSAGE);
            return element;
        }
    }

    static class NotifyTypeProvider extends
            ExtensionElementProvider<ExtensionElement> {

        @Override
        public ExtensionElement parse(XmlPullParser parser, int initialDepth)
                throws XmlPullParserException, IOException, SmackException {
            boolean done = false;
            IMBaseDefine.NotifyType notifyType = null;

            while (!done) {
                int eventType = parser.next();
                String name = parser.getName();
                // XML Tab标签
                if (eventType == XmlPullParser.START_TAG) {
                    if (name.equals("code")) {
                        String codeValue = parser.nextText();
                        notifyType = IMBaseDefine.NotifyType.getInstanceByCode(codeValue);
                    }
                }
                if (eventType == XmlPullParser.END_TAG) {
                    if (name.equals("notifyType")) {
                        done = true;
                    }
                }
            }
            NotifyExtensionElement element = new NotifyExtensionElement(notifyType,
                   IMBaseDefine.NameSpaceType.NOTIFY);
            return element;
        }
    }

}
