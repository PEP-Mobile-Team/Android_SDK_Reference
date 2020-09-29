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
package com.foxit.uiextensions.modules.dynamicxfa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.addon.xfa.DocProviderCallback;
import com.foxit.sdk.addon.xfa.XFADoc;
import com.foxit.sdk.addon.xfa.XFAPage;
import com.foxit.sdk.addon.xfa.XFAWidget;
import com.foxit.sdk.common.fxcrt.PointF;
import com.foxit.sdk.common.fxcrt.RectF;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.modules.print.PDFPrint;
import com.foxit.uiextensions.modules.print.XFAPrintAdapter;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppUtil;

public class XFADocProvider extends DocProviderCallback {
    private PDFViewCtrl pdfViewCtrl;
    private boolean bWillClose = false;
    private boolean isScaling = false;
    private Context mContext;
    private Paint mCursorPaint;

    public XFADocProvider(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.pdfViewCtrl = pdfViewCtrl;

        mCursorPaint = new Paint();
        mCursorPaint.setColor(Color.BLACK);
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setAntiAlias(true);
        mCursorPaint.setDither(true);
        mCursorPaint.setStrokeWidth(AppDisplay.getInstance(mContext).dp2px(1));

        pdfViewCtrl.registerDrawEventListener(mDrawEventListener);
    }

    public void setScaleState(boolean isScaling) {
        this.isScaling = isScaling;
    }

    public void setWillClose(boolean willClose) {
        bWillClose = willClose;
    }

    @Override
    public void release() {
        pdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        pdfViewCtrl = null;
    }

    @Override
    public void invalidateRect(int page_index, RectF rect, int flag) {
        if (bWillClose || isScaling) return;
        if (!pdfViewCtrl.isPageVisible(page_index)) return;
        android.graphics.RectF viewRect = new android.graphics.RectF(0, 0, pdfViewCtrl.getDisplayViewWidth(), pdfViewCtrl.getDisplayViewHeight());
        android.graphics.RectF pdfRect = AppUtil.toRectF(rect);
        pdfViewCtrl.convertPdfRectToPageViewRect(pdfRect, pdfRect, page_index);
        android.graphics.RectF _rect = new android.graphics.RectF(pdfRect);
        pdfViewCtrl.convertPageViewRectToDisplayViewRect(pdfRect, pdfRect, page_index);
        if (!viewRect.intersect(pdfRect)) return;
        _rect.inset(-5, -5);
        pdfViewCtrl.refresh(page_index, AppDmUtil.rectFToRect(_rect));
    }

    private android.graphics.RectF mDisplayCaretRect;
    private int mDisplayCaretPageIndex;
    private boolean mCaretIsVisible = false;
    private boolean mCursorCountdown = false;
    private Handler handler = new Handler();

    @Override
    public void displayCaret(int page_index, boolean is_visible, RectF rect) {
        mCaretIsVisible = is_visible;
        mDisplayCaretRect = AppUtil.toRectF(rect);
        mDisplayCaretPageIndex = page_index;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {
        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            if (mDisplayCaretPageIndex == pageIndex && mCaretIsVisible) {
                if (!mCursorCountdown) {
                    android.graphics.RectF pageViewRectF = new android.graphics.RectF();
                    pdfViewCtrl.convertPdfRectToPageViewRect(mDisplayCaretRect, pageViewRectF, mDisplayCaretPageIndex);

                    int rotation = pdfViewCtrl.getViewRotation();
                    boolean vert = rotation == 1 || rotation == 3;
                    if (vert) {
                        float y = Math.max(pageViewRectF.top, pageViewRectF.bottom);
                        canvas.drawLine(pageViewRectF.left, y, pageViewRectF.right, y, mCursorPaint);
                    } else {
                        float x = Math.max(pageViewRectF.left, pageViewRectF.right);
                        canvas.drawLine(x, pageViewRectF.top, x, pageViewRectF.bottom, mCursorPaint);
                    }
                    mCursorPaint.setColor(mCursorPaint.getColor() == Color.BLACK ? Color.TRANSPARENT : Color.BLACK);
                    mCursorCountdown = handler.postDelayed(runnable, 400);
                }
            } else {
                if (mCursorCountdown && runnable != null) {
                    handler.removeCallbacks(runnable);
                    mCursorCountdown = false;
                }
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mCaretIsVisible) {
                android.graphics.RectF pageViewRectF = new android.graphics.RectF();
                pdfViewCtrl.convertPdfRectToPageViewRect(mDisplayCaretRect, pageViewRectF, mDisplayCaretPageIndex);
                pageViewRectF.inset(-5, -5);
                pdfViewCtrl.invalidate(AppDmUtil.rectFToRect(pageViewRectF));
            }
            mCursorCountdown = false;
        }
    };

