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
package com.foxit.uiextensions.security.digitalsignature;

import com.foxit.sdk.pdf.TrustedCertStoreCallback;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;

import org.bouncycastle.cert.X509CertificateHolder;

import java.io.IOException;
import java.math.BigInteger;

public class FxTrustedCertStoreCallback extends TrustedCertStoreCallback {
    private DigitalSignatureUtil mDsgUtil;

    public FxTrustedCertStoreCallback(DigitalSignatureUtil util) {
        mDsgUtil = util;
    }

    @Override
    public boolean isCertTrusted(byte[] cert) {
        try {
            X509CertificateHolder x509CertificateHolder = new X509CertificateHolder(cert);
            BigInteger bigInteger = x509CertificateHolder.getSerialNumber();
            if (bigInteger.compareTo(BigInteger.ZERO) < 0) {
                bigInteger = new BigInteger(1, bigInteger.toByteArray());
            }
            String sn = bigInteger.toString(16).toUpperCase();
            CertificateFileInfo fileInfo = mDsgUtil.getCertDataSupport().queryTrustCert(sn);
            return fileInfo != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isCertTrustedRoot(byte[] cert) {
        //default: use trusted cert as root.
        return isCertTrusted(cert);
    }
}
