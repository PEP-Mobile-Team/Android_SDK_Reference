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
package com.foxit.uiextensions.modules.thumbnail;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.addon.xfa.XFAPage;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;

import androidx.annotation.NonNull;

public class ThumbnailItem implements Comparable<ThumbnailItem> {
    private boolean isSelected;
    private final PDFViewCtrl mPDFView;
    private Point mThumbnailSize;
    private final Point mBackgroundSize;
    private int mPageIndex = -1;
    private boolean mbNeedCompute;
    private Rect mThumbnailRect;
    private Bitmap mBitmap;

    public final static int EDIT_NO_VIEW = 0;
    public final static int EDIT_LEFT_VIEW = 1;
    public final static int EDIT_RIGHT_VIEW = 2;

    public int editViewFlag = 0;
    private boolean isRendering = false;

    public ThumbnailItem(int pageIndex, Point backgroundSize, PDFViewCtrl pdfViewCtrl) {
        mPageIndex = pageIndex;
        mPDFView = pdfViewCtrl;
        mBackgroundSize = backgroundSize;
        isSelected = false;
        mbNeedCompute = true;
    }

    public int getIndex() {
        return mPageIndex;
    }

    public void setIndex(int pageIndex) {
       this.mPageIndex = pageIndex;
    }

    public PDFViewCtrl getPDFView() {
        return mPDFView;
    }

    public boolean isRendering() {
        return isRendering;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void resetRending(boolean rendering) {
        isRendering = rendering;
    }

    public boolean needRecompute() {
        return mbNeedCompute;
    }

    public PDFPage getPage() {
        try {
            return mPDFView.getDoc().getPage(mPageIndex);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public boolean setRotation(int rotation) {
        int[] pageIndexes = new int[1];
        pageIndexes[0] = getIndex();
        boolean success = mPDFView.rotatePages(pageIndexes, rotation);
        mbNeedCompute = true;
        return success;
    }

    public int getRotation() {
        try {
            return getPage() != null ? getPage().getRotation() : 0;
        }catch (PDFException e){
            e.printStackTrace();
        }
        return 0;
    }

    private void compute() {
        if (mThumbnailRect == null) {
            mThumbnailRect = new Rect();
        }
        if (mThumbnailSize == null) {
            mThumbnailSize = new Point();
        }

        try {
            float psWidth;
            float psHeight;
            if (mPDFView.isDynamicXFA()) {
                XFAPage page = mPDFView.getXFADoc().getPage(mPageIndex);
                psWidth = page.getWidth();
                psHeight = page.getHeight();
            } else {
                PDFPage page = getPage();
                if (page == null)
                    return;
                psWidth = page.getWidth();
                psHeight = page.getHeight();
            }

            if (mPDFView.getViewRotation() == Constants.e_Rotation90 ||
                    mPDFView.getViewRotation() == Constants.e_Rotation270) {
                float tmp = psWidth;
                psWidth = psHeight;
                psHeight = tmp;
            }

            float scale = Math.min(mBackgroundSize.x / psWidth, mBackgroundSize.y / psHeight);
            psWidth *= scale;
            psHeight *= scale;
            int left = (int) (mBackgroundSize.x / 2.0f - psWidth / 2.0f);
            int top = (int) (mBackgroundSize.y / 2.0f - psHeight / 2.0f);
            final int right = mBackgroundSize.x - left;
            final int bottom = mBackgroundSize.y - top;
            mThumbnailRect.set(left, top, right, bottom);
            mThumbnailSize.set((int) psWidth, (int) psHeight);

            mbNeedCompute = false;
        }catch (PDFException e){
            e.printStackTrace();
        }
    }

    public Point getSize() {
        if (mbNeedCompute)
            compute();
        return new Point(mThumbnailSize);
    }

    public Rect getRect() {
        if (mbNeedCompute)
            compute();
        return new Rect(mThumbnailRect);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ThumbnailItem)) return false;
        return this == o || this.getIndex() == ((ThumbnailItem) o).getIndex();
    }

    @Override
    public int compareTo(@NonNull ThumbnailItem another) {
        return getIndex() - another.getIndex();
    }
}
