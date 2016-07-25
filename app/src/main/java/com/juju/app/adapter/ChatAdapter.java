package com.juju.app.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.juju.app.R;
import com.juju.app.activity.chat.PreviewMessageImagesActivity;
import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.AudioMessage;
import com.juju.app.entity.chat.ImageMessage;
import com.juju.app.entity.chat.TextMessage;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.enums.RenderType;
import com.juju.app.golobal.DBConstant;
import com.juju.app.golobal.IntentConstant;
import com.juju.app.golobal.MessageConstant;
import com.juju.app.service.im.IMService;
import com.juju.app.service.im.audio.AudioPlayerHandler;
import com.juju.app.tools.Emoparser;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.CommonUtil;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.FileUtil;
import com.juju.app.utils.Logger;
import com.juju.app.utils.StringUtils;
import com.juju.app.utils.ToastUtil;
import com.juju.app.view.groupchat.AudioRenderView;
import com.juju.app.view.groupchat.ImageRenderView;
import com.juju.app.view.groupchat.MessageOperatePopup;
import com.juju.app.view.groupchat.NormalNotifyRenderView;
import com.juju.app.view.groupchat.TextRenderView;
import com.juju.app.view.groupchat.TimeRenderView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 项目名称：juju
 * 类描述：群聊适配器
 * 创建人：gm
 * 日期：2016/3/4 14:23
 * 版本：V1.0.0
 */
public class ChatAdapter extends BaseAdapter {

    private Logger logger = Logger.getLogger(ChatAdapter.class);

    //消息集合
    private ArrayList<Object> msgObjectList = new ArrayList<Object>();

    private Context ctx;

    private IMService imService;
    private UserEntity loginUser;



    public void setImService(IMService imService, UserEntity loginUser) {
        this.imService = imService;
        this.loginUser = loginUser;
    }

