package com.example.project3.UIComponents;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.example.project3.Common.CameraSource;

import java.util.ArrayList;
import java.util.List;

public class GraphicsOverlay extends View {
    private final Object lock = new Object();
    private int previewWidth = 0;
    private float widthScaleFactor = 1.0f;
    private int previewHeight = 0;
    private float heightScaleFactor = 1.0f;
    private int facing = CameraSource.CAMERA_FACING_BACK;
    private final List<Graphic> graphics = new ArrayList<>();

    public GraphicsOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
    }

    public void remove(Graphic graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }

    public abstract static class Graphic {
        private final GraphicsOverlay overlay;

        public Graphic(GraphicsOverlay overlay) {
            this.overlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        protected float scaleX(float horizontal) {
            return horizontal * overlay.widthScaleFactor;
        }

        protected float scaleY(float vertical) {
            return vertical * overlay.heightScaleFactor;
        }

        protected Context getApplicationContext() {
            return overlay.getContext().getApplicationContext();
        }

        protected float translateX(float x) {
            return overlay.facing == CameraSource.CAMERA_FACING_FRONT ? overlay.getWidth() - scaleX(x) : scaleX(x);
        }

        protected float translateY(float y) {
            return scaleY(y);
        }

        protected void postInvalidate() {
            overlay.postInvalidate();
        }
    }



    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (lock) {
            this.previewWidth = previewWidth;
            this.previewHeight = previewHeight;
            this.facing = facing;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (lock) {
            if (previewWidth != 0 && previewHeight != 0) {
                widthScaleFactor = (float) canvas.getWidth() / (float) previewWidth;
                heightScaleFactor = (float) canvas.getHeight() / (float) previewHeight;
            }
            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }
}