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
package com.foxit.uiextensions.utils.event;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.util.ArrayList;


public class AppEventManager implements ILifecycleEventListener {

    private ArrayList<ILifecycleEventListener> mLifecycleEventList;


    public AppEventManager() {
        mLifecycleEventList = new ArrayList<ILifecycleEventListener>();
//        App.getInstance().getThreadManager().getMainThreadHandler().postDelayed(timeRunnable, 1000);
    }

    Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            AppThreadManager.getInstance().getMainThreadHandler().postDelayed(timeRunnable, 1000);
        }
    };

    @Override
    public void onCreate(Activity act, Bundle savedInstanceState) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onCreate(act, savedInstanceState);
        }
    }

    @Override
    public void onStart(Activity act) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onStart(act);
        }
    }

    @Override
    public void onPause(Activity act) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onPause(act);
        }
    }

    @Override
    public void onResume(Activity act) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onResume(act);
        }
    }

    @Override
    public void onStop(Activity act) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onStop(act);
        }
    }

    @Override
    public void onDestroy(Activity act) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onDestroy(act);
        }
    }

    @Override
    public void onSaveInstanceState(Activity act, Bundle bundle) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onSaveInstanceState(act, bundle);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onHiddenChanged(hidden);
        }
    }

    @Override
    public void onActivityResult(Activity act, int requestCode, int resultCode, Intent data) {
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onActivityResult(act, requestCode, resultCode, data);
        }
    }

}
