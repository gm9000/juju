package com.juju.app.helper.chat;

/**
 * 项目名称：juju
 * 类描述：序列号生成器
 * 创建人：gm
 * 日期：2016/4/13 19:36
 * 版本：V1.0.0
 */
public class SequenceNumberMaker {

    private volatile short mSquence = 0;

    private volatile long preMsgId = 0;

    private static SequenceNumberMaker maker = new SequenceNumberMaker();

    private SequenceNumberMaker() {
    }

    public static SequenceNumberMaker getInstance() {
        return maker;
    }

    public short make() {
        synchronized (this) {
            mSquence++;
            if (mSquence >= Short.MAX_VALUE)
                mSquence = 1;
        }
        return mSquence;
    }

    /**依旧比较 Ugly 的解决办法 (2小时 46分 10000秒)
     * 多线程情况下，生成相同的msgId
     * */
    public int makelocalUniqueMsgId(){
        synchronized(SequenceNumberMaker.this) {
            int timeStamp = (int) (System.currentTimeMillis() % 10000000);
            int localId = timeStamp + 90000000;
            //logger.e("#yingmu2#之前的msgId:%d",preMsgId);
            if (localId == preMsgId) {
                localId++;
                if (localId >= 100000000) {
                    localId = 90000000;
                }
            }
            preMsgId = localId;
            return localId;
        }
    }

    /**比较 Ugly 的解决办法,但是比较实用*/
    public boolean isFailure(int msgId){
        if(msgId >= 90000000){
            return true;
        }
        return false;
    }

    public int makelocalUniqueMsgId(long time){
        int timeStamp = (int) (time % 10000000);
        int localId = timeStamp + 90000000;
        //logger.e("#yingmu2#之前的msgId:%d",preMsgId);
        if (localId >= 100000000) {
            localId = 90000000;
        }
        return localId;
    }
}
