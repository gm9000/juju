package com.juju.app.view.wheel.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.juju.app.R;
import com.juju.app.view.wheel.WheelStyle;
import com.juju.app.view.wheel.WheelView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期选择对话框
 * <p/>
 * Created by huangzj on 2015/10/25.
 */
public class SelectDateTimeDialog extends BaseDialog {

    private View dialogView;
    private WheelView yearWheel;
    private WheelView monthWheel;
    private WheelView dayWheel;
    private WheelView leftWheel;
    private WheelView rightWheel;


    int selectYear;
    int selectMonth;

    private OnClickListener onClickListener;

    /**
     * 创建一个日期选择对话框
     *
     * @param mContext
     */
    public SelectDateTimeDialog(Context mContext) {
        this.context = mContext;
        create();
    }

    /**
     * 创建一个日期选择对话框
     *
     * @param mContext
     */
    public SelectDateTimeDialog(Context mContext, OnClickListener listener) {
        this.context = mContext;
        onClickListener = listener;
        create();
    }

    /**
     * 创建选择日期对话框
     */
    private void create() {

        if (dialog != null) {
            return;
        }

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        dialogView = layoutInflater.inflate(R.layout.dialog_wheel_date_time, null);
        yearWheel = (WheelView) dialogView.findViewById(R.id.select_date_wheel_year_wheel);
        monthWheel = (WheelView) dialogView.findViewById(R.id.select_date_month_wheel);
        dayWheel = (WheelView) dialogView.findViewById(R.id.select_date_day_wheel);

        leftWheel = (WheelView) dialogView.findViewById(R.id.select_time_wheel_left);
        rightWheel = (WheelView) dialogView.findViewById(R.id.select_time_wheel_right);

        yearWheel.setWheelStyle(WheelStyle.STYPE_SHORT_YEAR);
        yearWheel.setOnSelectListener(new WheelView.onSelectListener() {
            @Override
            public void onSelect(int index, String text) {
                selectYear = index + WheelStyle.onLineYear;
                dayWheel.setWheelItemList(WheelStyle.createDayString(selectYear, selectMonth));
            }
        });

        monthWheel.setWheelStyle(WheelStyle.STYLE_MONTH);
        monthWheel.setOnSelectListener(new WheelView.onSelectListener() {
            @Override
            public void onSelect(int index, String text) {
                selectMonth = index + 1;
                dayWheel.setWheelItemList(WheelStyle.createDayString(selectYear, selectMonth));
            }
        });

        dayWheel.setWheelStyle(WheelStyle.STYLE_DAY);

        leftWheel.setWheelStyle(WheelStyle.STYLE_HOUR);
        rightWheel.setWheelStyle(WheelStyle.STYLE_CUT_MINUTE);

        Button cancelBt = (Button) dialogView.findViewById(R.id.select_date_cancel);
        cancelBt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    if (!onClickListener.onCancel()) {
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }
        });
        Button sureBt = (Button) dialogView.findViewById(R.id.select_date_sure);
        sureBt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int year = yearWheel.getCurrentItem() + WheelStyle.onLineYear;
                int month = monthWheel.getCurrentItem();
                int day = dayWheel.getCurrentItem() + 1;
                int daySize = dayWheel.getItemCount();
                if (day > daySize) {
                    day = day - daySize;
                }

                int hour = leftWheel.getCurrentItem();
                int minute = rightWheel.getCurrentItem()*5;

                if (onClickListener != null) {
                    if (!onClickListener.onSure(year, month, day,hour,minute )) {
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }
        });

        dialog = new Dialog(context, R.style.dialog_fullscreen_style);
        dialog.setContentView(dialogView);

//        new AlertDialog.Builder(context).setView(dialogView).create();
    }

    /**
     * 显示选择日期对话框
     *
     * @param year  默认显示的年
     * @param month 默认月
     * @param day   默认日
     */
    public void show(int year, int month, int day,int mHour,int mMinute) {
        if (dialog == null || dialog.isShowing()) {
            return;
        }
        dayWheel.setWheelItemList(WheelStyle.createDayString(year - WheelStyle.onLineYear, month + 1));
        yearWheel.setCurrentItem(year - WheelStyle.onLineYear);
        monthWheel.setCurrentItem(month);
        dayWheel.setCurrentItem(day - 1);
        leftWheel.setCurrentItem(mHour);
        rightWheel.setCurrentItem(mMinute);
        dialog.show();
    }

    public void showTimeStr(String dateString) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (dialog == null || dialog.isShowing()) {
            return;
        }
        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int mHour = calendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = calendar.get(Calendar.MINUTE);

        dayWheel.setWheelItemList(WheelStyle.createDayString(year - WheelStyle.onLineYear, month + 1));
        yearWheel.setCurrentItem(year - WheelStyle.onLineYear);
        monthWheel.setCurrentItem(month);
        dayWheel.setCurrentItem(day - 1);
        leftWheel.setCurrentItem(mHour);
        rightWheel.setCurrentItem(mMinute/5);
        dialog.show();
    }


    /**
     * 选择日期对话框回调
     *
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
        onClickListener = listener;
    }

    /**
     * 选择日期对话框回调接口，调用者实现
     *
     * @author huangzj
     */
    public interface OnClickListener {
        boolean onSure(int year, int month, int day, int hour, int minute);
        boolean onCancel();
    }
}