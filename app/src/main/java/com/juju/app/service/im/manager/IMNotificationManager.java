package com.juju.app.service.im.manager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import com.juju.app.R;
import com.juju.app.activity.chat.ChatActivity;
import com.juju.app.activity.party.MyInviteListActivity;
import com.juju.app.bean.UserInfoBean;
import com.juju.app.config.HttpConstants;
import com.juju.app.entity.User;
import com.juju.app.entity.chat.GroupEntity;
import com.juju.app.entity.chat.OtherMessageEntity;
import com.juju.app.entity.chat.UnreadEntity;
import com.juju.app.event.GroupEvent;
import com.juju.app.event.NotificationMessageEvent;
import com.juju.app.event.NotifyMessageEvent;
import com.juju.app.event.UnreadEvent;
import com.juju.app.event.notify.InviteUserEvent;
import com.juju.app.golobal.Constants;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IMBaseDefine;
import com.juju.app.helper.IMUIHelper;
import com.juju.app.service.im.sp.ConfigurationSp;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.JacksonUtil;
import com.juju.app.utils.Logger;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.packet.Message;

/**
 * 项目名称：juju
 * 类描述：通知管理服务
 * 创建人：gm
 * 日期：2016/6/2 11:39
 * 版本：V1.0.0
 */
public class IMNotificationManager extends IMManager {

    private Logger logger = Logger.getLogger(IMNotificationManager.class);

    private volatile static IMNotificationManager inst;

    private ConfigurationSp configurationSp;

    private UserInfoBean userInfoBean;


    public static IMNotificationManager instance() {
        if(inst == null) {
            synchronized (IMNotificationManager.class) {
                if(inst == null) {
                    inst = new IMNotificationManager();
                }
            }
        }
        return inst;
    }

    @Override
    public void doOnStart() {
        cancelAllNotifications();
    }

    @Override
    public void reset() {
        EventBus.getDefault().unregister(this);
        cancelAllNotifications();
    }

    public void onLoginSuccess(){
        userInfoBean = BaseApplication.getInstance().getUserInfoBean();
        String loginId = userInfoBean.getJujuNo();
        configurationSp = ConfigurationSp.instance(ctx, loginId);
        if(!EventBus.getDefault().isRegistered(inst)){
            EventBus.getDefault().register(inst);
        }
    }

    /**
     * 接收通知信息
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4OtherMessage(NotificationMessageEvent event) {
        handleOtherMessageRecv(event);
    }

    private void handleOtherMessageRecv(NotificationMessageEvent event) {
        OtherMessageEntity entity = event.entity;
        logger.d("notification#recv other message");
        String fromId = entity.getPeerId(false);
        logger.d("notification#msg no one handled, fromId:%s", fromId);

        // 全局开关
        boolean  globallyOnOff = configurationSp.getCfg(Constants.SETTING_GLOBAL,
                ConfigurationSp.CfgDimension.NOTIFICATION);
        if (globallyOnOff) {
            logger.d("notification#shouldGloballyShowNotification is false, return");
            return;
        }
        // 判断是否是自己的消息
        if(fromId.indexOf(userInfoBean.getJujuNo()) < 0){
            showOtherMessageNotification(event);
        }
    }

    private void showOtherMessageNotification(final NotificationMessageEvent event) {
        OtherMessageEntity entity = event.entity;
        // 服务端有些特定的支持 尺寸是不是要调整一下 todo 100*100  下面的就可以不要了
        ImageSize targetSize = new ImageSize(80, 80);
        String avatarUrl = "";
        String title = "";
        String detailTitle = "";
        String userNo = "";
        String userName = "";
        String groupName = "";
        String actionName = "";
        String actionName1 = "";
        String targetNickName = "";
        String sessionKey = "";

        switch (event.event) {
            case INVITE_USER_RECEIVED:
                InviteUserEvent.InviteUserBean inviteUserBean =
                        (InviteUserEvent.InviteUserBean) JacksonUtil
                                .turnString2Obj(entity.getContent(),
                                        IMBaseDefine.NotifyType.INVITE_USER.getCls());
                userNo = inviteUserBean.userNo;
                userName = inviteUserBean.nickName;
                groupName = inviteUserBean.groupName;
                sessionKey = DBConstant.SESSION_TYPE_GROUP+"_"+inviteUserBean.groupId+"@"
                        +userInfoBean.getmMucServiceName()+ "."+userInfoBean.getmServiceName();
                title = IMBaseDefine.NotifyType.INVITE_USER.desc();
                detailTitle = title;
                avatarUrl = HttpConstants.getPortraitUrl()+userNo;
                actionName = "邀请";
                actionName1 = "加入";
                targetNickName = "您";
                break;
//            case INVITE_GROUP_NOTIFY_RES_RECEIVED:
//                IMBaseDefine.InviteGroupNotifyResBean inviteGroupNotifyResBean =
//                        (IMBaseDefine.InviteGroupNotifyResBean) JacksonUtil
//                                .turnString2Obj(entity.getContent(),
//                                        IMBaseDefine.NotifyType.INVITE_GROUP_NOTIFY_RES.getCls());
//                userNo = inviteGroupNotifyResBean.userNo;
//                userName = inviteGroupNotifyResBean.userName;
//                groupName = inviteGroupNotifyResBean.groupName;
//                title = IMBaseDefine.NotifyType.INVITE_GROUP_NOTIFY_RES.desc();
//                detailTitle = title;
//                avatarUrl = HttpConstants.getPortraitUrl()+userNo;
//                if(inviteGroupNotifyResBean.status == 1) {
//                    actionName = "同意";
//                } else {
//                    actionName = "拒绝";
//                }
//                actionName1 = "加入";
//                break;
        }

        //获取头像
        avatarUrl = IMUIHelper.getRealAvatarUrl(avatarUrl);
        final String ticker = String.format("[%s]%s: %s%s%s%s",detailTitle, userName, actionName,
                targetNickName, actionName1, groupName);
        final int notificationId = getSessionNotificationId(userNo);
        final Intent intent = new Intent(ctx, ChatActivity.class);
        intent.putExtra(Constants.SESSION_ID_KEY, sessionKey);
        logger.d("notification#notification avatarUrl:%s", avatarUrl);
        final String finalTitle = title;
        ImageLoader.getInstance().loadImage(avatarUrl, targetSize, null, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view,
                                          Bitmap loadedImage) {
                logger.d("notification#icon onLoadComplete");
                // holder.image.setImageBitmap(loadedImage);
                showInNotificationBar(finalTitle,ticker,loadedImage,notificationId,intent);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view,
                                        FailReason failReason) {
                logger.d("notification#icon onLoadFailed");
                // 服务器支持的格式有哪些
                // todo eric default avatar is too small, need big size(128 * 128)
                Bitmap defaultBitmap = BitmapFactory.decodeResource(ctx.getResources(),
                        IMUIHelper.getDefaultAvatarResId(R.mipmap.tt_default_user_portrait_corner));
                showInNotificationBar(finalTitle,ticker,defaultBitmap,notificationId,intent);
            }
        });
    }


    /*****************************************************************************************/

