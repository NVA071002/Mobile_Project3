package com.example.project3.Visions;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;

import com.example.project3.Common.CameraImageGraphic;
import com.example.project3.Common.CameraSource;
import com.example.project3.Common.FrameMetadata;
import com.example.project3.Common.VisionProcessorBase;
import com.example.project3.Interfaces.FaceDetectStatus;
import com.example.project3.Interfaces.FrameReturn;
import com.example.project3.Models.RectModel;
import com.example.project3.R;
import com.example.project3.UIComponents.GraphicsOverlay;

public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> implements FaceDetectStatus {
    private static final String TAG = "FaceDetectionProcessor";
    public FaceDetectStatus faceDetectStatus;
    private final FirebaseVisionFaceDetector detector;
    private final Bitmap overlayBitmap;
    public FrameReturn frameHandler;

    public FaceDetectionProcessor(Resources resources) {
        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build();
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        overlayBitmap = BitmapFactory.decodeResource(resources, R.drawable.clown_nose);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close face detector: " + e);
        }
    }

    @Override
    public Task<List<FirebaseVisionFace>> detectInImage(@NonNull FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

    @Override
    public void onFaceLocated(RectModel rectModel) {
        if (faceDetectStatus != null) {
            faceDetectStatus.onFaceLocated(rectModel);
        }
    }

    @Override
    public void onFaceNotLocated() {
        if (faceDetectStatus != null) {
            faceDetectStatus.onFaceNotLocated();
        }
    }

    public void onSuccess(Bitmap originalCameraImage, List<FirebaseVisionFace> faces, FrameMetadata frameMetadata, GraphicsOverlay graphicsOverlay) {
        graphicsOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicsOverlay, originalCameraImage);
            graphicsOverlay.add(imageGraphic);
        }
        for (int i = 0; i < faces.size(); i++) {
            FirebaseVisionFace face = faces.get(i);
            if (frameHandler != null) {
                frameHandler.onFrame(originalCameraImage, face, frameMetadata, graphicsOverlay);
            }
            int cameraFacing = (frameMetadata != null && frameMetadata.getCameraFacing() != FrameMetadata.UNKNOWN_CAMERA_FACING) ? frameMetadata.getCameraFacing() : CameraSource.CAMERA_FACING_BACK;
            FaceGraphic faceGraphic = new FaceGraphic(graphicsOverlay, face, cameraFacing, overlayBitmap);
            faceGraphic.setFaceDetectStatus(this);
            graphicsOverlay.add(faceGraphic);
        }
        graphicsOverlay.postInvalidate();
    }
}
