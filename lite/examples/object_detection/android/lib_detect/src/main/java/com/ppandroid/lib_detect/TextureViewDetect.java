package com.ppandroid.lib_detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.TextureView;

import com.ppandroid.lib_detect.base.BaseDetect;
import com.ppandroid.lib_detect.observable.SimpleTextureObserver;
import com.ppandroid.lib_detect.tracking.MultiBoxTracker;
import com.ppandroid.lib_detect.view.OverlayView;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.tensorflow.lite.examples.detection.tflite.Detector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class TextureViewDetect extends BaseDetect {
    private Context context;
    private TextureView textureView;
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

    public TextureViewDetect(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
        detectEngine = new DetectEngine(context);
        tracker = new MultiBoxTracker(context);

    }

    public TextureViewDetect(Context context, TextureView textureView, OverlayView trackingOverlay) {
        this.context = context;
        this.textureView = textureView;
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
        new SimpleTextureObserver(textureView)
                .toObservable()
                //背压最后一个结果，影响不大，不可能溢出
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.newThread())
                .flatMap((Function<Boolean, Flowable<Long>>) aBoolean -> {
                    if (aBoolean) {//如果TextureView可用开启定时
                        previewWidth = textureView.getWidth();
                        previewHeight = textureView.getHeight();
                        //rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                        frameToCropTransform = detectEngine.getFrameToCropTransform(previewWidth, previewHeight);
                        cropToFrameTransform = new Matrix();
                        frameToCropTransform.invert(cropToFrameTransform);
                        tracker.setFrameConfiguration(previewWidth, previewHeight, detectEngine.getScreenOrientation());
                        return startTimeInerval();
                    } else {////如果TextureView不可用
                        return Flowable.just(-1L);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(new FlowableSubscriber<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Subscription s) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        if (trackingOverlay != null) {
                            trackingOverlay.postInvalidate();
                        }
                        rgbFrameBitmap = textureView.getBitmap();
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

                        tracker.trackResults(mappedRecognitions, aLong);
                        trackingOverlay.postInvalidate();
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    Flowable<Long> longFlowable;

    private Flowable<Long> startTimeInerval() {
        if (longFlowable == null) {
            longFlowable = Flowable.interval(500, TimeUnit.MILLISECONDS).onBackpressureLatest();
        }

        return longFlowable;
    }
}