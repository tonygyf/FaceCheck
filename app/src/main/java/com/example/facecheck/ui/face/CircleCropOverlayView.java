package com.example.facecheck.ui.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CircleCropOverlayView extends View {
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int cropSizePx;
    private float cropCenterX = -1f;
    private float cropCenterY = -1f;
    private float downOffsetX = 0f;
    private float downOffsetY = 0f;
    private boolean dragging = false;

    public CircleCropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dimPaint.setColor(0xA6000000);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3f);
        strokePaint.setColor(Color.WHITE);
    }

    public void setCropSize(int sizePx) {
        this.cropSizePx = Math.max(120, sizePx);
        ensureCenterInitialized();
        invalidate();
    }

    public RectF getCropRectInView(int viewW, int viewH) {
        if (cropCenterX < 0 || cropCenterY < 0) {
            cropCenterX = viewW / 2f;
            cropCenterY = viewH / 2f;
        }
        float half = cropSizePx / 2f;
        float minX = half;
        float maxX = Math.max(half, viewW - half);
        float minY = half;
        float maxY = Math.max(half, viewH - half);
        cropCenterX = Math.max(minX, Math.min(maxX, cropCenterX));
        cropCenterY = Math.max(minY, Math.min(maxY, cropCenterY));
        float left = cropCenterX - half;
        float top = cropCenterY - half;
        return new RectF(left, top, left + cropSizePx, top + cropSizePx);
    }

    private void ensureCenterInitialized() {
        if (cropCenterX < 0 || cropCenterY < 0) {
            cropCenterX = getWidth() / 2f;
            cropCenterY = getHeight() / 2f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && (cropCenterX < 0 || cropCenterY < 0)) {
            cropCenterX = w / 2f;
            cropCenterY = h / 2f;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cropSizePx <= 0) return false;
        RectF rect = getCropRectInView(getWidth(), getHeight());
        float radius = cropSizePx / 2f;
        float cx = rect.centerX();
        float cy = rect.centerY();
        float dx = event.getX() - cx;
        float dy = event.getY() - cy;
        boolean insideCircle = dx * dx + dy * dy <= radius * radius;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!insideCircle) return false;
                dragging = true;
                downOffsetX = event.getX() - cropCenterX;
                downOffsetY = event.getY() - cropCenterY;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) return false;
                cropCenterX = event.getX() - downOffsetX;
                cropCenterY = event.getY() - downOffsetY;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!dragging) return false;
                dragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return false;
        }
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
