package com.example.facecheck.ui.checkin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CalendarDecoratorView extends View {

    private Paint redPaint;
    private Paint bluePaint;
    private Map<Integer, Integer> dateDots = new HashMap<>(); // Key: Day of month, Value: Color

    public CalendarDecoratorView(Context context) {
        super(context);
        init();
    }

    public CalendarDecoratorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setColor(Color.RED);

        bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bluePaint.setColor(Color.BLUE);
    }

    public void setDates(Map<Integer, Integer> dates) {
        this.dateDots = dates;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dateDots == null || dateDots.isEmpty()) {
            return;
        }

        // WARNING: These are magic numbers and might need adjustment on different devices/screen densities.
        int cellWidth = getWidth() / 7;
        int cellHeight = getHeight() / 7; // Usually 6 rows, but let's use 7 for more padding
        float dotRadius = 8f;
        float dotOffsetX = cellWidth * 0.8f; // Offset to the top-right corner
        float dotOffsetY = cellHeight * 0.2f;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 for Sunday, 1 for Monday...

        for (Map.Entry<Integer, Integer> entry : dateDots.entrySet()) {
            int day = entry.getKey();
            int color = entry.getValue();

            int position = day + firstDayOfWeek - 1;
            int row = position / 7;
            int col = position % 7;

            float cx = col * cellWidth + dotOffsetX;
            float cy = row * cellHeight + dotOffsetY;

            Paint paint = (color == Color.RED) ? redPaint : bluePaint;
            canvas.drawCircle(cx, cy, dotRadius, paint);
        }
    }
}
