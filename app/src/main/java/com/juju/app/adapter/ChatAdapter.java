package com.juju.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.juju.app.R;
import com.juju.app.entity.User;
import com.juju.app.entity.base.MessageEntity;
import com.juju.app.entity.chat.TextMessage;
import com.juju.app.entity.chat.UserEntity;
import com.juju.app.enums.RenderType;
import com.juju.app.golobal.DBConstant;
import com.juju.app.service.im.IMService;
import com.juju.app.tools.Emoparser;
import com.juju.app.ui.base.BaseApplication;
import com.juju.app.utils.CommonUtil;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.Logger;
import com.juju.app.view.groupchat.MessageOperatePopup;
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
    private ArrayList<Object> msgObjectList = new ArrayList<>();

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
//                    convertView = audioMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_AUDIO:
//                    convertView = audioMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_GIF_IMAGE:
//                    convertView = GifImageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_GIF_IMAGE:
//                    convertView = GifImageMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_IMAGE:
//                    convertView = imageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_IMAGE:
//                    convertView = imageMsgRender(position, convertView, parent, false);
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
//                        ImageMessage imageMessage = (ImageMessage) info;
//                        if (CommonUtil.gifCheck(imageMessage.getUrl())) {
//                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_GIF_IMAGE
//                                    : RenderType.MESSAGE_TYPE_OTHER_GIF_IMAGE;
//                        } else {
//                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_IMAGE
//                                    : RenderType.MESSAGE_TYPE_OTHER_IMAGE;
//                        }

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
//        if (msg instanceof ImageMessage) {
//            ImageMessage.addToImageMessageList((ImageMessage) msg);
//        }
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

//        timeRenderView = TimeRenderView.inflater(ctx, parent);

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
        User user = imService.getContactManager().findContact(textMessage.getFromId());


//        UserEntity userEntity = new UserEntity();
//        //测试使用
//        if(textMessage.getFromId().equals("100000001")) {
//            userEntity.setAvatar("http://img4.duitang.com/uploads/item/201511/07/20151107174431_emPdc.jpeg");
//            userEntity.setMainName("100000001");
//        } else if (textMessage.getFromId().indexOf("100000002") >= 0 ) {
//            userEntity.setAvatar("http://cdn.duitang.com/uploads/item/201511/07/20151107210255_UzQaN.thumb.700_0.jpeg");
//            userEntity.setMainName("100000002");
//        }



//        if(BaseApplication.getInstance().getUserInfoBean().getmAccount().equals("100000001")) {
//            userEntity.setAvatar("http://img4.duitang.com/uploads/item/201511/07/20151107174431_emPdc.jpeg");
//        } else {
//            userEntity.setAvatar("http://cdn.duitang.com/uploads/item/201601/08/20160108130846_MS4TW.thumb.700_0.png");
//        }



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

        // url 路径可以设定 跳转哦哦
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
//        getImageList();
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

}
