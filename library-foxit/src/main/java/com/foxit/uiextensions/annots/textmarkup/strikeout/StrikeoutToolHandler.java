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
package com.foxit.uiextensions.annots.textmarkup.strikeout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupUtil;
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

public class StrikeoutToolHandler implements ToolHandler {
    private Context mContext;
    private Paint mPaint;
    private int mColor;
    private int mCurrentIndex;
    private int mOpacity;
    protected SelectInfo mSelectInfo;
    private RectF mTmpRect;
    private RectF mTmpDesRect;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiextensionsManager;

    private boolean mIsContinuousCreate = false;

    public StrikeoutToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mSelectInfo = new SelectInfo();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mTmpRect = new RectF();
        mTmpDesRect = new RectF();

        init();
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removeProbarListener() {
        mPropertyChangeListener = null;
    }

    private void init() {
        mUiextensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        //PropertyBar
        mPropertyBar = mUiextensionsManager.getMainFrame().getPropertyBar();

        if (mUiextensionsManager.getConfig().modules.getAnnotConfig().isLoadStrikeout()){
            mUiextensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
                @Override
                public void onMTClick(int type) {
                    mUiextensionsManager.setCurrentToolHandler(StrikeoutToolHandler.this);
                    mUiextensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                }

                @Override
                public int getType() {
                    return MoreTools.MT_TYPE_STRIKEOUT;
                }
            });
        }
    }

    protected void unInit() {
    }


    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_STRIKEOUT;
    }

    @Override
    public void onActivate() {
        resetLineData();
        resetPropertyBar();
        resetAnnotBar();
    }

    private void resetLineData() {
        mSelectInfo.mStartChar = mSelectInfo.mEndChar = -1;
        mSelectInfo.mRectArray.clear();
        mSelectInfo.mBBox.setEmpty();
        mTmpRect.setEmpty();
    }

    @Override
    public void onDeactivate() {
    }


    private boolean onSelectDown(int pageIndex, PointF point, SelectInfo selectInfo) {
        if (selectInfo == null) return false;
        try {
            mCurrentIndex = pageIndex;
            selectInfo.mRectArray.clear();
            selectInfo.mStartChar = selectInfo.mEndChar = -1;
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (!page.isParsed()) {
                Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, false);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }
            }
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);

            PointF pagePt = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, pagePt, mCurrentIndex);
            int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 30);
            if (index >= 0) {
                selectInfo.mStartChar = selectInfo.mEndChar = index;
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return false;
        }
        return true;
    }

    private boolean onSelectMove(int pageIndex, PointF point, SelectInfo selectInfo) {
        if (selectInfo == null) return false;
        if (mCurrentIndex != pageIndex) return false;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (!page.isParsed()) {
                Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, false);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }
            }
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);

            PointF pagePt = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, pagePt, mCurrentIndex);
            int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 30);
            if (index >= 0) {
                if (selectInfo.mStartChar < 0) selectInfo.mStartChar = index;
                selectInfo.mEndChar = index;
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return false;
        }
        return true;
    }

    protected boolean onSelectRelease(int pageIndex, SelectInfo selectInfo, boolean addUndo, Event.Callback result) {
        if (selectInfo == null) return false;
        int size = selectInfo.mRectArray.size();
        if (size == 0) return false;
        RectF rectF = new RectF();

        mPdfViewCtrl.convertPageViewRectToPdfRect(selectInfo.mBBox, rectF, pageIndex);
        addAnnot(pageIndex, addUndo, selectInfo.mRectArray, rectF, selectInfo, result);

        return true;
    }

    protected void selectCountRect(int pageIndex, SelectInfo selectInfo) {
        if (selectInfo == null) return;

        int start = selectInfo.mStartChar;
        int end = selectInfo.mEndChar;
        if (start == end && start == -1) return;
        if (end < start) {
            int tmp = end;
            end = start;
            start = tmp;
        }

        selectInfo.mRectArray.clear();
        selectInfo.mRectVert.clear();
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            if (!page.isParsed()) {
                Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, false);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }
            }
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);

            int count = textPage.getTextRectCount(start, end - start + 1);
            for (int i = 0; i < count; i++) {
                RectF crect = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(AppUtil.toRectF(textPage.getTextRect(i)), crect, pageIndex);
                int rotate = textPage.getBaselineRotation(i);
                boolean vert = rotate == 1 || rotate == 3;
                selectInfo.mRectVert.add(vert);
                selectInfo.mRectArray.add(crect);
                selectInfo.mRotation.add(rotate);
                if (i == 0) {
                    selectInfo.mBBox = new RectF(crect);
                } else {
                    reSizeRect(selectInfo.mBBox, crect);
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void reSizeRect(RectF MainRt, RectF rect) {
        if (rect.left < MainRt.left) MainRt.left = rect.left;
        if (rect.right > MainRt.right) MainRt.right = rect.right;
        if (rect.bottom > MainRt.bottom) MainRt.bottom = rect.bottom;
        if (rect.top < MainRt.top) MainRt.top = rect.top;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        PointF point = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                onSelectDown(pageIndex, point, mSelectInfo);
                break;
            case MotionEvent.ACTION_MOVE:
                onSelectMove(pageIndex, point, mSelectInfo);
                selectCountRect(pageIndex, mSelectInfo);
                invalidateTouch(mSelectInfo, pageIndex);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mSelectInfo.mSubJect = "Strikeout";
                mSelectInfo.mColor = mColor;
                mSelectInfo.mOpacity = mOpacity;
                onSelectRelease(pageIndex, mSelectInfo, true, null);
                return true;
            default:
                break;
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

    private void invalidateTouch(SelectInfo selectInfo, int pageIndex) {
        if (selectInfo == null) return;
        RectF rectF = new RectF();
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mSelectInfo.mBBox, rectF, pageIndex);
        RectF rF = calculate(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        mPdfViewCtrl.invalidate(rect);
        mTmpRect.set(rectF);
    }

    private RectF calculate(RectF desRectF, RectF srcRectF) {
        if (srcRectF.isEmpty()) return desRectF;
        int count = 0;
        if (desRectF.left == srcRectF.left && desRectF.top == srcRectF.top) count++;
        if (desRectF.right == srcRectF.right && desRectF.top == srcRectF.top) count++;
        if (desRectF.left == srcRectF.left && desRectF.bottom == srcRectF.bottom) count++;
        if (desRectF.right == srcRectF.right && desRectF.bottom == srcRectF.bottom) count++;
        mTmpDesRect.set(desRectF);
        if (count == 2) {
            mTmpDesRect.union(srcRectF);
            RectF rectF = new RectF();
            rectF.set(mTmpDesRect);
            mTmpDesRect.intersect(srcRectF);
            rectF.intersect(mTmpDesRect);
            return rectF;
        } else if (count == 3 || count == 4) {
            return mTmpDesRect;
        } else {
            mTmpDesRect.union(srcRectF);
            return mTmpDesRect;
        }
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mCurrentIndex != pageIndex || mSelectInfo.mRectArray.size() == 0) return;
        Rect clipRect = canvas.getClipBounds();
        int i = 0;
        PointF startPointF = new PointF();
        PointF endPointF = new PointF();
        RectF widthRect = new RectF();
        int pageRotation;
        try {
            pageRotation = mPdfViewCtrl.getDoc().getPage(pageIndex).getRotation();
        } catch (PDFException e) {
            pageRotation = 0;
        }

        for (RectF rect : mSelectInfo.mRectArray) {
            Rect r = new Rect();
            rect.round(r);
            if (r.intersect(clipRect)) {

                RectF tmpF = new RectF();
                tmpF.set(rect);

                if (i < mSelectInfo.mRectVert.size()) {
                    int rotation = (mSelectInfo.mRotation.get(i) + pageRotation + mPdfViewCtrl.getViewRotation()) % 4;
                    boolean vert = rotation == 1 || rotation == 3;
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rect, widthRect, pageIndex);

                    //reset Paint width
                    if ((widthRect.top - widthRect.bottom) > (widthRect.right - widthRect.left)) {
                        TextMarkupUtil.resetDrawLineWidth(mPdfViewCtrl, pageIndex, mPaint, widthRect.right, widthRect.left);
                    } else {
                        TextMarkupUtil.resetDrawLineWidth(mPdfViewCtrl, pageIndex, mPaint, widthRect.top, widthRect.bottom);
                    }

                    if (vert) {
                        startPointF.x = tmpF.right - (tmpF.right - tmpF.left) / 2;
                        startPointF.y = tmpF.top;
                        endPointF.x = startPointF.x;
                        endPointF.y = tmpF.bottom;
                    } else {
                        startPointF.x = tmpF.left;
                        startPointF.y = tmpF.bottom - (tmpF.bottom - tmpF.top) / 2;
                        endPointF.x = tmpF.right;
                        endPointF.y = startPointF.y;
                    }

                    canvas.save();
                    canvas.drawLine(startPointF.x, startPointF.y, endPointF.x, endPointF.y, mPaint);
                    canvas.restore();
                }
            }
            i++;
        }
    }

    public class SelectInfo {
        public boolean mIsFromTS;
        public String mSubJect;
        public int mStartChar;
        public int mEndChar;
        public RectF mBBox;
        public ArrayList<RectF> mRectArray;
        public ArrayList<Boolean> mRectVert;
        public ArrayList<Integer> mRotation;
        public int mColor;
        public int mOpacity;

        public SelectInfo() {
            mBBox = new RectF();
            mRectArray = new ArrayList<RectF>();
            mRectVert = new ArrayList<Boolean>();
            mRotation = new ArrayList<Integer>();
        }

        public void clear() {
            mIsFromTS = false;
            mStartChar = mEndChar = -1;
            mBBox.setEmpty();
            mRectArray.clear();
        }
    }

    private void addAnnot(final int pageIndex, final boolean addUndo, final ArrayList<RectF> rectArray, final RectF rectF, final SelectInfo selectInfo, final Event.Callback result) {
        StrikeOut annot = null;
        PDFPage page = null;
        try {
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            annot = (StrikeOut) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_StrikeOut, AppUtil.toFxRectF(rectF)), Annot.e_StrikeOut);
            if (annot == null) {
                if (!misFromSelector) {
                    if (!mIsContinuousCreate) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    }
                }
                misFromSelector = false;
                return;
            }

        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }

        int color = mColor;
        float opacity = mOpacity / 255f;
        if (selectInfo.mSubJect != null && selectInfo.mSubJect.equalsIgnoreCase("Replace")) {
            color = selectInfo.mColor;
            opacity = selectInfo.mOpacity / 255f;
        }
        final StrikeoutAddUndoItem undoItem = new StrikeoutAddUndoItem(mPdfViewCtrl);
        undoItem.mType = Annot.e_StrikeOut;
        undoItem.mColor = color;
        undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mQuadPoints = new QuadPointsArray();

        for (int i = 0; i < rectArray.size(); i++) {
            if (i < selectInfo.mRectVert.size()) {
                RectF rF = new RectF();

                mPdfViewCtrl.convertPageViewRectToPdfRect(rectArray.get(i), rF, pageIndex);
                QuadPoints quadPoint = new QuadPoints();
                if (selectInfo.mRectVert.get(i)) {
                    com.foxit.sdk.common.fxcrt.PointF point1 = new com.foxit.sdk.common.fxcrt.PointF(rF.left, rF.top);
                    quadPoint.setFirst(point1);
                    com.foxit.sdk.common.fxcrt.PointF point2 = new com.foxit.sdk.common.fxcrt.PointF(rF.left, rF.bottom);
                    quadPoint.setSecond(point2);
                    com.foxit.sdk.common.fxcrt.PointF point3 = new com.foxit.sdk.common.fxcrt.PointF(rF.right, rF.top);
                    quadPoint.setThird(point3);
                    com.foxit.sdk.common.fxcrt.PointF point4 = new com.foxit.sdk.common.fxcrt.PointF(rF.right, rF.bottom);
                    quadPoint.setFourth(point4);
                } else {
                    com.foxit.sdk.common.fxcrt.PointF point1 = new com.foxit.sdk.common.fxcrt.PointF(rF.left, rF.top);
                    quadPoint.setFirst(point1);
                    com.foxit.sdk.common.fxcrt.PointF point2 = new com.foxit.sdk.common.fxcrt.PointF(rF.right, rF.top);
                    quadPoint.setSecond(point2);
                    com.foxit.sdk.common.fxcrt.PointF point3 = new com.foxit.sdk.common.fxcrt.PointF(rF.left, rF.bottom);
                    quadPoint.setThird(point3);
                    com.foxit.sdk.common.fxcrt.PointF point4 = new com.foxit.sdk.common.fxcrt.PointF(rF.right, rF.bottom);
                    quadPoint.setFourth(point4);
                }
                undoItem.mQuadPoints.add(quadPoint);
            }
        }

        undoItem.mContents = getContent(page, selectInfo);
        undoItem.mNM = AppDmUtil.randomUUID(null);
        if (selectInfo.mSubJect == null) {
            selectInfo.mSubJect = "Strikeout";
        }
        undoItem.mSubject = selectInfo.mSubJect;
        undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
        undoItem.mFlags = 4;
        undoItem.mOpacity = opacity;
        undoItem.mPageIndex = pageIndex;

        StrikeoutEvent event = new StrikeoutEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
        final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager();
        //for replace caret
        if (selectInfo.mSubJect.equalsIgnoreCase("Replace")) {
            if (result != null) {
                result.result(event, true);
            }
            resetLineData();
            return;
        }

        final StrikeOut finalAnnot = annot;
        final PDFPage finalPage = page;
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    if (selectInfo.mSubJect.equalsIgnoreCase("Strikeout")) {
                        uiExtensionsManager.getDocumentManager().onAnnotAdded(finalPage, finalAnnot);
                    }

                    if (addUndo) {
                        uiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                    }
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        invalidate(pageIndex, rectF, result);
                    }

                    resetLineData();

                    if (!misFromSelector) {
                        if (!mIsContinuousCreate) {
                            uiExtensionsManager.setCurrentToolHandler(null);
                        }
                    }
                    misFromSelector = false;
                }
                if (result != null) {
                    result.result(event, success);
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private String getContent(PDFPage page, SelectInfo selectInfo) {
        int start = selectInfo.mStartChar;
        int end = selectInfo.mEndChar;
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        String content = null;
        try {
            if (page.isParsed() != true) {
                Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, false);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }
            }
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            content = textPage.getChars(start, end - start + 1);
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return null;
        }
        return content;
    }

    private void invalidate(int pageIndex, RectF dmRectF, final Event.Callback result) {
        if (dmRectF == null) {
            if (result != null) {
                result.result(null, true);
            }
            return;
        }
        RectF rectF = new RectF();
        mPdfViewCtrl.convertPdfRectToPageViewRect(dmRectF, rectF, pageIndex);
        Rect rect = new Rect();
        rectF.roundOut(rect);
        mPdfViewCtrl.refresh(pageIndex, rect);
    }

    protected void setPaint(int color, int opacity) {
        mColor = color;
        mOpacity = opacity;
        mPaint.setColor(mColor);
        mPaint.setAlpha(mOpacity);
        setProItemColor(color);
    }

    private void setProItemColor(int color){
        if (mPropertyItem == null) return;
        mPropertyItem.setCentreCircleColor(color);
    }

    private void resetPropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_STRIKEOUT.length];
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_STRIKEOUT, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_STRIKEOUT[0];
        mPropertyBar.setColors(colors);
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

                if (StrikeoutToolHandler.this == mUiextensionsManager.getCurrentToolHandler()) {
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

                mIsContinuousCreate = !mIsContinuousCreate;
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

    protected void addAnnot(final int pageIndex, final boolean addUndo, AnnotContent contentSupplier, ArrayList<RectF> rectFs, final RectF dmRectf,
                         SelectInfo selectInfo, final Event.Callback result) {

        try {
            final StrikeOut annot = (StrikeOut) AppAnnotUtil.createAnnot(mPdfViewCtrl.getDoc().getPage(pageIndex).addAnnot(Annot.e_StrikeOut, AppUtil.toFxRectF(dmRectf)), Annot.e_StrikeOut);

            final StrikeoutAddUndoItem undoItem = new StrikeoutAddUndoItem(mPdfViewCtrl);
            undoItem.mPageIndex = pageIndex;

            TextMarkupContent tmContent = TextMarkupContent.class.cast(contentSupplier);
            ArrayList<PointF> pointsList = tmContent.getQuadPoints();
            undoItem.mQuadPoints = new QuadPointsArray();
            for (int i = 0; i < pointsList.size() / 4; i++) {
                QuadPoints quadPoints = new QuadPoints();
                com.foxit.sdk.common.fxcrt.PointF p1 = new com.foxit.sdk.common.fxcrt.PointF(pointsList.get(4 * i).x, pointsList.get(4 * i).y);
                com.foxit.sdk.common.fxcrt.PointF p2 = new com.foxit.sdk.common.fxcrt.PointF(pointsList.get(4 * i + 1).x, pointsList.get(4 * i + 1).y);
                com.foxit.sdk.common.fxcrt.PointF p3 = new com.foxit.sdk.common.fxcrt.PointF(pointsList.get(4 * i + 2).x, pointsList.get(4 * i + 2).y);
                com.foxit.sdk.common.fxcrt.PointF p4 = new com.foxit.sdk.common.fxcrt.PointF(pointsList.get(4 * i + 3).x, pointsList.get(4 * i + 3).y);
                quadPoints.set(p1, p2, p3, p4);
                undoItem.mQuadPoints.add(quadPoints);
            }

            undoItem.mColor = contentSupplier.getColor();
            undoItem.mOpacity = contentSupplier.getOpacity() / 255f;
//            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = contentSupplier.getModifiedDate();
            undoItem.mNM = contentSupplier.getNM();
            undoItem.mSubject = contentSupplier.getSubject();
            undoItem.mContents = contentSupplier.getContents();
            undoItem.mIntent = contentSupplier.getIntent();
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mFlags = 4;

            StrikeoutEvent event = new StrikeoutEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            //for replace caret.
            if (!AppUtil.isEmpty(undoItem.mIntent) && undoItem.mIntent.equalsIgnoreCase("StrikeOutTextEdit")) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }

            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (undoItem.mIntent == null || !undoItem.mIntent.equalsIgnoreCase("StrikeOutTextEdit")) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
                        }

                        if (addUndo) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            invalidate(pageIndex, dmRectf, result);
                        }
                    }

                    if (result != null) {
                        result.result(event, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);

        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                return true;
            }
        }
        return false;
    }

    private boolean misFromSelector = false;

    protected void setFromSelector(boolean b) {
        misFromSelector = b;
    }
}