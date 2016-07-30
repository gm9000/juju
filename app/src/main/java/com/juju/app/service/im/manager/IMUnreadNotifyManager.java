package com.juju.app.service.im.manager;

import com.juju.app.bean.UserInfoBean;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.entity.notify.GroupNotifyEntity;
import com.juju.app.event.JoinChatRoomEvent;
import com.juju.app.golobal.AppContext;
import com.juju.app.golobal.JujuDbUtils;
import com.juju.app.service.im.thread.GetHisNotifysThread;
import com.juju.app.service.im.thread.MergeMessageThread;
import com.juju.app.utils.Logger;
import com.juju.app.utils.ThreadPoolUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.ex.DbException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 项目名称：juju
 * 类描述：未读通知类消息相关的处理，归属于messageEvent中
 * 可以理解为MessageManager的又一次拆分
 * 为session提供未读支持
 * 创建人：gm
 * 日期：2016/4/20 19:43
 * 版本：V1.0.0
 */
public class IMUnreadNotifyManager extends IMManager {


    private volatile static IMUnreadNotifyManager inst = null;

    //双重判断+volatile（禁止JMM重排序）保证线程安全
    public static IMUnreadNotifyManager instance() {
        if(inst == null) {
            synchronized (IMUnreadNotifyManager.class) {
                if(inst == null) {
                    inst = new IMUnreadNotifyManager();
                }
            }
        }
        return inst;
    }

    private Logger logger = Logger.getLogger(IMUnreadNotifyManager.class);

    //未读消息查询超时时间
    private final static int UNREAD_REQ_TIMEOUT = 10;

    /**key=> sessionKey*/
    private ConcurrentHashMap<String, UnreadEntity> unreadNotifyMap = new ConcurrentHashMap<>();

    List<String> chatRoomIds = new ArrayList<>();


    public ConcurrentHashMap<String, UnreadEntity> getUnreadNotifyMap() {
        return unreadNotifyMap;
    }

    @Override
    public void doOnStart() {
        UserInfoBean userInfoBean = AppContext.getUserInfoBean();
        chatRoomIds.add(userInfoBean.getmRoomName() +
                "@" + userInfoBean.getmMucServiceName() + "." + userInfoBean.getmServiceName());
    }

    public void onNormalLoginOk(){
        onLocalLoginOk();
        onLocalNetOk();
    }

    public void onLocalLoginOk() {
        if (!EventBus.getDefault().isRegistered(inst)) {
            EventBus.getDefault().register(inst);
        }
        unreadNotifyMap.clear();
    }

    /**
     * 未读消息由用户加入群组后 通知接收,初始化需要清理
     */
    public void onLocalNetOk(){

    }

    /**
     * 上下文环境的更新
     * 1. 环境变量的clear
     * 2. eventBus的清空
     */
    @Override
    public void reset() {
        EventBus.getDefault().unregister(inst);
        unreadNotifyMap.clear();
    }

    /**
     * 初始化IMUnreadMsgManager后,请求未读消息列表
     */
    private void reqUnreadNotifyList(List<String> groupPeerIds, int timeOut) {
        logger.i("unread#reqUnreadMsgContactList");

        if(groupPeerIds != null && groupPeerIds.size() >0) {
            final List<String> newGroupPeerIds = new ArrayList<>(groupPeerIds);
            final CountDownLatch countDownLatch = new CountDownLatch(groupPeerIds.size());
            ThreadPoolUtil.instance().executeImTask(new Runnable() {
                @Override
                public void run() {
                    for(final String peerId : newGroupPeerIds) {
                        reqUnreadNotify(peerId, countDownLatch);
                    }
                }
            });
            try {
                //消息收取时间不能超过1分钟 (不能在MAIN线程中执行)
                countDownLatch.await(timeOut, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //获取群组未读消息
    private void reqUnreadNotify(final String chatRoomId, final CountDownLatch countDownLatch) {

        try {
            GroupNotifyEntity groupNotifyEntity = JujuDbUtils.getInstance().selector(GroupNotifyEntity.class).where("id", "=", chatRoomId.split("@")[0]).findFirst();
            if(groupNotifyEntity != null){
                GetHisNotifysThread getHisNotifysThread = new GetHisNotifysThread
                        (chatRoomId, groupNotifyEntity.getTime()+1, socketService);
                Thread thread = new Thread(getHisNotifysThread,
                        MergeMessageThread.class.getSimpleName());
                ThreadPoolUtil.instance().executeImTask(thread);
            }else{
                countDownLatch.countDown();
            }
        } catch (DbException e) {
            e.printStackTrace();
        }

    }

    /**
     * IMGroupManager登陆成功后，需要获取未读通知消息消息
     * @param joinChatRoomEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent4JoinChatRoom(final JoinChatRoomEvent joinChatRoomEvent) {
        EventBus.getDefault().removeStickyEvent(joinChatRoomEvent);
        switch (joinChatRoomEvent.event){
            case JOIN_OK_4_UNREAD_NOTIFY_REQ:
                ThreadPoolUtil.instance().executeImTask(new Runnable() {
                    @Override
                    public void run() {
                        reqUnreadNotifys(joinChatRoomEvent.joinedGroupPeerIds, UNREAD_REQ_TIMEOUT);
                    }
                });
                break;
        }
    }

    /**
     * 初始化IMUnreadNotifyManager后,请求未读通知列表
     */
    private void reqUnreadNotifys(List<String> groupPeerIds, int timeOut) {
        logger.i("unread#reqUnreadMsgContactList");
        if(groupPeerIds != null && groupPeerIds.size() >0) {
            final List<String> newGroupPeerIds = new ArrayList<>(groupPeerIds);
            final CountDownLatch countDownLatch = new CountDownLatch(groupPeerIds.size());
            ThreadPoolUtil.instance().executeImTask(new Runnable() {
                @Override
                public void run() {
                    for(final String peerId : newGroupPeerIds) {
                        reqUnreadNotify(peerId, countDownLatch);
                    }
                }
            });
            try {
                //消息收取时间不能超过1分钟 (不能在MAIN线程中执行)
                countDownLatch.await(timeOut, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //通知刷新未读消息(GroupChatFragment)
    }
}
