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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.freetext.FtTextUtil;
import com.foxit.uiextensions.annots.freetext.FtUtil;
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
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppKeyboardUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.UnsupportedEncodingException;

public class CalloutToolHandler implements ToolHandler {
    private Context mContext;
    private FtTextUtil mTextUtil;

    private int mColor;
    private int mBorderColor;
    private int mOpacity;
    private int mFontId;
    private float mFontSize;
    private int mBorderType;
    private EditText mEditView;
    private boolean mCreating;
    private int mCreateIndex;
    private PointF mTextStartPt = new PointF();
    private PointF mTextStartPdfPt = new PointF();
    private PointF mDownPdfPt = new PointF();
    private PointF mEditPoint = new PointF();
    public int mLastPageIndex = -1;
    private String mAnnotText;
    private float mBBoxWidth;
    private float mBBoxHeight;
    private boolean mIsContinue;
    private boolean mIsSelcetEndText = false;

    private PointF mKneePoint = new PointF();
    private PointF mEndingPoint = new PointF();
    private Paint mPaintOut;
    private int mCurrentPosition;
    private boolean mCreateAlive = true;
    private boolean mIsCreated;

    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    public interface CreateAnnotResult {
        public void callBack();
    }

    public CalloutToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiExtensionsManager.getMainFrame().getPropertyBar();
        mBorderColor = PropertyBar.PB_COLORS_CALLOUT[6];

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);
        mPaintOut.setColor(mBorderColor);

        mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);

        pdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {
            }

            @Override
            public void onDocOpened(PDFDoc document, int errCode) {
            }

            @Override
            public void onDocWillClose(PDFDoc document) {
            }

            @Override
            public void onDocClosed(PDFDoc document, int errCode) {
                mAnnotText = "";
                mTextStartPt.set(0, 0);
                mEditPoint.set(0, 0);
                mLastPageIndex = -1;
                mBBoxHeight = 0;
                mBBoxWidth = 0;
                AppUtil.dismissInputSoft(mEditView);
                mUiExtensionsManager.getRootView().removeView(mEditView);
                mEditView = null;
                mBBoxHeight = 0;
                mBBoxWidth = 0;
                mCreating = false;
                mIsContinue = false;
                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                if (mTextUtil != null) {
                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                    mTextUtil.setKeyboardOffset(0);
                }
            }

            @Override
            public void onDocWillSave(PDFDoc document) {
            }

            @Override
            public void onDocSaved(PDFDoc document, int errCode) {
            }
        });

        mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                mUiExtensionsManager.setCurrentToolHandler(CalloutToolHandler.this);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_CALLOUT;
            }
        });
    }

    @Override
    public String getType() {
        return TH_TYPE_CALLOUT;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
        mCreateAlive = true;
        mIsCreated = false;
        ViewGroup parent = mUiExtensionsManager.getRootView();
        AppKeyboardUtil.setKeyboardListener(parent, parent, new AppKeyboardUtil.IKeyboardListener() {
            @Override
            public void onKeyboardOpened(int keyboardHeight) {

            }

            @Override
            public void onKeyboardClosed() {
                mCreateAlive = false;
            }
        });

        resetPropertyBar();
        resetAnnotBar();
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    private void resetPropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_CALLOUT.length];
        System.arraycopy(PropertyBar.PB_COLORS_CALLOUT, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_CALLOUT[0];
        mPropertyBar.setColors(colors);

        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, mTextUtil.getSupportFontName(mFontId));
        mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, mFontSize);
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_FONTSIZE
                | PropertyBar.PROPERTY_FONTNAME;
    }

    private void resetAnnotBar() {
        mUiExtensionsManager.getMainFrame().getToolSetBar().removeAllItems();
        mOKItem = new BaseItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
                mUiExtensionsManager.setCurrentToolHandler(null);
            }
        });

        mPropertyItem = new PropertyCircleItemImp(mContext) {

            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (CalloutToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
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
        mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinue));

        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }

                mIsContinue = !mIsContinue;
                mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinue));
                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinue);
            }
        });

        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
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
    public void onDeactivate() {
        if (mEditView != null) {
            mIsContinue = false;
            if (!mIsCreated) {
                createCOAnnot();
            }
        }
        AppKeyboardUtil.removeKeyboardListener(mUiExtensionsManager.getRootView());
        mCreateAlive = true;
    }

    @Override
    public boolean onTouchEvent(final int pageIndex, MotionEvent e) {
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        final PointF pdfPoint = new PointF(point.x, point.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);

        float x = point.x;
        float y = point.y;

        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mUiExtensionsManager.getCurrentToolHandler() == this) {
                    if (mCreating) {
                        return false;
                    } else {
                        mDownPdfPt.set(x, y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mDownPdfPt, mDownPdfPt, pageIndex);
                        mBBoxWidth = 0;
                        mBBoxHeight = 0;
                        if (mLastPageIndex == -1) {
                            mLastPageIndex = pageIndex;
                        }
                        calculateTextPositionByDown(mPdfViewCtrl, x, y);
                        mTextStartPt.set(calculateTextStartPtByDown(mPdfViewCtrl, x, y).x,
                                calculateTextStartPtByDown(mPdfViewCtrl, x, y).y);
                        mTextStartPdfPt.set(mTextStartPt.x, mTextStartPt.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mTextStartPdfPt, mTextStartPdfPt, pageIndex);

                        mCreateIndex = pageIndex;
                        return true;
                    }
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                if (mUiExtensionsManager.getCurrentToolHandler() == this && mEditView == null) {
                    mEditView = new EditText(mContext);
                    mEditView.setLayoutParams(new LayoutParams(1, 1));
                    mEditView.addTextChangedListener(new TextWatcher() {

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            mAnnotText = FtTextUtil.filterEmoji(String.valueOf(s));
                            mPdfViewCtrl.invalidate();
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
                            mBBoxWidth = FtUtil.widthOnPageView(mPdfViewCtrl, mLastPageIndex, FtUtil.DEFAULTTEXTWIDTH);
                        }

                        @Override
                        public void onMaxHeightChanged(float maxHeight) {
                            mBBoxHeight = maxHeight;
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
                            PointF point = new PointF(editPointX, editPointY);
                            mPdfViewCtrl.convertPageViewPtToPdfPt(point, point, pageIndex);
                            mEditPoint.set(point.x, point.y);
                        }

                    });
                    mUiExtensionsManager.getRootView().addView(mEditView);
                    mPdfViewCtrl.invalidate();

                    AppUtil.showSoftInput(mEditView);
                    mTextUtil.getBlink().postDelayed((Runnable) mTextUtil.getBlink(), 500);
                    mCreating = true;
                }
                mCreateAlive = true;
                return false;
            case MotionEvent.ACTION_CANCEL:
                mTextStartPt.set(0, 0);
                mEditPoint.set(0, 0);
                mCreateAlive = true;
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point);
    }

    @Override
    public boolean isContinueAddAnnot() {
        return mIsContinue;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        mIsContinue = continueAddAnnot;
    }

    private boolean onSingleTapOrLongPress(final int pageIndex, final PointF point) {
        float x = point.x;
        float y = point.y;
        if (mUiExtensionsManager.getCurrentToolHandler() == this && mEditView != null) {
            RectF rectF = new RectF(mTextStartPt.x, mTextStartPt.y,
                    mTextStartPt.x + getBBoxWidth(pageIndex), mTextStartPt.y + getBBoxHeight(pageIndex));
            if (rectF.contains(x, y)) {
                PointF pointF = new PointF(x, y);
                mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointF, pageIndex);
                mEditPoint.set(pointF.x, pointF.y);
                mTextUtil.resetEditState();

                RectF _rect = new RectF(rectF);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(_rect, _rect, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(_rect));
                AppUtil.showSoftInput(mEditView);
                return true;
            } else {
                if (!mIsContinue) {
                    mUiExtensionsManager.setCurrentToolHandler(null);
                }
                if (mCreateAlive) {
                    mCreateAlive = false;
                    if (mUiExtensionsManager.getCurrentToolHandler() == CalloutToolHandler.this) {
                        createCOAnnot();
                    }
                    return true;
                } else {
                    mCreateAlive = true;
                }
                mIsContinue = true;
                setCreateAnnotListener(new CreateAnnotResult() {
                    @Override
                    public void callBack() {
                        mDownPdfPt.set(point.x, point.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mDownPdfPt, mDownPdfPt, pageIndex);
                        mBBoxWidth = 0;
                        mBBoxHeight = 0;
                        if (mLastPageIndex == -1) {
                            mLastPageIndex = pageIndex;
                        }
                        calculateTextPositionByDown(mPdfViewCtrl, point.x, point.y);
                        mTextStartPt.set(calculateTextStartPtByDown(mPdfViewCtrl, point.x, point.y).x,
                                calculateTextStartPtByDown(mPdfViewCtrl, point.x, point.y).y);
                        mTextStartPdfPt.set(mTextStartPt.x, mTextStartPt.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mTextStartPdfPt, mTextStartPdfPt, pageIndex);
                        mCreateIndex = pageIndex;
                        mPdfViewCtrl.invalidate();
                        if (mEditView != null) {
                            AppUtil.showSoftInput(mEditView);
                        }
                    }
                });
                createCOAnnot();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        canvas.save();
        if (mLastPageIndex == pageIndex && mEditView != null) {
            PointF textStartPoint = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(textStartPoint, textStartPoint, pageIndex);

            PointF editPoint = new PointF(mEditPoint.x, mEditPoint.y);
            if (editPoint.x != 0 || editPoint.y != 0) {
                mPdfViewCtrl.convertPdfPtToPageViewPt(editPoint, editPoint, pageIndex);
            }

            mTextUtil.setTextString(pageIndex, mAnnotText, true);
            mTextUtil.setStartPoint(textStartPoint);
            mTextUtil.setEditPoint(editPoint);
            mTextUtil.setMaxRect(FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULTTEXTWIDTH),
                    mPdfViewCtrl.getPageViewHeight(pageIndex) - textStartPoint.y);
            mTextUtil.setTextColor(mColor, AppDmUtil.opacity100To255(mOpacity));
            mTextUtil.setFont(mTextUtil.getSupportFontName(mFontId), mFontSize);

            if (mIsSelcetEndText) {
                mTextUtil.setEndSelection(mEditView.getSelectionEnd() + 1);
            } else {
                mTextUtil.setEndSelection(mEditView.getSelectionEnd());
            }

            mTextUtil.loadText();
            mTextUtil.drawText(canvas);

            // canvas frame board
            mPaintOut.setStrokeWidth(FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_BORDER_WIDTH));
            mPaintOut.setPathEffect(FtUtil.getDashPathEffect(mContext, mPdfViewCtrl, pageIndex, mBorderType, true));

            mKneePoint.set(calculateKneePtByTextStartPt(mPdfViewCtrl, textStartPoint.x, textStartPoint.y).x,
                    calculateKneePtByTextStartPt(mPdfViewCtrl, textStartPoint.x, textStartPoint.y).y);
            mEndingPoint.set(calculateEndingPtByTextStartPt(mPdfViewCtrl, textStartPoint.x, textStartPoint.y).x,
                    calculateEndingPtByTextStartPt(mPdfViewCtrl, textStartPoint.x, textStartPoint.y).y);
            PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, pageIndex);
            canvas.drawLine(startPoint.x, startPoint.y, mKneePoint.x, mKneePoint.y, mPaintOut);
            canvas.drawLine(mKneePoint.x, mKneePoint.y, mEndingPoint.x, mEndingPoint.y, mPaintOut);

            RectF frameRectF = new RectF();
            frameRectF.set(
                    textStartPoint.x,
                    textStartPoint.y,
                    textStartPoint.x + getBBoxWidth(pageIndex),
                    textStartPoint.y + getBBoxHeight(pageIndex));
            frameRectF.inset(FtUtil.widthOnPageView(mPdfViewCtrl, mLastPageIndex,
                    FtUtil.DEFAULT_BORDER_WIDTH) / 2, FtUtil.widthOnPageView(mPdfViewCtrl,
                    mLastPageIndex, FtUtil.DEFAULT_BORDER_WIDTH) / 2);
            if (mBorderType == FtUtil.BORDERCLOUD) {
                canvas.drawPath(FtUtil.getCloudy_Rectangle(mPdfViewCtrl, pageIndex, frameRectF, 0.0f), mPaintOut);
            } else {
                canvas.drawRect(frameRectF, mPaintOut);
            }

            Path arrowPath = FtUtil.getArrowPath(mPdfViewCtrl, mLastPageIndex, mKneePoint.x,
                    mKneePoint.y, startPoint.x, startPoint.y);
            canvas.drawPath(arrowPath, mPaintOut);
        }

        canvas.restore();
    }

    private void createCOAnnot() {
        PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
        mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mCreateIndex);
        final RectF pdfRectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mCreateIndex), pdfPointF.y
                + getBBoxHeight(mCreateIndex));
        final RectF textRectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mCreateIndex), pdfPointF.y
                + getBBoxHeight(mCreateIndex));

        PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
        mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);
        RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startPoint.x, startPoint.y, mKneePoint.x,
                mKneePoint.y, mEndingPoint.x, mEndingPoint.y);
        pdfRectF.union(borderRect);
        mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, mCreateIndex);
        mPdfViewCtrl.convertPageViewRectToPdfRect(textRectF, textRectF, mCreateIndex);

        String content = "";
        try {
            if (!TextUtils.isEmpty(mAnnotText))
                content = new String(mAnnotText.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        final CalloutAddUndoItem undoItem = new CalloutAddUndoItem(mPdfViewCtrl);
        undoItem.mNM = AppDmUtil.randomUUID(null);
        undoItem.mPageIndex = mCreateIndex;
        undoItem.mColor = mBorderColor;
        undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
        undoItem.mContents = content;
        undoItem.mFontId = mFontId;
        undoItem.mFont = mTextUtil.getSupportFont(mFontId);
        undoItem.mFontSize = mFontSize;
        undoItem.mFlags = Annot.e_FlagPrint;
        undoItem.mTextColor = mColor;
        undoItem.mDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
        undoItem.mSubject = "Callout";
        undoItem.mIntent = "FreeTextCallout";
        undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
        undoItem.mBBox = new RectF(pdfRectF.left, pdfRectF.top, pdfRectF.right, pdfRectF.bottom);
        undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        PointF startingPt = new PointF(startPoint.x, startPoint.y);
        PointF kneePt = new PointF(mKneePoint.x, mKneePoint.y);
        PointF endingPt = new PointF(mEndingPoint.x, mEndingPoint.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(startingPt, startingPt, mCreateIndex);
        mPdfViewCtrl.convertPageViewPtToPdfPt(kneePt, kneePt, mCreateIndex);
        mPdfViewCtrl.convertPageViewPtToPdfPt(endingPt, endingPt, mCreateIndex);
        undoItem.mStartingPt = new PointF(startingPt.x, startingPt.y);
        undoItem.mKneePt = new PointF(kneePt.x, kneePt.y);
        undoItem.mEndingPt = new PointF(endingPt.x, endingPt.y);
        undoItem.mTextBBox = new RectF(textRectF.left, textRectF.top, textRectF.right, textRectF.bottom);
        undoItem.mBorderType = mBorderType;

        RectF annotRect = new RectF(undoItem.mTextBBox);
        mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mCreateIndex);
        undoItem.mComposedText = mTextUtil.getComposedText(mPdfViewCtrl, mCreateIndex, annotRect, content, mTextUtil.getSupportFontName(mFontId), mFontSize);
        undoItem.mTextLineCount = undoItem.mComposedText.size();

        if (!undoItem.mContents.equals(" ")) {
            RectF adjustBBox = new RectF(annotRect);
            mTextUtil.adjustTextRect(mPdfViewCtrl, mCreateIndex, mTextUtil.getSupportFontName(undoItem.mFontId), undoItem.mFontSize, adjustBBox, undoItem.mComposedText);
            mPdfViewCtrl.convertPageViewRectToPdfRect(adjustBBox, adjustBBox, mCreateIndex);
            RectF bbox = new RectF(undoItem.mBBox);
            FtUtil.adjustKneeAndEndingPt(bbox, adjustBBox, undoItem.mStartingPt, undoItem.mKneePt, undoItem.mEndingPt);
            undoItem.mBBox = new RectF(bbox);
            undoItem.mTextBBox = new RectF(adjustBBox);
        }

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mCreateIndex);
            int rotation = (page.getRotation() + mPdfViewCtrl.getViewRotation()) % 4;
            undoItem.mRotation = rotation == 0 ? rotation : 4 - rotation;
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(undoItem.mBBox)), Annot.e_FreeText);
            CalloutEvent event = new CalloutEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (FreeText) annot, mPdfViewCtrl);
            mUiExtensionsManager.getDocumentManager().setHasModifyTask(true);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        mUiExtensionsManager.getDocumentManager().onAnnotAdded(page, annot);
                        mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                        mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                        if (mPdfViewCtrl.isPageVisible(mCreateIndex)) {
                            try {
                                RectF rectF = AppUtil.toRectF(annot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF,rectF,mCreateIndex);
                                mPdfViewCtrl.refresh(mCreateIndex, AppDmUtil.rectFToRect(rectF));

                                if (mIsContinue && mCreateAlive) {
                                    mEditView.setText("");
                                } else {
                                    AppUtil.dismissInputSoft(mEditView);
                                    mUiExtensionsManager.getRootView().removeView(mEditView);
                                    mEditView = null;
                                    mCreating = false;
                                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                                    mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                    if (mPdfViewCtrl.isPageVisible(mCreateIndex) &&
                                            (mCreateIndex == mPdfViewCtrl.getPageCount() - 1 ||
                                            (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) &&
                                            mCreateIndex == mPdfViewCtrl.getCurrentPage()) {
                                        PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(mCreateIndex), mPdfViewCtrl.getPageViewHeight(mCreateIndex));
                                        mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, mCreateIndex);
                                        if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                            mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                            mTextUtil.setKeyboardOffset(0);
                                            PointF startPoint = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
                                            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);
                                            PointF _point = mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y);
                                            mPdfViewCtrl.gotoPage(mCreateIndex, _point.x, _point.y);
                                        }
                                    }
                                }

                                mAnnotText = "";
                                mTextStartPt.set(0, 0);
                                mEditPoint.set(0, 0);
                                mLastPageIndex = -1;
                                mBBoxHeight = 0;
                                mBBoxWidth = 0;
                                if (mIsContinue && mListener != null) {
                                    mListener.callBack();
                                }
                            }catch (PDFException e){
                                e.printStackTrace();
                            }
                        }
                    } else {
                        mAnnotText = "";
                        mTextStartPt.set(0, 0);
                        mEditPoint.set(0, 0);
                        mLastPageIndex = -1;
                        mBBoxHeight = 0;
                        mBBoxWidth = 0;
                        AppUtil.dismissInputSoft(mEditView);
                        mUiExtensionsManager.getRootView().removeView(mEditView);
                        mEditView = null;
                        mBBoxHeight = 0;
                        mBBoxWidth = 0;
                        mCreating = false;
                        mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                        mTextUtil.setKeyboardOffset(0);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void setBorderColor(int color, int opacity) {
        mBorderColor = color;
        mPaintOut.setColor(AppDmUtil.calColorByMultiply(mBorderColor,  AppDmUtil.opacity100To255(opacity)));
    }

    protected void onColorValueChanged(int color) {
        mColor = color;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mLastPageIndex);
            RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startPoint.x, startPoint.y, mKneePoint.x,
                    mKneePoint.y, mEndingPoint.x, mEndingPoint.y);
            borderRect.union(rectF);
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(borderRect));
        }

        setProItemColor(color);
    }

    private void setProItemColor(int color){
        if (mPropertyItem == null) return;
        mPropertyItem.setCentreCircleColor(color);
    }

    protected void onOpacityValueChanged(int opacity) {
        mOpacity = opacity;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mLastPageIndex);
            RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startPoint.x, startPoint.y, mKneePoint.x,
                    mKneePoint.y, mEndingPoint.x, mEndingPoint.y);
            borderRect.union(rectF);
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(borderRect));
        }
    }

    protected void onFontValueChanged(String font) {
        mFontId = mTextUtil.getSupportFontID(font);
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mLastPageIndex);
            RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startPoint.x, startPoint.y, mKneePoint.x,
                    mKneePoint.y, mEndingPoint.x, mEndingPoint.y);
            borderRect.union(rectF);
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(borderRect));
        }
    }

    protected void onFontSizeValueChanged(float fontSize) {
        mFontSize = fontSize;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mLastPageIndex);
            RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startPoint.x, startPoint.y, mKneePoint.x,
                    mKneePoint.y, mEndingPoint.x, mEndingPoint.y);
            borderRect.union(rectF);
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(borderRect));
        }
    }

    protected void onBorderTypeValueChanged(int borderType) {
        mBorderType = 1;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mLastPageIndex);
            RectF borderRect = FtUtil.getBorderRectByStartKneeAndEnding(startPoint.x, startPoint.y, mKneePoint.x,
                    mKneePoint.y, mEndingPoint.x, mEndingPoint.y);
            borderRect.union(rectF);
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(borderRect));
        }
    }

    private PointF calculateKneePtByTextStartPt(PDFViewCtrl pdfViewCtrl, float stX, float stY) {
        float width = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH);
        PointF pointKnee = new PointF();
        switch (mCurrentPosition) {
            case FtUtil.LEFTTOTOP:
                pointKnee.set(calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).x + width,
                        calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).y);
                break;
            case FtUtil.LEFTTOBOTTOM:
                pointKnee.set(calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).x + width,
                        calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).y);
                break;
            case FtUtil.RIGHTTOTOP:
                pointKnee.set(calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).x - width,
                        calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).y);
                break;
            case FtUtil.RIGHTTOBOTTOM:
                pointKnee.set(calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).x - width,
                        calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).y);
                break;
            case FtUtil.MIDBOTTOM:
                pointKnee.set(calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).x,
                        calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).y - width);
                break;
            case FtUtil.MIDTOP:
                pointKnee.set(calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).x,
                        calculateEndingPtByTextStartPt(pdfViewCtrl, stX, stY).y + width);
                break;
            default:
                break;
        }
        return pointKnee;
    }

    private PointF calculateEndingPtByTextStartPt(PDFViewCtrl pdfViewCtrl, float stX, float stY) {
        PointF pointEnd = new PointF();
        float width = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULTTEXTWIDTH);
        switch (mCurrentPosition) {
            case FtUtil.LEFTTOTOP:
            case FtUtil.LEFTTOBOTTOM:
                pointEnd.set(stX + width, stY + getBBoxHeight(mLastPageIndex) / 2);
                break;
            case FtUtil.RIGHTTOBOTTOM:
            case FtUtil.RIGHTTOTOP:
                pointEnd.set(stX, stY + getBBoxHeight(mLastPageIndex) / 2);
                break;
            case FtUtil.MIDBOTTOM:
                pointEnd.set(stX + width / 2, stY);
                break;
            case FtUtil.MIDTOP:
                pointEnd.set(stX + width / 2, stY + getBBoxHeight(mLastPageIndex));
                break;
            default:
                break;
        }
        return pointEnd;
    }

    private PointF calculateTextStartPtByDown(PDFViewCtrl pdfViewCtrl, float stX, float stY) {

        PointF pointknee = new PointF();
        PointF pointend = new PointF();
        PointF pointTextStart = new PointF();

        float kneeToend = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH);
        float startToKnee = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULT_STARTTOKNEE_WIDTH);
        float rectText = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULTTEXTWIDTH);
        switch (mCurrentPosition) {
            case FtUtil.LEFTTOTOP:
                pointknee.set(stX - startToKnee, stY - startToKnee);
                pointend.set(pointknee.x - kneeToend, pointknee.y);
                pointTextStart.set(pointend.x - rectText, pointend.y + getBBoxHeight(mLastPageIndex) / 2);
                break;
            case FtUtil.LEFTTOBOTTOM:
                pointknee.set(stX - startToKnee, stY + startToKnee);
                pointend.set(pointknee.x - kneeToend, pointknee.y);
                pointTextStart.set(pointend.x - rectText, pointend.y + getBBoxHeight(mLastPageIndex) / 2);
                break;
            case FtUtil.RIGHTTOBOTTOM:
                pointknee.set(stX + startToKnee, stY + startToKnee);
                pointend.set(pointknee.x + kneeToend, pointknee.y);
                pointTextStart.set(pointend.x, pointend.y + getBBoxHeight(mLastPageIndex) / 2);
                break;
            case FtUtil.RIGHTTOTOP:
                pointknee.set(stX + startToKnee, stY - startToKnee);
                pointend.set(pointknee.x + kneeToend, pointknee.y);
                pointTextStart.set(pointend.x, pointend.y + getBBoxHeight(mLastPageIndex) / 2);
                break;
            case FtUtil.MIDBOTTOM:
                pointknee.set(stX, stY + startToKnee);
                pointend.set(pointknee.x, pointknee.y + kneeToend);
                pointTextStart.set(pointend.x - rectText / 2, pointend.y);
                break;
            case FtUtil.MIDTOP:
                pointknee.set(stX, stY - startToKnee);
                pointend.set(pointknee.x, pointknee.y - kneeToend);
                pointTextStart.set(pointend.x - rectText / 2, pointend.y - getBBoxHeight(mLastPageIndex));
                break;
            default:
                break;
        }
        return pointTextStart;
    }

    private void calculateTextPositionByDown(PDFViewCtrl pdfViewCtrl, float stX, float stY) {
        float kneeToend = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULT_KENNTOEND_WIDTH);
        float startToKnee = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULT_STARTTOKNEE_WIDTH);
        float rectText = FtUtil.widthOnPageView(pdfViewCtrl, mLastPageIndex, FtUtil.DEFAULTTEXTWIDTH);

        // left top

        if (stX > kneeToend + startToKnee + rectText
                && stY > startToKnee + mTextUtil.getFontHeight(pdfViewCtrl, mLastPageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) / 2) {
            mCurrentPosition = FtUtil.LEFTTOTOP;
        }
        // left bottom
        else if (stX > kneeToend + startToKnee + rectText
                && stY < startToKnee + mTextUtil.getFontHeight(pdfViewCtrl, mLastPageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) / 2) {
            mCurrentPosition = FtUtil.LEFTTOBOTTOM;
        }
        // right bottom
        else if (mPdfViewCtrl.getPageViewWidth(mLastPageIndex) - stX > kneeToend + startToKnee + rectText
                && stY < startToKnee + mTextUtil.getFontHeight(pdfViewCtrl, mLastPageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) / 2) {
            mCurrentPosition = FtUtil.RIGHTTOBOTTOM;
        }
        // right top
        else if (mPdfViewCtrl.getPageViewWidth(mLastPageIndex) - stX > kneeToend + startToKnee + rectText
                && stY > startToKnee + mTextUtil.getFontHeight(pdfViewCtrl, mLastPageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) / 2) {
            mCurrentPosition = FtUtil.RIGHTTOTOP;
        }
        // bottom
        else if (mPdfViewCtrl.getPageViewWidth(mLastPageIndex) - stX < kneeToend + startToKnee + rectText
                && stX < kneeToend + startToKnee + rectText
                && stY < startToKnee + kneeToend + mTextUtil.getFontHeight(pdfViewCtrl, mLastPageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) / 2) {
            mCurrentPosition = FtUtil.MIDBOTTOM;
        }
        // top
        else if (mPdfViewCtrl.getPageViewWidth(mLastPageIndex) - stX < kneeToend + startToKnee + rectText
                && stX < kneeToend + startToKnee + rectText
                && stY > startToKnee + kneeToend + mTextUtil.getFontHeight(pdfViewCtrl, mLastPageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) / 2) {
            mCurrentPosition = FtUtil.MIDTOP;
        } else {
            mCurrentPosition = FtUtil.MIDTOP;
        }
    }

    private CreateAnnotResult mListener;

    private void setCreateAnnotListener(CreateAnnotResult listener) {
        mListener = listener;
    }

    private float getBBoxHeight(int pageIndex){
        return mBBoxHeight == 0 ? mTextUtil.getFontHeight(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) : mBBoxHeight;
    }

    private float getBBoxWidth(int pageIndex){
        return mBBoxWidth == 0 ? FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULTTEXTWIDTH) : mBBoxWidth;
    }

}
