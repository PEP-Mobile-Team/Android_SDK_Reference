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
package com.foxit.uiextensions.annots.stamp;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.fxcrt.RectF;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StampEvent extends EditAnnotEvent {

    public StampEvent(int eventType, StampUndoItem undoItem, Stamp stamp, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = stamp;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }
        Stamp annot = (Stamp) mAnnot;
        StampAddUndoItem undoItem = (StampAddUndoItem) mUndoItem;
        FileOutputStream fos = null;
        try {
            annot.setUniqueID(mUndoItem.mNM);
            annot.setFlags(Annot.e_FlagPrint);

            annot.setFlags(mUndoItem.mFlags);
            if (mUndoItem.mCreationDate != null && AppDmUtil.isValidDateTime(mUndoItem.mCreationDate)) {
                annot.setCreationDateTime(mUndoItem.mCreationDate);
            }

            if (mUndoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(mUndoItem.mModifiedDate)) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }

            if (mUndoItem.mAuthor != null) {
                annot.setTitle(mUndoItem.mAuthor);
            }

            if (mUndoItem.mSubject != null) {
                annot.setSubject(mUndoItem.mSubject);
            }

            if (undoItem.mIconName != null) {
                annot.setIconName(undoItem.mIconName);
            }
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }
            annot.setContent(mUndoItem.mContents);
            if (undoItem.mStampType >= 17 && undoItem.mStampType <= 21) {
                if (DynamicStampIconProvider.getInstance().getDoc(undoItem.mIconName + Annot.e_Stamp) == null) {// no cache
                    String filename = "DynamicStamps/" + undoItem.mIconName.substring(4, undoItem.mIconName.length()) + ".pdf";
                    InputStream is = mPdfViewCtrl.getContext().getAssets().open(filename);
                    if (is == null) {
                        return  false;
                    }
                    byte[] buffer = new byte[1 << 13];

                    String path = AppFileUtil.getDiskCachePath(mPdfViewCtrl.getContext());
                    if (path == null) {
                        path = "/mnt/sdcard/FoxitSDK/";
                    } else if (!path.endsWith("/")) {
                        path += "/";
                    }

                    String dirPath = path + "DynamicStamps/";
                    File dir = new File(dirPath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    path += filename;

                    File file = new File(path);
                    fos = new FileOutputStream(file);
                    int n = 0;
                    while(-1 != (n = is.read(buffer))) {
                        fos.write(buffer, 0, n);
                    }

                    PDFDoc pdfDoc = new PDFDoc(path);
                    pdfDoc.load(null);
                    DynamicStampIconProvider.getInstance().addDocMap(undoItem.mIconName + Annot.e_Stamp, pdfDoc);

                    is.close();
                }
            } else {
                if (undoItem.mBitmap != null){
                    annot.setBitmap(undoItem.mBitmap);
                }
            }

            annot.setRotation(undoItem.mRotation * 90);
            annot.resetAppearanceStream();

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    @Override
    public boolean modify() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }
        Stamp annot = (Stamp) mAnnot;
        try {
            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }
            annot.setContent(mUndoItem.mContents);
            RectF rectF = new RectF(mUndoItem.mBBox.left, mUndoItem.mBBox.bottom, mUndoItem.mBBox.right, mUndoItem.mBBox.top);
            annot.move(rectF);
            annot.resetAppearanceStream();
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }

        try {
            ((Markup)mAnnot).removeAllReplies();
            PDFPage page = mAnnot.getPage();
            page.removeAnnot(mAnnot);
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }
}
