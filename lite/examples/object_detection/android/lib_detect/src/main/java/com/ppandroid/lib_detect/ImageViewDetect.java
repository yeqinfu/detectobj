package com.ppandroid.lib_detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.ImageView;

import com.ppandroid.lib_detect.base.BaseDetect;
import com.ppandroid.lib_detect.tracking.MultiBoxTracker;
import com.ppandroid.lib_detect.view.OverlayView;

import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ImageViewDetect extends BaseDetect {
    private Context context;
    private ImageView imageView;
    private DetectEngine detectEngine;

    /*textureView的宽高*/
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    /*textureView的宽高 对应大小的帧图*/
    private Bitmap rgbFrameBitmap = null;

    /*追踪的view*/
    OverlayView trackingOverlay;
    private MultiBoxTracker tracker;
    /*TextureView的矩阵到检测矩阵的矩阵变化*/
    private Matrix frameToCropTransform;
    /*逆矩阵*/
    private Matrix cropToFrameTransform;

    public ImageViewDetect(Context context, ImageView imageView) {
        this.context = context;
        this.imageView = imageView;
        detectEngine = new DetectEngine(context);
        tracker = new MultiBoxTracker(context);

    }

    public ImageViewDetect(Context context, ImageView imageView, OverlayView trackingOverlay) {
        this.context = context;
        this.imageView = imageView;
        detectEngine = new DetectEngine(context);
        setTrackingOverlay(trackingOverlay);
        tracker = new MultiBoxTracker(context);
    }

    public void setTrackingOverlay(OverlayView trackingOverlay) {
        this.trackingOverlay = trackingOverlay;
        trackingOverlay.addCallback(
                canvas -> {
                    tracker.draw(canvas);
                    if (isDebug()) {
                        tracker.drawDebug(canvas);
                    }
                });
    }

    private Bitmap cropCopyBitmap = null;

    @Override
    public void start() {
        Observable.just(imageView)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<ImageView>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull ImageView imageView) {
                        previewWidth = imageView.getWidth();
                        previewHeight = imageView.getHeight();
                        //rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                        frameToCropTransform = detectEngine.getFrameToCropTransform(previewWidth, previewHeight);
                        cropToFrameTransform = new Matrix();
                        frameToCropTransform.invert(cropToFrameTransform);
                        tracker.setFrameConfiguration(previewWidth, previewHeight, detectEngine.getScreenOrientation());


                        if (trackingOverlay != null) {
                            trackingOverlay.postInvalidate();
                        }

                        imageView.setDrawingCacheEnabled(true);
                        rgbFrameBitmap= Bitmap.createBitmap(imageView.getDrawingCache());
                        imageView.setDrawingCacheEnabled(false);
                        final Canvas canvas = new Canvas(detectEngine.getCroppedBitmap());
                        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
                        final List<Detector.Recognition> results = detectEngine.recognizeImage();
                        cropCopyBitmap = Bitmap.createBitmap(detectEngine.getCroppedBitmap());
                        final Canvas canvas2 = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Detector.Recognition> mappedRecognitions =
                                new ArrayList<Detector.Recognition>();

                        for (final Detector.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }

                        tracker.trackResults(mappedRecognitions, 0);
                        trackingOverlay.postInvalidate();

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });



    }



} 