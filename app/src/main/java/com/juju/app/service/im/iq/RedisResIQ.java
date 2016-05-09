package com.juju.app.service.im.iq;

import org.jivesoftware.smack.packet.IQ;

/**
 * 项目名称：juju
 * 类描述：redis查询响应消息自定义IQ
 * 创建人：gm
 * 日期：2016/4/28 17:18
 * 版本：V1.0.0
 */
public class RedisResIQ extends IQ {

    private String command;

    private String content;


    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RedisResIQ(String childElementName, String childElementNamespace) {
        super(childElementName, childElementNamespace);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
            IQChildElementXmlStringBuilder xml) {
        xml.attribute("command", command);
        xml.append(">");
        xml.append(content);
        return xml;
    }

}
