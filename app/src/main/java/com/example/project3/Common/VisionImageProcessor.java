package com.example.project3.Common;

import android.graphics.Bitmap;
import com.google.firebase.ml.common.FirebaseMLException;
import com.example.project3.UIComponents.GraphicsOverlay;
import java.nio.ByteBuffer;

public interface VisionImageProcessor {
    void process(ByteBuffer data, FrameMetadata frameMetadata, GraphicsOverlay graphicsOverlay) throws FirebaseMLException;

    void process(Bitmap bitmap, GraphicsOverlay graphicsOverlay);

    void stop();
}