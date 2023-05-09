package com.example.project3.Common;

import android.graphics.Bitmap;
import androidx.annotation.GuardedBy;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.example.project3.Common.BitmapUtils;
import com.example.project3.UIComponents.GraphicsOverlay;
import java.nio.ByteBuffer;

public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

    @GuardedBy("this")
    private ByteBuffer latestImage;

    @GuardedBy("this")
    private FrameMetadata latestImageMetadata;

    @GuardedBy("this")
    private ByteBuffer processingImage;

    @GuardedBy("this")
    private FrameMetadata processingMetadata;

    @Override
    public void process(ByteBuffer data, FrameMetadata frameMetadata, GraphicsOverlay graphicsOverlay) {
        latestImage = data;
        latestImageMetadata = frameMetadata;
        if (processingImage == null && processingMetadata == null) {
            processLatestImage(graphicsOverlay);
        }
    }

    @Override
    public void process(Bitmap bitmap, GraphicsOverlay graphicsOverlay) {
        detectInVisionImage(null, FirebaseVisionImage.fromBitmap(bitmap), null, graphicsOverlay);
    }

    private synchronized void processLatestImage(GraphicsOverlay graphicsOverlay) {
        processingImage = latestImage;
        processingMetadata = latestImageMetadata;
        latestImage = null;
        latestImageMetadata = null;
        if (processingImage != null && processingMetadata != null) {
            processImage(processingImage, processingMetadata, graphicsOverlay);
        }
    }

    private void processImage(ByteBuffer data, FrameMetadata frameMetadata, GraphicsOverlay graphicsOverlay) {
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setWidth(frameMetadata.getWidth())
                .setHeight(frameMetadata.getHeight())
                .setRotation(frameMetadata.getRotation())
                .build();
        Bitmap bitmap = BitmapUtils.getBitmap(data, frameMetadata);
        detectInVisionImage(bitmap, FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata, graphicsOverlay);
    }

    private void detectInVisionImage(Bitmap originalCameraImage, FirebaseVisionImage image, FrameMetadata metadata, GraphicsOverlay graphicsOverlay) {
        detectInImage(image)
                .addOnSuccessListener(
                        results -> {
                            onSuccess(originalCameraImage, results, metadata, graphicsOverlay);
                        })
                .addOnFailureListener(
                        e -> {
                            onFailure(e);
                        });
    }

    @Override
    public void stop() {}

    protected abstract Task<T> detectInImage(FirebaseVisionImage image);

    protected abstract void onSuccess(Bitmap originalCameraImage, T results, FrameMetadata frameMetadata, GraphicsOverlay graphicsOverlay);

    protected void onFailure(Exception e) {}
}
