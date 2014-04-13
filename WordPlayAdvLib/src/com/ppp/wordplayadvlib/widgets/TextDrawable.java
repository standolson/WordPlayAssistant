package com.ppp.wordplayadvlib.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class TextDrawable extends Drawable {

    private final String text;
    private final Paint paint;
    private final float xPos;
    private final float yPos;

    public TextDrawable(Context context, String text, int color, int textSize, int padding)
    {

    	float density = context.getResources().getDisplayMetrics().density;

        this.text = text;
        this.xPos = padding * density;
        this.yPos = density * (textSize + padding);

        this.paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(density * textSize);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);

    }

    @Override
    public void draw(Canvas canvas) { canvas.drawText(text, xPos, yPos, paint); }

    @Override
    public void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Override
    public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }

    @Override
    public int getOpacity() { return PixelFormat.TRANSLUCENT; }

}