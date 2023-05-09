package com.example.project3.Common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
//import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.util.Size;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import androidx.annotation.RequiresPermission;
//import androidx.camera.core.Camera;
//import com.google.android.gms.vision.CameraSource;
import com.example.project3.UIComponents.GraphicsOverlay;
import kotlin.jvm.Synchronized;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CameraSource {
    private static final String TAG = "CameraSource";
    protected Activity activity;
    private GraphicsOverlay graphicsOverlay;
    private Camera camera = null;
    private int cameraFacing = CameraSource.CAMERA_FACING_FRONT;
    private int rotation = 0;
    public Size previewSize = null;
    private final float requestedFps = 20.0f;
    private final boolean requestedAutoFocus = true;
    private SurfaceTexture dummySurfaceTexture = null;
    private boolean usingSurfaceTexture = false;
    private Thread processingThread = null;
    private final FrameProcessingRunnable processingRunnable;
    private final Object processorLock = new Object();
    private VisionImageProcessor frameProcessor = null;
    private final Map<byte[], ByteBuffer> bytesToByteBuffer = new ConcurrentHashMap<>();

    public CameraSource(Activity activity, GraphicsOverlay graphicsOverlay) {
        this.activity = activity;
        this.graphicsOverlay = graphicsOverlay;
        processingRunnable = new FrameProcessingRunnable();
    }

    public void release() {
        synchronized (processorLock) {
            stop();
            processingRunnable.release();
            cleanScreen();
            if (frameProcessor != null)
                frameProcessor.stop();
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    public CameraSource start() throws IOException {
        if (camera != null) {
            return this;
        }
        camera = createCamera();
        dummySurfaceTexture = new SurfaceTexture(DUMMY_TEXTURE_NAME);
        camera.setPreviewTexture(dummySurfaceTexture);
        usingSurfaceTexture = true;
        camera.startPreview();
        processingThread = new Thread(processingRunnable);
        processingRunnable.setActive(true);
        processingThread.start();
        return this;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    public CameraSource start(SurfaceHolder surfaceHolder) throws IOException {
        if (camera != null) {
            return this;
        }
        camera = createCamera();
        camera.setPreviewDisplay(surfaceHolder);
        camera.startPreview();
        processingThread = new Thread(processingRunnable);
        processingRunnable.setActive(true);
        processingThread.start();
        usingSurfaceTexture = false;
        return this;
    }

    @Synchronized
    public void stop() {
        processingRunnable.setActive(false);
        if (processingThread != null) {
            try {
                processingThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "Frame processing thread interrupted on release.");
            }
            processingThread = null;
        }
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallbackWithBuffer(null);
            try {
                if (usingSurfaceTexture) {
                    camera.setPreviewTexture(null);
                } else {
                    camera.setPreviewDisplay(null);
                }
            } catch (Exception e) {
                Log.e("TAG", "Failed to clear camera preview: " + e);
            }
            camera.release();
            camera = null;
        }
        bytesToByteBuffer.clear();


    }

    @SuppressLint("InlinedApi")
    private Camera createCamera() throws IOException {
        int requestedCameraID = getIdForRequestedCamera(cameraFacing);
        if (requestedCameraID == -1) {
            throw new IOException("Could not find requested camera");
        }
        Camera camera = Camera.open(requestedCameraID);
        SizePair sizePair = selectSizePair(camera, requestedPreviewWidth, requestedPreviewHeight);
        if (sizePair == null) {
            throw new IOException("Could not find Preview Size.");
        }
        Size pictureSize = sizePair.pictureSize();
        previewSize = sizePair.previewSize();
        int[] previewFpsRange = selectPreviewFpsRange(camera, requestedFps);
        if (previewFpsRange == null) {
            throw new IOException("Could not find suitable preview Frames per second range");
        }
        Camera.Parameters parameters = camera.getParameters();
        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        }
        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        parameters.setPreviewFpsRange(
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
        );
        parameters.setPreviewFormat(ImageFormat.NV21);
        setRotation(camera, parameters, requestedCameraID);
        if (requestedAutoFocus) {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else {
                Log.i(TAG, "Camera auto focus is not supported on this device.");
            }
        }
        camera.setParameters(parameters);
        camera.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        return camera;
    }

    private static class SizePair {
        private final Size preview;
        private final Size picture;

        SizePair(Camera.Size previewSize, Camera.Size pictureSize) {
            preview = new Size(previewSize.width, previewSize.height);
            if (pictureSize != null) {
                picture = new Size(pictureSize.width, pictureSize.height);
            } else {
                picture = null;
            }
        }

        Size previewSize() {
            return preview;
        }

        Size pictureSize() {
            return picture;
        }
    }

    private void setRotation(Camera camera, Camera.Parameters parameters, int cameraID) {
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        int degrees = 0;
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                Log.e(TAG, "Bad rotation value: " + rotation);
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, cameraInfo);
        int angle;
        int displayAngle;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360;
            displayAngle = (360 - angle) % 360;
        } else {
            angle = (cameraInfo.orientation - degrees + 360) % 360;
            displayAngle = angle;
        }
        this.rotation = angle / 90;
        camera.setDisplayOrientation(displayAngle);
        parameters.setRotation(angle);
    }

    @SuppressLint("InlineApi")
    private byte[] createPreviewBuffer(Size previewSize) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        long sizeInBits = previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        int bufferSize = (int) Math.ceil(sizeInBits / 8.0) + 1;
        byte[] byteArray = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        assert (buffer.hasArray() && buffer.array() == byteArray) : "Failed to create valid buffer for camera source.";
        bytesToByteBuffer.put(byteArray, buffer);
        return byteArray;
    }

    private class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            processingRunnable.setnextFrame(data, camera);
        }
    }

    public void setMachineLearningFrameProcessor(VisionImageProcessor processor) {
        synchronized (processorLock) {
            cleanScreen();
            if (frameProcessor != null) {
                frameProcessor.stop();
            }
            frameProcessor = processor;
        }
    }

    private class FrameProcessingRunnable implements Runnable
    {
        private final Object lock = new Object();
        private volatile boolean active = true;
        private ByteBuffer pendingFrameData;

        @SuppressLint("Assert")
        public void release() {
            assert processingThread.getState() == Thread.State.TERMINATED;
        }

        public void setActive(boolean active) {
            synchronized (lock) {
                this.active = active;
                lock.notifyAll();
            }
        }

        public void setnextFrame(byte[] data, Camera camera) {
            synchronized (lock) {
                if (pendingFrameData != null) {
                    camera.addCallbackBuffer(pendingFrameData.array());
                    pendingFrameData = null;
                }
                if (!bytesToByteBuffer.containsKey(data)) {
                    Log.d(TAG, "Skipping frame. Could not find ByteBuffer associated with the image"
                            + "data from the camera.");
                    return;
                }
                pendingFrameData = bytesToByteBuffer.get(data);
                lock.notifyAll();
            }
        }
        @SuppressLint("InlineApi")
        @Override
        public void run() {
            ByteBuffer data;
            while (true){
                synchronized(lock){
                    while (active && pendingFrameData==null) {
                        try{
                            lock.wait();
                        }catch (InterruptedException e){
                            Log.d(TAG,"Frame processing loop terminated.");
                            return;
                        }
                    }
                    if(!active){
                        return;
                    }
                    data = pendingFrameData;
                    pendingFrameData=null;
                }
                try{
                    synchronized(processorLock)
                    {
                        Log.d(TAG,"Process an image");
                        frameProcessor.process(
                                data,
                                new FrameMetadata.Builder()
                                        .setWidth(previewSize.getWidth())
                                        .setHeight(previewSize.getHeight())
                                        .setRotation(rotation)
                                        .setCameraFacing(cameraFacing)
                                        .build(),
                                graphicsOverlay
                        );
                    }
                }catch (Throwable t){
                    Log.e(TAG,"Exception throw from receiver.",t);
                }finally {
                    camera.addCallbackBuffer(data.array());
                }
            }
        }
    }
    private void cleanScreen() {
        graphicsOverlay.clear();
    }

    public static final int CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private static final int DUMMY_TEXTURE_NAME = 100;
    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;
    public static final int requestedPreviewWidth = 480;
    public static final int requestedPreviewHeight = 360;

    private static int getIdForRequestedCamera(int facing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return i;
            }
        }
        return -1;
    }

    private static SizePair selectSizePair(Camera camera, int desiredWidth, int desiredHeight) {
        List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);
        SizePair selectedPair = null;
        int minDiff = Integer.MAX_VALUE;
        for (SizePair sizePair : validPreviewSizes) {
            Size size = sizePair.previewSize();
            int diff = Math.abs(size.getWidth() - desiredWidth) + Math.abs(size.getHeight() - desiredHeight);
            if (diff < minDiff) {
                selectedPair = sizePair;
                minDiff = diff;
            }
        }
        return selectedPair;
    }

    public Size getPreviewSize() {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        // Choose an appropriate preview size here, e.g. the largest one
        Camera.Size previewSize = previewSizes.get(0);
        return new Size(previewSize.width, previewSize.height);
    }

    public int getCameraFacing() {
        return cameraFacing;
    }

    public int cameraID() throws RuntimeException {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == cameraFacing) {
                return i;
            }
        }
        throw new RuntimeException("Could not find camera ID for camera facing " + cameraFacing);
    }

    private static List<SizePair> generateValidPreviewSizeList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        List<SizePair> validPreviewSizes = new ArrayList<>();
        for (Camera.Size previewSize : supportedPreviewSizes) {
            float previewAspectRatio = (float) previewSize.width / previewSize.height;
            for (Camera.Size pictureSize : supportedPictureSizes) {
                float pictureAspectRatio = (float) pictureSize.width / pictureSize.height;
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(new SizePair(previewSize, pictureSize));
                    break;
                }
            }
        }
        if (validPreviewSizes.size() == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
            for (Camera.Size previewSize : supportedPreviewSizes) {
                validPreviewSizes.add(new SizePair(previewSize, null));
            }
        }
        return validPreviewSizes;
    }

    private static int[] selectPreviewFpsRange(Camera camera, float desiredPreviewFps) {
        int desiredPreviewFpsScaled = (int) (desiredPreviewFps * 1000.0f);
        int[] selectedFpsRange = null;
        int minDiff = Integer.MAX_VALUE;
        List<int[]> previewFpsRangeList = camera.getParameters().getSupportedPreviewFpsRange();
        for (int[] range : previewFpsRangeList) {
            int deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            int deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            int diff = Math.abs(deltaMin) + Math.abs(deltaMax);
            if (diff < minDiff) {
                selectedFpsRange = range;
                minDiff = diff;
            }
        }
        return selectedFpsRange;
    }

    public CameraSource() {
        graphicsOverlay.clear();
        processingRunnable = new FrameProcessingRunnable();
        if (Camera.getNumberOfCameras() == 1) {
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(0, cameraInfo);
            cameraFacing = cameraInfo.facing;
        }
    }


}
