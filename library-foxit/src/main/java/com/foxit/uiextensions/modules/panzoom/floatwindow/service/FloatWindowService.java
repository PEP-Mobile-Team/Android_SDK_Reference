/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.panzoom.floatwindow.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.View;

import com.foxit.uiextensions.modules.panzoom.PanZoomView;
import com.foxit.uiextensions.modules.panzoom.floatwindow.FloatWindowManager;
import com.foxit.uiextensions.modules.panzoom.floatwindow.FloatWindowUtil;

import androidx.annotation.Nullable;

public class FloatWindowService extends Service {
    public class FloatWindowBinder extends Binder {
        public FloatWindowService getService() {
            return FloatWindowService.this;
        }
    }

    private FloatWindowManager floatWindowManager;
    private PanZoomView mPanZoomView;
    private FloatWindowBinder mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new FloatWindowBinder();

        floatWindowManager = new FloatWindowManager(this);
        mPanZoomView = new PanZoomView(this, FloatWindowUtil.getInstance().getPdfViewCtrl());
        floatWindowManager.addFloatWindow(mPanZoomView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        floatWindowManager.removeFloatWindow();
        mPanZoomView = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    public View getFloatWindow() {
        return mPanZoomView;
    }
}