    public ChatAdapter(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public int getCount() {
        if (null == msgObjectList) {
            return 0;
        } else {
            return msgObjectList.size();
        }
    }

    @Override
    public Object getItem(int position) {
        if (position >= getCount() || position < 0) {
            return null;
        }
        return msgObjectList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        try {
            final int typeIndex = getItemViewType(position);
            RenderType renderType = RenderType.values()[typeIndex];
            // 改用map的形式
            switch (renderType) {
                case MESSAGE_TYPE_INVALID:
                    // 直接返回
                    logger.e("[fatal erro] render type:MESSAGE_TYPE_INVALID");
                    break;
                case MESSAGE_TYPE_TIME_TITLE:
                    convertView = timeBubbleRender(position, convertView, parent);
                    break;
                case MESSAGE_TYPE_MINE_AUDIO:
                    convertView = audioMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_AUDIO:
                    convertView = audioMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_GIF_IMAGE:
//                    convertView = GifImageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_GIF_IMAGE:
//                    convertView = GifImageMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_IMAGE:
                    convertView = imageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_IMAGE:
                    convertView = imageMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_TETX:
                    convertView = textMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_TEXT:
                    convertView = textMsgRender(position, convertView, parent, false);
                    break;

                case MESSAGE_TYPE_MINE_GIF:
//                    convertView = gifMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_GIF:
//                    convertView = gifMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_NORMAL_NOTIFY:
                    convertView = normalNotifyRender(position, convertView, parent);
                    break;
            }
            return convertView;
        } catch (Exception e) {
            logger.e("chat#%s", e);
            return null;
        }
    }


    @Override
    public int getItemViewType(int position) {
        try {
            /**默认是失败类型*/
            RenderType type = RenderType.MESSAGE_TYPE_INVALID;

            Object obj = msgObjectList.get(position);
            if (obj instanceof Long) {
                type = RenderType.MESSAGE_TYPE_TIME_TITLE;
            } else if (obj instanceof MessageEntity) {
                MessageEntity info = (MessageEntity) obj;
                boolean isMine = info.getFromId().equals(loginUser.getPeerId());
                switch (info.getDisplayType()) {
                    case DBConstant.SHOW_AUDIO_TYPE:
                        type = isMine ? RenderType.MESSAGE_TYPE_MINE_AUDIO
                                : RenderType.MESSAGE_TYPE_OTHER_AUDIO;
                        break;
                    case DBConstant.SHOW_IMAGE_TYPE:
                        ImageMessage imageMessage = (ImageMessage) info;
                        type = isMine ? RenderType.MESSAGE_TYPE_MINE_IMAGE
                                : RenderType.MESSAGE_TYPE_OTHER_IMAGE;
                        break;
                    case DBConstant.SHOW_ORIGIN_TEXT_TYPE:
                        if (info.isGIfEmo()) {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_GIF
                                    : RenderType.MESSAGE_TYPE_OTHER_GIF;
                        } else {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_TETX
                                    : RenderType.MESSAGE_TYPE_OTHER_TEXT;
                        }

                        break;
                    case DBConstant.SHOW_NOTIFY_TYPE:
                        type = RenderType.MESSAGE_TYPE_NORMAL_NOTIFY;
                        break;
                    case DBConstant.SHOW_MIX_TEXT:
                        //
                        logger.e("混合的消息类型%s", obj);
                    default:
                        break;
                }
            }
            return type.ordinal();
        } catch (Exception e) {
            logger.e(e.getMessage());
            return RenderType.MESSAGE_TYPE_INVALID.ordinal();
        }
    }



    /**
     * ----------------------添加历史消息-----------------
     */
    public void addItem(final MessageEntity msg) {
        if (msg.getDisplayType() == DBConstant.MSG_TYPE_SINGLE_TEXT) {
            if (isMsgGif(msg)) {
                msg.setGIfEmo(true);
            } else {
                msg.setGIfEmo(false);
            }
        }
        long nextTime = msg.getCreated();
        if (getCount() > 0) {
            Object object = msgObjectList.get(getCount() - 1);
            if (object instanceof MessageEntity) {
                long preTime = ((MessageEntity) object).getCreated();
                //是否需要显示时间
                boolean needTime = DateUtil.needDisplayTime(preTime, nextTime);
                if (needTime) {
                    Long in = nextTime;
                    msgObjectList.add(in);
                }
            }
        } else {
            Long in = msg.getCreated();
            msgObjectList.add(in);
        }
//        /**消息的判断*/
//        if (msg.getDisplayType() == DBConstant.SHOW_MIX_TEXT) {
//            MixMessage mixMessage = (MixMessage) msg;
//            msgObjectList.addAll(mixMessage.getMsgList());
//        } else {
//            msgObjectList.add(msg);
//        }
        if (msg instanceof ImageMessage) {
            ImageMessage.addToImageMessageList((ImageMessage) msg);
        }
        msgObjectList.add(msg);
        logger.d("#messageAdapter#addItem");
        notifyDataSetChanged();
    }

    private boolean isMsgGif(MessageEntity msg) {
        String content = msg.getContent();
        // @YM 临时处理  牙牙表情与消息混合出现的消息丢失
        if (TextUtils.isEmpty(content)
                || !(content.startsWith("[") && content.endsWith("]"))) {
            return false;
        }
        return Emoparser.getInstance(this.ctx).isMessageGif(msg.getContent());
    }

    /**
     * 时间气泡的渲染展示
     */
    private View timeBubbleRender(int position, View convertView, ViewGroup parent) {
        TimeRenderView timeRenderView;
        Long timeBubble = (Long) msgObjectList.get(position);
        if (null == convertView) {
            timeRenderView = TimeRenderView.inflater(ctx, parent);
        } else {
            // 不用再使用tag 标签了
            if(convertView instanceof  TimeRenderView) {
                timeRenderView = (TimeRenderView) convertView;
            } else {
                timeRenderView = TimeRenderView.inflater(ctx, parent);
            }
        }
        timeRenderView.setTime(timeBubble);
        return timeRenderView;
    }

    /**
     * text类型的: 1. 设定内容Emoparser
     * 2. 点击事件  单击跳转、 双击方法、长按pop menu
     * 点击头像的事件 跳转
     *
     * @param position
     * @param convertView
     * @param viewGroup
     * @param isMine
     * @return
     */
    private View textMsgRender(final int position, View convertView,
                               final ViewGroup viewGroup, final boolean isMine) {
        TextRenderView textRenderView;
        final TextMessage textMessage = (TextMessage) msgObjectList.get(position);
        User user = imService.getContactManager().findContactByFormId(textMessage.getFromId());
        if (null == convertView) {
            textRenderView = TextRenderView.inflater(ctx, viewGroup, isMine); //new TextRenderView(ctx,viewGroup,isMine);
        } else {
            if(convertView instanceof TextRenderView
                    && (isMine == ((TextRenderView) convertView).isMine())) {
                textRenderView = (TextRenderView) convertView;
            } else {
                textRenderView = TextRenderView.inflater(ctx, viewGroup, isMine); //new TextRenderView(ctx,viewGroup,isMine);
            }
        }
        final TextView textView = textRenderView.getMessageContent();

        // 失败事件添加
        textRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
//                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
//                popup.show(textView, DBConstant.SHOW_ORIGIN_TEXT_TYPE, true, isMine);
            }
        });

        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 弹窗类型
