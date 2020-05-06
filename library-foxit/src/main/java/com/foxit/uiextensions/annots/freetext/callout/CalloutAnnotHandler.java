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
package com.foxit.uiextensions.annots.freetext.callout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.fxcrt.PointFArray;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.freetext.FtTextUtil;
import com.foxit.uiextensions.annots.freetext.FtUtil;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


public class CalloutAnnotHandler implements AnnotHandler {
    private Context mContext;

    private AnnotMenu mAnnotMenu;
    private PropertyBar mPropertyBar;
    private boolean mEditingProperty;
    private ArrayList<Integer> mMenuText;
    private boolean mModifyed;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private Annot mBitmapAnnot;
    private int mBBoxSpace;
    private float mOffset;
    private Paint mPaintOut;
    private Paint mPaintCtr;
    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private EditText mEditView;
    private FtTextUtil mTextUtil;
    private float mBBoxWidth;
    private float mBBoxHeight;
    private boolean mIsSelcetEndText = false;

    private int mTempLastColor;
    private int mTempLastOpacity;
    private int mTempLastFontId;
    private float mTempLastFontSize;
    private RectF mTempLastBBox;
    private String mTempLastContent;
    private PointF mTempLastStartingPt;
    private PointF mTempLastKneePt;
    private PointF mTempLastEndingPt;
    private RectF mTempLastTextBBox;
    private int mTempLastBorderType;
    private ArrayList<String> mTempLastComposedText = new ArrayList<String>();
    private boolean mEditState;
    private PointF mEditPoint = new PointF(0, 0);
    private RectF mDownRect = new RectF();
    private RectF mDocViewBBox = new RectF();

    private int mCurrentCtr = FtUtil.CTR_NONE;
    private int mLastOper = FtUtil.OPER_DEFAULT;

    private PDFViewCtrl mPdfViewCtrl;

