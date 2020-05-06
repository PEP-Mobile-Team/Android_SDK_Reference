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
package com.foxit.uiextensions.annots.freetext.textbox;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.EditText;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Font;
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

public class TextBoxAnnotHandler implements AnnotHandler {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private AnnotMenu mAnnotMenu;
    private PropertyBar mPropertyBar;

    private ArrayList<Integer> mMenuText;
    private boolean mEditingProperty;
    private boolean mModifyed;

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
    private int mTempLastFontID;

    private float mTempLastFontSize;
    private RectF mTempLastBBox;
    private String mTempLastContent;
    private RectF mTempLastTextBBox;
    private boolean mEditState;
    private PointF mEditPoint = new PointF(0, 0);
    private RectF mDownRect = new RectF();
    private RectF mDocViewBBox = new RectF();

    private int mCurrentCtr = FtUtil.CTR_NONE;
    private int mLastOper = FtUtil.OPER_DEFAULT;

    public TextBoxAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);

        mPaintCtr = new Paint();
        mPaintCtr.setAntiAlias(true);
        mPaintCtr.setStyle(Paint.Style.FILL_AND_STROKE);
        AppAnnotUtil annotUtil = new AppAnnotUtil(mContext);
        mPaintCtr.setStrokeWidth(annotUtil.getAnnotBBoxStrokeWidth());

        mMenuText = new ArrayList<Integer>();

        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mBitmapAnnot = null;
    }

    private void preparePropertyBar(FreeText annot) {
        mPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
        int[] colors = new int[PropertyBar.PB_COLORS_TEXTBOX.length];
        System.arraycopy(PropertyBar.PB_COLORS_TEXTBOX, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TEXTBOX[0];
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
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        deleteAnnot(annot, true, null);
                    }
                } else if (btType == AnnotMenu.AM_BT_EDIT) {
                    mAnnotMenu.dismiss();
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().addView(mEditView);
                    mTextUtil.getBlink().postDelayed((Runnable) mTextUtil.getBlink(), 500);
                    mEditView.setSelection(mEditView.getText().length());
                    AppUtil.showSoftInput(mEditView);
                    mEditState = true;
                    try {
                        int pageIndex = annot.getPage().getIndex();
                        RectF rectF = AppUtil.toRectF(annot.getRect());
                        final RectF viewRect = new RectF(rectF.left, rectF.top, rectF.right, rectF.bottom);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(viewRect));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                } else if (btType == AnnotMenu.AM_BT_STYLE) {
                    RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewBBox);
                    mPropertyBar.show(rectF, false);
                    mAnnotMenu.dismiss();
                } else if (btType == AnnotMenu.AM_BT_FLATTEN) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                }
            }
        });
    }

    private void resetAnnotationMenuResource(Annot annot) {
        mMenuText.clear();
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
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

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            AppUtil.dismissInputSoft(mEditView);
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
            mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final TextBoxDeleteUndoItem undoItem = new TextBoxDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();

            int id = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            undoItem.mFont = mTextUtil.getSupportFont(id);
            undoItem.mPageIndex = pageIndex;
            undoItem.mColor = Color.RED;
            undoItem.mTextColor = da.getText_color();
            undoItem.mFontSize = da.getText_size();
            undoItem.mDaFlags = da.getFlags();
            undoItem.mIntent = ((FreeText) annot).getIntent();
            undoItem.mOpacity = ((Markup) annot).getOpacity();
            undoItem.mContents = annot.getContent();
//            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mBBox = AppUtil.toRectF(annot.getRect());
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mTextRectF = AppUtil.toRectF(((FreeText) annot).getInnerRect());
            undoItem.mRotation = ((FreeText) annot).getRotation();

            RectF annotRect = new RectF(undoItem.mTextRectF);
            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            TextBoxEvent event = new TextBoxEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FreeText) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
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

    protected void deleteAnnot(final Annot annot, final TextBoxDeleteUndoItem undoItem, final Event.Callback result) {
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            TextBoxEvent event = new TextBoxEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FreeText) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
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

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this && !mEditState) {

            try {
                mDocViewBBox = AppUtil.toRectF(((FreeText) curAnnot).getInnerRect());
                int pageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewBBox, mDocViewBBox, pageIndex);
                    mDocViewBBox.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                    Matrix matrix = new Matrix();
                    switch (mLastOper) {
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
                    float width = FtUtil.widthOnPageView(mPdfViewCtrl, curAnnot.getPage().getIndex(), 2);
                    mDocViewBBox.inset(-width * 0.5f, -width * 0.5f);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewBBox, mDocViewBBox, pageIndex);
                    mDocViewBBox.inset(-AppAnnotUtil.getAnnotBBoxSpace(), -AppAnnotUtil.getAnnotBBoxSpace());
                    mAnnotMenu.update(mDocViewBBox);
                    if (mPropertyBar.isShowing()) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewBBox);
                        mPropertyBar.update(rectF);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public int getType() {
        return AnnotHandler.TYPE_FREETEXT_TEXTBOX;
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

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        try {
            PointF pdfPoint = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, pdfPoint, annot.getPage().getIndex());

            RectF bbox = AppUtil.toRectF(((FreeText) annot).getInnerRect());
            RectF _bbox = normalize(bbox);
            if (_bbox.contains(pdfPoint.x, pdfPoint.y)) {
                if (mEditState) {
                    AppUtil.showSoftInput(mEditView);
                }
                return true;
            }

            RectF tempRect = normalize(getAnnotBBox(annot));
            if (tempRect.contains(pdfPoint.x, pdfPoint.y)) {
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        mEditView = new EditText(mContext);
        mEditView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
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
            mTempLastTextBBox = AppUtil.toRectF(freeText.getInnerRect());

            mTempLastFontID = mTextUtil.getSupportFontID(defaultAppearance, mPdfViewCtrl.getDoc());
            mTempLastFontSize = defaultAppearance.getText_size();
            mTempLastContent = annot.getContent();
            if (mTempLastContent == null) {
                mTempLastContent = "";
            }

            int pageIndex = annot.getPage().getIndex();
            RectF menuRect = new RectF(mTempLastTextBBox);
            mPdfViewCtrl.convertPdfRectToPageViewRect(menuRect, menuRect, pageIndex);
            float width = FtUtil.widthOnPageView(mPdfViewCtrl, annot.getPage().getIndex(), 2);
            menuRect.inset(-width * 0.5f, -width * 0.5f);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);

            menuRect.inset(-AppAnnotUtil.getAnnotBBoxSpace(), -AppAnnotUtil.getAnnotBBoxSpace());
            prepareAnnotMenu(annot);
            mAnnotMenu.show(menuRect);
            preparePropertyBar(freeText);

            mOffset = FtUtil.widthOnPageView(mPdfViewCtrl, annot.getPage().getIndex(), FtUtil.CTRLPTTOUCHEXT * 4);
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
                TextBoxAnnotHandler.this.onMaxHeightChanged(annot, maxHeight);
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
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
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
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
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
                    RectF annotRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);

                    RectF pdfRectF = new RectF(textRect.left, textRect.top, textRect.right, textRect.top + mBBoxHeight);
                    annotRect.union(pdfRectF);

