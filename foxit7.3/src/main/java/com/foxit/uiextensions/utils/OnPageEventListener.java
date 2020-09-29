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
package com.foxit.uiextensions.utils;

import com.foxit.sdk.PDFViewCtrl;

public class OnPageEventListener implements PDFViewCtrl.IPageEventListener{
    @Override
    public void onPageChanged(int oldPageIndex, int curPageIndex) {

    }

    @Override
    public void onPageInvisible(int index) {

    }

    @Override
    public void onPageVisible(int index) {

    }

    @Override
    public void onPageJumped() {

    }

    @Override
    public void onPagesWillRotate(int[] pageIndexes, int rotation) {

    }

    @Override
    public void onPagesWillRemove(int[] pageIndexes) {

    }

    @Override
    public void onPageWillMove(int index, int dstIndex) {

    }

    @Override
    public void onPageMoved(boolean success, int index, int dstIndex) {

    }

    @Override
    public void onPagesRemoved(boolean success, int[] pageIndexes) {

    }

    @Override
    public void onPagesRotated(boolean success, int[] pageIndexes, int rotation) {

    }

    @Override
    public void onPagesWillInsert(int dstIndex, int[] pageRanges) {

    }

    @Override
    public void onPagesInserted(boolean success, int dstIndex, int[] range) {

    }
}
