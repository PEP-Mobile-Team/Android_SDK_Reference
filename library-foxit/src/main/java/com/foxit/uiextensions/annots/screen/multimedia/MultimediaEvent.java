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
package com.foxit.uiextensions.annots.screen.multimedia;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Image;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Rendition;
import com.foxit.sdk.pdf.actions.Action;
import com.foxit.sdk.pdf.actions.RenditionAction;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.ByteArrayOutputStream;

public class MultimediaEvent extends EditAnnotEvent {

    private MultimediaUtil mMultimediaUtil;

    public MultimediaEvent(int eventType, MultimediaUndoItem undoItem, Screen screen, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = screen;
        mPdfViewCtrl = pdfViewCtrl;

        mMultimediaUtil = new MultimediaUtil(mPdfViewCtrl.getContext());
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Screen)) {
            return false;
        }
        Screen annot = (Screen) mAnnot;
        MultimediaUndoItem undoItem = (MultimediaUndoItem) mUndoItem;
        try {
            if (mUndoItem.mModifiedDate != null && AppDmUtil.isValidDateTime(mUndoItem.mModifiedDate)) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }
            annot.setFlags(mUndoItem.mFlags);
            annot.setUniqueID(mUndoItem.mNM);
            annot.setContent(undoItem.mContents);
            annot.setBorderColor(mUndoItem.mColor);
            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setWidth(mUndoItem.mLineWidth);
            annot.setBorderInfo(borderInfo);

            // Prepare rendition action
            Action action = Action.create(mPdfViewCtrl.getDoc(), Action.e_TypeRendition);
            RenditionAction renditionAction = new RenditionAction(action);
            renditionAction.setOperationType(RenditionAction.e_OpTypeAssociate);
            renditionAction.setScreenAnnot(annot);

            // Prepare rendition
            Rendition rendition = new Rendition(mPdfViewCtrl.getDoc(), null);
            rendition.setRenditionName(undoItem.mFileName);
            rendition.setMediaClipName(undoItem.mFileName);
            rendition.setPermission(Rendition.e_MediaPermTempAccess);

            // Prepare Filespec
            FileSpec fileSpec = new FileSpec(mPdfViewCtrl.getDoc());
            fileSpec.setFileName(undoItem.mFileName);
            fileSpec.embed(undoItem.mFilePath);
            rendition.setMediaClipFile(fileSpec);
            rendition.setMediaClipContentType(undoItem.mMediaClipContentType);
            renditionAction.insertRendition(rendition, -1);
            annot.setAction(renditionAction);

            if (undoItem.mPDFDictionary == null) {
                Bitmap bitmap = undoItem.mPreviewBitmap;
                if (bitmap == null || bitmap.isRecycled()) {
                    if (undoItem.mMediaClipContentType.contains("audio")) {
                        bitmap = BitmapFactory.decodeResource(mPdfViewCtrl.getContext().getResources(), R.drawable.audio_play);
                    } else {
                        bitmap = mMultimediaUtil.getVideoThumbnail(mPdfViewCtrl, undoItem.mFilePath);
                    }
                }

                byte[] bytes = bitmap2Bytes(bitmap);
                Image image = new Image(bytes);
                annot.setImage(image, 0, 0);
                bitmap.recycle();
                undoItem.mPreviewBitmap = null;
                bitmap = null;
            } else {
                annot.setMKDict(undoItem.mPDFDictionary);
            }

            annot.setRotation(undoItem.mRotation);
            annot.resetAppearanceStream();

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
        return false;
    }

    @Override
    public boolean modify() {
        if (mAnnot == null || !(mAnnot instanceof Screen)) {
            return false;
        }
        Screen annot = (Screen) mAnnot;
        try {
            annot.setBorderColor(mUndoItem.mColor);
            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setWidth(mUndoItem.mLineWidth);
            annot.setBorderInfo(borderInfo);

            MultimediaModifyUndoItem undoItem = (MultimediaModifyUndoItem) mUndoItem;
            annot.move(AppUtil.toFxRectF(undoItem.mBBox));
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.resetAppearanceStream();
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof Screen)) {
            return false;
        }

        try {
            PDFPage page = mAnnot.getPage();
            page.removeAnnot(mAnnot);
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

}
