package com.example.douyingetting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Clips complete page snapshots so every component changes along the same circle. */
final class ThemeRevealView extends View {
    private final Bitmap before;
    private final float centerX;
    private final float centerY;
    private final boolean expanding;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path circle = new Path();
    private Bitmap after;
    private float radius;

    ThemeRevealView(Context context, Bitmap before, float centerX, float centerY, boolean expanding) {
        super(context);
        this.before = before;
        this.centerX = centerX;
        this.centerY = centerY;
        this.expanding = expanding;
        setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setAfter(Bitmap after) {
        this.after = after;
    }

    void setRadius(float radius) {
        this.radius = radius;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (after == null) {
            canvas.drawBitmap(before, 0, 0, paint);
            return;
        }
        canvas.drawBitmap(expanding ? before : after, 0, 0, paint);
        circle.reset();
        circle.addCircle(centerX, centerY, radius, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(circle);
        canvas.drawBitmap(expanding ? after : before, 0, 0, paint);
        canvas.restoreToCount(save);
    }
}
