/**
 * Copyright (C) 2003-2020, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.panzoom.floatwindow;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;

import com.foxit.uiextensions.modules.panzoom.floatwindow.receiver.HomeKeyReceiver;
import com.foxit.uiextensions.modules.panzoom.floatwindow.service.FloatWindowService;

public class FloatWindowController {
    private Context mContext;
    private static boolean isBindService = false;
    private View mFloatWindow = null;

    public FloatWindowController(Context context) {
        mContext = context;
    }

    public void startFloatWindowServer() {
        if (isBindService) {
            stopFloatWindowServer();
        }
        Intent intent = new Intent(mContext, FloatWindowService.class);
        mContext.startService(intent);
        isBindService = true;
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopFloatWindowServer() {
        Intent intent = new Intent(mContext, FloatWindowService.class);
        if (isBindService) {
            mContext.unbindService(mServiceConnection);
            isBindService = false;
        }
        mContext.stopService(intent);
        mFloatWindow = null;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatWindowService.FloatWindowBinder  binder = (FloatWindowService.FloatWindowBinder) service;
            mFloatWindow = binder.getService().getFloatWindow();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public View getFloatWindow() {
        return mFloatWindow;
    }

    private HomeKeyReceiver mHomeKeyReceiver = null;
    public void registerDefaultHomeKeyReceiver() {
        mHomeKeyReceiver = new HomeKeyReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        mContext.registerReceiver(mHomeKeyReceiver, homeFilter);
    }

    public void unregisterDefaultHomeKeyReceiver() {
        if (null != mHomeKeyReceiver) {
            mContext.unregisterReceiver(mHomeKeyReceiver);
        }
    }

    public void registerHomeKeyEventListener(HomeKeyReceiver.IHomeKeyEventListener listener) {
        if (mHomeKeyReceiver == null) return;
        mHomeKeyReceiver.registerHomeKeyEventListener(listener);
    }

    public void unregisterHomeKeyEventListener(HomeKeyReceiver.IHomeKeyEventListener listener) {
        if (mHomeKeyReceiver == null) return;
        mHomeKeyReceiver.unregisterHomeKeyEventListener(listener);
    }
}
