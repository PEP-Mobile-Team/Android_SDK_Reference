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
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.FreeText;
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


public class TypewriterAnnotHandler implements AnnotHandler {

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
    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private EditText mEditView;
    private FtTextUtil mTextUtil;
    private float mBBoxWidth;
    private float mBBoxHeight;

    private int mTempLastColor;
    private int mTempLastOpacity;
    private int mTempLastFontId;
    private float mTempLastFontSize;
    private RectF mTempLastBBox;
    private String mTempLastContent;
    private boolean mEditState;
    private PointF mEditPoint = new PointF(0, 0);
    private RectF mDocViewBBox = new RectF();
    private boolean mIsSelcetEndText = false;

    private PDFViewCtrl mPdfViewCtrl;
    private float deltaWidth = 0f;
    private float deltaHeight = 0f;

    TypewriterAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);
        mPaintOut.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());
        mPaintOut.setStrokeWidth(AppAnnotUtil.getInstance(context).getAnnotBBoxStrokeWidth());

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
        return Annot.e_FreeText;
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
        RectF bbox = getAnnotBBox(annot);
        if (bbox == null) return false;
        try {
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, annot.getPage().getIndex());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return bbox.contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        deltaWidth = 0;
        deltaHeight = 0;
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

            int pageIndex = annot.getPage().getIndex();

            RectF mPageViewRect = AppUtil.toRectF(annot.getRect());
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            prepareAnnotMenu(annot);
            mAnnotMenu.show(menuRect);
            preparePropertyBar(freeText);
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mTextUtil.setOnWidthChanged(new FtTextUtil.OnTextValuesChangedListener() {

            @Override
            public void onMaxWidthChanged(float maxWidth) {
                if (mBBoxWidth != maxWidth) {
                    mBBoxWidth = maxWidth;
                    try {
                        RectF textRect = AppUtil.toRectF(annot.getRect());
                        int pageIndex = annot.getPage().getIndex();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);
                        if (mPdfViewCtrl.isPageVisible(pageIndex) && mBBoxWidth > textRect.width()) {
                            textRect.set(textRect.left, textRect.top, textRect.left + mBBoxWidth, textRect.bottom);
                            RectF rectChanged = new RectF(textRect);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                            annot.move(AppUtil.toFxRectF(textRect));
                            annot.resetAppearanceStream();
                            rectChanged.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectChanged, rectChanged, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectChanged));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onMaxHeightChanged(float maxHeight) {
                if (mBBoxHeight != maxHeight) {
                    mBBoxHeight = maxHeight;
                    try {
                        RectF textRect = AppUtil.toRectF(annot.getRect());
                        int pageIndex = annot.getPage().getIndex();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);
                        if (mPdfViewCtrl.isPageVisible(pageIndex) && mBBoxHeight > textRect.height()) {
                            textRect.set(textRect.left, textRect.top, textRect.right, textRect.top + mBBoxHeight);
                            RectF rectChanged = new RectF(textRect);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                            annot.move(AppUtil.toFxRectF(textRect));
                            annot.resetAppearanceStream();
                            rectChanged.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectChanged, rectChanged, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectChanged));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }

                }

            }

            @Override
            public void onCurrentSelectIndex(int selectIndex) {
                System.err.println("selectindex  " + selectIndex);
                if (selectIndex >= mEditView.getText().length()) {
                    selectIndex = mEditView.getText().length();
                    mIsSelcetEndText = true;
                } else {
                    mIsSelcetEndText = false;
                }
                mEditView.setSelection(selectIndex);
            }

            @Override
            public void onEditPointChanged(float editPointX, float editPointY) {

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

        mEditView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    annot.setContent(String.valueOf(s));
                    annot.resetAppearanceStream();
                    RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                    int pageIndex = annot.getPage().getIndex();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    RectF pdfRectF = new RectF(pageViewRect.left, pageViewRect.top,
                            pageViewRect.left + mBBoxWidth, pageViewRect.top + mBBoxHeight);
                    RectF rect = new RectF(pdfRectF.left, pdfRectF.top,
                            pdfRectF.left + mBBoxWidth, pdfRectF.top + mBBoxHeight);
                    rect.inset(-AppDisplay.getInstance(mContext).dp2px(200), -AppDisplay.getInstance(mContext).dp2px(200));
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        // get annot bitmap and update page view
        try {
            RectF viewRect = AppUtil.toRectF(annot.getRect());
            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
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
        mAnnotMenu.dismiss();
        if (mEditingProperty) {
            mEditingProperty = false;
            mPropertyBar.dismiss();
        }

        try {
            PDFPage page = annot.getPage();
            if (page != null && !page.isEmpty()) {
                final int pageIndex = page.getIndex();
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();

                if (mEditView !=null && TextUtils.isEmpty(mEditView.getText().toString())){
                    deleteAnnot(annot, true, true, null);
                } else {
                    RectF annotRectF;
                    String annotContent;
                    if (mEditView != null && !mEditView.getText().toString().equals(mTempLastContent)) {
                        mModifyed = true;

                        RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                        RectF pdfRectF = new RectF(pageViewRect.left, pageViewRect.top, pageViewRect.left + mBBoxWidth,
                                pageViewRect.top + mBBoxHeight);

                        RectF rectF = new RectF(pdfRectF); // page view rect
                        mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);

                        String content = mEditView.getText().toString();
                        int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                        float fontSize = da.getText_size();

                        ArrayList<String> composeText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, rectF, content,
                                mTextUtil.getSupportFontName(fontId), fontSize, ((FreeText) annot).getRotation(), mEditState);
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < composeText.size(); i ++) {
                            sb.append(composeText.get(i));
                            if (i != composeText.size() - 1 && sb.charAt(sb.length() - 1) != '\n'
                                    && sb.charAt(sb.length() - 1) != '\r') {
                                sb.append("\r");
                            }
                        }

                        annotRectF = pdfRectF;
                        annotContent = sb.toString();
                    } else {
                        annotRectF = AppUtil.toRectF(annot.getRect());
                        annotContent = annot.getContent();
                    }

                    if (needInvalid) {
                        if (mModifyed){
                            boolean isModifyJni = mTempLastColor != da.getText_color()
                                    || mTempLastOpacity != (int) (((FreeText) annot).getOpacity() * 255f)
                                    || !mTempLastBBox.equals(annotRectF)
                                    || !mTempLastContent.equals(annotContent)
                                    || mTempLastFontSize != da.getText_size()
                                    || mTempLastFontId != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                            modifyAnnot(pageIndex, annot, annotRectF, da.getText_color(),
                                    (int) (((FreeText) annot).getOpacity() * 255f + 0.5f), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()),
                                    da.getText_size(), annotContent, isModifyJni);
                        } else {
                            da.setText_color(mTempLastColor);
                            ((FreeText) annot).setOpacity(mTempLastOpacity / 255f);
                            annot.move(AppUtil.toFxRectF(mTempLastBBox));
                            da.setText_size(mTempLastFontSize);
                            Font font = mTextUtil.getSupportFont(mTempLastFontId);
                            da.setFont(font);
                            ((FreeText) annot).setDefaultAppearance(da);
                            annot.setContent(mTempLastContent);
                            annot.resetAppearanceStream();
                        }
                    }
                }

                if (mPdfViewCtrl.isPageVisible(pageIndex) && needInvalid) {
                    final RectF viewRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
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
                                if (mPdfViewCtrl.isPageVisible(pageIndex) &&
                                        (pageIndex == mPdfViewCtrl.getPageCount() - 1
                                        || (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) &&
                                        pageIndex == mPdfViewCtrl.getCurrentPage()) {
                                    PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                                    mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, pageIndex);
                                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                        mTextUtil.setKeyboardOffset(0);
                                        PointF startPoint = new PointF(viewRect.left, viewRect.top);
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
        int[] colors = new int[PropertyBar.PB_COLORS_TYPEWRITER.length];
        System.arraycopy(PropertyBar.PB_COLORS_TYPEWRITER, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TYPEWRITER[0];
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
                        deleteAnnot(annot, true, false, null);
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
        TypewriterAnnotContent lContent = (TypewriterAnnotContent) content;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final FreeText annot = (FreeText) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(content.getBBox())), Annot.e_FreeText);
            final TypewriterAddUndoItem undoItem = new TypewriterAddUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(lContent);
            undoItem.mPageIndex = pageIndex;
            undoItem.mFont = mTextUtil.getSupportFont(mTextUtil.getSupportFontID(lContent.getFontName()));
            undoItem.mFontSize = lContent.getFontSize();
            undoItem.mTextColor = lContent.getColor();
            undoItem.mDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_FlagPrint;
            undoItem.mIntent = "FreeTextTypewriter";

            TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
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
                                rect.inset(-10, -10);
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
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        deleteAnnot(annot, addUndo, false, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
        PointF devPoint = new PointF(e.getX(), e.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPoint, point, pageIndex);

        float evX = point.x;
        float evY = point.y;
        int action = e.getAction();
        try {

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()
                            && isHitAnnot(annot, point) && !mEditState) {
                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);
                        mTouchCaptured = true;
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured && pageIndex == annot.getPage().getIndex()
                            && annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && !mEditState) {

                        if (evX != mLastPoint.x || evY != mLastPoint.y) {
                            RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                            pageViewRect.set(pageViewRect.left - mOffset, pageViewRect.top, pageViewRect.left + mBBoxWidth + mOffset, pageViewRect.top + mBBoxHeight);

                            RectF rectInv = new RectF(pageViewRect);
                            RectF rectChanged = new RectF(pageViewRect);

                            rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                            rectChanged.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                            float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, annot.getPage().getIndex(), 2);
                            float adjustx = 0;
                            float adjusty = 0;
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
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (mTouchCaptured && annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);

                        RectF rectChanged = new RectF(pageViewRect);
                        rectChanged.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                        RectF rectInViewerF = new RectF(rectChanged);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                        if (mEditingProperty) {
                        } else {
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(rectInViewerF);
                            } else {
                                mAnnotMenu.show(rectInViewerF);
                            }
                        }
                        mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);
                        DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                        if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF _rect = new RectF(rectChanged);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(_rect, _rect, pageIndex);
                            _rect.right += deltaWidth;
                            _rect.bottom -= deltaHeight;
                            int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                            float fontSize = da.getText_size();
                            ArrayList<String> composeText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, _rect, annot.getContent(), mTextUtil.getSupportFontName(fontId), fontSize);

                            StringBuffer sb = new StringBuffer();
                            for (int i = 0; i < composeText.size(); i++) {
                                sb.append(composeText.get(i));
                                char ch = sb.charAt(sb.length() - 1);
                                if (i != composeText.size() - 1 && ch != '\n' && ch != '\r') {
                                    sb.append("\r");
                                }
                            }
                            String annotContent = sb.toString();
                            mEditView.setText(annotContent);
                            modifyAnnot(pageIndex, annot, rectChanged,
                                    (int) da.getText_color(),
                                    (int) (((FreeText) (annot)).getOpacity() * 255f), fontId, fontSize,
                                    annotContent, false);
                        }

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        return true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mEditPoint.set(0, 0);
                    return false;
                default:
                    break;
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
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
                    RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(pageViewRect));
                    AppUtil.showSoftInput(mEditView);
                    return true;
                } else if (pageIndex == annot.getPage().getIndex()
                        && !isHitAnnot(annot, point)
                        && mEditView != null && !mEditView.getText().toString().equals(annot.getContent())) {
                    RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    RectF pdfRectF = new RectF(pageViewRect.left, pageViewRect.top,
                            pageViewRect.left + mBBoxWidth, pageViewRect.top + mBBoxHeight);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);
                    annot.move(new com.foxit.sdk.common.fxcrt.RectF(pdfRectF.left, pdfRectF.bottom, pdfRectF.right, pdfRectF.top));
                    DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();

                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), (int) da.getText_color(),
                            (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), mEditView.getText()
                                    .toString(), false);
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

            if (AppAnnotUtil.equals(mBitmapAnnot, annot) && annot.getPage().getIndex() == pageIndex) {
                canvas.save();
                Rect _rect = new Rect(0, 0, 10, 0);
                RectF rectF = new RectF(_rect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                mOffset = rectF.width();

                RectF frameRectF = new RectF();
                RectF rect = AppUtil.toRectF(annot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
                rect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                PointF editPoint = new PointF(mEditPoint.x, mEditPoint.y);
                if (editPoint.x != 0 || editPoint.y != 0) {
                    mPdfViewCtrl.convertPdfPtToPageViewPt(editPoint, editPoint, pageIndex);
                }
                mTextUtil.setTextString(pageIndex, annot.getContent(), mEditState);
                mTextUtil.setStartPoint(new PointF(rect.left, rect.top));
                mTextUtil.setEditPoint(editPoint);
                mTextUtil.setMaxRect(rect.width() + deltaWidth + mOffset, mPdfViewCtrl.getPageViewHeight(pageIndex) - rect.top);

                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                int opacity = (int) (((FreeText) annot).getOpacity() * 100);
                mTextUtil.setTextColor(da.getText_color(), AppDmUtil.opacity100To255(opacity));
                mTextUtil.setFont(mTextUtil.getSupportFontName(mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())),da.getText_size());
                if (mIsSelcetEndText) {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd() + 1);
                } else {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd());
                }
                mTextUtil.loadText(true);
                mTextUtil.drawText(canvas);
                if (!mEditState) {
                    frameRectF.set(rect.left - mOffset, rect.top, rect.left + mBBoxWidth + mOffset, rect.top + mBBoxHeight);
                    mPaintOut.setColor(da.getText_color() | 0xFF000000);
                    canvas.drawRect(frameRectF, mPaintOut);

                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot != null && ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this && !mEditState) {
            try {
                mDocViewBBox = AppUtil.toRectF(curAnnot.getRect());
                int pageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewBBox, mDocViewBBox, pageIndex);
                    mDocViewBBox.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewBBox, mDocViewBBox, pageIndex);
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
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && color != (int) da.getText_color()) {
                    int pageIndex = annot.getPage().getIndex();
                    modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), color, (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(), false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void onOpacityValueChanged(int opacity) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null && ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                    && AppDmUtil.opacity100To255(opacity) != (int) (((FreeText) annot).getOpacity() * 255f)) {
                int pageIndex = annot.getPage().getIndex();
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                modifyAnnot(pageIndex, annot, AppUtil.toRectF(annot.getRect()), (int) da.getText_color(),
                        AppDmUtil.opacity100To255(opacity),
                        mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), da.getText_size(), annot.getContent(), false);
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
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && fontId != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                    int pageIndex = annot.getPage().getIndex();
                    RectF rectF = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    float fontWidth = mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, fontName, da.getText_size());
                    if (rectF.width() < fontWidth) {
                        rectF.set(rectF.left, rectF.top, rectF.left + fontWidth, rectF.bottom);
                    }
                    RectF rectChanged = new RectF(rectF);
                    rectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);
                    modifyAnnot(pageIndex, annot, rectChanged, (int) da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f),
                            fontId, da.getText_size(), annot.getContent(), false);
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
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this
                        && fontSize != da.getText_size()) {
                    int pageIndex = annot.getPage().getIndex();
                    RectF rectF = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                    float fontWidth = mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(fontId), da.getText_size());
                    if (rectF.width() < fontWidth) {
                        rectF.set(rectF.left, rectF.top, rectF.left + fontWidth, rectF.bottom);
                    }
                    RectF rectChanged = new RectF(rectF);
                    rectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);
                    modifyAnnot(pageIndex, annot, rectChanged, (int) da.getText_color(), (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), fontSize, annot.getContent(), false);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private void modifyAnnot(int pageIndex, Annot annot, RectF bbox,
                             int color, int opacity, int fontId, float fontSize, String content, boolean isModifyJni) {
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

        modifyAnnot(pageIndex, (FreeText) annot, bbox, color, opacity, fontId, fontSize, content, isModifyJni, true, "FreeTextTypewriter", null);
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, boolean isInEditMode, final Event.Callback result) {
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();

            final TypewriterDeleteUndoItem undoItem = new TypewriterDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            if (isInEditMode) {
                DefaultAppearance da = ((FreeText)annot).getDefaultAppearance();
                undoItem.mFont = mTextUtil.getSupportFont(mTempLastFontId);
                undoItem.mContents = mTempLastContent;
                undoItem.mFontSize = mTempLastFontSize;
                undoItem.mTextColor = mTempLastColor;
                undoItem.mColor = mTempLastColor;
                undoItem.mOpacity = mTempLastOpacity / 255f;
                undoItem.mBBox = mTempLastBBox;

                undoItem.mDaFlags = da.getFlags();
                undoItem.mRotation = ((FreeText) annot).getRotation();
            } else {
                if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
                    AppUtil.dismissInputSoft(mEditView);
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                }

                DefaultAppearance da = ((FreeText)annot).getDefaultAppearance();
                int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
                undoItem.mFont = mTextUtil.getSupportFont(fontId);
                undoItem.mContents = annot.getContent();
                undoItem.mFontSize = da.getText_size();
                undoItem.mTextColor = da.getText_color();
                undoItem.mDaFlags = da.getFlags();
                undoItem.mRotation = ((FreeText) annot).getRotation();
            }

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FreeText) annot, mPdfViewCtrl);
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }

            final RectF viewRect = AppUtil.toRectF(annot.getRect());
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

    protected void deleteAnnot(final Annot annot, final TypewriterDeleteUndoItem undoItem, final Event.Callback result) {
        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FreeText) annot, mPdfViewCtrl);
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
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
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

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }
        modifyAnnot(annot, (TypewriterAnnotContent) content, addUndo, result);
    }

    private void modifyAnnot(Annot annot, TypewriterAnnotContent content, boolean addUndo, Event.Callback result) {
        FreeText lAnnot = (FreeText) annot;

        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

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
            if (content.getFontSize() == 0) {
                fontSize = 24;
            } else {
                fontSize = content.getFontSize();
            }
            DefaultAppearance da = lAnnot.getDefaultAppearance();
            modifyAnnot(pageIndex, lAnnot, AppUtil.toRectF(annot.getRect()), (int) da.getText_color(), (int) (lAnnot.getOpacity() * 255f), fontId, fontSize, contents, true, addUndo, "", result);
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    protected void modifyAnnot(final int pageIndex, final Annot annot, final RectF bbox,
                               final int color, final int opacity, final int fontId, final float fontSize, String content,
                               boolean isModifyJni, final boolean addUndo, final String fromType, final Event.Callback result) {

        final TypewriterModifyUndoItem undoItem = new TypewriterModifyUndoItem(mPdfViewCtrl);
        undoItem.setCurrentValue(annot);
        undoItem.mPageIndex = pageIndex;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mColor = color;
        undoItem.mOpacity = opacity / 255f;
        undoItem.mBBox = new RectF(bbox);
        if (content == null) {
            content = "";
        }
        undoItem.mContents = content;
        undoItem.mFont = mTextUtil.getSupportFont(fontId);
        undoItem.mFontSize = fontSize;
        undoItem.mTextColor = color;

        undoItem.mOldBBox = new RectF(mTempLastBBox);
        undoItem.mOldColor = mTempLastColor;
        undoItem.mOldOpacity = mTempLastOpacity / 255f;
        undoItem.mOldFont = mTextUtil.getSupportFont(mTempLastFontId);
        undoItem.mOldFontSize = mTempLastFontSize;
        undoItem.mOldTextColor = mTempLastColor;
        undoItem.mOldContents = mTempLastContent;

        try {
            final RectF tempRectF = AppUtil.toRectF(annot.getRect());
            if (isModifyJni) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(true);
                TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (addUndo) {
                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                            }
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            if (fromType.equals("")) {
                                mModifyed = true;
                            }

                            try {
                                RectF newViewRect = AppUtil.toRectF(annot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(newViewRect, newViewRect, pageIndex);

                                RectF oldViewRect = new RectF(bbox);
                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldViewRect, oldViewRect, pageIndex);

                                float tmp = oldViewRect.width() - newViewRect.width();
                                if (tmp >= 1.0f && deltaWidth < tmp + 10) {
                                    deltaWidth = tmp + 10;
                                }

                                tmp = newViewRect.height() - oldViewRect.height();
                                if (tmp >= 1.0f) {
                                    deltaHeight = tmp;
                                }

                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);

                                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                    RectF viewRect = AppUtil.toRectF(annot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                    viewRect.union(tempRectF);
                                    viewRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
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
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
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
                    ft_Annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                    ft_Annot.setContent(content);
                    ft_Annot.resetAppearanceStream();

                    RectF newViewRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(newViewRect, newViewRect, pageIndex);

                    RectF oldViewRect = new RectF(bbox);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(oldViewRect, oldViewRect, pageIndex);

                    float tmp = oldViewRect.width() - newViewRect.width();
                    if (tmp >= 1.0f && deltaWidth < tmp + 10) {
                        deltaWidth = tmp + 10;
                    }

                    tmp = newViewRect.height() - oldViewRect.height();
                    if (tmp >= 1.0f) {
                        deltaHeight = tmp;
                    }

                    RectF viewRect = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                    viewRect.union(tempRectF);
                    viewRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
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
