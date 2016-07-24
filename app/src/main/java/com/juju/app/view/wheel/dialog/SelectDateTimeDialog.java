package com.juju.app.view.wheel.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.juju.app.R;
import com.juju.app.view.wheel.WheelStyle;
import com.juju.app.view.wheel.WheelView;
import com.rey.material.app.BottomSheetDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class SelectDateTimeDialog extends BottomSheetDialog {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private View dialogView;
    private WheelView yearWheel;
    private WheelView monthWheel;
    private WheelView dayWheel;
    private WheelView leftWheel;
    private WheelView rightWheel;

    private TextView txtWeek;


    int selectYear;
    int selectMonth;

    private OnClickListener onClickListener;

    /**
     * 创建一个日期选择对话框
     *
     * @param mContext
     */
    public SelectDateTimeDialog(Context mContext) {
        super(mContext);
        create();
    }

    /**
     * 创建一个日期选择对话框
     *
     * @param mContext
     */
    public SelectDateTimeDialog(Context mContext, OnClickListener listener) {
        super(mContext);
        onClickListener = listener;
        create();
    }

    /**
     * 创建选择日期对话框
     */
    public void create() {

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        dialogView = layoutInflater.inflate(R.layout.dialog_wheel_date_time, null);

        txtWeek = (TextView) dialogView.findViewById(R.id.txt_week) ;

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
                showCurrentSWheelWeek();
            }
        });

        monthWheel.setWheelStyle(WheelStyle.STYLE_MONTH);
        monthWheel.setOnSelectListener(new WheelView.onSelectListener() {
            @Override
            public void onSelect(int index, String text) {
                selectMonth = index + 1;
                dayWheel.setWheelItemList(WheelStyle.createDayString(selectYear, selectMonth));
                showCurrentSWheelWeek();
            }
        });

        dayWheel.setWheelStyle(WheelStyle.STYLE_DAY);
        dayWheel.setOnSelectListener(new WheelView.onSelectListener() {
            @Override
            public void onSelect(int index, String text) {
                showCurrentSWheelWeek();
            }
        });

        leftWheel.setWheelStyle(WheelStyle.STYLE_HOUR);
        rightWheel.setWheelStyle(WheelStyle.STYLE_CUT_MINUTE);

        Button cancelBt = (Button) dialogView.findViewById(R.id.select_date_cancel);
        cancelBt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    if (!onClickListener.onCancel()) {
                        dismiss();
                    }
                } else {
                    dismiss();
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
                        dismiss();
                    }
                } else {
                    dismiss();
                }
            }
        });

        contentView(dialogView);

//        new AlertDialog.Builder(context).setView(dialogView).create();
    }

    private void showCurrentWeek(Calendar calendar) {

        switch (calendar.get(Calendar.DAY_OF_WEEK)){
            case 1:
                txtWeek.setText("星期日");
                break;
            case 2:
                txtWeek.setText("星期一");
                break;
            case 3:
                txtWeek.setText("星期二");
                break;
            case 4:
                txtWeek.setText("星期三");
                break;
            case 5:
                txtWeek.setText("星期四");
                break;
            case 6:
                txtWeek.setText("星期五");
                break;
            case 7:
                txtWeek.setText("星期六");
                break;
        }

    }

    private void showCurrentSWheelWeek() {

        int year = yearWheel.getCurrentItem() + WheelStyle.onLineYear;
        int month = monthWheel.getCurrentItem();
        int day = dayWheel.getCurrentItem() + 1;
        int daySize = dayWheel.getItemCount();
        if (day > daySize) {
            day = day - daySize;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.YEAR,year);
        calendar.set(Calendar.HOUR_OF_DAY,9);
        showCurrentWeek(calendar);

    }



    /**
     * 显示选择日期对话框
     *
     * @param year  默认显示的年
     * @param month 默认月
     * @param day   默认日
     */
    public void show(int year, int month, int day,int mHour,int mMinute) {
        dayWheel.setWheelItemList(WheelStyle.createDayString(year - WheelStyle.onLineYear, month + 1));
        yearWheel.setCurrentItem(year - WheelStyle.onLineYear);
        monthWheel.setCurrentItem(month);
        dayWheel.setCurrentItem(day - 1);
        leftWheel.setCurrentItem(mHour);
        rightWheel.setCurrentItem(mMinute);

        showCurrentSWheelWeek();
        show();
    }

    public void showTimeStr(String dateString) {

        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        showCurrentWeek(calendar);

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
        show();
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
