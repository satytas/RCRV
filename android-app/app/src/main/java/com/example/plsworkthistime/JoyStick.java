package com.example.plsworkthistime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import androidx.annotation.NonNull;

public class JoyStick extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
    private OnDirChangeListener dirChangeListener;
    private float centerX, centerY, circleRadius, dotRadius;
    public String dir = "None";
    public double angle = 0;

    private static final int OUTER_CIRCLE_COLOR = Color.parseColor("#4285F4");
    private static final int INNER_CIRCLE_COLOR = Color.parseColor("#FFFFFF");
    private static final int INNER_CIRCLE_BORDER = Color.parseColor("#CCCCCC");
    private static final int CENTER_DOT_COLOR = Color.parseColor("#4285F4");

    private static final float OUTER_STROKE_WIDTH = 6f;
    private static final float INNER_STROKE_WIDTH = 3f;

    public JoyStick(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
        init();
    }

    public interface OnDirChangeListener {
        void onDirChange(String direction, double angle);
    }

    private void init() {
        getHolder().addCallback(this);
        setOnTouchListener(this);
    }

    private void drawJoyStick(float posX, float posY) {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) return;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Draw outer circle
        paint.setColor(OUTER_CIRCLE_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(OUTER_STROKE_WIDTH);
        paint.setAlpha(180);
        canvas.drawCircle(centerX, centerY, circleRadius, paint);

        // Draw inner semi-transparent background
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(OUTER_CIRCLE_COLOR);
        paint.setAlpha(30);
        canvas.drawCircle(centerX, centerY, circleRadius, paint);

        // Draw the joystick handle
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INNER_CIRCLE_COLOR);
        paint.setAlpha(255);
        canvas.drawCircle(posX, posY, dotRadius, paint);

        // Draw border around joystick handle
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(INNER_CIRCLE_BORDER);
        paint.setStrokeWidth(INNER_STROKE_WIDTH);
        canvas.drawCircle(posX, posY, dotRadius, paint);

        // Draw center dot of joystick handle
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CENTER_DOT_COLOR);
        canvas.drawCircle(posX, posY, dotRadius/3, paint);

        getHolder().unlockCanvasAndPost(canvas);
    }

    private void setDimensions() {
        centerX = getWidth() / 2f;
        centerY = getHeight() / 2f;
        circleRadius = Math.min(getWidth(), getHeight()) / 2.5f;
        dotRadius = Math.min(getWidth(), getHeight()) / 7f;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        setDimensions();
        drawJoyStick(centerX, centerY);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!v.equals(this)) return false;

        if (event.getAction() != MotionEvent.ACTION_UP) {
            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            float posX, posY;

            if (distance > circleRadius) {
                float ratio = circleRadius / distance;
                posX = centerX + dx * ratio;
                posY = centerY + dy * ratio;
            } else {
                posX = event.getX();
                posY = event.getY();
            }

            drawJoyStick(posX, posY);
            updateDir(posX, posY);

            if (dirChangeListener != null) dirChangeListener.onDirChange(dir, angle);
        } else {
            drawJoyStick(centerX, centerY);
            dir = "None";
            angle = -1;
            if (dirChangeListener != null) dirChangeListener.onDirChange(dir, angle);
        }

        return true;
    }

    private void updateDir(float posX, float posY) {
        angle = Math.toDegrees(Math.atan2(centerY - posY, posX - centerX));
        angle = angle < 0 ? angle + 360 : angle;

        if (angle > 22.5 && angle < 67.5) dir = "UR";
        else if (angle >= 67.5 && angle < 112.5) dir = "U";
        else if (angle >= 112.5 && angle < 157.5) dir = "UL";
        else if (angle >= 157.5 && angle < 202.5) dir = "L";
        else if (angle >= 202.5 && angle < 247.5) dir = "DL";
        else if (angle >= 247.5 && angle < 292.5) dir = "D";
        else if (angle >= 292.5 && angle < 337.5) dir = "DR";
        else dir = "R";
    }

    public void setDirChangeListener(OnDirChangeListener listener) {
        this.dirChangeListener = listener;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {}
}