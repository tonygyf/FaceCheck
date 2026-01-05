package com.example.facecheck.ui.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.util.Log;

import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;

public class TouchImageView extends AppCompatImageView {
    private Matrix matrix;
    private int mode = NONE;

    // Remember some things for zooming
    private PointF last = new PointF();
    private PointF start = new PointF();
    private float minScale = 0.5f;
    private float maxScale = 4.0f;
    private float[] m;

    private int viewWidth, viewHeight;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final int DRAW = 3; // New Drawing Mode

    private float saveScale = 1f;
    private float origWidth, origHeight;
    private int oldMeasuredWidth, oldMeasuredHeight;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    // Drawing variables
    private boolean isDrawingMode = false;
    private PointF drawStart = new PointF();
    private PointF drawEnd = new PointF();
    private List<RectF> faceRects = new ArrayList<>(); // Store rects in Bitmap Coordinates
    private RectF currentDrawingRect = null;
    private Paint boxPaint;

    public TouchImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    private void sharedConstructing(Context context) {
        super.setClickable(true);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        matrix = new Matrix();
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        // Init Paint
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);

        setOnTouchListener((v, event) -> {
            if (isDrawingMode) {
                return handleDrawingTouch(event);
            }
            // Normal Zoom/Pan logic
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);
            PointF curr = new PointF(event.getX(), event.getY());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    last.set(curr);
                    start.set(last);
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        float deltaX = curr.x - last.x;
                        float deltaY = curr.y - last.y;
                        float fixTransX = getFixDragTrans(deltaX, viewWidth, origWidth * saveScale);
                        float fixTransY = getFixDragTrans(deltaY, viewHeight, origHeight * saveScale);
                        matrix.postTranslate(fixTransX, fixTransY);
                        fixTrans();
                        last.set(curr.x, curr.y);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mode = NONE;
                    int xDiff = (int) Math.abs(curr.x - start.x);
                    int yDiff = (int) Math.abs(curr.y - start.y);
                    if (xDiff < 3 && yDiff < 3)
                        performClick();
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }

            setImageMatrix(matrix);
            invalidate();
            return true;
        });
    }

    private boolean handleDrawingTouch(MotionEvent event) {
        PointF curr = new PointF(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawStart.set(curr);
                drawEnd.set(curr);
                mode = DRAW;
                currentDrawingRect = new RectF();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAW) {
                    drawEnd.set(curr);
                    // Update current rect (View coords)
                    currentDrawingRect.set(
                            Math.min(drawStart.x, drawEnd.x),
                            Math.min(drawStart.y, drawEnd.y),
                            Math.max(drawStart.x, drawEnd.x),
                            Math.max(drawStart.y, drawEnd.y));
                    invalidate(); // Trigger Draw
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mode == DRAW) {
                    mode = NONE;
                    // Validate Rect size
                    if (currentDrawingRect != null && currentDrawingRect.width() > 10
                            && currentDrawingRect.height() > 10) {
                        // Convert to Bitmap Coords
                        RectF bitmapRect = mapRectToBitmap(currentDrawingRect);
                        if (bitmapRect != null) {
                            faceRects.add(bitmapRect);
                        }
                    }
                    currentDrawingRect = null;
                    invalidate();
                }
                break;
        }
        return true;
    }

    private RectF mapRectToBitmap(RectF viewRect) {
        Matrix inverse = new Matrix();
        if (matrix.invert(inverse)) {
            RectF bitmapRect = new RectF();
            inverse.mapRect(bitmapRect, viewRect);

            // Constrain
            Drawable drawable = getDrawable();
            if (drawable != null) {
                bitmapRect.left = Math.max(0, bitmapRect.left);
                bitmapRect.top = Math.max(0, bitmapRect.top);
                bitmapRect.right = Math.min(drawable.getIntrinsicWidth(), bitmapRect.right);
                bitmapRect.bottom = Math.min(drawable.getIntrinsicHeight(), bitmapRect.bottom);
            }
            return bitmapRect;
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw existing rects
        for (RectF rect : faceRects) {
            RectF viewRect = new RectF();
            matrix.mapRect(viewRect, rect);
            canvas.drawRect(viewRect, boxPaint);
        }

        // Draw current dragging rect
        if (isDrawingMode && currentDrawingRect != null) {
            canvas.drawRect(currentDrawingRect, boxPaint);
        }
    }

    public void setDrawingMode(boolean drawing) {
        isDrawingMode = drawing;
    }

    public boolean isDrawingMode() {
        return isDrawingMode;
    }

    public void undoLastRect() {
        if (!faceRects.isEmpty()) {
            faceRects.remove(faceRects.size() - 1);
            invalidate();
        }
    }

    public void clearRects() {
        faceRects.clear();
        invalidate();
    }

    public List<RectF> getFaceRects() {
        return faceRects;
    }

    public void setMaxZoom(float x) {
        maxScale = x;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight)
                matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2, viewHeight / 2);
            else
                matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());

            fixTrans();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float origScale = saveScale;
            float mScaleFactor;

            if (saveScale == 1) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else {
                saveScale = 1;
                mScaleFactor = 1 / origScale;
            }
            matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2, viewHeight / 2);
            fixTrans();
            return true;
        }
    }

    void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];

        float fixTransX = getFixTrans(transX, viewWidth, origWidth * saveScale);
        float fixTransY = getFixTrans(transY, viewHeight, origHeight * saveScale);

        if (fixTransX != 0 || fixTransY != 0)
            matrix.postTranslate(fixTransX, fixTransY);
    }

    float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }

    float getFixDragTrans(float delta, float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return 0;
        }
        return delta;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        // Rescale image on rotation
        if (oldMeasuredWidth == viewWidth && oldMeasuredHeight == viewHeight
                || viewWidth == 0 || viewHeight == 0)
            return;
        oldMeasuredHeight = viewHeight;
        oldMeasuredWidth = viewWidth;

        if (saveScale == 1) {
            // Fit to screen.
            float scale;
            Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0)
                return;
            int bmWidth = drawable.getIntrinsicWidth();
            int bmHeight = drawable.getIntrinsicHeight();

            Log.d("bmSize", "bmWidth: " + bmWidth + " bmHeight : " + bmHeight);

            float scaleX = (float) viewWidth / (float) bmWidth;
            float scaleY = (float) viewHeight / (float) bmHeight;
            scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);

            // Center the image
            float redundantYSpace = (float) viewHeight - (scale * (float) bmHeight);
            float redundantXSpace = (float) viewWidth - (scale * (float) bmWidth);
            redundantYSpace /= (float) 2;
            redundantXSpace /= (float) 2;

            matrix.postTranslate(redundantXSpace, redundantYSpace);

            origWidth = viewWidth - 2 * redundantXSpace;
            origHeight = viewHeight - 2 * redundantYSpace;
            setImageMatrix(matrix);
        }
        fixTrans();
    }
}
