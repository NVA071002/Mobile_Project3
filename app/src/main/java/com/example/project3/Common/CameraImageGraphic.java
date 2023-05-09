package com.example.project3.Common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.project3.UIComponents.GraphicsOverlay;

public class CameraImageGraphic extends GraphicsOverlay.Graphic {

    private final Bitmap bitmap;

    public CameraImageGraphic(GraphicsOverlay overlay, Bitmap bitmap) {
        super(overlay);
        this.bitmap = bitmap;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);
    }
}
