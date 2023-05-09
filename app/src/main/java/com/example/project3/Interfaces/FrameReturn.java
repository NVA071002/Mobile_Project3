package com.example.project3.Interfaces;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import com.example.project3.Common.FrameMetadata;
import com.example.project3.UIComponents.GraphicsOverlay;

public interface FrameReturn {
    void onFrame(Bitmap image, FirebaseVisionFace face, FrameMetadata frameMetadata, GraphicsOverlay graphicsOverlay);
    void onCreate(Bundle saveInstanceState);
}
