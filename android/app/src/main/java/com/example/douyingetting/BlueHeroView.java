package com.example.douyingetting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Home grid with the approved transparent sticker illustration. */
final class BlueHeroView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final boolean dark;
    private final Bitmap rabbit;

    BlueHeroView(Context context, boolean dark, Bitmap rabbit) {
        super(context);
        this.dark = dark;
        this.rabbit = rabbit;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        paint.setStrokeWidth(density);
        paint.setColor(dark ? 0x20456BFF : 0xFFDCE6FF);
        for (float x = 0; x < getWidth(); x += 16 * density) canvas.drawLine(x, 0, x, getHeight(), paint);
        for (float y = 0; y < getHeight(); y += 16 * density) canvas.drawLine(0, y, getWidth(), y, paint);
        float size = Math.min(154 * density, getWidth() * 0.43f);
        float left = getWidth() - size + density;
        paint.setAlpha(255);
        canvas.drawBitmap(rabbit, null, new RectF(left, 10 * density, left + size, 10 * density + size), paint);
    }
}