    @Override
    public boolean getPopupPos(int page_index, float min_popup, float max_popup, RectF rect_widget, RectF inout_rect_popup) {
        Rect pageViewRect = pdfViewCtrl.getPageViewRect(page_index);
        pdfViewCtrl.convertPageViewRectToPdfRect(AppDmUtil.rectToRectF(pageViewRect), AppDmUtil.rectToRectF(pageViewRect), page_index);

        inout_rect_popup.setLeft(0);
        if (rect_widget.getRight() > pageViewRect.right) {
            inout_rect_popup.setLeft(inout_rect_popup.getLeft() - (rect_widget.getRight() - pageViewRect.right));
            inout_rect_popup.setRight(inout_rect_popup.getRight() - (rect_widget.getRight() - pageViewRect.right));
        }

        if (pageViewRect.bottom - rect_widget.getBottom() >= max_popup) {
            inout_rect_popup.setTop(Math.abs(rect_widget.getBottom() - rect_widget.getTop()));
            inout_rect_popup.setBottom(inout_rect_popup.getTop() - max_popup);
            return true;
        }

        if (rect_widget.getTop() - pageViewRect.top >= max_popup) {
            inout_rect_popup.setTop(-max_popup);
            inout_rect_popup.setBottom(inout_rect_popup.getTop() - max_popup);
            return true;
        }

        if (pageViewRect.bottom - rect_widget.getBottom() >= min_popup) {
            inout_rect_popup.setTop(Math.abs(rect_widget.getTop() - rect_widget.getBottom()));
            inout_rect_popup.setBottom(inout_rect_popup.getTop() - (pageViewRect.bottom - rect_widget.getBottom()));
            return true;
        }

        if (rect_widget.getTop() - pageViewRect.top >= min_popup) {
            inout_rect_popup.setTop(-(inout_rect_popup.getTop() - pageViewRect.top));
            inout_rect_popup.setBottom(inout_rect_popup.getTop() - (rect_widget.getTop() - pageViewRect.top));
            return true;
        }
        return false;
    }

    @Override
    public boolean popupMenu(int page_index, PointF rect_popup) {
        return true;
    }

    @Override
    public int getCurrentPage(XFADoc doc) {
        return pdfViewCtrl.getCurrentPage();
    }

    @Override
    public void setCurrentPage(XFADoc doc, int current_page_index) {
    }

    @Override
    public void setChangeMark(XFADoc doc) {
        if (pdfViewCtrl != null) {
            ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
        }
    }

    @Override
    public String getTitle(XFADoc doc) {
        return "";
    }

    private int mLastFocuIndex = -1;

    @Override
    public void setFocus(XFAWidget xfa_widget) {
        try {
            if (pdfViewCtrl == null) return;
            if (xfa_widget.isEmpty()) return;
            XFAPage xfaPage = xfa_widget.getXFAPage();
            int pageIndex = xfaPage.getIndex();
            boolean isPageVisible = false;
            int[] visiablePages = pdfViewCtrl.getVisiblePages();
            for (int i = 0;i< visiablePages.length;i++){
                int visiablePageIndex = visiablePages[i];
                if (visiablePageIndex == pageIndex){
                    isPageVisible = true;
                    break;
                }
            }

            if (!isPageVisible){
                if (pdfViewCtrl.isPageVisible(mLastFocuIndex) && mLastFocuIndex != pageIndex){
                    Rect rectF = new Rect(0, 0, pdfViewCtrl.getPageViewWidth(mLastFocuIndex), pdfViewCtrl.getPageViewHeight(mLastFocuIndex));
                    pdfViewCtrl.refresh(mLastFocuIndex, rectF);
                }

                pdfViewCtrl.gotoPage(pageIndex);
                mLastFocuIndex = pageIndex;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exportData(XFADoc doc, String file_path) {
    }

    @Override
    public void importData(XFADoc doc, String file_path) {
    }

    @Override
    public void gotoURL(XFADoc doc, String url) {
    }

    @Override
    public void print(XFADoc doc, int start_page_index, int end_page_index, int options) {
        if ((options & DocProviderCallback.e_PrintOptionShowDialog) == DocProviderCallback.e_PrintOptionShowDialog) {

            boolean isPrintAnnot = false;
            if ((options & DocProviderCallback.e_PrintOptionPrintAnnot) == DocProviderCallback.e_PrintOptionPrintAnnot) {
                isPrintAnnot = true;
            }
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
            String filename = AppFileUtil.getFileNameWithoutExt(pdfViewCtrl.getFilePath());
            XFAPrintAdapter adapter = new XFAPrintAdapter(uiExtensionsManager.getAttachedActivity(), pdfViewCtrl.getXFADoc(), filename, isPrintAnnot, null);
            new PDFPrint
                    .Builder(uiExtensionsManager.getAttachedActivity(), pdfViewCtrl.getFilePath())
                    .setAdapter(adapter)
                    .setPageCount(pdfViewCtrl.getPageCount())
                    .print();
        }
    }

    @Override
    public int getHighlightColor(XFADoc doc) {
        return (int) ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getFormHighlightColor();
    }

    @Override
    public boolean submitData(XFADoc doc, String target, int format, int text_encoding, String content) {
        return true;
    }

    @Override
    public void pageViewEvent(int page_index, int page_view_event_type) {
        try {
            pdfViewCtrl.updatePagesLayout();

            if (page_index == -1) page_index = pdfViewCtrl.getXFADoc().getPageCount();
            if (DocProviderCallback.e_PageViewEventTypeAdded == page_view_event_type) {
                ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).onXFAPagesInserted(true, page_index);
            } else if (DocProviderCallback.e_PageViewEventTypeRemoved == page_view_event_type) {
                ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).onXFAPageRemoved(true, page_index);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void widgetEvent(XFAWidget xfa_widget, int widget_event_type) {
        if (widget_event_type == XFADocProvider.e_WidgetEventTypeAdded) {
            ((UIExtensionsManager) (pdfViewCtrl.getUIExtensionsManager())).onXFAWidgetAdded(xfa_widget);
        } else if (widget_event_type == XFADocProvider.e_WidgetEventTypeBeforeRemoved) {
            ((UIExtensionsManager) (pdfViewCtrl.getUIExtensionsManager())).onXFAWidgetWillRemove(xfa_widget);
        }
    }
}
