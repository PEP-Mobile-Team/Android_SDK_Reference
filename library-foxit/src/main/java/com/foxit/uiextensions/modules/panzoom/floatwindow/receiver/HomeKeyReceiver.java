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
package com.foxit.uiextensions.modules.panzoom.floatwindow.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

public class HomeKeyReceiver extends BroadcastReceiver {
    public interface IHomeKeyEventListener {
        void onHomeKeyPressed();
    }

    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private ArrayList<IHomeKeyEventListener> mHomeKeyListeners = new ArrayList<IHomeKeyEventListener>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
            String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);

            if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                onHomeKeyPressed();
            }
        }
    }

    public void registerHomeKeyEventListener(IHomeKeyEventListener listener) {
        mHomeKeyListeners.add(listener);
    }

    public void unregisterHomeKeyEventListener(IHomeKeyEventListener listener) {
        mHomeKeyListeners.remove(listener);
    }

    private void onHomeKeyPressed() {
        for (IHomeKeyEventListener listener : mHomeKeyListeners) {
            listener.onHomeKeyPressed();
        }
    }
}
