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
package com.foxit.uiextensions.annots.redaction;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.fxcrt.RectFArray;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.freetext.FtTextUtil;
import com.foxit.uiextensions.config.uisettings.annotations.annots.RedactConfig;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.ColorView;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class RedactAnnotHandler implements AnnotHandler {
    private static final int DEFAULT_BORDER_COLOR = PropertyBar.PB_COLORS_REDACT[6] | 0xFF000000;

    public static final int CTR_NONE = -1;
    public static final int CTR_LT = 1;
    public static final int CTR_T = 2;
    public static final int CTR_RT = 3;
    public static final int CTR_R = 4;
    public static final int CTR_RB = 5;
    public static final int CTR_B = 6;
    public static final int CTR_LB = 7;
    public static final int CTR_L = 8;
    private int mCurrentCtr = CTR_NONE;

    public static final int OPER_DEFAULT = -1;
    public static final int OPER_SCALE_LT = 1;// old:start at 0
    public static final int OPER_SCALE_T = 2;
    public static final int OPER_SCALE_RT = 3;
    public static final int OPER_SCALE_R = 4;
    public static final int OPER_SCALE_RB = 5;
    public static final int OPER_SCALE_B = 6;
    public static final int OPER_SCALE_LB = 7;
    public static final int OPER_SCALE_L = 8;
    public static final int OPER_TRANSLATE = 9;
    private int mLastOper = OPER_DEFAULT;

    private float mCtlPtLineWidth = 2;
    private float mCtlPtRadius = 5;
    private float mCtlPtTouchExt = 20;
    private float mCtlPtDeltyXY = 20;// Additional refresh range

    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private UIExtensionsManager mUiExtensionsManager;
    private RedactToolHandler mToolHandler;
    private FtTextUtil mTextUtil;
    private Annot mLastAnnot;
    private ColorView mColorView;
    private RedactModule mRedactModule;

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_REDACT.length];
    private ArrayList<Integer> mMenuItems;
    private AnnotMenu mAnnotMenu;

    private Paint mPaintBbox;
    private Paint mPaintAnnot;
    private Paint mCtlPtPaint;
    private int mPaintBoxOutset;

    private boolean mIsAnnotModified;
    private int mDefaultFillColor;
    private int mDefaultTextSize;

    public RedactAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, RedactModule redactModule) {
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;
        this.mRedactModule = redactModule;
        mUiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();

        RedactConfig config = mUiExtensionsManager.getConfig().uiSettings.annotations.redaction;
        mDefaultFillColor = config.fillColor;
        mDefaultTextSize = config.textSize;

        mPropertyBar = new PropertyBarImpl(context, pdfViewCtrl);
        mMenuItems = new ArrayList<Integer>();
        mAnnotMenu = new AnnotMenuImpl(context, pdfViewCtrl);

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setColor(DEFAULT_BORDER_COLOR);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(AppAnnotUtil.getInstance(context).getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());

        mPaintAnnot = new Paint();
        mPaintAnnot.setColor(mDefaultFillColor | 0xFF000000);
        mPaintAnnot.setAntiAlias(true);
        mPaintAnnot.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        mCtlPtPaint = new Paint();
        mPaintBoxOutset = AppResource.getDimensionPixelSize(mContext, R.dimen.annot_highlight_paintbox_outset);

        mTextUtil = new FtTextUtil(context, pdfViewCtrl);
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    @Override
    public int getType() {
        return Annot.e_Redact;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        RectF rectF = null;
        try {
            rectF = AppUtil.toRectF(annot.getRect());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF rectF = null;
        try {
            rectF = AppUtil.toRectF(annot.getRect());
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, annot.getPage().getIndex());
        } catch (PDFException e) {
            return false;
        }
        return rectF.contains(point.x, point.y);
    }

    private int mTempLastBorderColor;
    private int mTempLastFillColor;
    private int mTempLastTextColor;
    private float mTempLastFontSize;
    private int mTempLastFontId;
    private String mTempLastOverlayText;
    private String mTempLastContents;
    private RectF mTempLastBBox = new RectF();

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            Redact redact = (Redact) annot;
            if (redact == null || redact.isEmpty())
                return;
            mTempLastBorderColor = redact.getBorderColor();
            mTempLastFillColor = redact.getApplyFillColor();
            mTempLastOverlayText = redact.getOverlayText();
            mTempLastBBox = AppUtil.toRectF(annot.getRect());
            mTempLastContents = redact.getContent();

            mPaintAnnot.setColor(mTempLastFillColor | 0xFF000000);

            DefaultAppearance da = redact.getDefaultAppearance();
            mTempLastTextColor = da.getText_color();
            mTempLastFontSize = da.getText_size();
            mTempLastFontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());

            prepareAnnotMenu(annot);
            int _pageIndex = annot.getPage().getIndex();
            RectF annotRectF = AppUtil.toRectF(annot.getRect());
            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                RectF _rect = new RectF(annotRectF);
                mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, _pageIndex);
                RectF menuRect = new RectF(mPageViewRect);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, _pageIndex);
                mAnnotMenu.show(menuRect);

                mPdfViewCtrl.refresh(_pageIndex, AppDmUtil.rectFToRect(mPageViewRect));
                if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
            }

            preparePropertyBar(redact);
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    private Rect rectRoundOut(RectF rectF, int roundSize) {
        Rect rect = new Rect();
        rectF.roundOut(rect);
        rect.inset(-roundSize, -roundSize);
        return rect;
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        mAnnotMenu.dismiss();
        mMenuItems.clear();
        if (mPropertyBar.isShowing()) {
            mPropertyBar.dismiss();
        }

        try {
            Redact redact = (Redact) annot;
            DefaultAppearance da = redact.getDefaultAppearance();

            if (mIsAnnotModified && needInvalid) {
                if (mTempLastFillColor == redact.getApplyFillColor()
                        && mTempLastFontSize == da.getText_size()
                        && mTempLastTextColor == da.getText_color()
                        && mTempLastFontId == mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())
                        && mTempLastOverlayText.equals(redact.getOverlayText())
                        && mTempLastBBox.equals(AppUtil.toRectF(annot.getRect()))
                        && mTempLastContents.equals(redact.getContent())) {
                    modifyAnnot(redact, AppUtil.toRectF(annot.getRect()), redact.getApplyFillColor(),
                            da.getText_color(), da.getText_size(), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()),
                            redact.getOverlayText(), redact.getContent(), false, false, null);
                } else {
                    modifyAnnot(redact, AppUtil.toRectF(annot.getRect()), redact.getApplyFillColor(),
                            da.getText_color(), da.getText_size(), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()),
                            redact.getOverlayText(), redact.getContent(), true, true, null);
                }
            } else if (mIsAnnotModified) {
                String overlayText = redact.getOverlayText();
                if (!TextUtils.isEmpty(overlayText)) {
                    Font font = mTextUtil.getSupportFont(mTempLastFontId);
                    da.setFont(font);
                    da.setText_size(mTempLastFontSize);
                    da.setText_color(mTempLastTextColor);
                    redact.setOverlayText(mTempLastOverlayText);
                    redact.setDefaultAppearance(da);
                }
                annot.move(AppUtil.toFxRectF(mTempLastBBox));
                if (mTempLastBorderColor == 0){
                    redact.setBorderColor(DEFAULT_BORDER_COLOR);
                }
                redact.setApplyFillColor(mTempLastFillColor);
                annot.resetAppearanceStream();
            }

            mIsAnnotModified = false;
            if (needInvalid) {
                try {
                    int _pageIndex = annot.getPage().getIndex();
                    if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                        RectF pdfRect = AppUtil.toRectF(annot.getRect());
                        RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, _pageIndex);
                        Rect rect = rectRoundOut(viewRect, 2);
                        mPdfViewCtrl.refresh(_pageIndex, rect);
                        mLastAnnot = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
            mLastAnnot = null;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void prepareAnnotMenu(final Annot annot) {
        mMenuItems.clear();
        if (!mUiExtensionsManager.getDocumentManager().canAddAnnot()) {
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
        } else {
            if (!mUiExtensionsManager.getDocumentManager().isSign() && mUiExtensionsManager.getDocumentManager().canModifyContents())
                mMenuItems.add(AnnotMenu.AM_BT_APPLY);
            mMenuItems.add(AnnotMenu.AM_BT_STYLE);
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            mMenuItems.add(AnnotMenu.AM_BT_REPLY);
            mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
            if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                mMenuItems.add(AnnotMenu.AM_BT_DELETE);
            }
        }
        mAnnotMenu.setMenuItems(mMenuItems);

        mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                        deleteAnnot(annot, true, null);
                    }
                } else if (btType == AnnotMenu.AM_BT_STYLE) {
                    mAnnotMenu.dismiss();

                    try {
                        RectF annotRectF = AppUtil.toRectF(annot.getRect());
                        int _pageIndex = annot.getPage().getIndex();

                        RectF deviceRt = new RectF();
                        if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                            if (mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRt, _pageIndex)) {
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, annotRectF, _pageIndex);
                            }
                        }
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annotRectF);
                        mPropertyBar.show(rectF, false);
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                } else if (AnnotMenu.AM_BT_COMMENT == btType) {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    UIAnnotReply.showComments(mPdfViewCtrl, mUiExtensionsManager.getRootView(), annot);
                } else if (AnnotMenu.AM_BT_REPLY == btType) {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    UIAnnotReply.replyToAnnot(mPdfViewCtrl, mUiExtensionsManager.getRootView(), annot);
                } else if (AnnotMenu.AM_BT_FLATTEN == btType) {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                } else if (AnnotMenu.AM_BT_APPLY == btType) {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    UIAnnotRedaction.apply(mPdfViewCtrl, annot, new Event.Callback() {
                        @Override
                        public void result(Event event, boolean success) {
                            if (success) {
                                mLastAnnot = null;
                            }
                        }
                    });
                }
            }
        });
    }

    private void preparePropertyBar(Redact annot) {
        mPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));

        System.arraycopy(PropertyBar.PB_COLORS_REDACT, 0, mPBColors, 0, mPBColors.length);
        mPBColors[0] = PropertyBar.PB_COLORS_REDACT[0];
        mPropertyBar.setColors(mPBColors);
        try {
            mPropertyBar.setProperty(PropertyBar.PROPERTY_OVERLAY_TEXT, mTempLastOverlayText);
            DefaultAppearance da = annot.getDefaultAppearance();
            float textSize = da.getText_size();
            textSize = (textSize == 0) ? mDefaultTextSize : textSize;
            mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, da.getText_color());
            mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, mTextUtil.getSupportFontName(mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())));
            mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, textSize);
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mPropertyBar.setArrowVisible(false);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);

        mPropertyBar.addTab("", 0, mContext.getApplicationContext().getString(R.string.pb_fill_tab), 0);
        mColorView = getColorView(annot);
        mPropertyBar.addCustomItem(PropertyBar.PROPERTY_REDACT, mColorView, 0, 0);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_FONTNAME
                | PropertyBar.PROPERTY_FONTSIZE
                | PropertyBar.PROPERTY_OVERLAY_TEXT;
    }

    private ColorView getColorView(Annot annot) {
        boolean canEdit = !(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot));
        ColorView colorView = new ColorView(mContext, mUiExtensionsManager.getRootView(), mTempLastFillColor, mPBColors, canEdit);
        ViewGroup colorParent = (ViewGroup) colorView.getParent();
        if (colorParent != null) {
            colorParent.removeView(colorView);
        }
        colorView.setPropertyChangeListener(new PropertyBar.PropertyChangeListener() {
            @Override
            public void onValueChanged(long property, int value) {
                AnnotHandler currentAnnotHandler = mUiExtensionsManager.getCurrentAnnotHandler();
                if (currentAnnotHandler == RedactAnnotHandler.this) {
                    if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
                        try {
                            Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
                            Redact redact = (Redact) annot;
                            if (annot != null
                                    && value != redact.getApplyFillColor()) {
                                RectF rectF = AppUtil.toRectF(redact.getRect());
                                String overlayText = redact.getOverlayText();
                                DefaultAppearance da = redact.getDefaultAppearance();
                                float textSize = da.getText_size();
                                int textColor = da.getText_color();
                                int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());

                                modifyAnnot(redact, rectF, value, textColor,
                                        textSize, fontId, overlayText, redact.getContent(),
                                        false, false, null);
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onValueChanged(long property, float value) {
            }

            @Override
            public void onValueChanged(long property, String value) {
            }
        });
        if (!canEdit) {
            colorView.setAlpha(PropertyBar.PB_ALPHA);
        }
        return colorView;
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (mToolHandler != null) {
            mToolHandler.addAnnot(pageIndex, content, addUndo, false, result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        try {
            Redact redact = (Redact) annot;
            mTempLastFillColor = redact.getApplyFillColor();
            mTempLastBorderColor =redact.getBorderColor();
            DefaultAppearance da = redact.getDefaultAppearance();
            mTempLastTextColor = da.getText_color();
            mTempLastFontSize = da.getText_size();
            mTempLastFontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            mTempLastOverlayText = redact.getOverlayText();
            mTempLastContents = redact.getContent();
            mTempLastBBox = AppUtil.toRectF(annot.getRect());

            RectF bbox = AppUtil.toRectF(redact.getRect());
            String contents = redact.getContent();
            if (content.getBBox() != null)
                bbox = content.getBBox();
            if (content.getContents() != null)
                contents = content.getContents();
            modifyAnnot(redact, bbox, redact.getApplyFillColor(), da.getText_color(),
                    da.getText_size(), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), redact.getOverlayText(), contents,
                    true, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void modifyAnnot(final Annot annot, RectF bbox, int fillColor, int textColor,
                             float fontSize, int fontId, String overlayText, String content,
                             boolean isModifyJni, final boolean addUndo, final Event.Callback callback) {
        final RedactModifyUndoItem undoItem = new RedactModifyUndoItem(mPdfViewCtrl);
        try {
            fontSize = (fontSize == 0) ? mDefaultTextSize : fontSize;
            content = (content == null) ? "" : content;
            overlayText = (overlayText == null) ? "" : overlayText;
            int borderColor = annot.getBorderColor() == 0 ? DEFAULT_BORDER_COLOR : annot.getBorderColor();

            undoItem.setCurrentValue(annot);
            undoItem.mContents = content;
            undoItem.mPageIndex = annot.getPage().getIndex();
            undoItem.mBBox = new RectF(bbox);
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFillColor = fillColor;
            undoItem.mBorderColor = borderColor;
            undoItem.mTextColor = textColor;
            undoItem.mFontSize = fontSize;
            undoItem.mDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
            undoItem.mFont = mTextUtil.getSupportFont(fontId);
            undoItem.mOverlayText = overlayText;

            undoItem.mRedoBorderColor = borderColor;
            undoItem.mRedoFillColor = fillColor;
            undoItem.mRedoTextColor = textColor;
            undoItem.mRedoFontSize = fontSize;
            undoItem.mRedoFont = mTextUtil.getSupportFont(fontId);
            undoItem.mRedoOverlayText = overlayText;
            undoItem.mRedoContents = content;
            undoItem.mRedoBbox = new RectF(bbox);
            undoItem.mRedoDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;

            undoItem.mUndoBorderColor = mTempLastBorderColor == 0 ? DEFAULT_BORDER_COLOR : mTempLastBorderColor;
            undoItem.mUndoFillColor = mTempLastFillColor;
            undoItem.mUndoTextColor = mTempLastTextColor;
            undoItem.mUndoFontSize = mTempLastFontSize;
            undoItem.mUndoFont = mTextUtil.getSupportFont(mTempLastFontId);
            undoItem.mUndoOverlayText = mTempLastOverlayText;
            undoItem.mUndoContents = (mTempLastContents == null) ? "" : mTempLastContents;
            undoItem.mUndoBbox = new RectF(mTempLastBBox);
            undoItem.mUndoDaFlags = DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (isModifyJni) {
            mUiExtensionsManager.getDocumentManager().setHasModifyTask(addUndo);
            RedactEvent event = new RedactEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Redact) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (addUndo) {
                            mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                            mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                        }
                        try {
                            RectF tempRectF = mTempLastBBox;
                            int pageIndex = annot.getPage().getIndex();
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                                mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                annotRectF.union(tempRectF);
                                annotRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }

                    if (callback != null) {
                        callback.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        }

        try {
            if (isModifyJni) {
                mUiExtensionsManager.getDocumentManager().onAnnotModified(annot.getPage(), annot);
            }
            mIsAnnotModified = true;
            if (!isModifyJni) {
                RectF oldRect = AppUtil.toRectF(annot.getRect());

                Redact redact = (Redact) annot;
                DefaultAppearance da = redact.getDefaultAppearance();

                //overlaytext
                Font font = mTextUtil.getSupportFont(fontId);
                da.setFont(font);
                da.setText_size(fontSize);
                da.setText_color(textColor);
                da.setFlags(DefaultAppearance.e_FlagFont | DefaultAppearance.e_FlagTextColor | DefaultAppearance.e_FlagFontSize);
                redact.setDefaultAppearance(da);
                redact.setOverlayText(overlayText);

                redact.move(AppUtil.toFxRectF(bbox));
                redact.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                redact.setContent(content);
                redact.setApplyFillColor(fillColor);
                redact.setFlags(annot.getFlags());
                int borderColor = annot.getBorderColor() == 0 ? DEFAULT_BORDER_COLOR : annot.getBorderColor();
                redact.setBorderColor(borderColor);
                redact.resetAppearanceStream();

                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                int pageIndex = annot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                    mPdfViewCtrl.convertPdfRectToPageViewRect(oldRect, oldRect, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(oldRect, oldRect, pageIndex);

                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);
                    annotRectF.union(oldRect);
                    annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        deleteAnnot(annot, addUndo, result);
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        final DocumentManager documentManager = mUiExtensionsManager.getDocumentManager();
        if (annot == documentManager.getCurrentAnnot()) {
            documentManager.setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final int pageIndex = page.getIndex();

            final RedactDeleteUndoItem undoItem = new RedactDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            DefaultAppearance da = ((Redact) annot).getDefaultAppearance();
            undoItem.mFont = da.getFont();
            int fontId = Font.e_StdIDCourier;
            if (undoItem.mFont != null && !undoItem.mFont.isEmpty()) {
                fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());
            }
            undoItem.mFont = mTextUtil.getSupportFont(fontId);
            undoItem.mTextColor = da.getText_color();
            undoItem.mFontSize = da.getText_size();
            undoItem.mDaFlags = da.getFlags();
            undoItem.mContents = annot.getContent();
            undoItem.mBBox = AppUtil.toRectF(annot.getRect());
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();

            int borderColor = annot.getBorderColor();
            if (borderColor == 0) {
                borderColor = DEFAULT_BORDER_COLOR;
            }
            undoItem.mBorderColor = borderColor;
            undoItem.mFillColor = ((Redact) annot).getApplyFillColor();
            undoItem.mOverlayText = ((Redact) annot).getOverlayText();
            undoItem.mQuadPointsArray = ((Redact) annot).getQuadPoints();
            undoItem.mPageIndex = pageIndex;
            RectFArray rectFArray = new RectFArray();
            rectFArray.add(annot.getRect());
            undoItem.mRectFArray = rectFArray;

            documentManager.onAnnotWillDelete(page, annot);
            RedactEvent event = new RedactEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Redact) annot, mPdfViewCtrl);
            if (documentManager.isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        documentManager.onAnnotDeleted(page, annot);
                        if (addUndo) {
                            documentManager.addUndoItem(undoItem);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                            if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                                mLastAnnot = null;
                            }
                        } else {
                            if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                                mLastAnnot = null;
                            }
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
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
        if (!mRedactModule.canAddRedaction()) {
            return false;
        }
        // in pageView evX and evY
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float evX = point.x;
        float evY = point.y;

        int action = e.getAction();
        try {
            if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()
                    && pageIndex == annot.getPage().getIndex()) {
                Redact redact = (Redact) annot;
                QuadPointsArray quadPointsArray = redact.getQuadPoints();
                if (quadPointsArray.getSize() > 0) {
                    return false;
                }
            }

            switch (action) {
                case MotionEvent.ACTION_DOWN:

                    if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                        RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                        RectF pdfRect = AppUtil.toRectF(annot.getRect());
                        mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                        mCurrentCtr = isTouchControlPoint(pageViewBBox, evX, evY);

                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);

                        if (mCurrentCtr == CTR_LT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LT;
                            return true;
                        } else if (mCurrentCtr == CTR_T) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_T;
                            return true;
                        } else if (mCurrentCtr == CTR_RT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RT;
                            return true;
                        } else if (mCurrentCtr == CTR_R) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_R;
                            return true;
                        } else if (mCurrentCtr == CTR_RB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RB;
                            return true;
                        } else if (mCurrentCtr == CTR_B) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_B;
                            return true;
                        } else if (mCurrentCtr == CTR_LB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LB;
                            return true;
                        } else if (mCurrentCtr == CTR_L) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_L;
                            return true;
                        } else if (isHitAnnot(annot, point)) {
                            mTouchCaptured = true;
                            mLastOper = OPER_TRANSLATE;
                            return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (pageIndex == annot.getPage().getIndex() && mTouchCaptured
                            && annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()
                            && mUiExtensionsManager.getDocumentManager().canAddAnnot()) {
                        if (evX != mLastPoint.x && evY != mLastPoint.y) {
                            RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                            switch (mLastOper) {
                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(pageViewBBox);
                                    mAnnotMenuRect.set(pageViewBBox);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAnnotMenuRect.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mInvalidateRect.union(mAnnotMenuRect);

                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(mAnnotMenuRect);
                                    }
                                    if (mPropertyBar.isShowing()) {
                                        mPropertyBar.dismiss();
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_T: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {

                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_R: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_B: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_L: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mPropertyBar.isShowing()) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;

                                }
                                default:
                                    break;
                            }
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean ret = false;
                    if (mTouchCaptured
                            && annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                        pageViewRect.inset(mThickness / 2, mThickness / 2);

                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                break;
                            }
                            case OPER_SCALE_LT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_T: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_R: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_B: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_L: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                        float _lineWidth = annot.getBorderInfo().getWidth();
                        viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF bboxRect = new RectF(viewDrawBox);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, bboxRect, pageIndex);

                            Redact redact = (Redact) annot;
                            String overlayText = redact.getOverlayText();
                            DefaultAppearance da = redact.getDefaultAppearance();
                            float textSize = da.getText_size();
                            int textColor = da.getText_color();
                            int fontId = mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc());

                            modifyAnnot(redact, bboxRect, redact.getFillColor(),
                                    textColor, textSize, fontId,
                                    overlayText, redact.getContent(),
                                    false, false, null);
                        }

                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                        if (mAnnotMenu.isShowing()) {
                            mAnnotMenu.update(viewDrawBox);
                        } else {
                            mAnnotMenu.show(viewDrawBox);
                        }
                        ret = true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    return ret;
            }
            return false;

        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    private RectF mPageViewRect = new RectF(0, 0, 0, 0);
    private RectF mPageDrawRect = new RectF();
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);
    private float mThickness = 0f;
    private RectF mThicknessRectF = new RectF();

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mThicknessRectF.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mThicknessRectF, mThicknessRectF, pageIndex);
        return Math.abs(mThicknessRectF.width());
    }

    private int isTouchControlPoint(RectF rect, float x, float y) {
        PointF[] ctlPts = calculateControlPoints(rect);
        RectF area = new RectF();
        int ret = -1;
        for (int i = 0; i < ctlPts.length; i++) {
            area.set(ctlPts[i].x, ctlPts[i].y, ctlPts[i].x, ctlPts[i].y);
            area.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
            if (area.contains(x, y)) {
                ret = i + 1;
            }
        }
        return ret;
    }

    /*
     *   1-----2-----3
     *   |	         |
     *   |	         |
     *   8           4
     *   |           |
     *   |           |
     *   7-----6-----5
     *   */
    private RectF mMapBounds = new RectF();

    private PointF[] calculateControlPoints(RectF rect) {
        rect.sort();
        mMapBounds.set(rect);
        mMapBounds.inset(-mCtlPtRadius - mCtlPtLineWidth / 2f, -mCtlPtRadius - mCtlPtLineWidth / 2f);// control rect
        PointF p1 = new PointF(mMapBounds.left, mMapBounds.top);
        PointF p2 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.top);
        PointF p3 = new PointF(mMapBounds.right, mMapBounds.top);
        PointF p4 = new PointF(mMapBounds.right, (mMapBounds.bottom + mMapBounds.top) / 2);
        PointF p5 = new PointF(mMapBounds.right, mMapBounds.bottom);
        PointF p6 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.bottom);
        PointF p7 = new PointF(mMapBounds.left, mMapBounds.bottom);
        PointF p8 = new PointF(mMapBounds.left, (mMapBounds.bottom + mMapBounds.top) / 2);

        return new PointF[]{p1, p2, p3, p4, p5, p6, p7, p8};
    }

    private PointF mAdjustPointF = new PointF(0, 0);

    private PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        if (mLastOper != OPER_TRANSLATE) {
            rectF.inset(-mThickness / 2f, -mThickness / 2f);
        }

        if ((int) rectF.left < dxy) {
            adjustx = -rectF.left + dxy;
            rectF.left = dxy;
        }
        if ((int) rectF.top < dxy) {
            adjusty = -rectF.top + dxy;
            rectF.top = dxy;
        }

        if ((int) rectF.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
            adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectF.right - dxy;
            rectF.right = mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
        }
        if ((int) rectF.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
            adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectF.bottom - dxy;
            rectF.bottom = mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
        }
        mAdjustPointF.set(adjustx, adjusty);
        return mAdjustPointF;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!mRedactModule.canAddRedaction()) {
            return false;
        }
        try {
            PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

            mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
            RectF _rect = AppUtil.toRectF(annot.getRect());
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

            if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, point)) {
                    return true;
                } else {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    return true;
                }
            } else {
                mUiExtensionsManager.getDocumentManager().setCurrentAnnot(annot);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        Annot curAnnot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
        return !AppAnnotUtil.isSameAnnot(curAnnot, annot);
    }

    private Rect mRect = new Rect();
    private RectF mRectF = new RectF();

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
        if (annot == null || annot.isEmpty() || !(annot instanceof Redact)) return;
        if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;

        try {
            if (pageIndex != annot.getPage().getIndex()) return;

            if (AppAnnotUtil.equals(mLastAnnot, annot)) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                mPaintAnnot.setColor(((Redact) annot).getApplyFillColor() | 0xFF000000);

                QuadPointsArray quadPointsArray = ((Redact) annot).getQuadPoints();
                if (quadPointsArray.getSize() > 0) {
                    RectF rectF = AppUtil.toRectF(annot.getRect());
                    mRectF.set(rectF.left, rectF.top, rectF.right, rectF.bottom);
                    RectF deviceRt = new RectF();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mRectF, deviceRt, pageIndex);
                    deviceRt.roundOut(mRect);
                    mRect.inset(-mPaintBoxOutset, -mPaintBoxOutset);
                    canvas.drawRect(mRect, mPaintBbox);

                    ArrayList<com.foxit.sdk.common.fxcrt.RectF> rectFs = new ArrayList<>();
                    long count = quadPointsArray.getSize();
                    for (int i = 0; i < count; i++) {
                        PointF pointF1 = new PointF();
                        pointF1.set(AppUtil.toPointF(quadPointsArray.getAt(i).getFirst()));
                        PointF pointF4 = new PointF();
                        pointF4.set(AppUtil.toPointF(quadPointsArray.getAt(i).getFourth()));
                        com.foxit.sdk.common.fxcrt.RectF _rectF = new com.foxit.sdk.common.fxcrt.RectF(pointF1.x, pointF1.y, pointF4.x, pointF4.y);
                        rectFs.add(_rectF);
                    }
                    Rect clipRect = canvas.getClipBounds();
                    for (com.foxit.sdk.common.fxcrt.RectF rect : rectFs) {
                        RectF rectF1 = AppUtil.toRectF(rect);
                        mRectF.set(rectF1.left, rectF1.top, rectF1.right, rectF1.bottom);
                        RectF deviceRt1 = new RectF();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mRectF, deviceRt1, pageIndex);
                        deviceRt1.round(mRect);
                        if (mRect.intersect(clipRect)) {
                            canvas.drawRect(mRect, mPaintAnnot);
                        }
                    }
                    canvas.restore();
                } else {
                    float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                    mPaintAnnot.setStrokeWidth(thickness);
                    RectF _rect = AppUtil.toRectF(annot.getRect());
                    mViewDrawRectInOnDraw.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRectInOnDraw, mViewDrawRectInOnDraw, pageIndex);
                    mViewDrawRectInOnDraw.inset(thickness / 2f, thickness / 2f);
                    if (mLastOper == OPER_SCALE_LT) {// SCALE
                        mBBoxInOnDraw.set(mLastPoint.x, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                    } else if (mLastOper == OPER_SCALE_T) {
                        mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                    } else if (mLastOper == OPER_SCALE_RT) {
                        mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                    } else if (mLastOper == OPER_SCALE_R) {
                        mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                    } else if (mLastOper == OPER_SCALE_RB) {
                        mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mLastPoint.y);
                    } else if (mLastOper == OPER_SCALE_B) {
                        mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
                    } else if (mLastOper == OPER_SCALE_LB) {
                        mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
                    } else if (mLastOper == OPER_SCALE_L) {
                        mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                    }
                    mBBoxInOnDraw.inset(-thickness / 2f, -thickness / 2f);
                    if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
                        mBBoxInOnDraw = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxInOnDraw, mBBoxInOnDraw, pageIndex);
                        float dx = mLastPoint.x - mDownPoint.x;
                        float dy = mLastPoint.y - mDownPoint.y;

                        mBBoxInOnDraw.offset(dx, dy);
                    }
                    if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                        drawControlPoints(canvas, mBBoxInOnDraw);
                        // add Control Imaginary
                        drawControlImaginary(canvas, mBBoxInOnDraw);
                    }
                    mBBoxInOnDraw.inset(thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth()) / 2f, thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth()) / 2f);// draw Square
                    canvas.drawRect(mBBoxInOnDraw, mPaintAnnot);
                    canvas.restore();
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void drawControlPoints(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            mCtlPtPaint.setColor(DEFAULT_BORDER_COLOR);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    private Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mPaintBbox.setStrokeWidth(mCtlPtLineWidth);
        mPaintBbox.setColor(DEFAULT_BORDER_COLOR);
        mImaginaryPath.reset();
        // set path
        pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
        pathAddLine(mImaginaryPath, ctlPts[1].x + mCtlPtRadius, ctlPts[1].y, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y);
        pathAddLine(mImaginaryPath, ctlPts[2].x, ctlPts[2].y + mCtlPtRadius, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y + mCtlPtRadius, ctlPts[4].x, ctlPts[4].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[4].x - mCtlPtRadius, ctlPts[4].y, ctlPts[5].x + mCtlPtRadius, ctlPts[5].y);
        pathAddLine(mImaginaryPath, ctlPts[5].x - mCtlPtRadius, ctlPts[5].y, ctlPts[6].x + mCtlPtRadius, ctlPts[6].y);
        pathAddLine(mImaginaryPath, ctlPts[6].x, ctlPts[6].y - mCtlPtRadius, ctlPts[7].x, ctlPts[7].y + mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[7].x, ctlPts[7].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

        canvas.drawPath(mImaginaryPath, mPaintBbox);
    }

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }

    protected void onDrawForControls(Canvas canvas) {
        Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
        if (annot == null || !(annot instanceof Redact)) return;
        try {
            int annotPageIndex = annot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(annotPageIndex) && mLastAnnot != null) {
                com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
                mRectF.set(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(mRectF, deviceRt, annotPageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, mRectF, annotPageIndex);
                if (mPropertyBar.isShowing()) {
                    RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mRectF);
                    mPropertyBar.update(rectF);
                }
                mAnnotMenu.update(mRectF);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        if (mColorView != null) {
            mColorView.reset();
        }
    }

    protected void setToolHandler(RedactToolHandler toolHandler) {
        mToolHandler = toolHandler;
    }

    protected void onFontColorValueChanged(int value) {
        try {
            Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
            if (annot != null && mUiExtensionsManager.getCurrentAnnotHandler() == this) {

                Redact redact = (Redact) annot;
                DefaultAppearance da = redact.getDefaultAppearance();
                if (value != da.getText_color()) {
                    modifyAnnot(redact, AppUtil.toRectF(redact.getRect()), redact.getApplyFillColor(), value,
                            da.getText_size(), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), redact.getOverlayText(), redact.getContent(),
                            false, false, null);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void onFontSizeValueChanged(float value) {
        try {
            Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
            if (annot != null && mUiExtensionsManager.getCurrentAnnotHandler() == this) {

                Redact redact = (Redact) annot;
                DefaultAppearance da = redact.getDefaultAppearance();
                if (value != da.getText_size()) {
                    modifyAnnot(redact, AppUtil.toRectF(redact.getRect()), redact.getApplyFillColor(), da.getText_color(),
                            value, mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), redact.getOverlayText(), redact.getContent(),
                            false, false, null);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void onFontValueChanged(String fontName) {
        try {
            Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
            if (annot != null && mUiExtensionsManager.getCurrentAnnotHandler() == this) {

                Redact redact = (Redact) annot;
                DefaultAppearance da = redact.getDefaultAppearance();
                int fontId = mTextUtil.getSupportFontID(fontName);
                if (fontId != mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc())) {
                    modifyAnnot(redact, AppUtil.toRectF(redact.getRect()), redact.getApplyFillColor(), da.getText_color(),
                            da.getText_size(), fontId, redact.getOverlayText(), redact.getContent(),
                            false, false, null);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void onOverlayTextChanged(String value) {
        try {
            Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
            if (annot != null && mUiExtensionsManager.getCurrentAnnotHandler() == this) {

                Redact redact = (Redact) annot;
                DefaultAppearance da = redact.getDefaultAppearance();
                String overlayText = redact.getOverlayText();
                if (!value.equals(overlayText)) {
                    modifyAnnot(redact, AppUtil.toRectF(redact.getRect()), redact.getApplyFillColor(), da.getText_color(),
                            da.getText_size(), mTextUtil.getSupportFontID(da, mPdfViewCtrl.getDoc()), value, redact.getContent(),
                            false, false, null);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


}
