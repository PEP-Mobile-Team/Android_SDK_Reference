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
package com.foxit.uiextensions.annots.freetext.textbox;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

public class TextBoxToolHandler implements ToolHandler {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;
    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private FtTextUtil mTextUtil;

    private int mColor;
    private int mBorderColor;
    private int mOpacity;
    private int mFontId;
    private float mFontSize;
    private EditText mEditView;
    private boolean mCreating;
    private int mCreateIndex;
    private PointF mTextStartPt = new PointF();
    private PointF mTextStartPdfPt = new PointF();
    private PointF mDownPdfPt = new PointF();
    private PointF mEditPoint = new PointF();
    private String mAnnotText;
    private float mBBoxWidth;
    private float mBBoxHeight;
    private boolean mIsContinue;
    private boolean mIsSelcetEndText = false;

    private Paint mPaintOut;
    private boolean mCreateAlive = true;
    private boolean mIsCreated;

    private CreateAnnotResult mListener;

    public int mLastPageIndex = -1;
    private float mPageViewWidth;
    private float mPageViewHeigh;
    private int mTextBoxWidth;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    public interface CreateAnnotResult {
        public void callBack();
    }


    public TextBoxToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiExtensionsManager.getMainFrame().getPropertyBar();
        mBorderColor = PropertyBar.PB_COLORS_TEXTBOX[6];

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);
        mPaintOut.setColor(mBorderColor);

        mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);

        if (AppDisplay.getInstance(mContext).isPad()) {
            mTextBoxWidth = (int) mContext.getResources().getDimension(R.dimen.annot_textbox_width_pad);
        } else {
            mTextBoxWidth = (int) mContext.getResources().getDimension(R.dimen.annot_textbox_width_phone);
        }

        pdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {
            }

            @Override
            public void onDocOpened(PDFDoc pdfDoc, int i) {
            }

            @Override
            public void onDocWillClose(PDFDoc pdfDoc) {
            }

            @Override
            public void onDocClosed(PDFDoc pdfDoc, int i) {
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
            public void onDocWillSave(PDFDoc pdfDoc) {
            }

            @Override
            public void onDocSaved(PDFDoc pdfDoc, int i) {
            }
        });

        mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                mUiExtensionsManager.setCurrentToolHandler(TextBoxToolHandler.this);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_TEXTBOX;
            }
        });
    }


    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_TEXTBOX;
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

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    private void resetPropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_TEXTBOX.length];
        System.arraycopy(PropertyBar.PB_COLORS_TEXTBOX, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TEXTBOX[0];
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
                if (TextBoxToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
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

    private int getContinuousIcon(boolean isContinuous) {
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
                createTBAnnot();
            }
        }
        AppKeyboardUtil.removeKeyboardListener(mUiExtensionsManager.getRootView());
        mCreateAlive = true;
    }

    private void createTBAnnot() {
        PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
        mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mCreateIndex);
        final RectF rect = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mCreateIndex), pdfPointF.y
                + getBBoxHeight(mCreateIndex));
        final RectF pdfRectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mCreateIndex), pdfPointF.y
                + getBBoxHeight(mCreateIndex));
        final RectF textRectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mCreateIndex), pdfPointF.y
                + getBBoxHeight(mCreateIndex));

        PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
        mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);

        mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, mCreateIndex);
        mPdfViewCtrl.convertPageViewRectToPdfRect(textRectF, textRectF, mCreateIndex);
        String content = "";
        try {
            if (!TextUtils.isEmpty(mAnnotText))
                content = new String(mAnnotText.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        final TextBoxAddUndoItem undoItem = new TextBoxAddUndoItem(mPdfViewCtrl);
        undoItem.mNM = AppDmUtil.randomUUID(null);
        undoItem.mPageIndex = mCreateIndex;
        undoItem.mColor = mBorderColor;
        undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
        undoItem.mContents = content;
        undoItem.mFont = mTextUtil.getSupportFont(mFontId);
        undoItem.mFontSize = mFontSize;
        undoItem.mTextColor = mColor;
        undoItem.mDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
        undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
        undoItem.mBBox = new RectF(pdfRectF.left, pdfRectF.top, pdfRectF.right, pdfRectF.bottom);
        undoItem.mTextRectF = new RectF(textRectF.left, textRectF.top, textRectF.right, textRectF.bottom);
        undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mFlags = Annot.e_FlagPrint;
        undoItem.mIntent = null;
        undoItem.mSubject = "Textbox";

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mCreateIndex);
            int rotation = (page.getRotation() + mPdfViewCtrl.getViewRotation()) % 4;
            undoItem.mRotation = rotation == 0 ? rotation : 4 - rotation;
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(undoItem.mBBox)), Annot.e_FreeText);
            TextBoxEvent addEvent = new TextBoxEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (FreeText) annot, mPdfViewCtrl);
            mUiExtensionsManager.getDocumentManager().setHasModifyTask(true);

            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
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
                                        (mCreateIndex == mPdfViewCtrl.getPageCount() - 1
                                        || (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) &&
                                        mCreateIndex == mPdfViewCtrl.getCurrentPage()) {
                                    PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(mCreateIndex), mPdfViewCtrl.getPageViewHeight(mCreateIndex));
                                    mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, mCreateIndex);

                                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                        mTextUtil.setKeyboardOffset(0);
                                        PointF startPoint = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
                                        mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);

                                        mPdfViewCtrl.gotoPage(mCreateIndex,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).x,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, mCreateIndex, startPoint.x, startPoint.y).y);
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
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    protected void setBorderColor(int color, int opacity) {
        mBorderColor = color;
        mPaintOut.setColor(AppDmUtil.calColorByMultiply(mBorderColor, AppDmUtil.opacity100To255(opacity)));
    }

    protected void onColorValueChanged(int color) {
        mColor = color;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(rectF));
        }

        setProItemColor(color);
    }

    private void setProItemColor(int color) {
        if (mPropertyItem == null) return;
        mPropertyItem.setCentreCircleColor(color);
    }

    protected void onOpacityValueChanged(int opacity) {
        mOpacity = opacity;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(rectF));
        }
    }

    protected void onFontValueChanged(String fontName) {
        mFontId = mTextUtil.getSupportFontID(fontName);
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(rectF));
        }
    }

    protected void onFontSizeValueChanged(float fontSize) {
        mFontSize = fontSize;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            PointF pdfPointF = new PointF(mTextStartPdfPt.x, mTextStartPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(pdfPointF, pdfPointF, mLastPageIndex);
            RectF rectF = new RectF(pdfPointF.x, pdfPointF.y, pdfPointF.x + getBBoxWidth(mLastPageIndex), pdfPointF.y + getBBoxHeight(mLastPageIndex));
            mPdfViewCtrl.refresh(mLastPageIndex, AppDmUtil.rectFToRect(rectF));
        }
    }


    @Override
    public boolean onTouchEvent(final int pageIndex, MotionEvent motionEvent) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        PointF pdfPoint = new PointF(point.x, point.y);
        mPdfViewCtrl.convertPageViewPtToPdfPt(pdfPoint, pdfPoint, pageIndex);

        float x = point.x;
        float y = point.y;

        int action = motionEvent.getAction();
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

                        mPageViewWidth = mPdfViewCtrl.getPageViewWidth(pageIndex);
                        mPageViewHeigh = mPdfViewCtrl.getPageViewHeight(pageIndex);
                        mTextStartPt.set(x, y);
                        adjustStartPt(mPdfViewCtrl, pageIndex, mTextStartPt);
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
                    mEditView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
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
                            mBBoxWidth = FtUtil.widthOnPageView(mPdfViewCtrl, mLastPageIndex, mTextBoxWidth);
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

    private void adjustStartPt(PDFViewCtrl pdfViewCtrl, int pageIndex, PointF point) {
        float fontWidth = FtUtil.widthOnPageView(mPdfViewCtrl, mLastPageIndex, mTextBoxWidth);
        if (pdfViewCtrl.getPageViewWidth(pageIndex) - point.x < fontWidth) {
            point.x = mPageViewWidth - fontWidth;
        }

        float fontHeight = mTextUtil.getFontHeight(pdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize);
        if (pdfViewCtrl.getPageViewHeight(pageIndex) - point.y < fontHeight) {
            point.y = mPageViewHeigh - fontHeight;
        }
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
                    mTextStartPt.x + getBBoxWidth(mLastPageIndex), mTextStartPt.y + getBBoxHeight(pageIndex));
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
                    if (mUiExtensionsManager.getCurrentToolHandler() == this) {
                        createTBAnnot();
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

                        mTextStartPt.set(point.x, point.y);
                        adjustStartPt(mPdfViewCtrl, pageIndex, mTextStartPt);
                        mTextStartPdfPt.set(mTextStartPt.x, mTextStartPt.y);
                        mPdfViewCtrl.convertPageViewPtToPdfPt(mTextStartPdfPt, mTextStartPdfPt, pageIndex);
                        mCreateIndex = pageIndex;
                        mPdfViewCtrl.invalidate();
                        if (mEditView != null) {
                            AppUtil.showSoftInput(mEditView);
                        }
                    }
                });
                createTBAnnot();
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
            mTextUtil.setMaxRect(FtUtil.widthOnPageView(mPdfViewCtrl, mLastPageIndex, mTextBoxWidth) - mFontSize / 5, mPdfViewCtrl.getPageViewHeight(pageIndex) - textStartPoint.y);

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
            mPaintOut.setStrokeWidth(FtUtil.widthOnPageView(mPdfViewCtrl, mLastPageIndex,
                    FtUtil.DEFAULT_BORDER_WIDTH));

            PointF startPoint = new PointF(mDownPdfPt.x, mDownPdfPt.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(startPoint, startPoint, mCreateIndex);

            RectF frameRectF = new RectF();
            frameRectF.set(
                    textStartPoint.x,
                    textStartPoint.y,
                    textStartPoint.x + getBBoxWidth(pageIndex),
                    textStartPoint.y + getBBoxHeight(pageIndex));
            frameRectF.inset(FtUtil.widthOnPageView(mPdfViewCtrl, mLastPageIndex,
                    FtUtil.DEFAULT_BORDER_WIDTH) / 2, FtUtil.widthOnPageView(mPdfViewCtrl,
                    mLastPageIndex, FtUtil.DEFAULT_BORDER_WIDTH) / 2);
            canvas.drawRect(frameRectF, mPaintOut);
        }

        canvas.restore();
    }

    private void setCreateAnnotListener(CreateAnnotResult listener) {
        mListener = listener;
    }

    private float getBBoxHeight(int pageIndex) {
        return mBBoxHeight == 0 ? mTextUtil.getFontHeight(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(mFontId), mFontSize) : mBBoxHeight;
    }

    private float getBBoxWidth(int pageIndex){
        return mBBoxWidth == 0 ? FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, mTextBoxWidth) : mBBoxWidth;
    }

}
