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
package com.foxit.uiextensions.pdfreader.config;

import android.os.Build;

import com.foxit.uiextensions.BuildConfig;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;


public class AppBuildConfig {
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final int SDK_VERSION = Build.VERSION.SDK_INT;
    private static String CTPAPI_END_POINT_SERVER_ADDR = "https://www-fz02.connectedpdf.com";

    static String getCwsEndPointServerAddr() {
        return CTPAPI_END_POINT_SERVER_ADDR;
    }

    public static boolean isEntEndPoint(boolean forSubscripe) {
        if (forSubscripe) {
            return !AppUtil.isEqual(getEndPointServerAddr(), CTPAPI_END_POINT_SERVER_ADDR);
        } else {
            return !AppUtil.isEqual(getEndPointServerAddr(), getCwsEndPointServerAddr());
        }
    }

    public static boolean isEntEndPoint() {
        return !AppUtil.isEqual(getEndPointServerAddr(), getCwsEndPointServerAddr());
    }

    public static boolean restoreOriginalEndPointServerAddr(IResult<Void, Void, Void> result) {
        String endPoint = getEndPointServerAddr();
        if (AppUtil.isEqual(endPoint, getCwsEndPointServerAddr())) {
            return false;
        }
        setEndPointServerAddr(getCwsEndPointServerAddr(), result);

        return true;
    }

    public static String getEndPointServerAddr() {
            return getCwsEndPointServerAddr();
    }

    public static void setEndPointServerAddr(final String server, final IResult<Void, Void, Void> result) {
        final String oldEndPoint = getEndPointServerAddr();
        if (AppUtil.isEqual(server, oldEndPoint)) {
            if (result != null) {
                result.onResult(true, null, null, null);
            }
            return;
        }
    }

    public static String getLastEndPointServerAddr() {
        return getCwsEndPointServerAddr();
    }

    public static void setLastEndPointServerAddr(final String server) {
        if (AppUtil.isEqual(server, getCwsEndPointServerAddr())) {
            return;
        }
    }
}
