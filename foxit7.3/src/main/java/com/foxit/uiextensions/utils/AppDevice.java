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
package com.foxit.uiextensions.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import java.util.Locale;

public class AppDevice {
    public static final String PRODUCT_NAME = "FOXIT MOBILE SDK FOR ANDROID";
    public static final String PRODUCT_VENDOR = "FOXIT";
    static String mDeviceId = null;

    public static final int REQUEST_READ_PHONE_STATE = 100;
    private static final String[] PERMISSIONS_READ_PHONE_STATE = {
            Manifest.permission.READ_PHONE_STATE,
    };

    public static String getDeviceId(Activity activity) {
        if (mDeviceId != null)
            return mDeviceId;
        if (activity == null) {
//            AppLogger.i("!!!!!getDeviceId", "activity is null!!");
            return null;
        }

        boolean hasPermission = true;
        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;
            } else {
                hasPermission = false;
            }
        }

        try {
            TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = null;
            String androidId = null;
            if (tm != null && hasPermission) {
                deviceId = tm.getDeviceId();
            }
            androidId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);

            if (deviceId != null) {
//                AppLogger.i("==getDeviceId", "deviceId:" + deviceId);
            }
            if (androidId != null) {
//                AppLogger.i("==getDeviceId", "androidId:" + androidId);
            }

            if (deviceId != null && androidId != null) {
                mDeviceId = deviceId + androidId;
                return deviceId + androidId;
            } else if (deviceId != null || androidId != null) {
                mDeviceId = deviceId != null ? deviceId : androidId;
                return deviceId != null ? deviceId : androidId;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String mMacAddr = null;

    public static String getMacAddr(Activity activity) {
        if (mMacAddr != null)
            return mMacAddr;
        String macAddr = "00-00-00-00-00-00";
        if (activity == null) {
            return macAddr;
        }
        try {
            WifiManager wifimng = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifimng != null) {
                WifiInfo info = wifimng.getConnectionInfo();
                macAddr = info.getMacAddress();
                if (macAddr != null) {
                    macAddr = macAddr.toUpperCase(Locale.getDefault()).replace(":", "-");
                } else {
                    macAddr = "00-00-00-00-00-00";
                }
            }
//            AppLogger.i("==FX_DSModule", "macAddr:" + macAddr);
            mMacAddr = macAddr;
        } catch (Exception e) {
        }
        return macAddr;
    }

    public static String getDeviceModel() {
        return new Build().MODEL;
    }

    public static String getDeviceName() {
        return android.os.Build.MANUFACTURER;
    }
}
