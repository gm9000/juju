package com.juju.app.view.wheel;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import com.juju.app.R;

/**
 * 生成wheel的各种选项
 * <p/>
 * Created by huangzj on 2015/12/25.
 */
public class WheelStyle {

    /**
     * Wheel Style Hour
     */
    public static int STYLE_HOUR = 1;
    /**
     * Wheel Style Minute
     */
    public static int STYLE_MINUTE = 2;
    /**
     * Wheel Style Year
     */
    public static int STYLE_YEAR = 3;
    /**
     * Wheel Style Month
     */
    public static int STYLE_MONTH = 4;
    /**
     * Wheel Style Day
     */
    public static int STYLE_DAY = 5;
    /**
     * Wheel Style Light Time
     */
    public static int STYLE_LIGHT_TIME = 7;

    public static int STYPE_SHORT_YEAR = 8;

    public static int STYLE_CUT_MINUTE = 9;

    public static List<String> getItemList(Context context, int Style) {
        if (Style == STYLE_HOUR) {
            return createHourString();
        } else if (Style == STYLE_MINUTE) {
            return createMinuteString();
        } else if (Style == STYLE_CUT_MINUTE) {
            return createCUTMinuteString();
        } else if (Style == STYLE_YEAR) {
            return createYearString();
        } else if (Style == STYPE_SHORT_YEAR) {
            return createShortYearString();
        } else if (Style == STYLE_MONTH) {
            return createMonthString();
        } else if (Style == STYLE_DAY) {
            return createDayString();
        } else if (Style == STYLE_LIGHT_TIME) {
            return createWeekString(context);
        } else {
            new IllegalArgumentException("style is illegal");
        }
        return null;
    }

    private static List<String> createHourString() {
        List<String> wheelString = new ArrayList<String>();
        for (int i = 0; i < 24; i++) {
            wheelString.add(String.format("%02d 点", i));
        }
        return wheelString;
    }

    private static List<String> createMinuteString() {
        List<String> wheelString = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            wheelString.add(String.format("%02d 分", i));
        }
        return wheelString;
    }

    private static List<String> createCUTMinuteString() {
        List<String> wheelString = new ArrayList<>();
        for (int i = 0; i < 60; ) {
            wheelString.add(String.format("%02d 分", i));
            i= i + 5;
        }
        return wheelString;
    }

    public static final int minYear = 1980;
    public static final int onLineYear = 2016;
    public static final int maxYear = 2030;

    private static List<String> createYearString() {
        List<String> wheelString = new ArrayList<>();
        for (int i = minYear; i <= maxYear; i++) {
            wheelString.add("" + i);
        }
        return wheelString;
    }

    private static List<String> createShortYearString() {
        List<String> wheelString = new ArrayList<>();
        for (int i = onLineYear; i <= maxYear; i++) {
            wheelString.add(String.format("%d 年", i));
        }
        return wheelString;
    }

    private static List<String> createMonthString() {
        List<String> wheelString = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            wheelString.add(String.format("%02d 月", i));
        }
        return wheelString;
    }

    private static List<String> createDayString() {
        List<String> wheelString = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            wheelString.add(String.format("%02d 日", i));
        }
        return wheelString;
    }

    private static List<String> createWeekString(Context context) {
        List<String> wheelString = new ArrayList<>();
        String[] timeString = context.getResources().getStringArray(R.array.weeks);
        for (String week : timeString) {
            wheelString.add(week);
        }
        return wheelString;
    }

    public static List<String> createDayString(int year, int month) {
        List<String> wheelString = new ArrayList<>();
        int size;
        if (isLeapMonth(month)) {
            size = 31;
        } else if (month == 2) {
            if (isLeapYear(year)) {
                size = 29;
            } else {
                size = 28;
            }
        } else {
            size = 30;
        }

        for (int i = 1; i <= size; i++) {
            wheelString.add(String.format("%02d 日", i));
        }
        return wheelString;
    }

    /**
     * 计算闰月
     *
     * @param month
     * @return
     */
    private static boolean isLeapMonth(int month) {
        return month == 1 || month == 3 || month == 5 || month == 7
                || month == 8 || month == 10 || month == 12;
    }

    /**
     * 计算闰年
     *
     * @param year
     * @return
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }
}
