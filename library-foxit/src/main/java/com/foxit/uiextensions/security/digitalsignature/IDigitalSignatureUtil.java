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

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.foxit.sdk.pdf.Signature;


public interface IDigitalSignatureUtil {
    public void   addCertSignature(String docPath, String certPath, Bitmap bitmap, RectF rectF, int pageIndex, IDigitalSignatureCreateCallBack callBack);
    public void   addCertSignature(String docPath, String certPath, Signature signature, RectF rectF, int pageIndex, boolean isCustom, IDigitalSignatureCreateCallBack callBack);
    public void   addCertList(IDigitalSignatureCallBack callBack);
}