    public CalloutAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);

        mPaintCtr = new Paint();
        mPaintCtr.setAntiAlias(true);
        mPaintCtr.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintCtr.setStrokeWidth(AppAnnotUtil.getInstance(context).getAnnotBBoxStrokeWidth());

        mMenuText = new ArrayList<Integer>();

        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mBitmapAnnot = null;
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    @Override
    public int getType() {
        return AnnotHandler.TYPE_FREETEXT_CALLOUT;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return AppUtil.toRectF(annot.getRect());
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    private RectF normalize(RectF rectF) {
        RectF rect = new RectF(rectF);
        if (rect.bottom < rect.top) {
            float temp = rect.bottom;
            rect.bottom = rect.top;
            rect.top = temp;
        }
        return rect;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        try {
            PointF pdfPoint = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, pdfPoint, annot.getPage().getIndex());
            ArrayList<PointF> pts = FtUtil.getCalloutLinePoints((FreeText) annot);
            PointF startingPt = pts.get(0);
            PointF kneePt = pts.get(1);
            PointF endingPt = pts.get(2);

            RectF bbox = AppUtil.toRectF(((FreeText) annot).getInnerRect());
            RectF tempBBox = normalize(bbox);
            if (tempBBox.contains(pdfPoint.x, pdfPoint.y)) {
                if (mEditState) {
                    AppUtil.showSoftInput(mEditView);
                }
                return true;
            }

            RectF tempRect = normalize(getAnnotBBox(annot));
            if (tempRect.contains(pdfPoint.x, pdfPoint.y)
                    && (FtUtil.isIntersectPointInLine(pdfPoint.x, pdfPoint.y, startingPt.x, startingPt.y, kneePt.x, kneePt.y) || FtUtil
                    .isIntersectPointInLine(pdfPoint.x, pdfPoint.y, kneePt.x, kneePt.y, endingPt.x, endingPt.y))) {
                return true;
            }

        } catch (PDFException e) {

        }

        return false;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        mEditView = new EditText(mContext);
        mEditView.setLayoutParams(new LayoutParams(1, 1));

        try {
            mEditView.setText(annot.getContent());

            FreeText freeText = (FreeText) annot;
            DefaultAppearance da = freeText.getDefaultAppearance();
            Font oldFont = da.getFont();
            int fontId = Font.e_StdIDCourier;
            if (oldFont != null && !oldFont.isEmpty()) {
                fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            } else {
                da.setFlags(DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize);
            }
            if (da.getText_size() == 0.f) {
                da.setText_size(24.0f);
            }
            Font font = mTextUtil.getSupportFont(fontId);
            da.setFont(font);
            freeText.setDefaultAppearance(da);
            freeText.resetAppearanceStream();

            DefaultAppearance defaultAppearance = freeText.getDefaultAppearance();
            mTempLastColor = defaultAppearance.getText_color();
            mTempLastOpacity = (int) (freeText.getOpacity() * 255f + 0.5f);
            mTempLastBBox = AppUtil.toRectF(annot.getRect());
            mTempLastFontId = mTextUtil.getSupportFontID(defaultAppearance, mPdfViewCtrl.getDoc());
            mTempLastFontSize = defaultAppearance.getText_size();
            mTempLastContent = annot.getContent();
            if (mTempLastContent == null) {
                mTempLastContent = "";
            }

            ArrayList<PointF> points = FtUtil.getCalloutLinePoints(freeText);
            mTempLastStartingPt = points.get(0);
            mTempLastKneePt = points.get(1);
            mTempLastEndingPt = points.get(2);

            mTempLastTextBBox = AppUtil.toRectF(freeText.getInnerRect());
            mTempLastBorderType = 1;
            RectF annotRect = new RectF(mTempLastTextBBox);
            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);
                mTempLastComposedText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex,
                        annotRect, mTempLastContent, mTextUtil.getSupportFontName(mTempLastFontId), mTempLastFontSize);
            }

            RectF menuRect = new RectF(mTempLastTextBBox);
            mPdfViewCtrl.convertPdfRectToPageViewRect(menuRect, menuRect, pageIndex);
            float width = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 2);
            menuRect.inset(-width * 0.5f, -width * 0.5f);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            menuRect.inset(-AppAnnotUtil.getAnnotBBoxSpace(), -AppAnnotUtil.getAnnotBBoxSpace());
            prepareAnnotMenu(annot);
            mAnnotMenu.show(menuRect);
            preparePropertyBar(freeText);

            mOffset = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex,
                    FtUtil.CTRLPTTOUCHEXT * 4);
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mEditView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    annot.setContent(String.valueOf(s));
                    annot.resetAppearanceStream();
                    RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                    int pageIndex = annot.getPage().getIndex();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    RectF pdfRectF = new RectF(pageViewRect.left, pageViewRect.top, pageViewRect.left + mBBoxWidth,
                            pageViewRect.top + mBBoxHeight);
                    RectF rect = new RectF(pdfRectF.left, pdfRectF.top, pdfRectF.left + mBBoxWidth, pdfRectF.top
                            + mBBoxHeight);

                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mTextUtil.setOnWidthChanged(new FtTextUtil.OnTextValuesChangedListener() {

            @Override
            public void onMaxWidthChanged(float maxWidth) {
                if (mBBoxWidth != maxWidth) {
                    mBBoxWidth = maxWidth;
                }
            }

            @Override
            public void onMaxHeightChanged(float maxHeight) {
                if (mBBoxHeight != maxHeight) {
                    mBBoxHeight = maxHeight;
                    try {
                        RectF textRect = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                        int pageIndex = annot.getPage().getIndex();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);
                        if (mPdfViewCtrl.isPageVisible(pageIndex) && mBBoxHeight > textRect.height()) {
                            ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                            PointF startingPt = points.get(0);
                            PointF kneePt = points.get(1);
                            PointF endingPt = points.get(2);

                            mPdfViewCtrl.convertPdfPtToPageViewPt(startingPt, startingPt, pageIndex);
                            mPdfViewCtrl.convertPdfPtToPageViewPt(kneePt, kneePt, pageIndex);
                            mPdfViewCtrl.convertPdfPtToPageViewPt(endingPt, endingPt, pageIndex);
                            textRect.set(textRect.left, textRect.top, textRect.right, textRect.top + mBBoxHeight);
                            FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex,
                                    textRect, startingPt, kneePt, endingPt);
                            RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                    kneePt.x, kneePt.y, endingPt.x, endingPt.y);
                            borderRect.union(textRect);
                            RectF rectInv = new RectF(borderRect);
                            mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt, startingPt, pageIndex);
                            mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt, kneePt, pageIndex);
                            mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt, endingPt, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(borderRect, borderRect, pageIndex);
                            annot.move(AppUtil.toFxRectF(borderRect));

                            PointFArray pArray = new PointFArray();
                            pArray.add(AppUtil.toFxPointF(startingPt));
                            pArray.add(AppUtil.toFxPointF(kneePt));
                            pArray.add(AppUtil.toFxPointF(endingPt));
                            ((FreeText) annot).setCalloutLinePoints(pArray);
                            ((FreeText) annot).setInnerRect(AppUtil.toFxRectF(textRect));
                            annot.resetAppearanceStream();
                            rectInv.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCurrentSelectIndex(int selectIndex) {
                if (selectIndex >= mEditView.getText().length()) {
                    selectIndex = mEditView.getText().length();
                    mIsSelcetEndText = true;
                } else {
                    mIsSelcetEndText = false;
                }
                mEditView.setSelection(selectIndex);
            }

            @Override
            public void onEditPointChanged(float editPointX,
                                           float editPointY) {
                try {
                    int pageIndex = annot.getPage().getIndex();
                    PointF point = new PointF(editPointX, editPointY);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
                    mEditPoint.set(point.x, point.y);
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }

        });
        try {
            RectF viewRect = AppUtil.toRectF(annot.getRect());
            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                viewRect.inset(-40, -40);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mBitmapAnnot = annot;
                }
            } else {
                mBitmapAnnot = annot;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {
        mAnnotMenu.setListener(null);
        mAnnotMenu.dismiss();
        if (mEditingProperty) {
            mEditingProperty = false;
            mPropertyBar.dismiss();
        }

        try {
            PDFPage page = annot.getPage();
            if (page != null) {
                final int pageIndex = page.getIndex();
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();

                if (mEditView != null && !mEditView.getText().toString().equals(mTempLastContent)) {
                    RectF textRect = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);
                    RectF pdfRectF = new RectF(textRect.left, textRect.top, textRect.right, textRect.bottom);

                    ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                    PointF startingPt = points.get(0);
                    PointF kneePt = points.get(1);
                    PointF endingPt = points.get(2);

                    mPdfViewCtrl.convertPdfPtToPageViewPt(startingPt, startingPt, pageIndex);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(kneePt, kneePt, pageIndex);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(endingPt, endingPt, pageIndex);

                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex, pdfRectF,
                            startingPt, kneePt, endingPt);
                    RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);
                    borderRect.union(pdfRectF);

                    mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);
                    PointF startingPt2 = new PointF(startingPt.x, startingPt.y);
                    PointF kneePt2 = new PointF(kneePt.x, kneePt.y);
                    PointF endingPt2 = new PointF(endingPt.x, endingPt.y);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(borderRect, borderRect, pageIndex);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt2, startingPt2, pageIndex);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt2, kneePt2, pageIndex);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt2, endingPt2, pageIndex);

                    String content = mEditView.getText().toString();
                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                    float fontSize = da.getText_size();
                    modifyAnnot(pageIndex, annot, borderRect, pdfRectF,
                            startingPt2, kneePt2, endingPt2, (int) da.getText_color(),
                            (int) (((FreeText) annot).getOpacity() * 255f), fontId, fontSize, content, 1, false);
                }
                if (needInvalid && mModifyed) {
                    ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                    PointF startingPt = points.get(0);
                    PointF kneePt = points.get(1);
                    PointF endingPt = points.get(2);

                    if (mTempLastColor == (int)da.getText_color() && mTempLastOpacity == (int) (((FreeText) annot).getOpacity() * 255f)
                            && mTempLastBBox.equals(AppUtil.toRectF(annot.getRect()))
                            && mTempLastContent.equals(annot.getContent())
                            && mTempLastFontSize == da.getText_size()
                            && mTempLastFontId == mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())
                            && mTempLastStartingPt.equals(startingPt)
                            && mTempLastKneePt.equals(kneePt)
                            && mTempLastEndingPt.equals(endingPt)
                            && mTempLastTextBBox.equals(AppUtil.toRectF(((FreeText) annot).getInnerRect()))
                            && mTempLastBorderType == 1) {
                        modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()),
                                startingPt, kneePt, endingPt,
                                (int) da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f + 0.5f),
                                mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(),
                                1, false);
                    } else {
                        modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()),
                                startingPt, kneePt, endingPt,
                                (int) da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f + 0.5f),
                                mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(),
                                1, true);
                    }
                } else {
                    da.setText_color(mTempLastColor);
                    ((FreeText) annot).setOpacity(mTempLastOpacity / 255f);
                    annot.move(AppUtil.toFxRectF(mTempLastBBox));
                    Font font = mTextUtil.getSupportFont(mTempLastFontId);
                    da.setFont(font);
                    da.setText_size(mTempLastFontSize);
                    annot.setContent(mTempLastContent);

                    PointFArray _pArray = new PointFArray();
                    _pArray.add(AppUtil.toFxPointF(mTempLastStartingPt));
                    _pArray.add(AppUtil.toFxPointF(mTempLastKneePt));
                    _pArray.add(AppUtil.toFxPointF(mTempLastEndingPt));
                    ((FreeText) annot).setCalloutLinePoints(_pArray);
                    ((FreeText) annot).setInnerRect(AppUtil.toFxRectF(mTempLastTextBBox));
                    annot.resetAppearanceStream();
                }

                if (mPdfViewCtrl.isPageVisible(pageIndex)  && needInvalid) {
                    final RectF viewRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    Rect invalidateRect = AppDmUtil.rectFToRect(viewRect);
                    invalidateRect.inset(-200, -200);
                    mPdfViewCtrl.refresh(pageIndex, invalidateRect);
                    Task.CallBack callBack = new Task.CallBack() {
                        @Override
                        public void result(Task task) {
                            if (mBitmapAnnot != ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                                mBitmapAnnot = null;
                                AppUtil.dismissInputSoft(mEditView);
                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                                mEditState = false;
                                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                                mBBoxWidth = 0;
                                mBBoxHeight = 0;
                                mEditPoint.set(0, 0);
                                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                if (mPdfViewCtrl.isPageVisible(pageIndex) && (pageIndex == mPdfViewCtrl.getPageCount() - 1 ||
                                        (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) &&
                                        pageIndex == mPdfViewCtrl.getCurrentPage()) {
                                    PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                                    mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, pageIndex);
                                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                        mTextUtil.setKeyboardOffset(0);
                                        PointF startPoint = new PointF(viewRect.left, viewRect.top);
                                        PointF point = mTextUtil.getPageViewOrigin(mPdfViewCtrl, pageIndex, startPoint.x, startPoint.y);
                                        mPdfViewCtrl.gotoPage(pageIndex, point.x, point.y);
                                    }
                                }
                            }
                        }
                    };
                    mPdfViewCtrl.addTask(new Task(callBack) {
                        @Override
                        protected void execute() {

                        }
                    });

                } else {
                    mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                    mBitmapAnnot = null;
                    AppUtil.dismissInputSoft(mEditView);
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                    mEditState = false;
                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                    mBBoxWidth = 0;
                    mBBoxHeight = 0;
                    mEditPoint.set(0, 0);
                }
            } else {
                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                mBitmapAnnot = null;
                AppUtil.dismissInputSoft(mEditView);
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                mEditState = false;
                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                mBBoxWidth = 0;
                mBBoxHeight = 0;
                mEditPoint.set(0, 0);
            }
            mModifyed = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    private void preparePropertyBar(FreeText annot) {
        mPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
        int[] colors = new int[PropertyBar.PB_COLORS_CALLOUT.length];
        System.arraycopy(PropertyBar.PB_COLORS_CALLOUT, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_CALLOUT[0];
        mPropertyBar.setColors(colors);
        try {
            DefaultAppearance da = annot.getDefaultAppearance();
            mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) da.getText_color());
            mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (annot.getOpacity() * 255f + 0.5f)));
            mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, mTextUtil.getSupportFontName(mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())));
            mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, da.getText_size());
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mPropertyBar.setArrowVisible(false);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_FONTSIZE
                | PropertyBar.PROPERTY_FONTNAME;
    }



    private void prepareAnnotMenu(final Annot annot) {
        resetAnnotationMenuResource(annot);
        mAnnotMenu.setMenuItems(mMenuText);

        mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        deleteAnnot(annot, true, null);
                    }
                } else if (btType == AnnotMenu.AM_BT_EDIT) {
                    mAnnotMenu.dismiss();
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().addView(mEditView);
                    mTextUtil.getBlink().postDelayed((Runnable) mTextUtil.getBlink(), 500);
                    mEditView.setSelection(mEditView.getText().length());
                    AppUtil.showSoftInput(mEditView);
                    mEditState = true;
                    try {
                        int pageIndex = annot.getPage().getIndex();
                        RectF rectF = AppUtil.toRectF(annot.getRect());
                        final RectF viewRect = new RectF(rectF);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(viewRect));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }

                } else if (btType == AnnotMenu.AM_BT_STYLE) {
                    RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewBBox);
                    mPropertyBar.show(rectF, false);
                    mAnnotMenu.dismiss();
                } else if (btType == AnnotMenu.AM_BT_FLATTEN) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                }
            }
        });
    }

    /**
     * reset mAnnotationMenu text
     */
    private void resetAnnotationMenuResource(Annot annot) {
        mMenuText.clear();
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                mMenuText.add(AnnotMenu.AM_BT_EDIT);
            }
            mMenuText.add(AnnotMenu.AM_BT_STYLE);
            mMenuText.add(AnnotMenu.AM_BT_FLATTEN);
            if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                mMenuText.add(AnnotMenu.AM_BT_DELETE);
            }
        }
    }

    @Override
    public void addAnnot(final int pageIndex, AnnotContent content, final boolean addUndo, final Event.Callback result) {
        ICalloutAnnotContent lContent = (ICalloutAnnotContent) content;

        final CalloutAddUndoItem undoItem = new CalloutAddUndoItem(mPdfViewCtrl);
        undoItem.setCurrentValue(lContent);
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final FreeText annot = (FreeText) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(content.getBBox())), Annot.e_FreeText);
            RectF annotRect = new RectF(undoItem.mTextBBox);
            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);
            FtTextUtil textUtil = new FtTextUtil(mContext, mPdfViewCtrl);
            undoItem.mComposedText = textUtil.getComposedText(mPdfViewCtrl, pageIndex, annotRect,
                    undoItem.mContents, mTextUtil.getSupportFontName(undoItem.mFontId), undoItem.mFontSize);
            undoItem.mTextLineCount = undoItem.mComposedText.size();
            undoItem.mIntent = "FreeTextCallout";
            undoItem.mTextColor = lContent.getColor();
            undoItem.mDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_FlagPrint;
            if (!undoItem.mContents.equals(" ")) {
                RectF adjustBBox = new RectF(annotRect);
                textUtil.adjustTextRect(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(undoItem.mFontId), undoItem.mFontSize, adjustBBox, undoItem.mComposedText);
                mPdfViewCtrl.convertPageViewRectToPdfRect(adjustBBox, adjustBBox, pageIndex);
                RectF bbox = new RectF(undoItem.mBBox);
                FtUtil.adjustKneeAndEndingPt(bbox, adjustBBox, undoItem.mStartingPt, undoItem.mKneePt, undoItem.mEndingPt);
                undoItem.mBBox = new RectF(bbox);
                undoItem.mTextBBox = new RectF(adjustBBox);
            }

            final CalloutEvent event = new CalloutEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }
                        try {
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                RectF viewRect = AppUtil.toRectF(annot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                Rect rect = new Rect();
                                viewRect.roundOut(rect);
                                rect.inset(-30, -30);
                                mPdfViewCtrl.refresh(pageIndex, rect);
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }

                    }
                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, final Event.Callback result) {
        deleteAnnot(annot, addUndo, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
        PointF devPoint = new PointF(e.getX(), e.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPoint, point, pageIndex);
        PointF pageViewPt = new PointF(point.x, point.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pageViewPt, pageIndex);

        float evX = point.x;
        float evY = point.y;
        int action = e.getAction();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        mCurrentCtr = FtUtil.getTouchControlPoint(mPdfViewCtrl, pageIndex,
                                annot, evX, evY);
                        mDownRect = new RectF(AppUtil.toRectF(annot.getRect()));
                        RectF tempRect = normalize(AppUtil.toRectF(((FreeText) annot).getInnerRect()));
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mDownRect, mDownRect, pageIndex);
                        if (mCurrentCtr == FtUtil.CTR_BORDER) {
                            mLastOper = FtUtil.OPER_BORDER;
                            mTouchCaptured = true;
                            mDownPoint.set(evX, evY);
                            mLastPoint.set(evX, evY);
                            return true;
                        } else if (mCurrentCtr == FtUtil.CTR_STARTING) {
                            mLastOper = FtUtil.OPER_STARTING;
                            mTouchCaptured = true;
                            mDownPoint.set(evX, evY);
                            mLastPoint.set(evX, evY);
                            return true;
                        } else if (mCurrentCtr == FtUtil.CTR_KNEE) {
                            mLastOper = FtUtil.OPER_KNEE;
                            mTouchCaptured = true;
                            mDownPoint.set(evX, evY);
                            mLastPoint.set(evX, evY);
                            return true;

                        } else if (mCurrentCtr >= 0 && mCurrentCtr <= 7) {
                    /* Bitmap control point
                     * 0---1---2
					 * |       |
					 * 7       3
					 * |       |
					 * 6---5---4
					 */
                            mLastOper = FtUtil.OPER_SCALE;
                            mTouchCaptured = true;
                            mDownPoint.set(evX, evY);
                            mLastPoint.set(evX, evY);
                            return true;
                        } else if (tempRect.contains(pageViewPt.x, pageViewPt.y) && !mEditState) {
                            mCurrentCtr = FtUtil.CTR_TEXTBBOX;
                            mLastOper = FtUtil.OPER_TRANSLATE;
                            mTouchCaptured = true;
                            mDownPoint.set(evX, evY);
                            mLastPoint.set(evX, evY);
                            return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured && pageIndex == annot.getPage().getIndex()
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && !mEditState) {

                        if (evX != mLastPoint.x || evY != mLastPoint.y) {
                            RectF AllBBox = AppUtil.toRectF(annot.getRect());
                            RectF textBBox = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(AllBBox, AllBBox, pageIndex);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(textBBox, textBBox, pageIndex);


                            ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                            PointF startingPt = points.get(0);
                            PointF kneePt = points.get(1);
                            PointF endingPt = points.get(2);
                            mPdfViewCtrl.convertPdfPtToPageViewPt(startingPt, startingPt, pageIndex);
                            mPdfViewCtrl.convertPdfPtToPageViewPt(kneePt, kneePt, pageIndex);
                            mPdfViewCtrl.convertPdfPtToPageViewPt(endingPt, endingPt, pageIndex);

                            switch (mLastOper) {
                                case FtUtil.OPER_BORDER: {
                                    RectF rectInv = new RectF(AllBBox);
                                    RectF rectChanged = new RectF(AllBBox);

                                    rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    rectChanged.offset(evX - mDownPoint.x, evY - mDownPoint.y);

                                    float adjustx = 0;
                                    float adjusty = 0;
                                    float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 2);
                                    if (rectChanged.left < deltaXY) {
                                        adjustx = -rectChanged.left + deltaXY;
                                    }
                                    if (rectChanged.top < deltaXY) {
                                        adjusty = -rectChanged.top + deltaXY;
                                    }
                                    if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                                        adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right - deltaXY;
                                    }
                                    if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                        adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom - deltaXY;
                                    }
                                    rectChanged.offset(adjustx, adjusty);
                                    rectInv.union(rectChanged);
                                    rectInv.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));

                                    RectF rectInViewerF = new RectF(rectChanged);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(rectInViewerF);
                                    }
                                    if (mEditingProperty) {
                                        mPropertyBar.dismiss();
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustx, adjusty);
                                    break;
                                }
                                case FtUtil.OPER_STARTING: {
                                    RectF rectInv = new RectF(AllBBox);
                                    startingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex,
                                            textBBox, startingPt, kneePt, endingPt);
                                    RectF rectChanged = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);

                                    float adjustx = 0;
                                    float adjusty = 0;
                                    float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 2);
                                    if (rectChanged.left < deltaXY) {
                                        adjustx = -rectChanged.left + deltaXY;
                                    }
                                    if (rectChanged.top < deltaXY) {
                                        adjusty = -rectChanged.top + deltaXY;
                                    }
                                    if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                                        adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right - deltaXY;
                                    }
                                    if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                        adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom - deltaXY;
                                    }
                                    rectChanged.offset(adjustx, adjusty);
                                    rectInv.union(rectChanged);
                                    rectInv.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));

                                    RectF rectInViewerF = new RectF(rectChanged);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(rectInViewerF);
                                    }
                                    if (mEditingProperty) {
                                        mPropertyBar.dismiss();
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustx, adjusty);
                                    break;
                                }
                                case FtUtil.OPER_KNEE: {
                                    RectF rectInv = new RectF(AllBBox);

                                    if (kneePt.x == endingPt.x) {
                                        kneePt.offset(0, mLastPoint.y - mDownPoint.y);
                                    } else if (kneePt.y == endingPt.y) {
                                        kneePt.offset(mLastPoint.x - mDownPoint.x, 0);
                                    }
                                    RectF rectChanged = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);

                                    float adjustx = 0;
                                    float adjusty = 0;
                                    float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 2);
                                    if (rectChanged.left < deltaXY) {
                                        adjustx = -rectChanged.left + deltaXY;
                                    }
                                    if (rectChanged.top < deltaXY) {
                                        adjusty = -rectChanged.top + deltaXY;
                                    }
                                    if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                                        adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right - deltaXY;
                                    }
                                    if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                        adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom - deltaXY;
                                    }
                                    rectChanged.offset(adjustx, adjusty);
                                    rectInv.union(rectChanged);
                                    rectInv.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));

                                    RectF rectInViewerF = new RectF(rectChanged);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(rectInViewerF);
                                    }
                                    if (mEditingProperty) {
                                        mPropertyBar.dismiss();
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustx, adjusty);
                                    break;
                                }
                                case FtUtil.OPER_TRANSLATE: {
                                    RectF rectInv = new RectF(AllBBox);
                                    RectF rectBBoxChanged = new RectF(AllBBox);
                                    RectF rectChanged = new RectF(textBBox);

                                    rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    rectChanged.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                    kneePt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    endingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex,
                                            rectChanged, startingPt, kneePt, endingPt);
                                    rectBBoxChanged.union(FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y));

                                    float adjustx = 0;
                                    float adjusty = 0;
                                    float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 2);
                                    if (rectChanged.left < deltaXY) {
                                        adjustx = -rectChanged.left + deltaXY;
                                    }
                                    if (rectChanged.top < deltaXY) {
                                        adjusty = -rectChanged.top + deltaXY;
                                    }
                                    if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                                        adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right - deltaXY;
                                    }
                                    if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                        adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom - deltaXY;
                                    }
                                    rectChanged.offset(adjustx, adjusty);
                                    rectInv.union(rectBBoxChanged);
                                    rectInv.union(rectChanged);
                                    rectInv.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));

                                    RectF rectInViewerF = new RectF(rectChanged);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(rectInViewerF);
                                    }
                                    if (mEditingProperty) {
                                        mPropertyBar.dismiss();
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustx, adjusty);
                                    break;
                                }
                                case FtUtil.OPER_SCALE: {
                                    Matrix matrix = FtUtil.calculateScaleMatrix(mCurrentCtr, textBBox,
                                            mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    Matrix matrix2 = FtUtil.calculateScaleMatrix(mCurrentCtr, textBBox, evX - mDownPoint.x, evY
                                            - mDownPoint.y);
                                    RectF rectInv = new RectF(textBBox);
                                    RectF rectChanged = new RectF(textBBox);
                                    RectF rect2 = new RectF(textBBox);
                                    matrix2.mapRect(rect2);
                                    matrix.mapRect(rectInv);
                                    matrix.mapRect(rectChanged);

                                    RectF beforeBorder = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);
                                    if (kneePt.x < endingPt.x) {
                                        endingPt.set(rectChanged.left, (rectChanged.top + rectChanged.bottom) / 2);
                                        kneePt.set(
                                                endingPt.x
                                                        - FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH), endingPt.y);
                                    } else if (kneePt.x > endingPt.x) {
                                        endingPt.set(rectChanged.right, (rectChanged.top + rectChanged.bottom) / 2);
                                        kneePt.set(
                                                endingPt.x
                                                        + FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH), endingPt.y);
                                    } else if (kneePt.y < endingPt.y) {
                                        endingPt.set((rectChanged.left + rectChanged.right) / 2, rectChanged.top);
                                        kneePt.set(
                                                endingPt.x,
                                                endingPt.y
                                                        - FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH));
                                    } else if (kneePt.y > kneePt.y) {
                                        endingPt.set((rectChanged.left + rectChanged.right) / 2, rectChanged.bottom);
                                        kneePt.set(
                                                endingPt.x,
                                                endingPt.y
                                                        + FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH));
                                    }

                                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex,
                                            rectChanged, startingPt, kneePt, endingPt);
                                    RectF rectBorder = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);

                                    float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 8);
                                    PointF adjustXY = FtUtil.adjustScalePointF(mCurrentCtr, mPdfViewCtrl, pageIndex, rect2, deltaXY);

                                    rectInv.union(beforeBorder);
                                    rectInv.union(rectChanged);
                                    rectInv.union(rectBorder);
                                    rectInv.union(rect2);
                                    rectInv.inset(-deltaXY, -deltaXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));

                                    RectF rectInViewerF = new RectF(rectChanged);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(rectInViewerF);
                                    }
                                    if (mEditingProperty) {
                                        mPropertyBar.dismiss();
                                    }
                                    if (rect2.width() > mBBoxWidth && rect2.left + 1 < mDownRect.right
                                            && rect2.right > mDownRect.left + 1) {
                                        mLastPoint.set(evX, mLastPoint.y);
                                        mLastPoint.offset(adjustXY.x, 0);
                                    }

                                    if (rect2.height() > mBBoxHeight && rect2.top + 1 < mDownRect.bottom
                                            && rect2.bottom > mDownRect.top + 1) {
                                        mLastPoint.set(mLastPoint.x, evY);
                                        mLastPoint.offset(0, adjustXY.y);
                                    }
                                    break;
                                }
                                default:
                            }
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF allBBox = AppUtil.toRectF(annot.getRect());
                        RectF textBBox = AppUtil.toRectF(((FreeText) annot).getInnerRect());

                        mPdfViewCtrl.convertPdfRectToPageViewRect(allBBox, allBBox, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(textBBox, textBBox, pageIndex);

                        ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                        PointF startingPt = points.get(0);
                        PointF kneePt = points.get(1);
                        PointF endingPt = points.get(2);
                        mPdfViewCtrl.convertPdfPtToPageViewPt(startingPt, startingPt, pageIndex);
                        mPdfViewCtrl.convertPdfPtToPageViewPt(kneePt, kneePt, pageIndex);
                        mPdfViewCtrl.convertPdfPtToPageViewPt(endingPt, endingPt, pageIndex);
                        switch (mLastOper) {
                            case FtUtil.OPER_BORDER: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    RectF rectBBox = new RectF(allBBox);
                                    RectF textRect = new RectF(textBBox);
                                    rectBBox.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    textRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                                    PointF ptStarting = new PointF(startingPt.x, startingPt.y);
                                    PointF ptKnee = new PointF(kneePt.x, kneePt.y);
                                    PointF ptEnding = new PointF(endingPt.x, endingPt.y);
                                    ptStarting.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    ptKnee.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    ptEnding.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                                    RectF rectViewer = new RectF(textRect);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectViewer, rectViewer, pageIndex);
                                    if (!mEditingProperty) {
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.update(rectViewer);
                                        } else {
                                            mAnnotMenu.show(rectViewer);
                                        }
                                    }

                                    mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectBBox, rectBBox, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(ptStarting, ptStarting, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(ptKnee, ptKnee, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(ptEnding, ptEnding, pageIndex);
                                    DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                                    float fontSize = da.getText_size();
                                    modifyAnnot(pageIndex, annot, rectBBox,
                                            textRect, ptStarting, ptKnee, ptEnding,
                                            da.getText_color(), (int) (((FreeText) (annot)).getOpacity() * 255f), fontId,
                                            fontSize, annot.getContent(), 1, false);
                                }
                                break;
                            }
                            case FtUtil.OPER_STARTING: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    RectF rectBBox = new RectF(textBBox);
                                    RectF textRect = new RectF(textBBox);

                                    startingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex,
                                            textBBox, startingPt, kneePt, endingPt);
                                    RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);
                                    rectBBox.union(borderRect);

                                    RectF rectViewer = new RectF(textRect);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectViewer, rectViewer, pageIndex);
                                    if (!mEditingProperty) {
                                        mAnnotMenu.show(rectViewer);
                                    }

                                    mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectBBox, rectBBox, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt, startingPt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt, kneePt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt, endingPt, pageIndex);
                                    DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                                    float fontSize = da.getText_size();
                                    modifyAnnot(pageIndex, annot, rectBBox,
                                            textRect, startingPt, kneePt, endingPt,
                                            da.getText_color(), (int) (((FreeText) (annot)).getOpacity() * 255f), fontId,
                                            fontSize, annot.getContent(), 1, false);
                                }
                                break;
                            }
                            case FtUtil.OPER_KNEE: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    RectF rectBBox = new RectF(textBBox);
                                    RectF textRect = new RectF(textBBox);
                                    if (kneePt.x == endingPt.x) {
                                        kneePt.offset(0, mLastPoint.y - mDownPoint.y);
                                    } else if (kneePt.y == endingPt.y) {
                                        kneePt.offset(mLastPoint.x - mDownPoint.x, 0);
                                    }

                                    RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);
                                    rectBBox.union(borderRect);

                                    RectF rectViewer = new RectF(textRect);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectViewer, rectViewer, pageIndex);
                                    if (!mEditingProperty) {
                                        mAnnotMenu.show(rectViewer);
                                    }

                                    mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectBBox, rectBBox, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt, startingPt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt, kneePt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt, endingPt, pageIndex);
                                    DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                                    float fontSize = da.getText_size();
                                    modifyAnnot(pageIndex, annot, rectBBox,
                                            textRect, startingPt, kneePt, endingPt,
                                            da.getText_color(), (int) (((FreeText) (annot)).getOpacity() * 255f), fontId,
                                            fontSize, annot.getContent(), 1, false);
                                }
                                break;
                            }
                            case FtUtil.OPER_TRANSLATE: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    RectF textRect = new RectF(textBBox);
                                    textRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                                    RectF rectBBox = new RectF(textRect);

                                    kneePt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    endingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex,
                                            textRect, startingPt, kneePt, endingPt);
                                    RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);
                                    rectBBox.union(borderRect);

                                    RectF rectViewer = new RectF(textRect);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectViewer, rectViewer, pageIndex);
                                    if (!mEditingProperty) {
                                        mAnnotMenu.show(rectViewer);
                                    }

                                    mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectBBox, rectBBox, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt, startingPt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt, kneePt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt, endingPt, pageIndex);

                                    DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                                    float fontSize = da.getText_size();
                                    modifyAnnot(pageIndex, annot, rectBBox,
                                            textRect, startingPt, kneePt, endingPt,
                                            da.getText_color(), (int) (((FreeText) (annot)).getOpacity() * 255f), fontId,
                                            fontSize, annot.getContent(), 1, false);
                                }
                                break;
                            }
                            case FtUtil.OPER_SCALE: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    Matrix matrix = FtUtil.calculateScaleMatrix(mCurrentCtr, textBBox,
                                            mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    RectF rectBBox = new RectF(textBBox);
                                    RectF textRect = new RectF(textBBox);

                                    matrix.mapRect(rectBBox);
                                    matrix.mapRect(textRect);
                                    if (kneePt.x < endingPt.x) {
                                        endingPt.set(textRect.left, (textRect.top + textRect.bottom) / 2);
                                        kneePt.set(
                                                endingPt.x
                                                        - FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH), endingPt.y);
                                    } else if (kneePt.x > endingPt.x) {
                                        endingPt.set(textRect.right, (textRect.top + textRect.bottom) / 2);
                                        kneePt.set(
                                                endingPt.x
                                                        + FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH), endingPt.y);
                                    } else if (kneePt.y < endingPt.y) {
                                        endingPt.set((textRect.left + textRect.right) / 2, textRect.top);
                                        kneePt.set(
                                                endingPt.x,
                                                endingPt.y
                                                        - FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH));
                                    } else if (kneePt.y > kneePt.y) {
                                        endingPt.set((textRect.left + textRect.right) / 2, textRect.bottom);
                                        kneePt.set(
                                                endingPt.x,
                                                endingPt.y
                                                        + FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH));
                                    }

                                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex,
                                            textRect, startingPt, kneePt, endingPt);

                                    RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y,
                                            kneePt.x, kneePt.y, endingPt.x, endingPt.y);
                                    rectBBox.union(borderRect);

                                    RectF rectViewer = new RectF(textRect);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectViewer, rectViewer, pageIndex);
                                    if (!mEditingProperty) {
                                        mAnnotMenu.show(rectViewer);
                                    }

                                    mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectBBox, rectBBox, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt, startingPt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt, kneePt, pageIndex);
                                    mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt, endingPt, pageIndex);
                                    DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                                    float fontSize = da.getText_size();
                                    modifyAnnot(pageIndex, annot, rectBBox,
                                            textRect, startingPt, kneePt, endingPt,
                                            da.getText_color(), (int) (((FreeText) (annot)).getOpacity() * 255f), fontId,
                                            fontSize, annot.getContent(), 1, false);
                                }
                            }
                                  break;
                            default:
                                break;
                        }
                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        mLastOper = FtUtil.OPER_DEFAULT;
                        mCurrentCtr = FtUtil.CTR_NONE;
                        mTouchCaptured = false;
                        return true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = FtUtil.OPER_DEFAULT;
                    mCurrentCtr = FtUtil.CTR_NONE;
                    mTouchCaptured = false;
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mEditPoint.set(0, 0);
                    mLastOper = FtUtil.OPER_DEFAULT;
                    mCurrentCtr = FtUtil.CTR_NONE;
                    return false;
                default:
                    break;
            }
        } catch (PDFException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point, annot);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point, annot);
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        return AppAnnotUtil.isSameAnnot(curAnnot, annot) ? false : true;
    }

    private boolean onSingleTapOrLongPress(int pageIndex, PointF point, Annot annot) {

        try {
            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex()
                        && isHitAnnot(annot, point) && mEditState) {
                    PointF pointF = new PointF(point.x, point.y);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointF, pageIndex);
                    mEditPoint.set(pointF.x, pointF.y);
                    mTextUtil.resetEditState();
                    RectF pageViewRect = new RectF(AppUtil.toRectF(annot.getRect()));
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(pageViewRect));
                    return true;
                } else if (pageIndex == annot.getPage().getIndex()
                        && !isHitAnnot(annot, point)
                        && mEditView != null && !mEditView.getText().toString().equals(annot.getContent())) {
                    RectF textRect = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);
                    RectF pdfRectF;
                    if (mBBoxHeight <= 0){
                        pdfRectF = new RectF(textRect.left, textRect.top, textRect.right, textRect.bottom);
                    } else {
                        pdfRectF = new RectF(textRect.left, textRect.top, textRect.right, textRect.top + mBBoxHeight);
                    }

                    ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                    PointF startingPt = points.get(0);
                    PointF kneePt = points.get(1);
                    PointF endingPt = points.get(2);

                    mPdfViewCtrl.convertPdfPtToPageViewPt(startingPt, startingPt, pageIndex);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(kneePt, kneePt, pageIndex);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(endingPt, endingPt, pageIndex);
                    FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex, pdfRectF,
                            startingPt, kneePt, endingPt);
                    RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y, kneePt.x,
                            kneePt.y, endingPt.x, endingPt.y);
                    borderRect.union(pdfRectF);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(borderRect, borderRect, pageIndex);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt, startingPt, pageIndex);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt, kneePt, pageIndex);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt, endingPt, pageIndex);
                    DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                    modifyAnnot(pageIndex, annot, borderRect, pdfRectF,
                            startingPt, kneePt, endingPt, da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), mEditView.getText()
                                    .toString(), 1, false);
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    return true;
                } else {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    return true;
                }
            } else {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || !(annot instanceof FreeText)) {
            return;
        }
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this) return;
        try {
            if (!((FreeText) annot).getIntent().equalsIgnoreCase("FreeTextCallout")) {
                return;
            }
            if (AppAnnotUtil.equals(mBitmapAnnot, annot) && annot.getPage().getIndex() == pageIndex) {
                canvas.save();
                RectF textbbox = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(textbbox, textbbox, pageIndex);
                ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                PointF startingPt = points.get(0);
                PointF kneePt = points.get(1);
                PointF endingPt = points.get(2);

                mPdfViewCtrl.convertPdfPtToPageViewPt(startingPt, startingPt, pageIndex);
                mPdfViewCtrl.convertPdfPtToPageViewPt(kneePt, kneePt, pageIndex);
                mPdfViewCtrl.convertPdfPtToPageViewPt(endingPt, endingPt, pageIndex);

                Matrix matrix = new Matrix();
                switch (mLastOper) {
                    case FtUtil.OPER_BORDER:
                        matrix.preTranslate(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        startingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        kneePt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        endingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        matrix.mapRect(textbbox);
                        break;
                    case FtUtil.OPER_STARTING:
                        startingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                            FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex, textbbox,
                                    startingPt, kneePt, endingPt);
                        break;
                    case FtUtil.OPER_KNEE:
                        if (kneePt.x == endingPt.x) {
                            kneePt.offset(0, mLastPoint.y - mDownPoint.y);
                        } else if (kneePt.y == endingPt.y) {
                            kneePt.offset(mLastPoint.x - mDownPoint.x, 0);
                        }
                        break;
                    case FtUtil.OPER_SCALE:
                        matrix = FtUtil.calculateScaleMatrix(mCurrentCtr, textbbox, mLastPoint.x - mDownPoint.x, mLastPoint.y
                                - mDownPoint.y);
                        matrix.mapRect(textbbox);
                        if (kneePt.x < endingPt.x) {
                            endingPt.set(textbbox.left, (textbbox.top + textbbox.bottom) / 2);
                            kneePt.set(
                                    endingPt.x
                                            - FtUtil.widthOnPageView(mPdfViewCtrl,
                                            pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH),
                                    endingPt.y);
                        } else if (kneePt.x > endingPt.x) {
                            endingPt.set(textbbox.right, (textbbox.top + textbbox.bottom) / 2);
                            kneePt.set(
                                    endingPt.x
                                            + FtUtil.widthOnPageView(mPdfViewCtrl,
                                            pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH),
                                    endingPt.y);
                        } else if (kneePt.y < endingPt.y) {
                            endingPt.set((textbbox.left + textbbox.right) / 2, textbbox.top);
                            kneePt.set(
                                    endingPt.x,
                                    endingPt.y
                                            - FtUtil.widthOnPageView(mPdfViewCtrl,
                                            pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH));
                        } else if (kneePt.y > kneePt.y) {
                            endingPt.set((textbbox.left + textbbox.right) / 2, textbbox.bottom);
                            kneePt.set(
                                    endingPt.x,
                                    endingPt.y
                                            + FtUtil.widthOnPageView(mPdfViewCtrl,
                                            pageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH));
                        }
                            FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex, textbbox,
                                    startingPt, kneePt, endingPt);
                        break;
                    case FtUtil.OPER_TRANSLATE:
                        matrix.preTranslate(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        kneePt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        endingPt.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        matrix.mapRect(textbbox);
                            FtUtil.resetKneeAndEndingPt(mPdfViewCtrl, pageIndex, textbbox,
                                    startingPt, kneePt, endingPt);
                        break;
                    default:
                        break;
                }

                PointF editPoint = new PointF(mEditPoint.x, mEditPoint.y);
                if (editPoint.x != 0 || editPoint.y != 0) {
                    mPdfViewCtrl.convertPdfPtToPageViewPt(editPoint, editPoint, pageIndex);
                }

                mTextUtil.setTextString(pageIndex, annot.getContent(), mEditState);
                mTextUtil.setStartPoint(new PointF(textbbox.left, textbbox.top));
                mTextUtil.setEditPoint(editPoint);
                mTextUtil.setMaxRect(textbbox.width(), textbbox.height());
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                int opacity = (int) (((FreeText) annot).getOpacity() * 100);
                mTextUtil.setTextColor(da.getText_color(), AppDmUtil.opacity100To255(opacity));
                int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                mTextUtil.setFont(mTextUtil.getSupportFontName(fontId),da.getText_size());
                if (mIsSelcetEndText) {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd() + 1);
                } else {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd());
                }
                mTextUtil.loadText(true);
                mTextUtil.drawText(canvas);

                mPaintOut.setColor(Color.RED);
                mPaintOut.setStrokeWidth(FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_BORDER_WIDTH));
                mPaintOut.setPathEffect(FtUtil.getDashPathEffect(mContext, mPdfViewCtrl,
                        pageIndex, 1, true));
                canvas.drawLine(startingPt.x, startingPt.y, kneePt.x, kneePt.y, mPaintOut);
                canvas.drawLine(kneePt.x, kneePt.y, endingPt.x, endingPt.y, mPaintOut);

                // draw frame
                RectF frameRect = new RectF();
                frameRect.set(textbbox);
                frameRect.inset(FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex,
                        FtUtil.DEFAULT_BORDER_WIDTH) / 2, FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_BORDER_WIDTH) / 2);

                canvas.drawRect(frameRect, mPaintOut);

                // draw arrow
                Path arrowPath = FtUtil.getArrowPath(mPdfViewCtrl, pageIndex, kneePt.x,
                        kneePt.y, startingPt.x, startingPt.y);
                canvas.drawPath(arrowPath, mPaintOut);
                if (!mEditState) {
                    mPaintCtr.setColor(Color.WHITE);
                    mPaintCtr.setStyle(Paint.Style.FILL);

                    // draw textbbox control points
                    float[] ctlPts = FtUtil.calculateTextControlPoints(frameRect);
                    float radius = AppDisplay.getInstance(mContext).dp2px(FtUtil.RADIUS);
                    for (int i = 0; i < ctlPts.length; i += 2) {
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                    }
                    // draw borderbbox control points
                    canvas.drawCircle(startingPt.x, startingPt.y, radius, mPaintCtr);
                    canvas.drawCircle(kneePt.x, kneePt.y, radius, mPaintCtr);
                    mPaintCtr.setColor(Color.RED);
                    mPaintCtr.setStyle(Paint.Style.STROKE);
                    for (int i = 0; i < ctlPts.length; i += 2) {
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                    }
                    // draw borderbbox control points
                    canvas.drawCircle(startingPt.x, startingPt.y, radius, mPaintCtr);
                    canvas.drawCircle(kneePt.x, kneePt.y, radius, mPaintCtr);
                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this && !mEditState) {
            try {
                mDocViewBBox = AppUtil.toRectF(((FreeText) curAnnot).getInnerRect());
                int pageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewBBox, mDocViewBBox, pageIndex);

                    mDocViewBBox.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                    Matrix matrix = new Matrix();
                    switch (mLastOper) {
                        case FtUtil.OPER_BORDER:
                            matrix.preTranslate(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                            matrix.mapRect(mDocViewBBox);
                            break;
                        case FtUtil.OPER_SCALE:
                            matrix = FtUtil.calculateScaleMatrix(mCurrentCtr, mDocViewBBox, mLastPoint.x - mDownPoint.x, mLastPoint.y
                                    - mDownPoint.y);
                            matrix.mapRect(mDocViewBBox);
                            break;
                        case FtUtil.OPER_TRANSLATE:
                            matrix.preTranslate(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                            matrix.mapRect(mDocViewBBox);
                            break;
                        default:
                            break;
                    }
                    float width = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 2);
                    mDocViewBBox.inset(-width * 0.5f, -width * 0.5f);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewBBox, mDocViewBBox, pageIndex);
                    mDocViewBBox.inset(-AppAnnotUtil.getAnnotBBoxSpace(), -AppAnnotUtil.getAnnotBBoxSpace());
                    mAnnotMenu.update(mDocViewBBox);
                    if (mPropertyBar.isShowing()) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewBBox);
                        mPropertyBar.update(rectF);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    public void onColorValueChanged(int color) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null){

                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && color != (int) da.getText_color()) {
                    int pageIndex = annot.getPage().getIndex();
                    ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                    PointF startingPt = points.get(0);
                    PointF kneePt = points.get(1);
                    PointF endingPt = points.get(2);

                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()),
                            startingPt, kneePt,endingPt, color, (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(),
                            1, false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onOpacityValueChanged(int opacity) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null){
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && AppDmUtil.opacity100To255(opacity) != (int) (((FreeText) annot).getOpacity() * 255f)) {
                    int pageIndex = annot.getPage().getIndex();
                    ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                    PointF startingPt = points.get(0);
                    PointF kneePt = points.get(1);
                    PointF endingPt = points.get(2);

                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()),
                            startingPt, kneePt,endingPt, (int) da.getText_color(), AppDmUtil.opacity100To255(opacity),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(),
                            1, false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onFontValueChanged(String fontName) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null){
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                int fontId = mTextUtil.getSupportFontID(fontName);

                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && fontId != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                    int pageIndex = annot.getPage().getIndex();
                    RectF rectF = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    RectF textRectF = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(textRectF, textRectF, pageIndex);
                    float fontWidth = mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, fontName, da.getText_size());
                    if (textRectF.width() < fontWidth) {
                        textRectF.set(textRectF.left, textRectF.top,
                                textRectF.left + fontWidth, textRectF.bottom);
                    }
                    RectF rectChanged = new RectF(textRectF);
                    rectF.union(rectChanged);
                    rectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);

                    ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                    PointF startingPt = points.get(0);
                    PointF kneePt = points.get(1);
                    PointF endingPt = points.get(2);

                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), rectChanged,
                            startingPt, kneePt, endingPt, da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f), fontId,
                            da.getText_size(), annot.getContent(), 1, false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onFontSizeValueChanged(float fontSize) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null){
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && fontSize != da.getText_size()) {
                    int pageIndex = annot.getPage().getIndex();
                    RectF rectF = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
//                String content = annot.getContent();
                    RectF textRectF = AppUtil.toRectF((((FreeText) annot).getInnerRect()));
                    mPdfViewCtrl.convertPdfRectToPageViewRect(textRectF, textRectF, pageIndex);
                    float fontWidth = mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())), fontSize);
                    if (textRectF.width() < fontWidth) {
                        textRectF.set(textRectF.left, textRectF.top,
                                textRectF.left + fontWidth, textRectF.bottom);
                    }
                    RectF rectChanged = new RectF(textRectF);
                    rectF.union(rectChanged);
                    rectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);

                    ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
                    PointF startingPt = points.get(0);
                    PointF kneePt = points.get(1);
                    PointF endingPt = points.get(2);
                    //bbox union textbbox
                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), rectChanged,
                            startingPt, kneePt, endingPt, da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), fontSize, annot.getContent(), 1, false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

        // step 1: set current annot to null
        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            AppUtil.dismissInputSoft(mEditView);
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
            mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();

            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
            PointF startingPt = points.get(0);
            PointF kneePt = points.get(1);
            PointF endingPt = points.get(2);
            // step 2: delete annot in pdf
            final CalloutDeleteUndoItem undoItem = new CalloutDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            DefaultAppearance da = ((FreeText)annot).getDefaultAppearance();

            int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            undoItem.mFont = mTextUtil.getSupportFont(fontId);
            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = annot.getUniqueID();
            undoItem.mColor = Color.RED;
            undoItem.mTextColor = da.getText_color();
            undoItem.mDaFlags = da.getFlags();
            undoItem.mIntent = ((FreeText) annot).getIntent();
            undoItem.mOpacity = ((Markup)annot).getOpacity();
            undoItem.mBBox = AppUtil.toRectF(annot.getRect());