//                    mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(annotRect, annotRect, pageIndex);

                    String content = mEditView.getText().toString();
                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                    float fontSize = da.getText_size();

                    modifyAnnot(pageIndex, annot, annotRect, annotRect, da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f),
                            fontId, fontSize, content, false);
                }
                if (needInvalid && mModifyed) {
                    if (mTempLastColor == da.getText_color()
                            && mTempLastOpacity == (int) (((FreeText) annot).getOpacity() * 255f)
                            && mTempLastBBox.equals(AppUtil.toRectF(annot.getRect()))
                            && mTempLastContent.equals(annot.getContent())
                            && mTempLastFontSize == da.getText_size()
                            && mTempLastTextBBox.equals(AppUtil.toRectF(((FreeText) annot).getInnerRect()))
                            && mTempLastFontID == mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                        modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()), (int) da.getText_color(),
                                (int) (((FreeText) annot).getOpacity() * 255f + 0.5f), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()),
                                da.getText_size(), annot.getContent(), false);
                    } else {
                        modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()), (int) da.getText_color(),
                                (int) (((FreeText) annot).getOpacity() * 255f + 0.5f), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()),
                                da.getText_size(), annot.getContent(), true);
                    }
                } else {
                    da.setText_color(mTempLastColor);
                    ((FreeText) annot).setOpacity(mTempLastOpacity / 255f);
                    annot.move(AppUtil.toFxRectF(mTempLastBBox));
                    da.setText_size(mTempLastFontSize);
                    Font font = mTextUtil.getSupportFont(mTempLastFontID);
                    da.setFont(font);
                    ((FreeText) annot).setDefaultAppearance(da);
                    annot.setContent(mTempLastContent);
                    ((FreeText) annot).setInnerRect(AppUtil.toFxRectF(mTempLastTextBBox));
                    annot.resetAppearanceStream();
                }

                if (mPdfViewCtrl.isPageVisible(pageIndex) && needInvalid) {
                    final RectF pdfRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pdfRect, pdfRect, pageIndex);
                    RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                    viewRect.inset(-200, -200);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    Task.CallBack callBack = new Task.CallBack() {
                        @Override
                        public void result(Task task) {
                            if (mBitmapAnnot != ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                                mBitmapAnnot = null;
                                AppUtil.dismissInputSoft(mEditView);
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                                mEditState = false;
                                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                                mBBoxWidth = 0;
                                mBBoxHeight = 0;
                                mEditPoint.set(0, 0);
                                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                if (mPdfViewCtrl.isPageVisible(pageIndex) &&
                                        (pageIndex == mPdfViewCtrl.getPageCount() - 1
                                        || (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) &&
                                        pageIndex == mPdfViewCtrl.getCurrentPage()) {
                                    PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                                    mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, pageIndex);
                                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                        mTextUtil.setKeyboardOffset(0);
                                        PointF startPoint = new PointF(pdfRect.left, pdfRect.top);
                                        mPdfViewCtrl.gotoPage(pageIndex,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, pageIndex, startPoint.x, startPoint.y).x,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, pageIndex, startPoint.x, startPoint.y).y);
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
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
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
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
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

    @Override
    public void addAnnot(final int pageIndex, AnnotContent content, final boolean addUndo, final Event.Callback result) {
    }

    private void modifyAnnot(int pageIndex, Annot annot, RectF bbox, RectF textbox,
                             int color, int opacity, int fontId, float fontSize, String content, boolean isModifyJni) {
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

        modifyAnnot(pageIndex, (FreeText) annot, bbox, textbox, color, opacity, fontId, fontSize, content, isModifyJni, true, "TextBox", null);
    }

    protected void modifyAnnot(final int pageIndex, final Annot annot, RectF bbox, RectF textbox,
                               int color, int opacity, int fontId, final float fontSize, String content,
                               boolean isModifyJni, final boolean addUndo, final String fromType, final Event.Callback result) {

        final TextBoxModifyUndoItem undoItem = new TextBoxModifyUndoItem(mPdfViewCtrl);
        undoItem.setCurrentValue(annot);
        undoItem.mPageIndex = pageIndex;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mColor = Color.RED;
        undoItem.mOpacity = opacity / 255f;
        undoItem.mBBox = new RectF(bbox.left, bbox.top, bbox.right, bbox.bottom);
        undoItem.mTextRectF = new RectF(textbox.left, textbox.top, textbox.right, textbox.bottom);
        undoItem.mContents = (content == null) ? "" : content;
        undoItem.mFont = mTextUtil.getSupportFont(fontId);
        undoItem.mFontSize = fontSize;
        undoItem.mTextColor = color;

        RectF annotRect = new RectF(undoItem.mTextRectF);
        mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);

        undoItem.mOldBBox = new RectF(mTempLastBBox);
        undoItem.mOldColor = mTempLastColor;
        undoItem.mOldOpacity = mTempLastOpacity / 255f;

        undoItem.mOldFont = mTextUtil.getSupportFont(mTempLastFontID);
        undoItem.mOldFontSize = mTempLastFontSize;
        undoItem.mOldTextColor = mTempLastColor;
        undoItem.mOldContents = mTempLastContent;
        undoItem.mOldTextRectF = mTempLastTextBBox;

        try {
            final RectF tempRectF = AppUtil.toRectF(annot.getRect());
            if (isModifyJni) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(true);
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                TextBoxEvent event = new TextBoxEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);

                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (addUndo) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                            }

                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            if (fromType.equals("")) {
                                mModifyed = true;
                            }

                            try {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);

                                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
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
                mModifyed = true;
                if (isModifyJni) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
                }

                if (!isModifyJni) {
                    FreeText ft_Annot = (FreeText) annot;
                    DefaultAppearance da = ft_Annot.getDefaultAppearance();
                    da.setText_color(color);
                    Font font = mTextUtil.getSupportFont(fontId);
                    da.setFont(font);
                    da.setText_size(fontSize);
                    ft_Annot.setDefaultAppearance(da);
                    ft_Annot.setOpacity(opacity / 255f);
                    ft_Annot.move(AppUtil.toFxRectF(bbox));
                    ft_Annot.setInnerRect(AppUtil.toFxRectF(textbox));
                    ft_Annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                    ft_Annot.setContent(content);
                    ft_Annot.resetAppearanceStream();

                    RectF viewRect = AppUtil.toRectF(annot.getRect());
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                        viewRect.union(tempRectF);
                        viewRect.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                        viewRect.inset(-40, -40);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }
        modifyAnnot(annot, (TextBoxAnnotContent) content, addUndo, result);
    }

    private void modifyAnnot(Annot annot, TextBoxAnnotContent content, boolean isAddUndo, Event.Callback result) {
        FreeText lAnnot = (FreeText) annot;

        PDFPage page = null;
        try {
            page = annot.getPage();
            int pageIndex = page.getIndex();

            String contents = null;
            int fontId;
            float fontSize = 0;
            if (content.getContents() == null || content.getContents().equals("")) {
                contents = " ";
            } else {
                contents = content.getContents();
            }
            contents = FtTextUtil.filterEmoji(contents);
            fontId = mTextUtil.getSupportFontID(content.getFontName());
            fontSize = content.getFontSize();
            if (fontSize == 0) {
                fontSize = 24;
            }
            DefaultAppearance da = lAnnot.getDefaultAppearance();
            modifyAnnot(pageIndex, lAnnot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()), (int) da.getText_color(), (int) (lAnnot.getOpacity() * 255f), fontId, fontSize, contents, true, isAddUndo, "", result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
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
                        mCurrentCtr = FtUtil.getTouchControlPoint(mPdfViewCtrl, annot.getPage().getIndex(), annot, evX, evY);
                        mDownRect = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mDownRect, mDownRect, pageIndex);
                        RectF tempRect = normalize(AppUtil.toRectF(((FreeText) annot).getInnerRect()));
                        if (mCurrentCtr >= 0 && mCurrentCtr <= 7) {
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
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && !mEditState) {

                        if (evX != mLastPoint.x || evY != mLastPoint.y) {
                            RectF AllBBox = AppUtil.toRectF(annot.getRect());
                            RectF textBBox = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(AllBBox, AllBBox, pageIndex);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(textBBox, textBBox, pageIndex);


                            switch (mLastOper) {
                                case FtUtil.OPER_TRANSLATE: {
                                    RectF rectInv = new RectF(AllBBox);
                                    RectF rectChanged = new RectF(textBBox);

                                    rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    rectChanged.offset(evX - mDownPoint.x, evY - mDownPoint.y);

                                    float adjustx = 0;
                                    float adjusty = 0;
                                    float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, annot.getPage().getIndex(), 2);
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
                                    if (rectChanged.top < deltaXY && rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                        adjusty = -rectChanged.top + deltaXY;
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
                                case FtUtil.OPER_SCALE: {
                                    Matrix matrix = FtUtil.calculateScaleMatrix(mCurrentCtr, textBBox,
                                            mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    Matrix matrix2 = FtUtil.calculateScaleMatrix(mCurrentCtr, textBBox,
                                            evX - mDownPoint.x, evY - mDownPoint.y);
                                    RectF rectInv = new RectF(textBBox);
                                    RectF rectChanged = new RectF(textBBox);
                                    RectF rect2 = new RectF(textBBox);
                                    matrix2.mapRect(rect2);
                                    matrix.mapRect(rectInv);
                                    matrix.mapRect(rectChanged);

                                    float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, 8);
                                    PointF adjustXY = FtUtil.adjustScalePointF(mCurrentCtr, mPdfViewCtrl, pageIndex, rect2, deltaXY);

                                    rectInv.union(rectChanged);
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
                            }
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (mTouchCaptured
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF allBBox = AppUtil.toRectF(annot.getRect());
                        RectF textBBox = AppUtil.toRectF(((FreeText) annot).getInnerRect());

                        mPdfViewCtrl.convertPdfRectToPageViewRect(allBBox, allBBox, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(textBBox, textBBox, pageIndex);

                        switch (mLastOper) {
                            case FtUtil.OPER_TRANSLATE: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    RectF textRect = new RectF(textBBox);
                                    textRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                                    RectF annotRect = new RectF(allBBox);
                                    annotRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

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
                                    mPdfViewCtrl.convertPageViewRectToPdfRect(annotRect, annotRect, pageIndex);

                                    if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                        DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                                        int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                                        float fontSize = da.getText_size();

                                        modifyAnnot(pageIndex, annot, annotRect, textRect, (int) da.getText_color(),
                                                (int) (((FreeText) (annot)).getOpacity() * 255f), fontId,
                                                fontSize, annot.getContent(), false);
                                    }
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

                                    DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                                    float fontSize = da.getText_size();

                                    modifyAnnot(pageIndex, annot, rectBBox, textRect, (int) da.getText_color(),
                                            (int) (((FreeText) (annot)).getOpacity() * 255f), fontId,
                                            fontSize, annot.getContent(), false);
                                }
                                break;
                            }
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
        } catch (PDFException e2) {
            e2.printStackTrace();
        }
        return false;
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
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        return AppAnnotUtil.isSameAnnot(curAnnot, annot) ? false : true;
    }

    private boolean onSingleTapOrLongPress(int pageIndex, PointF point, Annot annot) {
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            try {
                if (pageIndex == annot.getPage().getIndex()
                        && isHitAnnot(annot, point) && mEditState) {
                    PointF pointF = new PointF(point.x, point.y);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointF, pageIndex);
                    mEditPoint.set(pointF.x, pointF.y);
                    mTextUtil.resetEditState();
                    RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(pageViewRect));
                    return true;
                } else if (pageIndex == annot.getPage().getIndex()
                        && !isHitAnnot(annot, point)
                        && mEditView != null && !mEditView.getText().toString().equals(annot.getContent())) {
                    RectF textRect = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);

                    RectF annotRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);

                    RectF pdfRectF = new RectF(textRect.left, textRect.top, textRect.right, textRect.top + mBBoxHeight);
                    annotRect.union(pdfRectF);

                    mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(annotRect, annotRect, pageIndex);

                    DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                    modifyAnnot(pageIndex, annot, annotRect, pdfRectF, (int) da.getText_color(),
                            (int) (((FreeText) annot).getOpacity() * 255f), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()),
                            da.getText_size(), mEditView.getText().toString(), false);
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    return true;
                } else {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    return true;
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
            return true;
        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || !(annot instanceof FreeText)) {
            return;
        }
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this)
            return;

        try {
            if (AppAnnotUtil.equals(mBitmapAnnot, annot) && annot.getPage().getIndex() == pageIndex) {
                canvas.save();
                RectF textbbox = new RectF(AppUtil.toRectF(((FreeText) annot).getInnerRect()));
                mPdfViewCtrl.convertPdfRectToPageViewRect(textbbox, textbbox, pageIndex);

                Matrix matrix = new Matrix();
                switch (mLastOper) {
                    case FtUtil.OPER_SCALE:
                        matrix = FtUtil.calculateScaleMatrix(mCurrentCtr, textbbox, mLastPoint.x - mDownPoint.x, mLastPoint.y
                                - mDownPoint.y);
                        matrix.mapRect(textbbox);
                        break;
                    case FtUtil.OPER_TRANSLATE:
                        matrix.preTranslate(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        matrix.mapRect(textbbox);
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

                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                float fontSize = da.getText_size();
                mTextUtil.setMaxRect(textbbox.width() - fontSize / 5, textbbox.height());
                int opacity = (int) (((FreeText) annot).getOpacity() * 100);
                mTextUtil.setTextColor((int) da.getText_color(), AppDmUtil.opacity100To255(opacity));
                int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                mTextUtil.setFont(mTextUtil.getSupportFontName(fontId), da.getText_size());
                if (mIsSelcetEndText) {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd() + 1);
                } else {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd());
                }
                mTextUtil.loadText(true);
                mTextUtil.drawText(canvas);

                mPaintOut.setColor(Color.RED);

                // draw frame
                RectF frameRect = new RectF();
                frameRect.set(textbbox);
                frameRect.inset(FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex,
                        FtUtil.DEFAULT_BORDER_WIDTH) / 2, FtUtil.widthOnPageView(mPdfViewCtrl, pageIndex, FtUtil.DEFAULT_BORDER_WIDTH) / 2);
                canvas.drawRect(frameRect, mPaintOut);

                if (!mEditState) {
                    mPaintCtr.setColor(Color.WHITE);
                    mPaintCtr.setStyle(Paint.Style.FILL);

                    // draw textbbox control points
                    float[] ctlPts = FtUtil.calculateTextControlPoints(frameRect);
                    float radius = AppDisplay.getInstance(mContext).dp2px(5);
                    for (int i = 0; i < ctlPts.length; i += 2) {
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                    }

                    mPaintCtr.setColor(Color.RED);
                    mPaintCtr.setStyle(Paint.Style.STROKE);
                    for (int i = 0; i < ctlPts.length; i += 2) {
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                        canvas.drawCircle(ctlPts[i], ctlPts[i + 1], radius, mPaintCtr);
                    }
                }

                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onColorValueChanged(int color) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null) {
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && color != (int) da.getText_color()) {
                    int pageIndex = annot.getPage().getIndex();
                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()), color, (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(), false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onOpacityValueChanged(int opacity) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null
                    && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                    && AppDmUtil.opacity100To255(opacity) != (int) (((FreeText) annot).getOpacity() * 255f)) {
                int pageIndex = annot.getPage().getIndex();
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), AppUtil.toRectF(((FreeText) annot).getInnerRect()), (int) da.getText_color(),
                        AppDmUtil.opacity100To255(opacity),
                        mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(), false);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onFontSizeValueChanged(float fontSize) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null) {
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && fontSize != da.getText_size()) {
                    int pageIndex = annot.getPage().getIndex();
                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                    float fontHeight = mTextUtil.getFontHeight(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(fontId), fontSize);
                    if (TextUtils.isEmpty(annot.getContent()) && mBBoxHeight != fontHeight) {
                        onMaxHeightChanged(annot, fontHeight);
                    }

                    RectF rectF = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    RectF textRectF = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(textRectF, textRectF, pageIndex);

                    float fontWidth = mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(fontId), fontSize);
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

                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), rectChanged, (int) da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), fontSize, annot.getContent(), false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onFontValueChanged(String fontName) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null) {
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                int fontId = mTextUtil.getSupportFontID(fontName);

                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && fontId != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                    int pageIndex = annot.getPage().getIndex();
                    float fontHeight = mTextUtil.getFontHeight(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(fontId), da.getText_size());
                    if (TextUtils.isEmpty(annot.getContent()) && mBBoxHeight != fontHeight) {
                        onMaxHeightChanged(annot, fontHeight);
                    }

                    RectF rectF = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    RectF textRectF = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(textRectF, textRectF, pageIndex);

                    float fontWidth = mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, fontName, da.getText_size());
                    if (textRectF.width() < fontWidth) {
                        textRectF.set(textRectF.left, textRectF.top, fontWidth, textRectF.bottom);
                    }
                    RectF rectChanged = new RectF(textRectF);
                    rectF.union(rectChanged);
                    rectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);

                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), rectChanged, (int) da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f),
                            fontId, da.getText_size(), annot.getContent(), false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void onMaxHeightChanged(Annot annot, float height) {
        if (mBBoxHeight != height) {
            mBBoxHeight = height;
            try {
                RectF textRect = AppUtil.toRectF(((FreeText) annot).getInnerRect());
                int pageIndex = annot.getPage().getIndex();
                mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);

                RectF annotRect = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, pageIndex);

                if (mPdfViewCtrl.isPageVisible(pageIndex) && mBBoxHeight > textRect.height()) {
                    textRect.set(textRect.left, textRect.top, textRect.right, textRect.top + mBBoxHeight);
                    annotRect.union(textRect);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                    RectF rectF = new RectF(annotRect);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectF, rectF, pageIndex);
                    annot.move(AppUtil.toFxRectF(rectF));
                    ((FreeText) annot).setInnerRect(AppUtil.toFxRectF(textRect));
                    annot.resetAppearanceStream();

                    RectF rectInv = new RectF(annotRect);
                    rectInv.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

}
