package com.leapord.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;


/**
 * Created by JokerFish on 2017/10/17.
 */

public class IndicatorBar extends View {
    private int mWidth;
    private int mHeight;
    private Paint bgPaint;
    private Paint pbPaint;
    private int pbHeight;
    private long maxValue = 100, minValue = 0, currentValue = 50, stepValue = 10;
    private float progress = 0.5F;
    private Paint valuePaint;
    private int xPosition;
    private int rest = dp2Px(getContext(), 10);
    private Paint strokePaint;
    private float halfIndicatorWith;
    private OnProgressChangedListener listener;
    private PopType popType = PopType.HIDE_MOVING;
    private int action;

    public enum PopType {
        OFEN, HIDE_MOVING
    }

    public void setOnProgressChangedListener(OnProgressChangedListener listener) {
        this.listener = listener;
    }

    public void setInitParams(long minValue, long maxValue, long stepValue) {
        if ((maxValue - minValue) % stepValue != 0) {
            throw new RuntimeException("incompatible step value");
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepValue = stepValue;
    }

    public void setProgress(float progress) {
        if (progress < 1 || progress > 100) {
            throw new RuntimeException("progress should between 0 and 1");
        }
        this.progress = progress / 100;
        currentValue = (long) (minValue + this.progress * (maxValue - minValue));
        xPosition = (int) (this.progress * mWidth);
    }

    public IndicatorBar(Context context) {
        this(context, null);
    }

    public IndicatorBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.IndicatorBar);
        int backgroundColor = typedArray.getColor(R.styleable.IndicatorBar_default_background, Color.GRAY);
        int progressColor = typedArray.getColor(R.styleable.IndicatorBar_progress_color, getResources().getColor(R.color.colorPrimary));
        int valueColor = typedArray.getColor(R.styleable.IndicatorBar_value_color, Color.WHITE);
        int valueTextSize = typedArray.getDimensionPixelSize(R.styleable.IndicatorBar_value_textSize, 32);
        typedArray.recycle();

        bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(backgroundColor);
        bgPaint.setStyle(Paint.Style.FILL);


        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setColor(Color.GRAY);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp2Px(context, 1));

        pbPaint = new Paint();
        pbPaint.setAntiAlias(true);
        pbPaint.setColor(progressColor);
        pbPaint.setStyle(Paint.Style.FILL);

        valuePaint = new Paint();
        valuePaint.setAntiAlias(true);
        valuePaint.setColor(valueColor);
        valuePaint.setStyle(Paint.Style.FILL);
        valuePaint.setTextSize(sp2Px(context, valueTextSize));


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 || h == 0) {
            mHeight = dp2Px(getContext(), 300);
            WindowManager wm = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            mWidth = wm.getDefaultDisplay().getWidth();
        } else {
            mHeight = h;
            mWidth = w;
        }
        pbHeight = mHeight / 8;
        xPosition = mWidth / 2;
        rest = mHeight / 10;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        drawBackground(canvas);
        drawProgress(canvas);
        drawIndicator(canvas);
        canvas.restore();
    }

    /**
     * 画指示器
     *
     * @param canvas
     */
    private void drawIndicator(Canvas canvas) {

        String value = String.valueOf(currentValue);
        Rect rect = new Rect();
        valuePaint.getTextBounds(value, 0, value.length(), rect);
        int x = xPosition - rect.width() / 2;
        halfIndicatorWith = rect.width() / 2 + rest;
        int roundX = (rect.height() + rest) / 2;
        int right = x + rect.width() + 2 * rest;
        int bottom = 2 * rest + rect.height();
        switch (popType) {
            case OFEN:
                drawPop(canvas, x, roundX, right, bottom);
                break;
            case HIDE_MOVING:
                if (action != MotionEvent.ACTION_MOVE) {
                    drawPop(canvas, x, roundX, right, bottom);
                }
                break;
        }
        int top = mHeight - rest * 2 - pbHeight;
        int roundX2 = (top + mHeight) / 3;
        drawRec(canvas, xPosition - rest * 2, top, xPosition + 2 * rest, mHeight, roundX2, valuePaint);

        drawRec(canvas, xPosition - rest * 2, top, xPosition + 2 * rest, mHeight, roundX2, strokePaint);
        int left1 = xPosition - rest * 2 / 3;
        int left2 = xPosition + rest * 2 / 3;
        drawRec(canvas, left1, mHeight - rest - pbHeight, left1 + dp2Px(getContext(), 3), mHeight - rest, 2, bgPaint);
        drawRec(canvas, left2 - dp2Px(getContext(), 3), mHeight - rest - pbHeight, left2, mHeight - rest, 2, bgPaint);

        switch (popType) {
            case OFEN:
                canvas.drawText(value, x, rest + rect.height(), valuePaint);
                break;
            case HIDE_MOVING:
                if (action != MotionEvent.ACTION_MOVE) {
                    canvas.drawText(value, x, rest + rect.height(), valuePaint);
                }
                break;
        }


    }

    private void drawPop(Canvas canvas, int x, int roundX, int right, int bottom) {
        drawRec(canvas, x - 2 * rest, 0, right, bottom, roundX, pbPaint);
        Path path = new Path();
        path.moveTo(xPosition - rest * 2 / 3, bottom);
        path.lineTo(xPosition + rest * 2 / 3, bottom);
        path.lineTo(xPosition, bottom + rest);
        path.close();
        canvas.drawPath(path, pbPaint);
    }

    /**
     * 兼容低版本画圆角矩形
     *
     * @param canvas
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @param round
     * @param paint
     */
    private void drawRec(Canvas canvas, float left, float top, float right, float bottom, float round, Paint paint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(left, top, right, bottom, round, round, paint);
        } else {
            Path path = new Path();
            path.moveTo(left + round, top);
            path.lineTo(right - round, top);
            path.quadTo(right, top, right, top + round);
            path.lineTo(right, bottom - round);
            path.quadTo(right, bottom, right - round, bottom);
            path.lineTo(left + bottom, bottom);
            path.quadTo(left, bottom, left, bottom - round);
            path.lineTo(left, top + round);
            path.quadTo(left, top, left + round, top);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    /**
     * 画背景
     *
     * @param canvas
     */
    private void drawBackground(Canvas canvas) {
        drawRec(canvas, xPosition, mHeight - pbHeight - rest, mWidth, mHeight - rest, pbHeight / 2, bgPaint);
    }

    /**
     * 画进度
     *
     * @param canvas
     */
    private void drawProgress(Canvas canvas) {
        drawRec(canvas, 0, mHeight - pbHeight - rest, xPosition, mHeight - rest, pbHeight / 2, pbPaint);
    }

    private int sp2Px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale / 2 + 0.5f);
    }

    private int dp2Px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return true;
        }
        action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                xPosition = (int) (event.getRawX() - halfIndicatorWith);
                if (xPosition <= halfIndicatorWith) {
                    xPosition = (int) halfIndicatorWith + dp2Px(getContext(), 1);
                    progress = 0;
                } else if (xPosition >= mWidth - halfIndicatorWith) {
                    xPosition = (int) (mWidth - halfIndicatorWith - dp2Px(getContext(), 1));
                    progress = 1;
                } else {
                    progress = (xPosition - halfIndicatorWith) / (mWidth - halfIndicatorWith * 2);
                }
                currentValue = (long) (minValue + (maxValue - minValue) * progress);
                Log.i("progress", "xPosition:" + xPosition + " mWidth:" + mWidth + " onTouchEvent: " + progress);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                justifySteps();
                if (listener != null) {
                    listener.onProgressChanged(this, progress, currentValue);
                }
                break;
        }
        return true;
    }

    private void justifySteps() {
        int x = (int) (progress * mWidth);
        int stepCount = (int) ((maxValue - minValue) / stepValue);
        int xStep = mWidth / stepCount;
        Log.i("progress", "justifySteps: " + stepCount + " x: " + x + " xStep: " + xStep);
        if (x % xStep != 0) {
            int i = x / xStep;
            float x1 = xStep * i;
            float x2 = xStep * (i + 1);
            progress = (x - x1) > (x2 - x) ? x2 / mWidth : x1 / mWidth;
            currentValue = (long) (minValue + (maxValue - minValue) * progress);
            xPosition = (int) ((x - x1) > (x2 - x) ? x2 : x1);
            if (xPosition <= halfIndicatorWith) {
                xPosition = (int) halfIndicatorWith + dp2Px(getContext(), 1);
                progress = 0;
            } else if (xPosition >= mWidth - halfIndicatorWith) {
                xPosition = (int) (mWidth - halfIndicatorWith - dp2Px(getContext(), 1));
                progress = 1;
            }
            invalidate();
        }
    }

    public interface OnProgressChangedListener {

        void onProgressChanged(IndicatorBar indicatorView, float progress, long value);

    }
}