//                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
//                boolean bResend = textMessage.getStatus() == MessageConstant.MSG_FAILURE;
//                popup.show(textView, DBConstant.SHOW_ORIGIN_TEXT_TYPE, bResend, isMine);
                return true;
            }
        });

        // url 路径可以设定 跳转
        final String content = textMessage.getContent();
//        textView.setOnTouchListener(new OnDoubleClickListener() {
//            @Override
//            public void onClick(View view) {
//                //todo
//            }
//
//            @Override
//            public void onDoubleClick(View view) {
//                Intent intent = new Intent(ctx, PreviewTextActivity.class);
//                intent.putExtra(IntentConstant.PREVIEW_TEXT_CONTENT, content);
//                ctx.startActivity(intent);
//            }
//        });
        textRenderView.render(textMessage, user, ctx);
        return textRenderView;
    }


    /**
     * 语音的路径，判断收发的状态
     * 展现的状态
     * 播放动画相关
     * 获取语音的读取状态/
     * 语音长按事件
     *
     * @param position
     * @param convertView
     * @param viewGroup
     * @param isMine
     * @return
     */
    private View audioMsgRender(final int position, View convertView, final ViewGroup viewGroup, final boolean isMine) {
        AudioRenderView audioRenderView;
        final AudioMessage audioMessage = (AudioMessage) msgObjectList.get(position);
        User entity = imService.getContactManager().findContactByFormId(audioMessage.getFromId());
        if (null == convertView) {
            audioRenderView = AudioRenderView.inflater(ctx, viewGroup, isMine); //new TextRenderView(ctx,viewGroup,isMine);
        } else {
            if(convertView instanceof AudioRenderView
                    && (isMine == ((AudioRenderView) convertView).isMine())) {
                audioRenderView = (AudioRenderView) convertView;
            } else {
                audioRenderView = AudioRenderView.inflater(ctx, viewGroup, isMine); //new TextRenderView(ctx,viewGroup,isMine);
            }
        }
        final String audioPath = audioMessage.getAudioPath();
        final View messageLayout = audioRenderView.getMessageLayout();
        if (!TextUtils.isEmpty(audioPath)) {
            // 播放的路径为空,这个消息应该如何展示
            messageLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(audioMessage, position));
                    boolean bResend = audioMessage.getStatus() == MessageConstant.MSG_FAILURE;
                    popup.show(messageLayout, DBConstant.SHOW_AUDIO_TYPE, bResend, isMine);
                    return true;
                }
            });
        }


        audioRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(audioMessage, position));
                popup.show(messageLayout, DBConstant.SHOW_AUDIO_TYPE, true, isMine);
            }
        });


        audioRenderView.setBtnImageListener(new AudioRenderView.BtnImageListener() {
            @Override
            public void onClickUnread() {
                logger.d("chat#audio#set audio meessage read status");
                audioMessage.setReadStatus(MessageConstant.AUDIO_READED);
//                imService.getDbInterface().insertOrUpdateMessage(audioMessage);
                imService.getMessageManager().replaceInto(audioMessage);
            }

            @Override
            public void onClickReaded() {
            }
        });
        audioRenderView.render(audioMessage, entity, ctx);
        return audioRenderView;
    }


    /**
     * 1.头像事件
     * mine:事件 other事件
     * 图片的状态  消息收到，没收到，图片展示成功，没有成功
     * 触发图片的事件  【长按】
     * <p/>
     * 图片消息类型的render
     *
     * @param position
     * @param convertView
     * @param parent
     * @param isMine
     * @return
     */
    private View imageMsgRender(final int position, View convertView, final ViewGroup parent, final boolean isMine) {
        ImageRenderView imageRenderView;
        final ImageMessage imageMessage = (ImageMessage) msgObjectList.get(position);
        User userEntity = imService.getContactManager().findContactByFormId(imageMessage.getFromId());

        if (null == convertView) {
            imageRenderView = ImageRenderView.inflater(ctx, parent, isMine);
        } else {
            //需要优化
//            if(convertView instanceof ImageRenderView
//                    && (isMine == ((ImageRenderView) convertView).isMine())) {
//                imageRenderView = (ImageRenderView) convertView;
//            } else {
//                imageRenderView = ImageRenderView.inflater(ctx, parent, isMine);
//            }
            imageRenderView = ImageRenderView.inflater(ctx, parent, isMine);
        }


        final ImageView messageImage = imageRenderView.getMessageImage();
        final int msgId = imageMessage.getMsgId();
        imageRenderView.setBtnImageListener(new ImageRenderView.BtnImageListener() {
            @Override
            public void onMsgFailure() {
                /**
                 * 多端同步也不会拉到本地失败的数据
                 * 只有isMine才有的状态，消息发送失败
                 * 1. 图片上传失败。点击图片重新上传??[也是重新发送]
                 * 2. 图片上传成功，但是发送失败。 点击重新发送??
                 */
                if (FileUtil.isSdCardAvailuable()) {
                    imageMessage.setStatus(MessageConstant.MSG_SENDING);
                    if (imService != null) {
                        imService.getMessageManager().resendMessage(imageMessage);
                    }
                    updateItemState(msgId, imageMessage);
                } else {
                    Toast.makeText(ctx, ctx.getString(R.string.sdcard_unavaluable), Toast.LENGTH_LONG).show();
                }
            }

            //DetailPortraitActivity 以前用的是DisplayImageActivity 这个类
            @Override
            public void onMsgSuccess() {
                Intent i = new Intent(ctx, PreviewMessageImagesActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(IntentConstant.CUR_MESSAGE, imageMessage);
                i.putExtras(bundle);
                ctx.startActivity(i);
                ((Activity) ctx).overridePendingTransition(R.anim.tt_image_enter, R.anim.tt_stay);
            }
        });

        // 设定触发loadImage的事件
        imageRenderView.setImageLoadListener(new ImageRenderView.ImageLoadListener() {

            @Override
            public void onLoadComplete(String loaclPath) {
                logger.d("chat#pic#save image ok");
                logger.d("pic#setsavepath:%s", loaclPath);
//                imageMessage.setPath(loaclPath);//下载的本地路径不再存储
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_SUCCESS);
                updateItemState(imageMessage);

            }

            @Override
            public void onLoadFailed() {
                logger.d("chat#pic#onBitmapFailed");
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_FAILURE);
                updateItemState(imageMessage);
                logger.d("download failed");
            }
        });

        final View messageLayout = imageRenderView.getMessageLayout();
        messageImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 创建一个pop对象，然后 分支判断状态，然后显示需要的内容
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(imageMessage, position));
                boolean bResend = (imageMessage.getStatus() == MessageConstant.MSG_FAILURE)
                        || (imageMessage.getLoadStatus() == MessageConstant.IMAGE_UNLOAD);
                popup.show(messageLayout, DBConstant.SHOW_IMAGE_TYPE, bResend, isMine);
                return true;
            }
        });

        /**父类控件中的发送失败view*/
        imageRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // 重发或者重新加载
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(imageMessage, position));
                popup.show(messageLayout, DBConstant.SHOW_IMAGE_TYPE, true, isMine);
            }
        });
        imageRenderView.render(imageMessage, userEntity, ctx);

        return imageRenderView;
    }

    /**
     * msgId 是消息ID
     * localId是本地的ID
     * position 是list 的位置
     * <p/>
     * 只更新item的状态
     * 刷新单条记录
     * <p/>
     */
    public void updateItemState(int position, final MessageEntity messageEntity) {
        //更新DB
        //更新单条记录
//        imService.getDbInterface().insertOrUpdateMessage(messageEntity);
        notifyDataSetChanged();
    }

    /**
     * 对于混合消息的特殊处理
     */
    public void updateItemState(final MessageEntity messageEntity) {
        String dbId = messageEntity.getId();
        int msgId = messageEntity.getMsgId();
        int len = msgObjectList.size();
        for (int index = len - 1; index > 0; index--) {
            Object object = msgObjectList.get(index);
            if (object instanceof MessageEntity) {
                MessageEntity entity = (MessageEntity) object;
                if (object instanceof ImageMessage) {
                    ImageMessage.addToImageMessageList((ImageMessage) object);
                }
                if (entity.getId() .equals(dbId) && entity.getMsgId() == msgId) {
                    msgObjectList.set(index, messageEntity);
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    private MessageOperatePopup currentPop;

    /**
     * 点击事件的定义
     */
    private MessageOperatePopup getPopMenu(ViewGroup parent, MessageOperatePopup.OnItemClickListener listener) {
        MessageOperatePopup popupView = MessageOperatePopup.instance(ctx, parent);
        currentPop = popupView;
        popupView.setOnItemClickListener(listener);
        return popupView;
    }


    /**
     * 普通通知的渲染展示
     */
    private View normalNotifyRender(int position, View convertView, ViewGroup parent) {
        NormalNotifyRenderView normalNotifyRenderView;
        TextMessage notifyMsg = (TextMessage) msgObjectList.get(position);
        if (null == convertView) {
            normalNotifyRenderView = NormalNotifyRenderView.inflater(ctx, parent);
        } else {
            // 不用再使用tag 标签了
            if(convertView instanceof  NormalNotifyRenderView) {
                normalNotifyRenderView = (NormalNotifyRenderView) convertView;
            } else {
                normalNotifyRenderView = NormalNotifyRenderView.inflater(ctx, parent);
            }
        }
        normalNotifyRenderView.setNotifyMsg(notifyMsg.getContent());
        return normalNotifyRenderView;
    }

    public void clearItem() {
        msgObjectList.clear();
    }


    /**
     * 下拉载入历史消息,从最上面开始添加
     */
    public void loadHistoryList(final List<MessageEntity> historyList) {
        logger.d("#chatAdapter#loadHistoryList");
        if (null == historyList || historyList.size() <= 0) {
            return;
        }
        Collections.sort(historyList, new MessageTimeComparator());
        ArrayList<Object> chatList = new ArrayList<>();
        long preTime = 0;
        long nextTime = 0;
        for (MessageEntity msg : historyList) {
            if (msg.getDisplayType() == DBConstant.MSG_TYPE_SINGLE_TEXT) {
                if (isMsgGif(msg)) {
                    msg.setGIfEmo(true);
                } else {
                    msg.setGIfEmo(false);
                }
            }
            nextTime = msg.getCreated();
            boolean needTimeBubble = DateUtil.needDisplayTime(preTime, nextTime);
            if (needTimeBubble) {
                Long in = nextTime;
                chatList.add(in);
            }
            preTime = nextTime;
//            if (msg.getDisplayType() == DBConstant.SHOW_MIX_TEXT) {
//                MixMessage mixMessage = (MixMessage) msg;
//                chatList.addAll(mixMessage.getMsgList());
//            } else {
//                chatList.add(msg);
//            }
            chatList.add(msg);
        }
        // 如果是历史消息，从头开始加
        msgObjectList.addAll(0, chatList);
        getImageList();
        logger.d("#chatAdapter#addItem");
        notifyDataSetChanged();
    }

    public static class MessageTimeComparator implements Comparator<MessageEntity> {
        @Override
        public int compare(MessageEntity lhs, MessageEntity rhs) {
            if (lhs.getCreated() == rhs.getCreated()) {
                return lhs.getMsgId() - rhs.getMsgId();
            }
            return (int)(lhs.getCreated() - rhs.getCreated());
        }
    }

    public MessageEntity getTopMsgEntity() {
        if (msgObjectList.size() <= 0) {
            return null;
        }
        for (Object result : msgObjectList) {
            if (result instanceof MessageEntity) {
                return (MessageEntity) result;
            }
        }
        return null;
    }


    private class OperateItemClickListener
            implements
            MessageOperatePopup.OnItemClickListener {

        private MessageEntity mMsgInfo;
        private int mType;
        private int mPosition;

        public OperateItemClickListener(MessageEntity msgInfo, int position) {
            mMsgInfo = msgInfo;
            mType = msgInfo.getDisplayType();
            mPosition = position;
        }

        @SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
        @Override
        public void onCopyClick() {
            try {
                ClipboardManager manager = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);

                logger.d("menu#onCopyClick content:%s", mMsgInfo.getContent());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    ClipData data = ClipData.newPlainText("data", mMsgInfo.getContent());
                    manager.setPrimaryClip(data);
                } else {
                    manager.setText(mMsgInfo.getContent());
                }
            } catch (Exception e) {
                logger.e(e.getMessage());
            }
        }

        @Override
        public void onResendClick() {
            try {
                if (mType == DBConstant.SHOW_AUDIO_TYPE
                        || mType == DBConstant.SHOW_ORIGIN_TEXT_TYPE) {

                    if (mMsgInfo.getDisplayType() == DBConstant.SHOW_AUDIO_TYPE) {
                        if (StringUtils.isBlank(mMsgInfo.getSendContent())) {
                            return;
                        }
                    }
                }

                else if (mType == DBConstant.SHOW_IMAGE_TYPE) {
                    logger.d("pic#resend");
                    // 之前的状态是什么 上传没有成功继续上传
                    // 上传成功，发送消息
                    ImageMessage imageMessage = (ImageMessage) mMsgInfo;
                    if (TextUtils.isEmpty(imageMessage.getPath())) {
                        Toast.makeText(ctx, ctx.getString(R.string.image_path_unavaluable), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                mMsgInfo.setStatus(MessageConstant.MSG_SENDING);
                msgObjectList.remove(mPosition);
                addItem(mMsgInfo);
                if (imService != null) {
                    imService.getMessageManager().resendMessage(mMsgInfo);
                }

            } catch (Exception e) {
                logger.e("chat#exception:" + e.toString());
            }
        }

        @Override
        public void onSpeakerClick() {
            AudioPlayerHandler audioPlayerHandler = AudioPlayerHandler.getInstance();
            if (audioPlayerHandler.getAudioMode(ctx) == AudioManager.MODE_NORMAL) {
                audioPlayerHandler.setAudioMode(AudioManager.MODE_IN_CALL, ctx);
                ToastUtil.showSpeeker(ctx, ctx.getText(R.string.audio_in_call), Toast.LENGTH_SHORT);
            } else {
                audioPlayerHandler.setAudioMode(AudioManager.MODE_NORMAL, ctx);
                ToastUtil.showSpeeker(ctx, ctx.getText(R.string.audio_in_speeker), Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     * 获取图片消息列表
     */
    private void getImageList() {
        for (int i = msgObjectList.size() - 1; i >= 0; --i) {
            Object item = msgObjectList.get(i);
            if (item instanceof ImageMessage) {
                ImageMessage.addToImageMessageList((ImageMessage) item);
            }
        }
    }
}
