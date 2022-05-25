package org.tensorflow.lite.examples.detection;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnResultCallbackListener;
import com.ppandroid.lib_detect.ImageViewDetect;
import com.ppandroid.lib_detect.view.OverlayView;

import java.util.ArrayList;
import java.util.List;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ImageView image_view=findViewById(R.id.image_view);
        OverlayView overlayView=findViewById(R.id.tracking_overlay);
        findViewById(R.id.btn_select_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PictureSelector.create(TestActivity.this)
                        .openGallery(PictureMimeType.ofImage())
                        .imageEngine(GlideEngine.createGlideEngine())
                        .forResult(new OnResultCallbackListener<LocalMedia>() {


                            @Override
                            public void onResult(List<LocalMedia> result) {
                                if (result.size()>0){
                                    Glide.with(TestActivity.this).load(result.get(0).getRealPath()).into(image_view);
                                }
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
            }
        });
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ImageViewDetect(TestActivity.this,image_view,overlayView).start();
            }
        });

    }
}