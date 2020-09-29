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
package com.foxit.uiextensions.annots.form;

import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.sdk.pdf.interform.FillerAssistCallback;
import com.foxit.sdk.pdf.interform.TimerCallback;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class FormFillerAssistImpl extends FillerAssistCallback {

    private PDFViewCtrl mPDFViewCtrl;
    protected boolean bWillClose = false;
    boolean bWillSave = false;
    private boolean isScaling = false;
    private FormFillerAnnotHandler.FillerFocusEventListener mFocusEventListener;

    public FormFillerAssistImpl(PDFViewCtrl pdfViewCtrl, FormFillerAnnotHandler.FillerFocusEventListener focusEventListener) {
        this.mPDFViewCtrl = pdfViewCtrl;
        mFocusEventListener = focusEventListener;
    }

    public void setScaling(boolean scaling) {
        isScaling = scaling;
    }

    @Override
    public void focusGotOnControl(Control control, String filedValue) {
        if (mFocusEventListener != null)
            mFocusEventListener.focusGotOnControl(control, filedValue);
    }

    @Override
    public void focusLostFromControl(Control control, String filedValue) {
        if (mFocusEventListener != null)
            mFocusEventListener.focusLostFromControl(control, filedValue);
    }

    private RectF mLastRefreshPdfRect;

    @Override
    public void refresh(PDFPage page, com.foxit.sdk.common.fxcrt.RectF rect) {
        if (bWillClose || bWillSave || isScaling) return;
        try {
            if (mLastRefreshPdfRect != null && !mLastRefreshPdfRect.equals(AppUtil.toRectF(rect))) {
                int pageIndex = page.getIndex();
                if (!mPDFViewCtrl.isPageVisible(pageIndex)) return;
                RectF viewRect = new RectF(0, 0, mPDFViewCtrl.getDisplayViewWidth(), mPDFViewCtrl.getDisplayViewHeight());
                RectF pdfRect = AppUtil.toRectF(rect);
                mPDFViewCtrl.convertPdfRectToPageViewRect(pdfRect, pdfRect, pageIndex);
                RectF _rect = new RectF(pdfRect);
                mPDFViewCtrl.convertPageViewRectToDisplayViewRect(pdfRect, pdfRect, pageIndex);
                if (!viewRect.intersect(pdfRect)) return;
                _rect.inset(-5, -5);
                mPDFViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(_rect));
            }
            mLastRefreshPdfRect = AppUtil.toRectF(rect);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
    }

    @Override
    public boolean setTimerCallback(int elapse, TimerCallback timer, Integer out_timer_id) {
        return false;
    }

    @Override
    public boolean killTimer(int timer_id) {
        return false;
    }
}