//            undoItem.mAuthor = annot.getAuthor();
            undoItem.mContents = (annot.getContent() == null) ? "" : annot.getContent();
            undoItem.mFontSize = da.getText_size();
//            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mStartingPt = startingPt;
            undoItem.mKneePt = kneePt;
            undoItem.mEndingPt = endingPt;
            undoItem.mTextBBox = AppUtil.toRectF(((FreeText) annot).getInnerRect());
            RectF annotRect = new RectF(undoItem.mTextBBox);

            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);
            undoItem.mComposedText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, annotRect, undoItem.mContents, mTextUtil.getSupportFontName(fontId), undoItem.mFontSize);
            undoItem.mTextLineCount = undoItem.mComposedText.size();
            undoItem.mRotation = ((FreeText) annot).getRotation();

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            CalloutEvent event = new CalloutEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FreeText) annot, mPdfViewCtrl);
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                        }
                    }
                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void modifyAnnot(int pageIndex, Annot annot, RectF bbox, RectF textBBox,
                             PointF startingPt, PointF kneePt, PointF endingPt, int color, int opacity, int fontId,
                             float fontSize, String content, int borderType, boolean isModifyJni) {
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }
        final CalloutModifyUndoItem undoItem = new CalloutModifyUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = pageIndex;
        undoItem.setCurrentValue(annot);
