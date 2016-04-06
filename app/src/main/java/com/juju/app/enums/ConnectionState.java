package com.juju.app.enums;

public enum ConnectionState {
	OFFLINE,		/// 离线
	CONNECTING,		/// 连接中
	ONLINE,			/// 在线
	DISCONNECTING,		/// 断线中
	DISCONNECTED,		/// 断线
	RECONNECT_NETWORK,	/// 等待网络正常
	RECONNECT_DELAYED;	/// 等待一个重新连接定时器
}