    /**
     * 接收未读消息通知
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4Unread(UnreadEvent event){
        switch (event.event){
            case UNREAD_MSG_RECEIVED:
                UnreadEntity unreadEntity = event.entity;
                handleMsgRecv(unreadEntity);
                break;
        }
    }


    /**
     * 接收屏蔽群通知
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent4Group(GroupEvent event){
        GroupEntity gEntity = event.getGroupEntity();
        if(event.getEvent()== GroupEvent.Event.SHIELD_GROUP_OK){
            if(gEntity == null){
                return;
            }
            cancelSessionNotifications(gEntity.getSessionKey());
        }
    }

    //取消通知
    public void cancelAllNotifications() {
        logger.d("notification#cancelAllNotifications");
        if(null == ctx){
            return;
        }
        NotificationManager notifyMgr = (NotificationManager) ctx.getSystemService(Context
                .NOTIFICATION_SERVICE);
        if (notifyMgr == null) {
            return;
        }
        notifyMgr.cancelAll();
    }

    private void handleMsgRecv(UnreadEntity entity) {
        logger.d("notification#recv unhandled message");
        String peerId = entity.getPeerId();
        int sessionType =  entity.getSessionType();
        logger.d("notification#msg no one handled, peerId:%s, sessionType:%d", peerId, sessionType);


        //判断是否设定了免打扰
        if(entity.isForbidden()){
            logger.d("notification#GROUP_STATUS_SHIELD");
            return;
        }

        // 全局开关
        boolean  globallyOnOff = configurationSp.getCfg(Constants.SETTING_GLOBAL,
                ConfigurationSp.CfgDimension.NOTIFICATION);
        if (globallyOnOff) {
            logger.d("notification#shouldGloballyShowNotification is false, return");
            return;
        }

        // 单独的设置
        boolean singleOnOff = configurationSp.getCfg(entity.getSessionKey(),
                ConfigurationSp.CfgDimension.NOTIFICATION);
        if (singleOnOff) {
            logger.d("notification#shouldShowNotificationBySession is false, return");
            return;
        }

        // 判断是否是自己的消息
        if(!userInfoBean.getJujuNo().equals(peerId)){
            showNotification(entity);
        }
    }

    private void showNotification(final UnreadEntity unreadEntity) {
        // todo eric need to set the exact size of the big icon
        // 服务端有些特定的支持 尺寸是不是要调整一下 todo 100*100  下面的就可以不要了
        ImageSize targetSize = new ImageSize(80, 80);
        String peerId = unreadEntity.getPeerId();
        String fromId = unreadEntity.getFromId();
//        int sessionType = unreadEntity.getSessionType();
        String avatarUrl = "";
        String title = "";
        String detailTitle = "";
        String content = unreadEntity.getLatestMsgData();
        String unit = ctx.getString(R.string.msg_cnt_unit);
        int totalUnread = unreadEntity.getUnReadCnt();
        GroupEntity group = IMGroupManager.instance().findGroup(peerId);
        if(group !=null){
            title = group.getMainName();
            avatarUrl = group.getAvatar() == null ? "" : group.getAvatar();
        }else{
            title = "Group_"+peerId;
            avatarUrl = "";
        }

        User userEntity = IMContactManager.instance().findContact(fromId);
        if(userEntity != null) {
            detailTitle = userEntity.getNickName();
        }
        //获取头像
        avatarUrl = IMUIHelper.getRealAvatarUrl(avatarUrl);
        final String ticker = String.format("[%d%s]%s: %s", totalUnread, unit, detailTitle, content);
        final int notificationId = getSessionNotificationId(unreadEntity.getSessionKey());
        final Intent intent = new Intent(ctx, ChatActivity.class);
        intent.putExtra(Constants.SESSION_ID_KEY, unreadEntity.getSessionKey());

        logger.d("notification#notification avatarUrl:%s", avatarUrl);
        final String finalTitle = title;
        ImageLoader.getInstance().loadImage(avatarUrl, targetSize, null, new SimpleImageLoadingListener() {

            @Override
            public void onLoadingComplete(String imageUri, View view,
                                          Bitmap loadedImage) {
                logger.d("notification#icon onLoadComplete");
                // holder.image.setImageBitmap(loadedImage);
                showInNotificationBar(finalTitle,ticker,loadedImage,notificationId,intent);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view,
                                        FailReason failReason) {
                logger.d("notification#icon onLoadFailed");
                // 服务器支持的格式有哪些
                // todo eric default avatar is too small, need big size(128 * 128)
                Bitmap defaultBitmap = BitmapFactory.decodeResource(ctx.getResources(),
                        IMUIHelper.getDefaultAvatarResId(unreadEntity.getSessionType()));
                showInNotificationBar(finalTitle,ticker,defaultBitmap,notificationId,intent);
            }
        });
    }

    //通过BKDR hash值获取notificationId
    public int getSessionNotificationId(String sessionKey) {
        logger.d("notification#getSessionNotificationId sessionTag:%s", sessionKey);
        int hashedNotificationId = (int) hashBKDR(sessionKey);
        logger.d("notification#hashedNotificationId:%d", hashedNotificationId);
        return hashedNotificationId;
    }

    //构建BKDR hash值
    private long hashBKDR(String str) {
        long seed = 131; // 31 131 1313 13131 131313 etc..
        long hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash * seed) + str.charAt(i);
        }
        return hash;
    }

    //通知栏提示消息
    private void showInNotificationBar(String title, String ticker,
                                       Bitmap iconBitmap, int notificationId, Intent intent) {
        logger.d("notification#showInNotificationBar title:%s ticker:%s",title,ticker);

        NotificationManager notifyMgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifyMgr == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        builder.setContentTitle(title);
        builder.setContentText(ticker);
        builder.setSmallIcon(R.mipmap.jlm);
        builder.setTicker(ticker);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);

        // this is the content near the right bottom side
        // builder.setContentInfo("content info");

        if (configurationSp.getCfg(Constants.SETTING_GLOBAL,ConfigurationSp.CfgDimension.VIBRATION)) {
            // delay 0ms, vibrate 200ms, delay 250ms, vibrate 200ms
            long[] vibrate = {0, 200, 250, 200};
            builder.setVibrate(vibrate);
        } else {
            logger.d("notification#setting is not using vibration");
        }

        // sound
        if (configurationSp.getCfg(Constants.SETTING_GLOBAL,ConfigurationSp.CfgDimension.SOUND)) {
            builder.setDefaults(Notification.DEFAULT_SOUND);
        } else {
            logger.d("notification#setting is not using sound");
        }
        if (iconBitmap != null) {
            logger.d("notification#fetch icon from network ok");
            builder.setLargeIcon(iconBitmap);
        } else {
            // do nothint ?
        }
        // if MessageActivity is in the background, the system would bring it to
        // the front
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        notifyMgr.notify(notificationId, notification);
    }

    /**
     * 在通知栏中删除特定回话的状态
     * @param sessionKey
     */
    public void cancelSessionNotifications(String sessionKey) {
        logger.d("notification#cancelSessionNotifications");
        NotificationManager notifyMgr = (NotificationManager) ctx
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (null == notifyMgr) {
            return;
        }
        int notificationId = getSessionNotificationId(sessionKey);
        notifyMgr.cancel(notificationId);
    }

}
