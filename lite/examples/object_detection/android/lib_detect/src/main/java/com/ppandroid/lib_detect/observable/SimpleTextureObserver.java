package com.ppandroid.lib_detect.observable;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import io.reactivex.Observable;


public class SimpleTextureObserver {
    private TextureView textureView;

    public SimpleTextureObserver(TextureView textureView) {
        this.textureView = textureView;
    }

    public Observable<Boolean> toObservable() {
        if (textureView.isAvailable()) {
            return Observable.just(true);
        }
        return Observable.create(subscriber -> textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                subscriber.onNext(true);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                subscriber.onNext(false);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        }));
    }

}