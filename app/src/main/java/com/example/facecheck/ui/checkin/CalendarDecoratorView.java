package com.example.facecheck.ui.checkin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CalendarDecoratorView extends View {

    private Paint textPaint;
    // Key: 日期数字(1-31), Value: 颜色
    private Map<Integer, Integer> dateDots = new HashMap<>();
    private Calendar currentMonthCalendar;
    private int firstDayOfWeek = Calendar.SUNDAY;

    public CalendarDecoratorView(Context context) {
        super(context);
        init();
    }

    public CalendarDecoratorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(getResources().getDisplayMetrics().scaledDensity * 14); // 14sp
    }

    public void setDatesWithCalendar(Map<Integer, Integer> dots, Calendar monthToDisplay, int firstDayOfWeek) {
        this.dateDots = dots != null ? dots : new HashMap<>();
        this.currentMonthCalendar = monthToDisplay;
        this.firstDayOfWeek = firstDayOfWeek;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 废弃此方案
    }
}
