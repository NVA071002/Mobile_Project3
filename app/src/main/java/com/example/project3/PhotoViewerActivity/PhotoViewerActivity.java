package com.example.project3.PhotoViewerActivity;

import android.os.Bundle;
import android.widget.ImageView;
import org.greenrobot.eventbus.EventBus;
import  com.example.project3.Base.BaseActivity;
import com.example.project3.Base.Cons;
import com.example.project3.Base.PublicMethods;
import com.example.project3.R;

public class PhotoViewerActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        EventBus.getDefault().post("Return");
        if (getIntent().hasExtra(Cons.IMG_EXTRA_KEY)) {
            ImageView imageView = findViewById(R.id.image);
            String imagePath = getIntent().getStringExtra(Cons.IMG_EXTRA_KEY);
            imageView.setImageBitmap(PublicMethods.getBitmapByPath(imagePath, Cons.IMG_FILE));
        }
    }

}