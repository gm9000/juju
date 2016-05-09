package com.juju.app.service.im.provider;

import com.juju.app.service.im.iq.RedisResIQ;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * 项目名称：juju
 * 类描述：IQ消息(redis)数据绑定业务处理
 * 创建人：gm
 * 日期：2016/4/28 17:15
 * 版本：V1.0.0
 */
public class RedisPacketExtensionProvider extends IQProvider<IQ> {

    public final static String NAME = "response";
    public final static String NAMESPACE = "com:jlm:iq:redis";


    @Override
    public IQ parse(XmlPullParser parser, int initialDepth)
            throws XmlPullParserException, IOException, SmackException {
        int eventType = parser.getEventType();
        RedisResIQ redisResIq = new RedisResIQ(NAME, NAMESPACE);
        while (true) {
            if (eventType == XmlPullParser.START_TAG) {
                if ("response".equals(parser.getName())) {
                    String command = parser.getAttributeValue("", "command");
                    redisResIq.setCommand(command);
                }
            } else if (eventType == XmlPullParser.TEXT) {
                String content = parser.getText();
                redisResIq.setContent(content);
            } else if (eventType == XmlPullParser.END_TAG) {
                if (NAME.equals(parser.getName())) {
                    break;
                }
            }
            eventType = parser.next();
        }
        return redisResIq;
    }
}
