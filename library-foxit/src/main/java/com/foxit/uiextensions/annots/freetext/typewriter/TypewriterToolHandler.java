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
package com.foxit.uiextensions.annots.freetext.typewriter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
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
import java.util.ArrayList;

public class TypewriterToolHandler implements ToolHandler {

    private Context mContext;
    private int mColor;
    private int mOpacity;
    private int mFontId;
    private float mFontSize;
    private EditText mEditView;
    private boolean mCreating;
    private int mCreateIndex;
    private PointF mStartPoint = new PointF(0, 0);
    private PointF mStartPdfPoint = new PointF(0, 0);
    private PointF mEditPoint = new PointF(0, 0);
    public int mLastPageIndex = -1;
    private String mAnnotText;
    private FtTextUtil mTextUtil;
    private float mPageViewWidth;
    private float mPageViewHeigh;
    private float mBBoxWidth;
    private float mBBoxHeight;

    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private CreateAnnotResult mListener;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;

    private boolean mCreateAlive = true;
    private boolean mIsCreated;
    private boolean mIsSelcetEndText = false;
    private boolean mIsContinueEdit;
    private boolean mIsContinuousCreate = false;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    public interface CreateAnnotResult {
        public void callBack();
    }

    public TypewriterToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiExtensionsManager.getMainFrame().getPropertyBar();

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
                mStartPoint.set(0, 0);
                mEditPoint.set(0, 0);
                mLastPageIndex = -1;
                AppUtil.dismissInputSoft(mEditView);
                mUiExtensionsManager.getRootView().removeView(mEditView);
                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                if (mTextUtil != null) {
                    mTextUtil.setKeyboardOffset(0);
                }
                mEditView = null;
                mBBoxHeight = 0;
                mBBoxWidth = 0;
                mCreating = false;
                if (mTextUtil != null) {
                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                }
                mIsContinueEdit = false;
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
                mUiExtensionsManager.setCurrentToolHandler(TypewriterToolHandler.this);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_TYPEWRITER;
            }
        });
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_TYPEWRITER;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
        mCreateAlive = true;
        mIsCreated = false;
        AppKeyboardUtil.setKeyboardListener(mUiExtensionsManager.getRootView(),
                mUiExtensionsManager.getRootView(), new AppKeyboardUtil.IKeyboardListener() {
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

    private void resetPropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_TYPEWRITER.length];
        System.arraycopy(PropertyBar.PB_COLORS_TYPEWRITER, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TYPEWRITER[0];
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

    private void resetAnnotBar(){
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
                if (TypewriterToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
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
            mIsContinueEdit = false;
            if (!mIsCreated) {
                createFTAnnot();
            }
        }
        AppKeyboardUtil.removeKeyboardListener(mUiExtensionsManager.getRootView());
        mCreateAlive = true;
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removeProbarListener() {
        mPropertyChangeListener = null;
    }

    @Override
    public boolean onTouchEvent(final int pageIndex, MotionEvent e) {

        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        PointF pdfPoint = new PointF(point.x, point.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);

        float x = point.x;
        float y = point.y;

        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mUiExtensionsManager.getCurrentToolHandler() == this) {
                    if (!mCreating) {
                        mPageViewWidth = mPdfViewCtrl.getPageViewWidth(pageIndex);
                        mPageViewHeigh = mPdfViewCtrl.getPageViewHeight(pageIndex);
                        mStartPoint.set(x, y);
                        adjustStartPt(mPdfViewCtrl, pageIndex, mStartPoint);
                        mStartPdfPoint.set(mStartPoint.x, mStartPoint.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mStartPdfPoint, mStartPdfPoint, pageIndex);
                        if (mLastPageIndex == -1) {
                            mLastPageIndex = pageIndex;
                        }
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
                        public void beforeTextChanged(CharSequence s, int start, int count,
                                                      int after) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });

                    mTextUtil.setOnWidthChanged(new FtTextUtil.OnTextValuesChangedListener() {

                        @Override
                        public void onMaxWidthChanged(float maxWidth) {
                            if (mBBoxWidth != maxWidth)
                                mBBoxWidth = maxWidth;
                        }

                        @Override
                        public void onMaxHeightChanged(float maxHeight) {
                            if (mBBoxHeight != maxHeight)
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
                mStartPoint.set(0, 0);
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
        return mIsContinuousCreate;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        mIsContinuousCreate = continueAddAnnot;
    }

    private boolean onSingleTapOrLongPress(final int pageIndex, final PointF point) {
        float x = point.x;
        float y = point.y;
        if (mUiExtensionsManager.getCurrentToolHandler() == this && mEditView != null) {
            RectF rectF = new RectF(mStartPoint.x, mStartPoint.y,
                    mStartPoint.x + mBBoxWidth, mStartPoint.y + mBBoxHeight);
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
                if (!mIsContinuousCreate) {
                    mUiExtensionsManager.setCurrentToolHandler(null);
                }
                if (mCreateAlive) {
                    mCreateAlive = false;
                    if (mUiExtensionsManager.getCurrentToolHandler() == TypewriterToolHandler.this) {
                        createFTAnnot();
                    }

                    return true;
                } else {
                    mCreateAlive = true;
                }
                mIsContinueEdit = true;
                setCreateAnnotListener(new CreateAnnotResult() {
                    @Override
                    public void callBack() {
                        mStartPoint.set(point.x, point.y);
                        adjustStartPt(mPdfViewCtrl, pageIndex, mStartPoint);
                        mStartPdfPoint.set(mStartPoint.x, mStartPoint.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mStartPdfPoint, mStartPdfPoint, pageIndex);
                        if (mLastPageIndex == -1) {
                            mLastPageIndex = pageIndex;
                        }
                        mCreateIndex = pageIndex;
                        if (mEditView != null) {
                            AppUtil.showSoftInput(mEditView);
                        }
                    }
                });
                createFTAnnot();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        canvas.save();
        if (mLastPageIndex == pageIndex && mEditView != null) {
            PointF startPoint = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, pageIndex);
            PointF editPoint = new PointF(mEditPoint.x, mEditPoint.y);
            if (editPoint.x != 0 || editPoint.y != 0) {
                mPdfViewCtrl.convertPdfPtToPageViewPt(editPoint, editPoint, pageIndex);
            }
            mTextUtil.setTextString(pageIndex, mAnnotText, true);
            mTextUtil.setStartPoint(startPoint);
            mTextUtil.setEditPoint(editPoint);
            mTextUtil.setMaxRect(mPdfViewCtrl.getPageViewWidth(pageIndex) - startPoint.x, mPdfViewCtrl.getPageViewHeight(pageIndex) - startPoint.y);
            mTextUtil.setTextColor(mColor, AppDmUtil.opacity100To255(mOpacity));
            mTextUtil.setFont(mTextUtil.getSupportFontName(mFontId), mFontSize);
            if (mIsSelcetEndText) {
                mTextUtil.setEndSelection(mEditView.getSelectionEnd() + 1);
            } else {
                mTextUtil.setEndSelection(mEditView.getSelectionEnd());
            }
            mTextUtil.loadText();
            mTextUtil.drawText(canvas);
        }

        canvas.restore();
    }

    private void createFTAnnot() {
        if (mAnnotText != null && mAnnotText.length() > 0) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mCreateIndex);
            final RectF rect = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + mBBoxWidth, pdfPointF.y
                    + mBBoxHeight);
            final RectF pdfRectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + mBBoxWidth, pdfPointF.y
                    + mBBoxHeight);

            RectF _rect = new RectF(pdfRectF);// page view rect
            mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, mCreateIndex);

            String content = "";
            try {
                content = new String(mAnnotText.getBytes(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            ArrayList<String> composeText = mTextUtil.getComposedText(mPdfViewCtrl, mCreateIndex, _rect, content, mTextUtil.getSupportFontName(mFontId), mFontSize);

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < composeText.size(); i ++) {
                sb.append(composeText.get(i));
                if (i != composeText.size() - 1 && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append("\r");
                }
            }
            String annotContent = sb.toString();

            try {
                final PDFPage page = mPdfViewCtrl.getDoc().getPage(mCreateIndex);
                final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(pdfRectF)), Annot.e_FreeText);

                final TypewriterAddUndoItem undoItem = new TypewriterAddUndoItem(mPdfViewCtrl);
                undoItem.mNM = AppDmUtil.randomUUID(null);
                undoItem.mPageIndex = mCreateIndex;
                undoItem.mColor = mColor;
                undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
                undoItem.mContents = annotContent;
                undoItem.mFont = mTextUtil.getSupportFont(mFontId);
                undoItem.mFontSize = mFontSize;
                undoItem.mTextColor = mColor;
                undoItem.mDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
                undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
                undoItem.mBBox = new RectF(pdfRectF.left, pdfRectF.top, pdfRectF.right, pdfRectF.bottom);
                undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mFlags = Annot.e_FlagPrint;
                undoItem.mIntent = "FreeTextTypewriter";
                undoItem.mSubject = "Typewriter";
                int rotation = (page.getRotation() + mPdfViewCtrl.getViewRotation()) % 4;
                undoItem.mRotation = rotation == 0 ? rotation : 4 - rotation;

                TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (FreeText) annot, mPdfViewCtrl);
                mUiExtensionsManager.getDocumentManager().setHasModifyTask(true);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {

                        if (success) {
                            mUiExtensionsManager.getDocumentManager().onAnnotAdded(page, annot);
                            mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                            mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                            if (mPdfViewCtrl.isPageVisible(mCreateIndex)) {
                                Rect mRect = new Rect((int) rect.left, (int) rect.top, (int) rect.right,
                                        (int) rect.bottom);
                                mPdfViewCtrl.refresh(mCreateIndex, mRect);

                                if (mIsContinueEdit && mCreateAlive) {
                                    mEditView.setText("");
                                } else {
                                    AppUtil.dismissInputSoft(mEditView);
                                    mUiExtensionsManager.getRootView().removeView(mEditView);
                                    mEditView = null;
                                    mCreating = false;
                                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                                    mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                    if (mPdfViewCtrl.isPageVisible(mCreateIndex) &&
                                            (mCreateIndex == mPdfViewCtrl.getPageCount() - 1
                                            || (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) &&
                                            mCreateIndex == mPdfViewCtrl.getCurrentPage()) {
                                        PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(mCreateIndex), mPdfViewCtrl.getPageViewHeight(mCreateIndex));

                                        mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, mCreateIndex);

                                        if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                            mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                            mTextUtil.setKeyboardOffset(0);
                                            PointF startPoint = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);

                                            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);
                                            mPdfViewCtrl.gotoPage(mCreateIndex,
                                                    mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).x,
                                                    mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).y);
                                        }
                                    }
                                }
                                mAnnotText = "";
                                mStartPoint.set(0, 0);
                                mEditPoint.set(0, 0);
                                mLastPageIndex = -1;
                                if (mIsContinueEdit && mListener != null) {
                                    mListener.callBack();
                                }
                            }
                        } else {
                            mAnnotText = "";
                            mStartPoint.set(0, 0);
                            mEditPoint.set(0, 0);
                            mLastPageIndex = -1;
                            AppUtil.dismissInputSoft(mEditView);
                            mUiExtensionsManager.getRootView().removeView(mEditView);
                            mEditView = null;
                            mBBoxHeight = 0;
                            mBBoxWidth = 0;
                            mCreating = false;
                            mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);
            } catch (PDFException e) {
                if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                }
            }
        } else {
            if (mIsContinueEdit && mCreateAlive && mListener != null) {
                mLastPageIndex = -1;
                mListener.callBack();
            } else {
                AppUtil.dismissInputSoft(mEditView);
                mUiExtensionsManager.getRootView().removeView(mEditView);
                mEditView = null;
                mCreating = false;
                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
            }

            mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
            if (mPdfViewCtrl.isPageVisible(mCreateIndex) &&
                    (mCreateIndex == mPdfViewCtrl.getPageCount() - 1
                    || (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE))) {
                PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(mCreateIndex), mPdfViewCtrl.getPageViewHeight(mCreateIndex));
                mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, mCreateIndex);
                if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                    mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                    mTextUtil.setKeyboardOffset(0);
                    PointF startPoint = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);
                    mPdfViewCtrl.gotoPage(mCreateIndex,
                            mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).x,
                            mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).y);

                    float fontWidth = mBBoxWidth == 0 ? FtUtil.widthOnPageView(mPdfViewCtrl, mCreateIndex, 10) : mBBoxWidth;
                    float fontHeight = mBBoxHeight == 0 ? mTextUtil.getFontHeight(mPdfViewCtrl, mCreateIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) : mBBoxHeight;
                    final RectF rect = new RectF(startPoint.x, startPoint.y, startPoint.x + fontWidth, startPoint.y + fontHeight);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, mCreateIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
                }
            }
        }
    }

    protected void onColorValueChanged(int color) {
        mColor = color;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
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
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
        }
    }

    protected void onFontValueChanged(String fontName) {
        mFontId = mTextUtil.getSupportFontID(fontName);
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
        }
    }

    protected void onFontSizeValueChanged(float fontSize) {
        mFontSize = fontSize;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mStartPdfPoint.x, mStartPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            adjustStartPt(mPdfViewCtrl, mLastPageIndex, pdfPointF);
            PointF pdfPtChanged = new PointF(pdfPointF.x, pdfPointF.y);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPtChanged, pdfPtChanged, mLastPageIndex);
            mStartPdfPoint.x = pdfPtChanged.x;
            mStartPdfPoint.y = pdfPtChanged.y;
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y,
                    pdfPointF.x + mBBoxWidth, pdfPointF.y + mBBoxHeight);
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            mPdfViewCtrl.refresh(mLastPageIndex, rect);
        }
    }

    private void adjustStartPt(PDFViewCtrl pdfViewCtrl, int pageIndex, PointF point) {
        float fontWidth = mTextUtil.getFontWidth(pdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize);
        if (pdfViewCtrl.getPageViewWidth(pageIndex) - point.x < fontWidth) {
            point.x = mPageViewWidth - fontWidth;
        }
        float fontHeight = mTextUtil.getFontHeight(pdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize);
        if (pdfViewCtrl.getPageViewHeight(pageIndex) - point.y < fontHeight) {
            point.y = mPageViewHeigh - fontHeight;
        }
    }

    private void setCreateAnnotListener(CreateAnnotResult listener) {
        mListener = listener;
    }

}
