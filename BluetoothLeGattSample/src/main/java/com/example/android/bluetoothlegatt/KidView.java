package com.example.android.bluetoothlegatt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created on 2017/6/5.
 *
 * @author lkuan
 */

public class KidView extends View {

    private int mColorUpdate = 0xffe4e4e4;
    private int mColorBluetooth = 0xff191c25;
    private Paint mPaint;
    private Path mPath;

    public KidView(Context context) {
        super(context);
        init();
    }

    public KidView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KidView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPath = new Path();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(mColorUpdate);
        mPath.moveTo(0, 0);
        mPath.lineTo(getWidth(), 0);
        mPath.lineTo(0, getHeight());
        mPath.close();
        canvas.drawPath(mPath, mPaint);
        mPath.reset();
        mPaint.setColor(mColorBluetooth);
        mPath.moveTo(0, getHeight());
        mPath.lineTo(getWidth(), 0);
        mPath.lineTo(getWidth(), getHeight());
        mPath.close();
        canvas.drawPath(mPath, mPaint);
    }
}
