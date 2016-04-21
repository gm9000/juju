package com.juju.app.enums;

/**
 * 消息渲染枚举类型
 */
public enum RenderType {
    //无效
    MESSAGE_TYPE_INVALID,
    //我的文本消息
    MESSAGE_TYPE_MINE_TETX ,
    //我的GIF
    MESSAGE_TYPE_MINE_GIF ,
    //我的图片
    MESSAGE_TYPE_MINE_IMAGE,
    //我的gif图片
    MESSAGE_TYPE_MINE_GIF_IMAGE,
    //我的语音
    MESSAGE_TYPE_MINE_AUDIO,
    //其他人的文本消息
    MESSAGE_TYPE_OTHER_TEXT,
    //其他人的GIF
    MESSAGE_TYPE_OTHER_GIF,
    //其他人的图片
    MESSAGE_TYPE_OTHER_IMAGE,
    //其他人的GIF图片
    MESSAGE_TYPE_OTHER_GIF_IMAGE,
    //其他人的语音
    MESSAGE_TYPE_OTHER_AUDIO,
    //时间
    MESSAGE_TYPE_TIME_TITLE
}
