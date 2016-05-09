
package com.juju.app.utils;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class DateUtil {

    private static Date preRefreshDateTime;

    public static boolean toRefresh() {
        Date curdate = new Date();
        if (null == preRefreshDateTime) {
            preRefreshDateTime = curdate;
            return true;
        }

        long timediff = (curdate.getTime() - preRefreshDateTime.getTime());
        preRefreshDateTime = curdate;
        return (timediff >= 60 * 60 * 1000);
    }

    @SuppressLint("SimpleDateFormat")
    public static Date getDate(Long dateLong) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String d = format.format(dateLong);
        Date date;
        try {
            date = format.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return date;
    }

    public static boolean needDisplayTime(int predateTime, int curdateTime) {
        long timediff = (curdateTime - predateTime);
        return (timediff >= 5 * 60 );
    }

    public static boolean needDisplayTime(long predateTime, long curdateTime) {
        long timediff = (curdateTime - predateTime);
        return (timediff >= 5 * 60 * 1000 );
    }

    public static String getTimeDiffDesc(Date date) {

        if (date == null) {
            return null;
        }

        String strDesc = null;
        Calendar curCalendar = Calendar.getInstance();
        Date curDate = new Date();
        curCalendar.setTime(curDate);
        Calendar thenCalendar = Calendar.getInstance();
        thenCalendar.setTime(date);

        String[] weekDays = {
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
        };
        int w = thenCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        // SimpleDateFormat format = new
        // SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar current = Calendar.getInstance();
        Calendar today = Calendar.getInstance(); // 今天
        today.set(Calendar.YEAR, current.get(Calendar.YEAR));
        today.set(Calendar.MONTH, current.get(Calendar.MONTH));
        today.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        // Date datetoday = today.getTime();
        // System.out.println(format.format(datetoday));

        Calendar yesterday = Calendar.getInstance(); // 昨天
        yesterday.setTime(curDate);
        yesterday.add(Calendar.DATE, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        // Date dateyestoday = yesterday.getTime();
        // System.out.println(format.format(dateyestoday));

        Calendar sevendaysago = Calendar.getInstance(); // 7天
        sevendaysago.setTime(curDate);
        sevendaysago.add(Calendar.DATE, -7);
        sevendaysago.set(Calendar.HOUR_OF_DAY, 0);
        sevendaysago.set(Calendar.MINUTE, 0);
        sevendaysago.set(Calendar.SECOND, 0);
        // Date datesevenago = sevendaysago.getTime();
        // System.out.println(format.format(datesevenago));
        /*
         * Date tasktime = yesterday.getTime(); SimpleDateFormat df=new
         * SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         * System.out.println(df.format(tasktime));
         */

        int thenMonth = thenCalendar.get(Calendar.MONTH);
        int thenDay = thenCalendar.get(Calendar.DAY_OF_MONTH);
        int h = thenCalendar.get(Calendar.HOUR_OF_DAY);
        int m = thenCalendar.get(Calendar.MINUTE);
        String sh = "", sm = "";
        if (h < 10)
            sh = "0";

        if (m < 10)
            sm = "0";
        if (thenCalendar.after(today))// today
        {
            if (h < 6) {
                strDesc = "凌晨 " + sh + h + " : " + sm + m;
            } else if (h < 12) {
                strDesc = "上午 " + sh + h + " : " + sm + m;
            } else if (h < 13) {
                strDesc = "下午 " + h + " : " + sm + m;
            } else if (h < 19) {
                strDesc = "下午 " + (h - 12) + " : " + sm + m;
            } else {
                strDesc = "晚上 " + (h - 12) + " : " + sm + m;
            }
        } else if (thenCalendar.before(today) && thenCalendar.after(yesterday)) {// yestoday
            // System.out.println("yestoday");
            if (h < 6) {
                strDesc = "昨天凌晨 " + sh + h + " : " + sm + m;
            } else if (h < 12) {
                strDesc = "昨天上午 " + sh + h + " : " + sm + m;
            } else if (h < 13) {
                strDesc = "昨天下午 " + h + " : " + sm + m;
            } else if (h < 19) {
                strDesc = "昨天下午 " + (h - 12) + " : " + sm + m;
            } else {
                strDesc = "昨天晚上 " + (h - 12) + " : " + sm + m;
            }
        } else if (thenCalendar.before(yesterday)
                && thenCalendar.after(sevendaysago)) {// 2 ~ 7days ago
            // System.out.println("2~7");
            if (h < 6) {
                strDesc = weekDays[w] + "凌晨 " + sh + h + " : " + sm + m;
            } else if (h < 12) {
                strDesc = weekDays[w] + "上午 " + sh + h + " : " + sm + m;
            } else if (h < 13) {
                strDesc = weekDays[w] + "下午 " + h + " : " + sm + m;
            } else if (h < 19) {
                strDesc = weekDays[w] + "下午 " + (h - 12) + " : " + sm + m;
            } else {
                strDesc = weekDays[w] + "晚上 " + (h - 12) + " : " + sm + m;
            }
        } else {
            // System.out.println("7~");
            if (h < 6) {
                strDesc = (thenMonth + 1) + "月" + thenDay + "日" + "凌晨 " + sh
                        + h + " : " + sm + m;
            } else if (h < 12) {
                strDesc = (thenMonth + 1) + "月" + thenDay + "日" + "上午 " + sh
                        + h + " : " + sm + m;
            } else if (h < 13) {
                strDesc = (thenMonth + 1) + "月" + thenDay + "日" + "下午 " + h
                        + " : " + sm + m;
            } else if (h < 19) {
                strDesc = (thenMonth + 1) + "月" + thenDay + "日" + "下午 "
                        + (h - 12) + " : " + sm + m;
            } else {
                strDesc = (thenMonth + 1) + "月" + thenDay + "日" + "晚上 "
                        + (h - 12) + " : " + sm + m;
            }
        }
        // System.out.println(strDesc);
        return strDesc;
    }

    public static String getTimeDisplay(Date date) {

        if (date == null) {
            return null;
        }

        String strDesc = null;
        Calendar curCalendar = Calendar.getInstance();
        Date curDate = new Date();
        curCalendar.setTime(curDate);
        Calendar thenCalendar = Calendar.getInstance();
        thenCalendar.setTime(date);

        String[] weekDays = {
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
        };
        int w = thenCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        // SimpleDateFormat format = new
        // SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar current = Calendar.getInstance();
        Calendar today = Calendar.getInstance(); // 今天
        today.set(Calendar.YEAR, current.get(Calendar.YEAR));
        today.set(Calendar.MONTH, current.get(Calendar.MONTH));
        today.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        Calendar yesterday = Calendar.getInstance(); // 昨天
        yesterday.setTime(curDate);
        yesterday.add(Calendar.DATE, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);

        Calendar sevendaysago = Calendar.getInstance(); // 7天
        sevendaysago.setTime(curDate);
        sevendaysago.add(Calendar.DATE, -7);
        sevendaysago.set(Calendar.HOUR_OF_DAY, 0);
        sevendaysago.set(Calendar.MINUTE, 0);
        sevendaysago.set(Calendar.SECOND, 0);

        long timediff = (curDate.getTime() - date.getTime());
        int theYear = thenCalendar.get(Calendar.YEAR);
        int thenMonth = thenCalendar.get(Calendar.MONTH);
        int thenDay = thenCalendar.get(Calendar.DAY_OF_MONTH);
        if (thenCalendar.after(today))// today
        {
            if (timediff < 5 * 60 * 1000) {
                strDesc = "刚刚";
            } else if (timediff < 60 * 60 * 1000) {
                strDesc = (timediff / 60 / 1000) + "分钟之前";
            } else {
                strDesc = (timediff / 3600 / 1000) + "小时之前";
            }
        } else if (thenCalendar.before(today) && thenCalendar.after(yesterday)) {// yestoday
            strDesc = "昨天";
        } else if (thenCalendar.before(yesterday)
                && thenCalendar.after(sevendaysago)) {// 2 ~ 7days ago
            strDesc = weekDays[w];
        } else {
            strDesc = "" + (theYear - 2000) + "-" + (thenMonth + 1) + "-"
                    + thenDay;
        }
        return strDesc;
    }

    public static String getTime4TimeTitle(Date date) {
        // M:月 d:天 a:上午或下午 h:12小时制的小时 m:分钟
        SimpleDateFormat format = new SimpleDateFormat("MM-dd a hh:mm",
                Locale.CHINA);
        return format.format(date);
    }

    public static int getCurTimeStamp() {

        return (int) (System.currentTimeMillis() / 1000);

    }

    public static String getSessionTime(Long mTimeStamp) {
        if (mTimeStamp <= 0) {
            return null;
        }
        String[] weekDays = {
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
        };
        String strDesc = null;
        SimpleDateFormat formatYear = new SimpleDateFormat("yy/MM/dd");
        SimpleDateFormat formatToday = new SimpleDateFormat("HH:mm");
        /**消息时间戳*/
        long changeTime = (long) mTimeStamp;
        long messageTimeStamp = changeTime;
        /**当前的时间戳*/
        long currentTimeStamp =System.currentTimeMillis();
        /**获取今天的 0 点时间戳*/
        long todayTimeStamp = getTimesmorning();
        /**获取 上一周 0点时间戳*/
        long rangeWeekStamp = todayTimeStamp - 86400000*6;

        /**今天的显示 hh:mm   (今天星期三)
         * 昨天
         * 星期一
         * 星期日 、 星期六、 星期五、星期四
         * yy-hh-mm
         * */
        do{
            long diff = currentTimeStamp -  messageTimeStamp;
            long diffToday = currentTimeStamp - todayTimeStamp;
            /**今天之内的*/
            if(diff < diffToday){
                strDesc = formatToday.format(messageTimeStamp);
                break;
            }

            long diffWeek = currentTimeStamp - rangeWeekStamp;
            /**最近一周的判断*/
            if(diff < diffWeek){
                /**昨天零点的时间*/
                long yesterday = todayTimeStamp - 86400000;
                long diffYesterday = currentTimeStamp - yesterday;
                if(diff < diffYesterday){
                    strDesc = "昨天";
                }else{
                    Calendar weekCal = Calendar.getInstance();
                    weekCal.setTimeInMillis(messageTimeStamp);
                    int w =  weekCal.get(Calendar.DAY_OF_WEEK) -1;
                    w = w<0?0:w;
                    strDesc = weekDays[w];
                }
                break;
            }
            /**年月日显示*/
            strDesc = formatYear.format(messageTimeStamp);
        }while(false);
        return strDesc;
    }


    public static String getSessionTime(int mTimeStamp) {
        if (mTimeStamp <= 0) {
            return null;
        }
        String[] weekDays = {
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
        };
        String strDesc = null;
        SimpleDateFormat formatYear = new SimpleDateFormat("yy/MM/dd");
        SimpleDateFormat formatToday = new SimpleDateFormat("HH:mm");
        /**消息时间戳*/
        long changeTime = (long) mTimeStamp;
        long messageTimeStamp = changeTime * 1000;
        /**当前的时间戳*/
        long currentTimeStamp =System.currentTimeMillis();
        /**获取今天的 0 点时间戳*/
        long todayTimeStamp = getTimesmorning();
        /**获取 上一周 0点时间戳*/
        long rangeWeekStamp = todayTimeStamp - 86400000*6;

        /**今天的显示 hh:mm   (今天星期三)
         * 昨天
         * 星期一
         * 星期日 、 星期六、 星期五、星期四
         * yy-hh-mm
         * */
        do{
            long diff = currentTimeStamp -  messageTimeStamp;
            long diffToday = currentTimeStamp - todayTimeStamp;
            /**今天之内的*/
            if(diff < diffToday){
                strDesc = formatToday.format(messageTimeStamp);
                break;
            }

            long diffWeek = currentTimeStamp - rangeWeekStamp;
            /**最近一周的判断*/
            if(diff < diffWeek){
                /**昨天零点的时间*/
                long yesterday = todayTimeStamp - 86400000;
                long diffYesterday = currentTimeStamp - yesterday;
                if(diff < diffYesterday){
                    strDesc = "昨天";
                }else{
                    Calendar weekCal = Calendar.getInstance();
                    weekCal.setTimeInMillis(messageTimeStamp);
                    int w =  weekCal.get(Calendar.DAY_OF_WEEK) -1;
                    w = w<0?0:w;
                    strDesc = weekDays[w];
                }
                break;
            }
            /**年月日显示*/
            strDesc = formatYear.format(messageTimeStamp);
        }while(false);
        return strDesc;
    }

    /**
     * 获取当天 零点的时间戳【linux】
     * @return
     */
    public  static long getTimesmorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }



    public static String getPastTimeDisplay(Date time){
        Date now = new Date();
        StringBuffer sb = new StringBuffer();
        long l=now.getTime()-time.getTime();
        long day=l/(24*60*60*1000);
        long hour=(l/(60*60*1000)-day*24);
        long min=((l/(60*1000))-day*24*60-hour*60);
        long s=(l/1000-day*24*60*60-hour*60*60-min*60);

        if(day > 0) {
            sb.append(day + "天");
        }else if(hour > 0 ) {
            sb.append(hour + "小时");
        }else if(min > 0 ) {
            sb.append(min + "分钟");
        }else {
            sb.append(s + "秒");
        }
        sb.append("前");
        return sb.toString();
    }
}