//        undoItem.mNM = annot.getNM();
//        undoItem.mAuthor = annot.getAuthor();// AppAnnotUtil.getAnnotAuthor(mRead.getDocViewer().getDocument());
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mColor = Color.RED;
        undoItem.mOpacity = opacity / 255f;
        undoItem.mFont = mTextUtil.getSupportFont(fontId);
        undoItem.mFontId = fontId;
        undoItem.mFontSize = fontSize;
        undoItem.mTextColor = color;
        undoItem.mContents = (content == null) ? "" : content;
        undoItem.mBBox = new RectF(bbox.left, bbox.top, bbox.right, bbox.bottom);
        undoItem.mTextBBox = new RectF(textBBox.left, textBBox.top, textBBox.right, textBBox.bottom);
        undoItem.mStartingPt = new PointF(startingPt.x, startingPt.y);
        undoItem.mKneePt = new PointF(kneePt.x, kneePt.y);
        undoItem.mEndingPt = new PointF(endingPt.x, endingPt.y);
        undoItem.mBorderType = borderType;

        RectF annotRect = new RectF(undoItem.mTextBBox);
        mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);

        undoItem.mComposedText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, annotRect, content, mTextUtil.getSupportFontName(fontId), fontSize, true);
        undoItem.mTextLineCount = undoItem.mComposedText.size();

        RectF adjustBBox = new RectF(annotRect);
        mTextUtil.adjustTextRect(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(fontId), undoItem.mFontSize, adjustBBox, undoItem.mComposedText);
        mPdfViewCtrl.convertPageViewRectToPdfRect(adjustBBox, adjustBBox, pageIndex);
        RectF bbox1 = new RectF(undoItem.mBBox);
        FtUtil.adjustKneeAndEndingPt(bbox1, adjustBBox, undoItem.mStartingPt, undoItem.mKneePt, undoItem.mEndingPt);
        undoItem.mBBox = new RectF(bbox1);
        undoItem.mTextBBox = new RectF(adjustBBox);

        undoItem.mLastColor = mTempLastColor;
        undoItem.mLastOpacity = mTempLastOpacity / 255f;
        undoItem.mLastBBox = mTempLastBBox;
        undoItem.mLastFont = mTextUtil.getSupportFont(mTempLastFontId);
        undoItem.mLastFontSize = mTempLastFontSize;
        undoItem.mLastContent = mTempLastContent;
        undoItem.mLastTextBBox = mTempLastTextBBox;
        undoItem.mLastStartingPt = mTempLastStartingPt;
        undoItem.mLastKneePt = mTempLastKneePt;
        undoItem.mLastEndingPt = mTempLastEndingPt;
        undoItem.mLastBorderType = mTempLastBorderType;
        undoItem.mLastComposedText = mTempLastComposedText;

        modifyAnnot(pageIndex, (FreeText) annot, undoItem, isModifyJni, true, "FreeTextCallout", null);
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }
        modifyAnnot(annot, (ICalloutAnnotContent) content, addUndo, result);

    }

    private void modifyAnnot(Annot annot, ICalloutAnnotContent content, boolean isAddUndo, Event.Callback result) {
        FreeText lAnnot = (FreeText) annot;
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }
        try {
            ArrayList<PointF> points = FtUtil.getCalloutLinePoints(((FreeText) annot));
            PointF startingPt = points.get(0);
            PointF kneePt = points.get(1);
            PointF endingPt = points.get(2);

            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            DefaultAppearance da = lAnnot.getDefaultAppearance();
            final CalloutModifyUndoItem undoItem = new CalloutModifyUndoItem(mPdfViewCtrl);
            undoItem.mNM = annot.getUniqueID();
            undoItem.setCurrentValue(content);
            undoItem.mLastColor = (int) da.getText_color();
            undoItem.mLastOpacity = (int) (lAnnot.getOpacity());
            undoItem.mLastBBox = new RectF(AppUtil.toRectF(annot.getRect()));
            undoItem.mLastTextBBox = new RectF(AppUtil.toRectF(lAnnot.getInnerRect()));
            undoItem.mLastContent = annot.getContent();
            int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            undoItem.mLastFont = mTextUtil.getSupportFont(fontId);
            undoItem.mLastFontSize = da.getText_size();
            undoItem.mLastBorderType = 1;
            undoItem.mLastStartingPt = startingPt;
            undoItem.mLastKneePt = kneePt;
            undoItem.mLastEndingPt = endingPt;
            if (undoItem.mSubject == null) {
                undoItem.mSubject = "";
            }

            RectF annotRect = new RectF(undoItem.mTextBBox);
            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);

            undoItem.mContents = FtTextUtil.filterEmoji(undoItem.mContents);
            undoItem.mLastComposedText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, annotRect,
                    lAnnot.getContent(), mTextUtil.getSupportFontName(undoItem.mFontId), undoItem.mFontSize);
            undoItem.mComposedText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, annotRect,
                    undoItem.mContents, mTextUtil.getSupportFontName(undoItem.mFontId), undoItem.mFontSize);
            undoItem.mTextLineCount = undoItem.mComposedText.size();

