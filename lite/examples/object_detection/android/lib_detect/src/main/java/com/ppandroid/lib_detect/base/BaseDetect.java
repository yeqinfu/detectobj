package com.ppandroid.lib_detect.base;

import android.util.Log;

public abstract class BaseDetect {
    private boolean debug = true;
    protected static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    protected static final DetectorMode MODE = DetectorMode.TF_OD_API;

    protected enum DetectorMode {
        TF_OD_API;
    }

    public boolean isDebug() {
        return debug;
    }

    private void log(String msg) {
        if (isDebug()) {
            Log.d("yeqinfu", msg);
        }

    }

    protected abstract void start();


} 