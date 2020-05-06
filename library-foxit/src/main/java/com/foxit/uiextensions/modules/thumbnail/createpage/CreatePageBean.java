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
package com.foxit.uiextensions.modules.thumbnail.createpage;


import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;

public class CreatePageBean {
    public static final int e_SizeLedger = 111;

    public enum PageSize {
        // unit is 1/72 inch
        SizeLetter(PDFPage.e_SizeLetter, 8.5f * 72, 11 * 72),
        SizeA3(PDFPage.e_SizeA3, 841.68f, 1190.88f),
        SizeA4(PDFPage.e_SizeA4, 595.44f, 841.68f),
        SizeLegal(PDFPage.e_SizeLegal, 8.5f * 72, 14 * 72),
        SizeLedger(e_SizeLedger, 11 * 72, 17 * 72);

        float width;
        float height;
        int type;

        PageSize(int type, float width, float height) {
            this.type = type;
            this.width = width;
            this.height = height;
        }

        public static PageSize valueOf(int type) {
            for (PageSize size : values()) {
                if (type == size.getType()) {
                    return size;
                }
            }
            throw new IllegalArgumentException("No matching constant for [ " + type + "]");
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        public float getType() {
            return type;
        }
    }

    private static final int DEFAULT_STYLE = PDFViewCtrl.PDF_PAGE_STYLE_TYPE_BLANK;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final int DEFAULT_SIZE = PDFPage.e_SizeLetter;
    private static final int DEFAULT_DIRECTION = Constants.e_Rotation0;
    private static final int DEFAULT_COUNTS = 1;

    private int mPageStyle = DEFAULT_STYLE;
    private int mPageCounts = DEFAULT_COUNTS;
    private int mPageSize = DEFAULT_SIZE;
    private int mPageColor = DEFAULT_COLOR;
    private int mPageDirection = DEFAULT_DIRECTION;
    private float mWidth = 0;
    private float mHeight = 0;

    public int getPageStyle() {
        return mPageStyle;
    }

    public void setPageStyle(int pageStyle) {
        this.mPageStyle = pageStyle;
    }

    public int getPageCounts() {
        return mPageCounts;
    }

    public void setPageCounts(int pageCounts) {
        this.mPageCounts = pageCounts;
    }

    public int getPageSize() {
        return mPageSize;
    }

    public void setPageSize(int pageSize) {
        this.mPageSize = pageSize;
    }

    public int getPageColor() {
        return mPageColor;
    }

    public void setPageColor(int pageColor) {
        this.mPageColor = pageColor;
    }

    public int getPageDirection() {
        return mPageDirection;
    }

    public void setPageDirection(int pageDirection) {
        this.mPageDirection = pageDirection;
    }

    public float getWidth() {
        if(mWidth == 0){
            mWidth = PageSize.valueOf(DEFAULT_SIZE).getWidth();
        }
        return mWidth;
    }

    public void setWidth(float width) {
        this.mWidth = width;
    }

    public float getHeight() {
        if(mHeight == 0){
            mHeight = PageSize.valueOf(DEFAULT_SIZE).getHeight();
        }
        return mHeight;
    }

    public void setHeight(float height) {
        this.mHeight = height;
    }
}