//            lAnnot.setAuthor(content.getAuthor());

            if (!undoItem.mContents.equals(" ")) {
                RectF adjustBBox = new RectF(annotRect);
                mTextUtil.adjustTextRect(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(undoItem.mFontId), undoItem.mFontSize, adjustBBox, undoItem.mComposedText);
                mPdfViewCtrl.convertPageViewRectToPdfRect(adjustBBox, adjustBBox, pageIndex);
                RectF bbox = new RectF(undoItem.mBBox);
                FtUtil.adjustKneeAndEndingPt(bbox, adjustBBox, undoItem.mStartingPt, undoItem.mKneePt, undoItem.mEndingPt);
                undoItem.mBBox = new RectF(bbox);
                undoItem.mTextBBox = new RectF(adjustBBox);
            }
            modifyAnnot(pageIndex, lAnnot, undoItem, true, isAddUndo, "", result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void modifyAnnot(final int pageIndex, final FreeText annot, final CalloutModifyUndoItem undoItem,
                               boolean isModifyJni, final boolean isAddUndo, final String fromType, final Event.Callback result) {
        try {
            final RectF tempRectF = AppUtil.toRectF(annot.getRect());
            if (isModifyJni) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(true);
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                CalloutEvent event = new CalloutEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (isAddUndo) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                            }
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            if (fromType.equals("")) {
                                mModifyed = true;
                            }
                            try {

                                if (mPdfViewCtrl.isPageVisible(pageIndex) && !isAddUndo) {
                                    RectF viewRect = AppUtil.toRectF(annot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                    viewRect.union(tempRectF);
                                    viewRect.inset(-40, -40);
                                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                                }
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }

                        }
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);
            }


                if (!fromType.equals("")) {

                    if (isModifyJni) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
                    }
                    mModifyed = true;

                    if (!isModifyJni) {
                        FreeText co_Annot = (FreeText) annot;
                        DefaultAppearance da = co_Annot.getDefaultAppearance();
                        da.setText_color(undoItem.mTextColor);
                        Font font = mTextUtil.getSupportFont(undoItem.mFontId);
                        da.setFont(font);
                        da.setText_size(undoItem.mFontSize);
                        co_Annot.setDefaultAppearance(da);
                        co_Annot.setOpacity(undoItem.mOpacity);

                        co_Annot.move(AppUtil.toFxRectF(undoItem.mBBox));
                        PointFArray _pArray = new PointFArray();
                        _pArray.add(AppUtil.toFxPointF(undoItem.mStartingPt));
                        _pArray.add(AppUtil.toFxPointF(undoItem.mKneePt));
                        _pArray.add(AppUtil.toFxPointF(undoItem.mEndingPt));
                        co_Annot.setCalloutLinePoints(_pArray);
                        co_Annot.setInnerRect(AppUtil.toFxRectF(undoItem.mTextBBox));
                        co_Annot.setContent(undoItem.mContents);
                        co_Annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                        co_Annot.resetAppearanceStream();

                        RectF annotRectF = AppUtil.toRectF(annot.getRect());
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                            annotRectF.union(tempRectF);
                            annotRectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                            annotRectF.inset(-40, -40);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                        }
                    }
                }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

}
