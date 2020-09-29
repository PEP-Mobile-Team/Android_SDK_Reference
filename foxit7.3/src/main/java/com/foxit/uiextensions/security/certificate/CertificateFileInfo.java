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
package com.foxit.uiextensions.security.certificate;

import com.foxit.uiextensions.modules.panel.bean.BaseBean;
import com.foxit.uiextensions.security.ICertificateSupport;

public class CertificateFileInfo extends BaseBean {

    public String filePath;
    public String fileName;
    public String issuer;
    public String serialNumber;
    public String publisher;
    public String password;
    public int permCode;

    public boolean isCertFile;
    public boolean isTrustCert;
    public boolean selected;
    public int radioButtonID;
    public ICertificateSupport.CertificateInfo certificateInfo;

    public String certName;
    public String subject;
    public int keyUsage;
    public String emailAddress;
    public String validFrom;
    public String validTo;

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CertificateFileInfo)) return false;
        CertificateFileInfo fi = (CertificateFileInfo) o;
        if (fi.isCertFile != isCertFile) return false;
        if (filePath == null) return false;
        return filePath.equals(fi.filePath);
    }
}
