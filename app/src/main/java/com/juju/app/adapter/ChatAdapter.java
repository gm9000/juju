package com.juju.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.juju.app.bean.groupchat.TimeTileMessageBean;
import com.juju.app.entity.MessageInfo;
import com.juju.app.golobal.Constants;
import com.juju.app.tools.Emoparser;
import com.juju.app.utils.CommonUtil;
import com.juju.app.utils.DateUtil;
import com.juju.app.utils.Logger;
import com.mogujie.widget.imageview.MGWebImageView;

import java.util.ArrayList;
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

    public static final int MESSAGE_TYPE_INVALID = -1;
    public static final int MESSAGE_TYPE_MINE_TETX = 0x00;
    public static final int MESSAGE_TYPE_MINE_IMAGE = 0x01;
    public static final int MESSAGE_TYPE_MINE_AUDIO = 0x02;
    public static final int MESSAGE_TYPE_OTHER_TEXT = 0x03;
    public static final int MESSAGE_TYPE_OTHER_IMAGE = 0x04;
    public static final int MESSAGE_TYPE_OTHER_AUDIO = 0x05;
    public static final int MESSAGE_TYPE_TIME_TITLE = 0x07;
    public static final int MESSAGE_TYPE_HISTORY_DIVIDER = 0x08;
    private static final int VIEW_TYPE_COUNT = 9;
    public static final String HISTORY_DIVIDER_TAG = "history_divider_tag";

    private Activity context = null;
    private LayoutInflater inflater = null;
    private ArrayList<Object> messageList = new ArrayList<Object>();

    public ChatAdapter(Activity cxt) {
        super();
        // todo eric any recycle reference issue here
        context = cxt;
        if (null != context) {
            inflater = ((Activity) context).getLayoutInflater();
        }
    }

    @Override
    public int getCount() {
        if (null == messageList) {
            return 0;
        } else {
            return messageList.size();
        }
    }

    @Override
    public Object getItem(int position) {
        if (position >= getCount() || position < 0) {
            return null;
        }
        return messageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        try {
            final int type = getItemViewType(position);
            // 所有需要被赋值的holder都是基于MessageHolderBase的
            MessageHolderBase holder = null;
//            if (convertView == null) {
                    if (type == MESSAGE_TYPE_TIME_TITLE) {
//                    convertView = inflater.inflate(R.layout.tt_message_title_time, parent, false);
                      convertView = LayoutInflater.from(context).inflate(R.layout.tt_message_title_time, parent, false);
                    // 时间
                    TimeTitleMessageHodler ttHodler = new TimeTitleMessageHodler();
                    convertView.setTag(ttHodler);
                    ttHodler.time_title = (TextView) convertView.findViewById(R.id.time_title);
                } else if (type == MESSAGE_TYPE_HISTORY_DIVIDER) {
                    // 历史消息
                    convertView = LayoutInflater.from(context).inflate(R.layout.tt_history_divider_item, parent, false);
                } else if (type == MESSAGE_TYPE_MINE_TETX) {
                   //我的消息
                    convertView = LayoutInflater.from(context).inflate(R.layout.tt_mine_text_message_item, parent, false);
                    holder = new TextMessageHolder();
                    convertView.setTag(holder);
                    fillTextMessageHolder((TextMessageHolder) holder, convertView);
                } else if (type == MESSAGE_TYPE_OTHER_TEXT) {
                    //其他人的消息
                    convertView = LayoutInflater.from(context).inflate(R.layout.tt_other_text_message_item, parent, false);
                    holder = new TextMessageHolder();
                    convertView.setTag(holder);
                    fillTextMessageHolder((TextMessageHolder) holder, convertView);
                }
//            } else {
//                if (type != MESSAGE_TYPE_TIME_TITLE) {
//                    holder = (MessageHolderBase) convertView.getTag();
//                }
//            }

            // 这些都是不需要被赋值的
            if (type == MESSAGE_TYPE_HISTORY_DIVIDER
                    || type == MESSAGE_TYPE_INVALID) {
                return convertView;
            }

            if (type == MESSAGE_TYPE_TIME_TITLE) {
                 TimeTileMessageBean msg = (TimeTileMessageBean) messageList.get(position);
                 ((TimeTitleMessageHodler) convertView.getTag()).time_title.setText(DateUtil.getTimeDiffDesc(msg.getTime()));
                return convertView;
            }
            final MessageInfo info = (MessageInfo) messageList.get(position);
            final View baseView = getBaseViewForMenu(holder, info);

            if (info.getMsgLoadState() == Constants.MESSAGE_STATE_FINISH_FAILED) {
                holder.messageFailed.setVisibility(View.VISIBLE);
                holder.messageFailed.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        logger.d("debug#onClick, msg:%s", info);
                        if (!info.isMy() && info.isImage()) {
                            logger.d("debug#pic#found failed receiving image message");
                            updateItemState(info.msgId, Constants.MESSAGE_STATE_UNLOAD);
                        }
                        int menuType = getMenuType(info);
                        if (menuType > 0) {
                            // logger.d("debug#showMenu  MessageInfo:%s", info);
                            showMenu(context, menuType, parent, info, baseView);
                        }
                    }
                });
            } else {
                holder.messageFailed.setVisibility(View.GONE);
            }
            if (info.getMsgLoadState() == Constants.MESSAGE_STATE_LOADDING
            && !info.isImage()) {
            holder.loadingProgressBar.setVisibility(View.VISIBLE);
                } else {
                    holder.loadingProgressBar.setVisibility(View.GONE);
                }
                if (type == MESSAGE_TYPE_MINE_TETX
                        || type == MESSAGE_TYPE_OTHER_TEXT) {
                    handleTextMessage((TextMessageHolder) holder, info, parent);
                }
                return convertView;
            } catch (Exception e) {
                if (null != e && null != logger) {
                    logger.e("chat#%s", e);
                }
                return null;
            }
    }


    @Override
    public int getItemViewType(int position) {
        try {
            if (position >= messageList.size()) {
                return MESSAGE_TYPE_INVALID;
            }
            Object obj = messageList.get(position);
            if (obj instanceof TimeTileMessageBean) {
                return MESSAGE_TYPE_TIME_TITLE;
            } else if (obj instanceof String && obj.equals(HISTORY_DIVIDER_TAG)) {
                return MESSAGE_TYPE_HISTORY_DIVIDER;
            } else {
                MessageInfo info = (MessageInfo) obj;
                if (info.getMsgFromUserId().equals("")) {
                    if (info.getDisplayType() == Constants.DISPLAY_TYPE_TEXT) {
                        return MESSAGE_TYPE_MINE_TETX;
                    } else if (info.getDisplayType() == Constants.DISPLAY_TYPE_IMAGE) {
                        return MESSAGE_TYPE_MINE_IMAGE;
                    } else if (info.getDisplayType() == Constants.DISPLAY_TYPE_AUDIO) {
                        return MESSAGE_TYPE_MINE_AUDIO;
                    }
                } else {
                    if (info.getDisplayType() == Constants.DISPLAY_TYPE_TEXT) {
                        return MESSAGE_TYPE_OTHER_TEXT;
                    } else if (info.getDisplayType() == Constants.DISPLAY_TYPE_IMAGE) {
                        return MESSAGE_TYPE_OTHER_IMAGE;
                    } else if (info.getDisplayType() == Constants.DISPLAY_TYPE_AUDIO) {
                        return MESSAGE_TYPE_OTHER_AUDIO;
                    }
                }
            }
            return MESSAGE_TYPE_INVALID;
        } catch (Exception e) {
            logger.e(e.getMessage());
            return MESSAGE_TYPE_INVALID;
        }
    }
    private static class MessageHolderBase {
        /**
         * 头像
         */
        MGWebImageView portrait;

        /**
         * 消息状态
         */
        ImageView messageFailed;

        ProgressBar loadingProgressBar;

        TextView name;
    }

    private static class TimeTitleMessageHodler  {
        TextView time_title;
    }

    private static class TextMessageHolder extends MessageHolderBase {
        /**
         * 文字消息体
         */
        TextView message_content;
    }

    private void fillTextMessageHolder(TextMessageHolder holder,
                                       View convertView) {
        fillBaseMessageholder(holder, convertView);

        holder.message_content = (TextView) convertView.findViewById(R.id.message_content);
    }

    private void fillBaseMessageholder(MessageHolderBase holder,
                                       View convertView) {
        holder.portrait = (MGWebImageView) convertView.findViewById(R.id.user_portrait);
        holder.messageFailed = (ImageView) convertView.findViewById(R.id.message_state_failed);
        holder.loadingProgressBar = (ProgressBar) convertView.findViewById(R.id.progressBar1);

        holder.name = (TextView) convertView.findViewById(R.id.userNoTxt);
        logger.d("name#holder.name:%s", holder.name);
    }

    private View getBaseViewForMenu(MessageHolderBase holder, MessageInfo msg) {
        if (msg.getDisplayType() == Constants.DISPLAY_TYPE_TEXT) {
            return ((TextMessageHolder) holder).message_content;
        }
//        else if (msg.getDisplayType() == Constants.DISPLAY_TYPE_IMAGE) {
//            return ((ImageMessageHolder) holder).message_layout;
//        } else if (msg.getDisplayType() == Constants.DISPLAY_TYPE_AUDIO) {
//            return ((AudioMessageHolder) holder).message_layout;
//        }

        else {
            return null;
        }
    }



    /**
     * @Description 由消息id去修改消息状态（用于只有id的情况）
     * @param msgId
     * @param state
     */
    public void updateItemState(String msgId, int state) {
        try {

            String stackTraceString = Log.getStackTraceString(new Throwable());
            stackTraceString.replaceAll("\n", "###");
            // logger.d("debug#updateItemState stack:%s", stackTraceString);
            MessageInfo msgInfo = getMsgInfo(msgId);
            if (msgInfo == null) {
                logger.e("chat#error can't find msgInfo:%s", msgId);
                return;
            }
            msgInfo.setMsgLoadState(state);
            // logger.d("debug#updateItemState,  msg:%s", msgInfo);
            notifyDataSetChanged();
        } catch (Exception e) {
            logger.e(e.getMessage());
        }
    }

    private MessageInfo getMsgInfo(String msgId) {
        for (Object obj : messageList) {
            if (!(obj instanceof MessageInfo)) {
                continue;
            }
            MessageInfo msgInfo = (MessageInfo) obj;

            if (msgInfo.msgId.equals(msgId)) {
                return msgInfo;
            }
        }

        return null;
    }

    private int getMenuType(MessageInfo msg) {
        if (msg.getDisplayType() == Constants.DISPLAY_TYPE_TEXT) {
            return Constants.POPUP_MENU_TYPE_TEXT;
        } else if (msg.getDisplayType() == Constants.DISPLAY_TYPE_IMAGE) {
            return Constants.POPUP_MENU_TYPE_IMAGE;
        } else if (msg.getDisplayType() == Constants.DISPLAY_TYPE_AUDIO) {
            return Constants.POPUP_MENU_TYPE_AUDIO;
        } else {
            return -1;
        }
    }

    /**
     * @Description 显示菜单
     * @param cxt
     * @param msg
     */
    private void showMenu(Context cxt, int menuType, View parent,
                          MessageInfo msg, View layout) {
//        boolean bIsSelf = msg.getMsgFromUserId().equals(CacheHub.getInstance().getLoginUserId());
//        OperateItemClickListener listener = new OperateItemClickListener(menuType);
//        listener.setMessageInfo(msg);
//
//        MessageOperatePopup popupView = new MessageOperatePopup(context, parent);
//        popupView.setOnItemClickListener(listener);
//        currentPopupView = popupView;
//
//        boolean bResend = msg.getMsgLoadState() == SysConstant.MESSAGE_STATE_FINISH_FAILED;
//        popupView.show(layout, menuType, bResend, bIsSelf);

    }

    private void handleTextMessage(final TextMessageHolder holder,
                                   final MessageInfo info, final View parent) {
        if (null == holder || null == info) {
            return;
        }
        holder.message_content.setText(Emoparser.getInstance(context).emoCharsequence(info.getMsgContent()));

        holder.message_content.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                showMenu(context, Constants.POPUP_MENU_TYPE_TEXT, parent, info, holder.message_content);
                return true;
            }
        });

        holder.message_content.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                CommonUtil.skipLink(context, info.getMsgContent());
            }
        });
        holder.message_content.setOnTouchListener(new onDoubleClick(info.getMsgContent()));
    }

    /**
     * @Description 双击事件
     * @author Nana
     * @date 2014-7-30
     */
    class onDoubleClick implements View.OnTouchListener {
        int count = 0;
        int firClick = 0;
        int secClick = 0;
        String content = null;

        private onDoubleClick(String txt) {
            content = txt;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (MotionEvent.ACTION_DOWN == event.getAction()) {
                count++;
                if (count == 1) {
                    firClick = (int) System.currentTimeMillis();

                } else if (count == 2) {
                    secClick = (int) System.currentTimeMillis();
                    if (secClick - firClick < 1000) {
                        if (v.getId() == R.id.message_content) {
//                            Intent intent = new Intent(context, PreviewTextActivity.class);
//                            intent.putExtra(SysConstant.PREVIEW_TEXT_CONTENT, content);
//                            context.startActivity(intent);
                        }
                    }
                    count = 0;
                    firClick = 0;
                    secClick = 0;
                }
            }
            return false;
        }
    }

    /**
     * 历史消息的添加请千万走这个函数
     *
     * @param fromStart
     * @param list
     */
    public void addItem(boolean fromStart, List<MessageInfo> list) {
        // logger.d("debug#dump addItem fromStart msgList");
        int index = 0;
        for (MessageInfo msgInfo : list) {
            // logger.d("debug#index:%d, msg:%s", index, msgInfo);
            ++index;
        }

        try {
            if (null == list || list.size() == 0) {
                return;
            }
            // 如果是历史消息，从头开始加
            messageList.addAll(fromStart ? 0 : messageList.size(), list);

            // 先取出需要加进去的数据数量
            final int count = list.size();
            // logger.d("debug#list size:%d", count);

            if (fromStart) {
                // 因为第一次插入历史数据的时候，需要插入一条divider数据，所以后移的偏移量是count + 1
                // updatePositonSeqMap(0, mHistoryFirstAdd ? count + 1 : count);
                // msgIndexMap.fix(0, count);
            }
            // 从加进去的那个info开始，赋值各条消息的状态
            for (int i = 0; i < count; i++) {
                MessageInfo info = (MessageInfo) list.get(i);
                if (null == info) {
                    continue;
                }
                // msgIndexMap.put(info.msgId, fromStart ? i : count + i);
            }

            int position = count - 1;
            while (position >= 0) {
                Object obj = messageList.get(position);
                if (!(obj instanceof MessageInfo)) {
                    position--;
                    continue;
                }

                MessageInfo preInfo = null;
                for (int i = position - 1; i >= 0; i--) {
                    if (messageList.get(i) instanceof MessageInfo) {
                        preInfo = (MessageInfo) messageList.get(i);
                        break;
                    }
                }

                MessageInfo info = (MessageInfo) obj;

                if (DateUtil.needDisplayTime(null == preInfo
                        ? null
                        : preInfo.getMsgCreateTime(), info.getMsgCreateTime())) {
                    // 如果当前位置已经有了time title，就不需要再加了
                    if (!(messageList.get(position) instanceof TimeTileMessageBean)) {
                        TimeTileMessageBean timeTile = new TimeTileMessageBean();
                        timeTile.setTime(info.getMsgCreateTime());

                        // 更新一下状态位置映射
                        // msgIndexMap.fix(position, 1);
                        messageList.add(position, timeTile);
                    }
                }

                position--;
            }
            // if (mHistoryFirstAdd) {
            // mHistoryFirstAdd = false;
            // }
        } catch (Exception e) {
            logger.e(e.getMessage());
        }
    }

    /**
     * 这个函数只能从末尾添加
     *
     * @param info
     *
     */
    public void addItem(MessageInfo info) {
        logger.d("chat#addItem");
        try {
            if (null == info || messageList.contains(info)) {
                logger.d("chat#already has this item");
                return;
            }
            messageList.add(info);
            final int count = messageList.size();
            MessageInfo preInfo = null;
            for (int i = count - 2; i >= 0; i--) {
                if (messageList.get(i) instanceof MessageInfo) {
                    preInfo = (MessageInfo) messageList.get(i);
                    break;
                }
            }

            if (DateUtil.needDisplayTime(null == preInfo
                    ? null
                    : preInfo.getMsgCreateTime(), info.getMsgCreateTime())) {
                TimeTileMessageBean timeTitle = new TimeTileMessageBean();
                timeTitle.setTime(info.getMsgCreateTime());
                // msgIndexMap.fix(count - 1, 1);
                messageList.add(count - 1, timeTitle);
            }

            logger.d("chat#finish add item");
        } catch (Exception e) {
            logger.e("chat#find exception:%s", e.getMessage());
            logger.e(e.getMessage());
        }
    }

}
