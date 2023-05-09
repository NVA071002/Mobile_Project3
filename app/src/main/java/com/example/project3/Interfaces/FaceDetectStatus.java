package com.example.project3.Interfaces;

import com.example.project3.Models.RectModel;

public interface FaceDetectStatus {
    void onFaceLocated(RectModel rectModel);
    void onFaceNotLocated();
}