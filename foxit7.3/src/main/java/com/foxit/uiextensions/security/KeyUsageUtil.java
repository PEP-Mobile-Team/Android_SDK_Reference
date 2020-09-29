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
package com.foxit.uiextensions.security;


import android.content.Context;
import android.text.TextUtils;

import com.foxit.uiextensions.R;

public class KeyUsageUtil {
    public static final int digitalSignature = 0x0001;
    public static final int nonRepudiation = 0x0002;
    public static final int keyEncipherment = 0x0004;
    public static final int dataEncipherment = 0x0008;
    public static final int keyAgreement = 0x0010;
    public static final int keyCertSign = 0x0020;
    public static final int cRLSign = 0x0040;
    public static final int encipherOnly = 0x0080;
    public static final int decipherOnly = 0x0100;
    public static final int unknowon = -1;

    private static KeyUsageUtil mKeyUsageUtil = null;
    private Context mContext;

    private KeyUsageUtil(Context context) {
        mContext = context;
    }

    public static KeyUsageUtil getInstance(Context context) {
        if (mKeyUsageUtil == null) {
            mKeyUsageUtil = new KeyUsageUtil(context);
        }
        return mKeyUsageUtil;
    }

    public String getUsage(int keyUsage){
        String usage = "";
        if (keyUsage != 0) {
            if ((keyUsage & dataEncipherment)  == dataEncipherment) {
                usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_data_encipherment);
            }

            if ((keyUsage & digitalSignature)  == digitalSignature) {
                if (!TextUtils.isEmpty(usage)) {
                    usage += ", ";
                }
                usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_digital_signature);
            }

            if ((keyUsage & keyAgreement)  == keyAgreement) {
                if (!TextUtils.isEmpty(usage)) {
                    usage += ", ";
                }
                usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_key_agreement);
            }

            if ((keyUsage & keyCertSign)  == keyCertSign) {
                if (!TextUtils.isEmpty(usage)) {
                    usage += ", ";
                }
                usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_key_cert_sign);
            }

            if ((keyUsage & keyEncipherment)  == keyEncipherment) {
                if (!TextUtils.isEmpty(usage)) {
                    usage += ", ";
                }
                usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_key_encipherment);
            }

            if ((keyUsage & nonRepudiation)  == nonRepudiation) {
                if (!TextUtils.isEmpty(usage)) {
                    usage += ", ";
                }
                usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_non_repudiation);
            }

            if ((keyUsage & cRLSign)  == cRLSign) {
                if (!TextUtils.isEmpty(usage)) {
                    usage += ", ";
                }
                usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_crl_sign);
            }
        }

        if (TextUtils.isEmpty(usage)) {
            usage = mContext.getApplicationContext().getString(R.string.cert_key_usage);
        }
        return usage;
    }

}
