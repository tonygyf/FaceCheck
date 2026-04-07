package com.example.facecheck.ui.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircleCropOverlayView extends View {
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int cropSizePx;

    public CircleCropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dimPaint.setColor(0xA6000000);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3f);
        strokePaint.setColor(Color.WHITE);
    }

    public void setCropSize(int sizePx) {
        this.cropSizePx = Math.max(120, sizePx);
        invalidate();
    }

    public RectF getCropRectInView(int viewW, int viewH) {
        float left = (viewW - cropSizePx) / 2f;
        float top = (viewH - cropSizePx) / 2f;
        return new RectF(left, top, left + cropSizePx, top + cropSizePx);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cropSizePx <= 0) return;

        RectF rect = getCropRectInView(getWidth(), getHeight());
        float cx = rect.centerX();
        float cy = rect.centerY();
        float radius = cropSizePx / 2f;

        Path overlay = new Path();
        overlay.setFillType(Path.FillType.EVEN_ODD);
        overlay.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        overlay.addCircle(cx, cy, radius, Path.Direction.CCW);
        canvas.drawPath(overlay, dimPaint);

        canvas.drawCircle(cx, cy, radius, strokePaint);
    }
}
