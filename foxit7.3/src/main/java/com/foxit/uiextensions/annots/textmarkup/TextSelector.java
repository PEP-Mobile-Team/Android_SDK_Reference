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
package com.foxit.uiextensions.annots.textmarkup;

import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public class TextSelector {

    private RectF mBBox;
    private ArrayList<RectF> mRectFList;

    private int mStartChar;
    private int mEndChar;
    private String mContents;

    private PDFViewCtrl mPdfViewCtrl;

    public TextSelector(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mStartChar = mEndChar = -1;
        mBBox = new RectF();
        mRectFList = new ArrayList<RectF>();
    }

    public void clear() {
        mStartChar = mEndChar = -1;
        mBBox.setEmpty();
        mRectFList.clear();
    }

    public void setStart(int start) {
        mStartChar = start;
    }

    public void setEnd(int end) {
        mEndChar = end;
    }

    public int getStart() {
        return mStartChar;
    }

    public int getEnd() {
        return mEndChar;
    }

    public void start(PDFPage page, int start) {
        computeSelected(page, start, start);
    }

    public void update(PDFPage page, int update) {
        if (mStartChar < 0) mStartChar = update;
        computeSelected(page, mStartChar, update);
    }

    public String getText(PDFPage page) {
        int start = Math.min(mStartChar, mEndChar);
        int end = Math.max(mStartChar, mEndChar);
        try {
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            mContents = textPage.getChars(start, end - start + 1);
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return null;
        }
        return mContents;
    }

    public RectF getBbox() {
        return mBBox;
    }

    public ArrayList<RectF> getRectFList() {
        return mRectFList;
    }

    public void computeSelected(PDFPage page, int start, int end) {
        if (page == null || start == -1) return;
        this.mStartChar = start;
        this.mEndChar = end;
        if (end < start) {
            int tmp = end;
            end = start;
            start = tmp;
        }
        mRectFList.clear();
        try {
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            int count = textPage.getTextRectCount(start, end - start + 1);
            for (int i = 0; i < count; i++) {
                RectF rectF = new RectF(AppUtil.toRectF(textPage.getTextRect(i)));
                mRectFList.add(rectF);
                if (i == 0) {
                    mBBox = new RectF(rectF);
                } else {
                    adjustBbox(mBBox, rectF);
                }
            }

        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    private void adjustBbox(RectF dst, RectF rect) {
        if (rect.left < dst.left) dst.left = rect.left;
        if (rect.right > dst.right) dst.right = rect.right;
        if (rect.bottom < dst.bottom) dst.bottom = rect.bottom;
        if (rect.top > dst.top) dst.top = rect.top;
    }

    public String getContents() {
        return mContents;
    }

    public void setContents(String mContents) {
        this.mContents = mContents;
    }
}
