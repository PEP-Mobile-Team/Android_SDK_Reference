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
package com.foxit.uiextensions.annots.multimedia;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneStateBroadCastReceiver extends BroadcastReceiver {

    private static AudioPlayService audioPlayService;

    public PhoneStateBroadCastReceiver(){}

    public void setAudioPlay(AudioPlayService aps) {
        audioPlayService = aps;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (audioPlayService == null) return;
        if (intent != null
                && intent.getAction() != null
                && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            audioPlayService.mute(true);
        } else {
            TelephonyManager tManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            switch (tManager.getCallState()) {
                case TelephonyManager.CALL_STATE_RINGING:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    audioPlayService.mute(true);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    audioPlayService.mute(false);
                    break;
                default:
                    break;
            }
        }
    }
}
