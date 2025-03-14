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
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(15);
        canvas.drawCircle(centerX, centerY, circleRadius, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        canvas.drawCircle(posX, posY, dotRadius, paint);

        getHolder().unlockCanvasAndPost(canvas);
    }

    private void setDimensions() {
        centerX = getWidth() / 2f;
        centerY = getHeight() / 2f;
        circleRadius = Math.min(getWidth(), getHeight()) / 3f;
        dotRadius = Math.min(getWidth(), getHeight()) / 10f;
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
            float posX = Math.max(dotRadius, Math.min(event.getX(), getWidth()-dotRadius));
            float posY = Math.max(dotRadius, Math.min(event.getY(), getHeight()-dotRadius));

            drawJoyStick(posX, posY);
            updateDir(posX, posY);

            if (dirChangeListener != null) dirChangeListener.onDirChange(dir, angle);
        }
        else{
            drawJoyStick(centerX, centerY);
            dirChangeListener.onDirChange("None", -1);
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

    public void setDirChangeListener(OnDirChangeListener listener) {this.dirChangeListener = listener;}

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {}


}
