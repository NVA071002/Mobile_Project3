package com.example.project3.Common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraMetadata;
import android.util.Log;
import androidx.camera.core.CameraInfo;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class BitmapUtils {
    public static Bitmap getBitmap(ByteBuffer data, FrameMetadata metadata) {
        data.rewind();
        byte[] imageBuffer = new byte[data.limit()];
        data.get(imageBuffer, 0, imageBuffer.length);
        try {
            YuvImage image = new YuvImage(
                    imageBuffer, ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
            if (image != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                stream.close();
                return rotateBitmap(bmp, metadata.getRotation(), metadata.getCameraFacing());
            }
        } catch (Exception e) {
            Log.e("VisionProcessorBase", "Error: " + e.getMessage());
        }
        return null;
    }


    private static Bitmap rotateBitmap(Bitmap bmp, int rotation, int cameraFacing) {
        Matrix matrix = new Matrix();
        int rotationDegree = 0;
        switch (rotation) {
            case FirebaseVisionImageMetadata.ROTATION_90:
                rotationDegree = 90;
                break;
            case FirebaseVisionImageMetadata.ROTATION_180:
                rotationDegree = 180;
                break;
            case FirebaseVisionImageMetadata.ROTATION_270:
                rotationDegree = 270;
                break;
            default:
                break;
        }

        matrix.postRotate(rotationDegree);
        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        } else {
            matrix.postScale(-1.0f, 1.0f);
            return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        }
    }
}
