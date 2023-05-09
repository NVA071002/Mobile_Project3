package com.example.project3.UIComponents;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//import com.google.android.gms.vision.CameraSource;

import com.example.project3.Common.CameraSource;
import java.io.IOException;
public class CameraReview extends ViewGroup {
//    private static Context context;
    private final SurfaceView surfaceView;
    private boolean startRequested = false;
    private boolean surfaceAvailable = false;
    private CameraSource cameraSource = null;
    private GraphicsOverlay overlay = null;
    private static final String TAG = "MlDemoApp:Preview";
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    public CameraReview(Context context, AttributeSet attrs) {
        super(context, attrs);
        surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(surfaceView);
    }

    public void start(CameraSource cameraSource) throws IOException {
        if (cameraSource == null) {
            stop();
        }
        this.cameraSource = cameraSource;
        if (this.cameraSource != null) {
            startRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, GraphicsOverlay overlay) throws IOException {
        this.overlay = overlay;
        start(cameraSource);
    }

    public void stop() {
        if (cameraSource != null) {
            cameraSource.stop();
        }
    }

    public void release() {
        if (cameraSource != null) {
            cameraSource.release();
            cameraSource = null;
        }
    }


    private void startIfReady() throws IOException {
        if (startRequested && surfaceAvailable) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                return;
            }
            if (startRequested && surfaceAvailable) {
                cameraSource.start();
                if (overlay != null) {
                    Size size = cameraSource.getPreviewSize();
                    int min = Math.min(size.getWidth(), size.getHeight());
                    int max = Math.max(size.getWidth(), size.getHeight());
                    if (isPortraitMode()) {
                        overlay.setCameraInfo(min, max, cameraSource.getCameraFacing());
                    } else {
                        overlay.setCameraInfo(max, min, cameraSource.getCameraFacing());
                    }
                    overlay.clear();
                }
                startRequested = false;
            }
        }
    }

    public void onLayout(boolean po, int p1, int p2, int p3, int p4) {
        int width = 320;
        int height = 240;
        if (cameraSource != null) {
            Size size = cameraSource.previewSize;
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }
        if (isPortraitMode()) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        int layoutWidth = p3 - p1;
        int layoutHeight = p4 - p2;

        int childWidth = layoutWidth;
        int childHeight = (int) (layoutWidth * ((float) height / (float) width));

        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int) (layoutHeight * ((float) width / (float) height));
        }

        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(0, 0, childWidth, childHeight);
            Log.d(TAG, "Assigned View: " + i);
        }
        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    private boolean isPortraitMode() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }
        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceAvailable = true;
            try {
                startIfReady();
            } catch (IOException e) {
                Log.e(TAG, "Could not start camera source.", e);
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }

//    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

//    public Preview(Context context) {
//        super(context);
//        surfaceView = new SurfaceView(context);
//        surfaceView.getHolder().addCallback(new SurfaceCallback());
//        addView(surfaceView);
//    }
}
