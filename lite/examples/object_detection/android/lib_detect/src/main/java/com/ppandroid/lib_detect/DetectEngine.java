package com.ppandroid.lib_detect;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.Surface;

import com.ppandroid.lib_detect.env.ImageUtils;
import com.ppandroid.lib_detect.tracking.MultiBoxTracker;

import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.util.List;

import static com.ppandroid.lib_detect.DetectConfig.TF_OD_API_INPUT_SIZE;
import static com.ppandroid.lib_detect.DetectConfig.TF_OD_API_IS_QUANTIZED;
import static com.ppandroid.lib_detect.DetectConfig.TF_OD_API_LABELS_FILE;
import static com.ppandroid.lib_detect.DetectConfig.TF_OD_API_MODEL_FILE;

/**
 * 检测工具类
 */
public class DetectEngine {
    private Detector detector;
    DetectEnum detectEnum = DetectEnum.IDLE;//默认状态
    int cropSize = TF_OD_API_INPUT_SIZE;//输入大小
    private Bitmap croppedBitmap = null;

    private Context context;

    public DetectEngine(Context context) {

        this.context=context;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
            detectEnum = DetectEnum.INIT_SUCCESS;
        } catch (final IOException e) {
            e.printStackTrace();
            detectEnum = DetectEnum.INIT_FAILED;
        }
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

    }
    public List<Detector.Recognition> recognizeImage(){
        List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
        return  results;
    }

    public Bitmap getCroppedBitmap() {
        return croppedBitmap;
    }
    public Matrix getFrameToCropTransform(int previewWidth,int previewHeight){
        boolean MAINTAIN_ASPECT = false;

        Matrix frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        getScreenOrientation(), MAINTAIN_ASPECT);
        return frameToCropTransform;

    }

    protected int getScreenOrientation() {
        if (context instanceof Activity){
            switch (((Activity) context).getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_270:
                    return 270;
                case Surface.ROTATION_180:
                    return 180;
                case Surface.ROTATION_90:
                    return 90;
                default:
                    return 0;
            }
        }else{
            return 0;
        }

    }
}