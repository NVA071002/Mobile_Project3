package com.example.project3.Visions;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.camera.core.CameraSelector.LensFacing;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.example.project3.Interfaces.FaceDetectStatus;
import com.example.project3.Models.RectModel;
import com.example.project3.UIComponents.GraphicsOverlay;

public class FaceGraphic extends GraphicsOverlay.Graphic {

    private GraphicsOverlay overlay;
    private FirebaseVisionFace firebaseVisionFace;
    private int facing;
    private Bitmap overlayBitmap;
    private Paint facePositionPaint;
    private Paint idPaint;
    private Paint boxPaint;
    private FaceDetectStatus faceDetectStatus;

    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    public FaceGraphic(GraphicsOverlay overlay, FirebaseVisionFace firebaseVisionFace, int facing, Bitmap overlayBitmap) {
        super(overlay);
        this.overlay = overlay;
        this.firebaseVisionFace = firebaseVisionFace;
        this.facing = facing;
        this.overlayBitmap = overlayBitmap;

        int selectedColor = Color.GREEN;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        idPaint = new Paint();
        idPaint.setColor(selectedColor);
        idPaint.setTextSize(ID_TEXT_SIZE);

        boxPaint = new Paint();
        boxPaint.setColor(selectedColor);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    public void setFaceDetectStatus(FaceDetectStatus faceDetectStatus) {
        this.faceDetectStatus = faceDetectStatus;
    }

    @Override
    public void draw(Canvas canvas) {
        FirebaseVisionFace face = firebaseVisionFace;
        if (face == null) {
            return;
        }
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());

        float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
        float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        canvas.drawRect(left, top, right, bottom, boxPaint);

        if (left < 190 && top < 450 && right > 850 && bottom > 1050) {
            if (faceDetectStatus != null) {
                faceDetectStatus.onFaceLocated(new RectModel(left, top, right, bottom));
            } else {
                if (faceDetectStatus != null) {
                    faceDetectStatus.onFaceNotLocated();
                }
            }
        }
    }
}
