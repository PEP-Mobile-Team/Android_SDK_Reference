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
package com.foxit.uiextensions.security.digitalsignature;

import com.foxit.sdk.pdf.TrustedCertStoreCallback;

import org.bouncycastle.cert.X509CertificateHolder;

import java.io.IOException;

public class FxTrustedCertStoreCallback extends TrustedCertStoreCallback {
    private X509CertificateHolder mCertificate;
    public FxTrustedCertStoreCallback(X509CertificateHolder x509CertificateHolder) {
        mCertificate = x509CertificateHolder;
    }

    @Override
    public boolean isCertTrusted(byte[] cert) {
        if (mCertificate == null) {
            return false;
        }
        try {
            X509CertificateHolder x509CertificateHolder = new X509CertificateHolder(cert);
            if (x509CertificateHolder.equals(mCertificate)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
