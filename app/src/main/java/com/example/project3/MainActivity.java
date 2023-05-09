package com.example.project3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.project3.Base.BaseActivity;
import com.example.project3.Base.Cons;
import com.example.project3.Base.PublicMethods;
import com.example.project3.Base.Screenshot;
import com.example.project3.Common.CameraSource;
import com.example.project3.Common.FrameMetadata;
import com.example.project3.Interfaces.FaceDetectStatus;
import com.example.project3.Interfaces.FrameReturn;
import com.example.project3.Models.RectModel;
import com.example.project3.PhotoViewerActivity.PhotoViewerActivity;
import com.example.project3.UIComponents.CameraReview;
import com.example.project3.UIComponents.GraphicsOverlay;
import com.example.project3.Visions.FaceDetectionProcessor;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.hsalf.smilerating.BaseRating;
import com.hsalf.smilerating.SmileRating;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class MainActivity extends BaseActivity implements ActivityCompat.OnRequestPermissionsResultCallback, FrameReturn, FaceDetectStatus {
    private Bitmap originalImage = null;
    private CameraSource cameraSource = null;
    private CameraReview preview = null;
    private GraphicsOverlay graphicsOverlay = null;
    private ImageView faceFrame = null;
    private ImageView test = null;
    private Button takePhoto = null;
    private SmileRating smileRating = null;
    private static final String FACE_DETECTION = "Face Detection";
    private static final String TAG = "MlKitTag";

    @Override
    public void onFrame(Bitmap image, FirebaseVisionFace face, FrameMetadata frameMetadata, GraphicsOverlay graphicsOverlay) {
        originalImage = image;
        if (face.getLeftEyeOpenProbability() < 0.4) {
            findViewById(R.id.rightEyeStatus).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.rightEyeStatus).setVisibility(View.INVISIBLE);
        }
        if (face.getRightEyeOpenProbability() < 0.4) {
            findViewById(R.id.leftEyeStatus).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.leftEyeStatus).setVisibility(View.INVISIBLE);
        }
        int smile = 0;
        if (face.getSmilingProbability() > 0.8) {
            smile = BaseRating.GREAT;
        } else if (face.getSmilingProbability() <= 0.8 && face.getSmilingProbability() > 0.6) {
            smile = BaseRating.GOOD;
        } else if (face.getSmilingProbability() <= 0.6 && face.getSmilingProbability() > 0.4) {
            smile = BaseRating.OKAY;
        } else if (face.getSmilingProbability() <= 0.4 && face.getSmilingProbability() > 0.2) {
            smile = BaseRating.BAD;
        }
        smileRating.setSelectedSmile(smile, true);
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_main);

        test = findViewById(R.id.test);
        preview = findViewById(R.id.preview);
        takePhoto = findViewById(R.id.takePhoto);
        graphicsOverlay = findViewById(R.id.overlay);
        smileRating = findViewById(R.id.smile_rating);

        if (PublicMethods.allPermissionGranted(this)) {
            createCameraSource();
        } else {
            PublicMethods.getRuntimePermissions(this);
        }
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    @Subscribe
    public void onAddSelected(String add) {
        if (add != null && add.equals("Return")) {
            takePhoto.setVisibility(View.VISIBLE);
            test.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
        EventBus.getDefault().unregister(this);
    }


    private void takePhoto() {
        takePhoto.setVisibility(View.GONE);
        test.setVisibility(View.GONE);

        Bitmap b = Screenshot.takeScreenshotOfRootView(test);
        test.setImageBitmap(b);

        String path = PublicMethods.saveToInternalStorage(b, Cons.IMG_FILE, mActivity);
        startActivity(
                new Intent(mActivity, PhotoViewerActivity.class).putExtra(Cons.IMG_EXTRA_KEY, path)
        );
    }

    private void createCameraSource() {
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicsOverlay);
        }
        try {
            FaceDetectionProcessor processor = new FaceDetectionProcessor(getResources());
            processor.frameHandler = this;
            processor.faceDetectStatus = this;
            cameraSource.setMachineLearningFrameProcessor(processor);
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + FACE_DETECTION, e);
            Toast.makeText(getApplicationContext(), "Can not create image processor: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    public void onPause() {
        super.onPause();
        preview.stop();
    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                preview.start(cameraSource, graphicsOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (PublicMethods.allPermissionGranted(this)) {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onFaceLocated(RectModel rectModel) {

    }

    @Override
    public void onFaceNotLocated() {

    }
}



