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
package com.foxit.uiextensions.annots.textmarkup.highlight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Highlight;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.PropertyCircleItemImp;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class HighlightToolHandler implements ToolHandler {
    private Paint mPaint;
    private int mColor;
    private int mCurrentIndex;
    private int mOpacity;
    private RectF mBBoxRect;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiextensionsManager;
    private Context mContext;
    private final TextSelector mTextSelector;

    private boolean mIsContinuousCreate = false;

    public HighlightToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mTextSelector = new TextSelector(pdfViewCtrl);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mBBoxRect = new RectF();

        this.mContext = context;
        mUiextensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiextensionsManager.getMainFrame().getPropertyBar();

        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                mUiextensionsManager.setCurrentToolHandler(HighlightToolHandler.this);
                mUiextensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_HIGHLIGHT;
            }
        });
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    private void resetPropertyBar() {
        int[] pbColors = new int[PropertyBar.PB_COLORS_HIGHLIGHT.length];
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_HIGHLIGHT, 0, pbColors, 0, pbColors.length);
        pbColors[0] = PropertyBar.PB_COLORS_HIGHLIGHT[0];
        mPropertyBar.setColors(pbColors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(mOpacity));
        mPropertyBar.reset(supportProperty);
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private void resetAnnotBar(){
        mUiextensionsManager.getMainFrame().getToolSetBar().removeAllItems();

        mOKItem = new BaseItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiextensionsManager.changeState(ReadStateConfig.STATE_EDIT);
                mUiextensionsManager.setCurrentToolHandler(null);
            }
        });

        mPropertyItem = new PropertyCircleItemImp(mContext) {

            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (HighlightToolHandler.this == mUiextensionsManager.getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };
        mPropertyItem.setTag(ToolbarItemConfig.ITEM_ANNOT_PROPERTY);
        mPropertyItem.setCentreCircleColor(mColor);

        final Rect rect = new Rect();
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPropertyBar.setArrowVisible(true);
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPropertyBar.show(new RectF(rect), true);
            }
        });


        mContinuousCreateItem = new BaseItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_CONTINUE);
        mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));

        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }

                mIsContinuousCreate= !mIsContinuousCreate;
                mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));
                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });

        mUiextensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mUiextensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mUiextensionsManager.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private int getContinuousIcon(boolean isContinuous){
        int iconId;
        if (isContinuous) {
            iconId = R.drawable.rd_annot_create_continuously_true_selector;
        } else {
            iconId = R.drawable.rd_annot_create_continuously_false_selector;
        }
        return iconId;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_HIGHLIGHT;
    }

    @Override
    public void onActivate() {
        mTextSelector.clear();
        mBBoxRect.setEmpty();
        resetPropertyBar();
        resetAnnotBar();
    }

    @Override
    public void onDeactivate() {
        mTextSelector.clear();
        mBBoxRect.setEmpty();
    }


    private RectF mTmpRectF = new RectF();
    private Rect mTmpRoundRect = new Rect();

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        try {
            PDFPage page = mUiextensionsManager.getDocumentManager().getPage(pageIndex, false);
            if (page == null) {
                return false;
            }
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            int action = motionEvent.getAction();

            PointF pagePt = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    mCurrentIndex = pageIndex;
                    int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 10);
                    if (index >= 0) {
                        mTextSelector.start(page, index);
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mCurrentIndex != pageIndex) return true;
                    int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 10);
                    if (index >= 0) {
                        mTextSelector.update(page, index);
                    }
                    invalidateTouch(mPdfViewCtrl, pageIndex, mTextSelector.getBbox());
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (mTextSelector.getRectFList().size() == 0) break;
                    mTextSelector.setContents(mTextSelector.getText(page));
                    addAnnot(mCurrentIndex, true, null, null);
                    return true;
                }
                default:
                    break;
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean isContinueAddAnnot() {
        return mIsContinuousCreate;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        mIsContinuousCreate = continueAddAnnot;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mCurrentIndex != pageIndex) return;
        Rect clipRect = canvas.getClipBounds();
        for (RectF rect : mTextSelector.getRectFList()) {
            mPdfViewCtrl.convertPdfRectToPageViewRect(rect, mTmpRectF, mCurrentIndex);
            mTmpRectF.round(mTmpRoundRect);
            if (mTmpRoundRect.intersect(clipRect)) {
                canvas.save();
                canvas.drawRect(mTmpRoundRect, mPaint);
                canvas.restore();
            }
        }
    }

    protected void setPaint(int color, int opacity) {
        mColor = color;
        mOpacity = opacity;
        mPaint.setColor(calColorByMultiply(mColor, mOpacity));
        setProItemColor(color);
    }

    private void setProItemColor(int color){
        if (mPropertyItem == null) return;
        mPropertyItem.setCentreCircleColor(color);
    }

    private int calColorByMultiply(int color, int opacity) {
        int rColor = color | 0xFF000000;
        int r = (rColor & 0xFF0000) >> 16;
        int g = (rColor & 0xFF00) >> 8;
        int b = (rColor & 0xFF);
        float rOpacity = opacity / 255.0f;
        r = (int) (r * rOpacity + 255 * (1 - rOpacity));
        g = (int) (g * rOpacity + 255 * (1 - rOpacity));
        b = (int) (b * rOpacity + 255 * (1 - rOpacity));
        rColor = (rColor & 0xFF000000) | (r << 16) | (g << 8) | (b);
        return rColor;
    }

    protected void addAnnot(final int pageIndex, final boolean addUndo, final AnnotContent contentSupplier, final Event.Callback result) {
        int color = mColor;
        final Highlight annot;
        try {
            PDFPage page = mUiextensionsManager.getDocumentManager().getPage(pageIndex, false);
            annot = (Highlight) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Highlight, new com.foxit.sdk.common.fxcrt.RectF(0, 0, 0, 0)), Annot.e_Highlight);
            if (annot == null) {
                if (!misFromSelector) {
                    if (!mIsContinuousCreate) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    }
                }
                return;
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }

        final HighlightAddUndoItem undoItem = new HighlightAddUndoItem(mPdfViewCtrl);

        undoItem.mColor =  color;
        undoItem.quadPointsArray = new QuadPointsArray();
        QuadPoints quadPoint = null;
        if (contentSupplier instanceof TextMarkupContent) {
            TextMarkupContent tmContent = TextMarkupContent.class.cast(contentSupplier);
            ArrayList<PointF> pointsList = tmContent.getQuadPoints();
            for (int i = 0; i < pointsList.size() / 4; i++) {
                quadPoint = new QuadPoints(AppUtil.toFxPointF(pointsList.get(4 * i).x, pointsList.get(4 * i).y),
                        AppUtil.toFxPointF(pointsList.get(4 * i + 1).x, pointsList.get(4 * i + 1).y),
                        AppUtil.toFxPointF(pointsList.get(4 * i + 2).x, pointsList.get(4 * i + 2).y),
                        AppUtil.toFxPointF(pointsList.get(4 * i + 3).x, pointsList.get(4 * i + 3).y));
                undoItem.quadPointsArray.add(quadPoint);
            }

            undoItem.mColor =  contentSupplier.getColor();
            undoItem.mOpacity = contentSupplier.getOpacity() / 255f;
        } else if (contentSupplier instanceof TextMarkupContentAbs) {
            TextMarkupContentAbs tmSelector = TextMarkupContentAbs.class.cast(contentSupplier);
            for (int i = 0; i < tmSelector.getTextSelector().getRectFList().size(); i++) {
                RectF rect = tmSelector.getTextSelector().getRectFList().get(i);
                quadPoint = new QuadPoints();
                quadPoint.setFirst(AppUtil.toFxPointF(rect.left, rect.top));
                quadPoint.setSecond(AppUtil.toFxPointF(rect.right, rect.top));
                quadPoint.setThird(AppUtil.toFxPointF(rect.left, rect.bottom));
                quadPoint.setFourth(AppUtil.toFxPointF(rect.right, rect.bottom));

                undoItem.quadPointsArray.add(quadPoint);
            }

            undoItem.mColor =  color;
            undoItem.mOpacity = mOpacity / 255f;
            undoItem.mContents = tmSelector.getContents();
        } else if (mTextSelector != null) {
            for (int i = 0; i < mTextSelector.getRectFList().size(); i++) {
                RectF rect = mTextSelector.getRectFList().get(i);
                quadPoint = new QuadPoints();
                quadPoint.setFirst(AppUtil.toFxPointF(rect.left, rect.top));
                quadPoint.setSecond(AppUtil.toFxPointF(rect.right, rect.top));
                quadPoint.setThird(AppUtil.toFxPointF(rect.left, rect.bottom));
                quadPoint.setFourth(AppUtil.toFxPointF(rect.right, rect.bottom));

                undoItem.quadPointsArray.add(quadPoint);
            }

            undoItem.mColor =  color;
            undoItem.mOpacity = mOpacity / 255f;
            undoItem.mContents = mTextSelector.getContents();
            undoItem.mFlags = Annot.e_FlagPrint;

        } else {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }

        undoItem.mNM = AppDmUtil.randomUUID(null);
        undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
        undoItem.mPageIndex = pageIndex;
        undoItem.mSubject = "Highlight";

        HighlightEvent event = new HighlightEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    try {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(mPdfViewCtrl.getDoc().getPage(pageIndex), annot);
                        if (addUndo) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF rectF = new RectF(AppUtil.toRectF(annot.getRect()));
                            invalidate(mPdfViewCtrl, pageIndex, rectF);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }

                    mTextSelector.clear();
                    mBBoxRect.setEmpty();
                    if (!misFromSelector) {
                        if (!mIsContinuousCreate) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        }
                    }
                    misFromSelector = false;
                }

                if (result != null) {
                    result.result(null, success);
                }
            }
        });

        mPdfViewCtrl.addTask(task);
    }

    private void invalidateTouch(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF rectF) {
        if (rectF == null) return;
        RectF rBBox = new RectF(rectF);
        pdfViewCtrl.convertPdfRectToPageViewRect(rBBox, rBBox, pageIndex);
        pdfViewCtrl.convertPageViewRectToDisplayViewRect(rBBox, rBBox, pageIndex);
        RectF rCalRectF = AppUtil.calculateRect(mBbox, mBBoxRect);
        rCalRectF.roundOut(mInvalidateRect);
        pdfViewCtrl.invalidate(mInvalidateRect);
        mBBoxRect.set(rBBox);
    }

    private RectF mBbox = new RectF();
    private Rect mInvalidateRect = new Rect();

    private void invalidate(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF rectF) {
        if (rectF == null || !pdfViewCtrl.isPageVisible(pageIndex)) return;
        mBbox.set(rectF);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, mBbox, pageIndex);
        mBbox.roundOut(mInvalidateRect);
        pdfViewCtrl.refresh(pageIndex, mInvalidateRect);
    }

    protected void removeProbarListener() {
        mPropertyChangeListener = null;
    }

    private boolean misFromSelector = false;

    public void setFromSelector(boolean b) {
        misFromSelector = b;
    }
}
