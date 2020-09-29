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
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Highlight;
import com.foxit.sdk.pdf.annots.TextMarkup;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.modules.tts.TTSInfo;
import com.foxit.uiextensions.modules.tts.TTSModule;
import com.foxit.uiextensions.modules.tts.TTSUtils;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


class HighlightAnnotHandler implements AnnotHandler {
    private Context mContext;
    private Paint mPaintBbox;
    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;

    private int mModifyColor;
    private int mModifyOpacity;
    private int mModifyAnnotColor;
    private boolean mIsAnnotModified;
    private Annot mLastAnnot;
    private int mBBoxSpace;
    private PropertyBar mAnnotPropertyBar;
    private boolean mIsEditProperty;
    private PDFViewCtrl mPdfViewCtrl;

    private AppAnnotUtil mAppAnnotUtil;
    private int mPaintBoxOutset;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    public HighlightAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mAppAnnotUtil = AppAnnotUtil.getInstance(context);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(mAppAnnotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(mAppAnnotUtil.getAnnotBBoxPathEffect());

        mMenuItems = new ArrayList<Integer>();

        mPaintBoxOutset = AppResource.getDimensionPixelSize(mContext, R.dimen.annot_highlight_paintbox_outset);
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

    @Override
    public int getType() {
        return Annot.e_Highlight;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return new RectF(AppUtil.toRectF(annot.getRect()));
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        return getAnnotBBox(annot).contains(point.x, point.y);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    private Rect mRect = new Rect();
    private RectF mRectF = new RectF();

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (!(annot instanceof Highlight)) return;
        if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
        try {
            int annotPageIndex = annot.getPage().getIndex();

            //update page
            if (pageIndex != annotPageIndex) return;

            if (AppAnnotUtil.equals(mLastAnnot, annot)) {
                RectF rectF = AppUtil.toRectF(annot.getRect());
                mRectF.set(rectF);
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(mRectF, deviceRt, pageIndex);
                deviceRt.roundOut(mRect);
                mRect.inset(-mPaintBoxOutset, -mPaintBoxOutset);
                canvas.save();
                canvas.drawRect(mRect, mPaintBbox);
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try {
            PointF pointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);

            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pointF)) {
                    return true;
                } else {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            } else {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }


    private int mUndoColor;
    private int mUndoOpacity;
    private String  mUndoContents;
    private int[] mPBColors = new int[PropertyBar.PB_COLORS_HIGHLIGHT.length];

    private int getPBCustomColor() {
        int color = PropertyBar.PB_COLORS_HIGHLIGHT[0];
        return color;
    }


    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotPropertyBar;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        try {
            mUndoColor = (int) annot.getBorderColor();
            mUndoOpacity = (int) (((Highlight) annot).getOpacity() * 255f + 0.5f);

            mPaintBbox.setColor((int) annot.getBorderColor() | 0xFF000000);
            mMenuItems.clear();
            final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
            if (uiExtensionsManager.getDocumentManager().canCopy()) {
                mMenuItems.add(AnnotMenu.AM_BT_COPY);
            }

            mAnnotPropertyBar.setArrowVisible(false);
            if (!uiExtensionsManager.getDocumentManager().canAddAnnot()) {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            } else {
                mMenuItems.add(AnnotMenu.AM_BT_STYLE);
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
                mMenuItems.add(AnnotMenu.AM_BT_REPLY);
                mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
                if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                    mMenuItems.add(AnnotMenu.AM_BT_DELETE);
                }
            }

            if (uiExtensionsManager.getDocumentManager().canCopy()
                    && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_TTS) != null
                    && ((TTSModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_TTS)).isSupperTts()) {
                mMenuItems.add(AnnotMenu.AM_BT_TTS);
            }

            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {

                @Override
                public void onAMClick(int type) {
                    try {
                        if (AnnotMenu.AM_BT_COPY == type) {
                            @SuppressWarnings("deprecation")
                            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setText(annot.getContent());
                            AppAnnotUtil.toastAnnotCopy(mContext);
                            uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                        } else if (AnnotMenu.AM_BT_DELETE == type) {
                            deleteAnnot(annot, true, null);
                        } else if (AnnotMenu.AM_BT_STYLE == type) {
                            mAnnotMenu.dismiss();
                            mIsEditProperty = true;
                            mAnnotPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                            System.arraycopy(PropertyBar.PB_COLORS_HIGHLIGHT, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = getPBCustomColor();
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (((Highlight) annot).getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);
                            RectF annotRectF = new RectF(AppUtil.toRectF(annot.getRect()));
                            int _pageIndex = annot.getPage().getIndex();

                            RectF deviceRt = new RectF();
                            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRt, _pageIndex)) {
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, annotRectF, _pageIndex);
                                }
                            }

                            RectF rectF = AppUtil.toGlobalVisibleRectF(uiExtensionsManager.getRootView(), annotRectF);
                            mAnnotPropertyBar.show(rectF, false);
                            mAnnotPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
                        } else if (AnnotMenu.AM_BT_COMMENT == type) {
                            uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                            UIAnnotReply.showComments(mPdfViewCtrl, uiExtensionsManager.getRootView(), annot);
                        } else if (AnnotMenu.AM_BT_REPLY == type) {
                            uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                            UIAnnotReply.replyToAnnot(mPdfViewCtrl, uiExtensionsManager.getRootView(), annot);
                        } else if (AnnotMenu.AM_BT_FLATTEN == type) {
                            uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                            UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                        } else if (AnnotMenu.AM_BT_TTS == type){
                            uiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                            TTSInfo ttsInfo = TTSUtils.getTTSInfoFormTextMarkup((TextMarkup) annot);
                            if (ttsInfo == null || AppUtil.isEmpty(ttsInfo.mText)){
                                return;
                            }
                            TTSModule ttsModule = (TTSModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_TTS);
                            if (ttsModule != null) {
                                ttsModule.speakFromTs(ttsInfo);
                            }
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });

            int _pageIndex = annot.getPage().getIndex();
            RectF annotRectF = AppUtil.toRectF(annot.getRect());

            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRt, _pageIndex);
                Rect rect = rectRoundOut(deviceRt, 0);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, annotRectF, _pageIndex);
                mAnnotMenu.show(annotRectF);
                mPdfViewCtrl.refresh(_pageIndex, rect);
                if (annot == uiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {
        mAnnotMenu.dismiss();
        mMenuItems.clear();
        if (mIsEditProperty) {
            mIsEditProperty = false;
            mAnnotPropertyBar.dismiss();
        }

        if (mIsAnnotModified && needInvalid) {
            if (mUndoColor != mModifyAnnotColor || mUndoOpacity != mModifyOpacity) {
                modifyAnnot(annot, mModifyColor, mModifyOpacity, null, true, null);
            }
        } else if (mIsAnnotModified) {
            try {
                annot.setBorderColor(mUndoColor);

                ((Highlight) annot).setOpacity(mUndoOpacity / 255f);
                annot.resetAppearanceStream();
            } catch (PDFException e) {
                if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                    mPdfViewCtrl.recoverForOOM();
                }
                return;
            }
        }
        mIsAnnotModified = false;
        if (needInvalid) {
            try {
                int _pageIndex = annot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                    RectF rectF = new RectF(AppUtil.toRectF(annot.getRect()));
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, _pageIndex);
                    Rect rect = rectRoundOut(rectF, 2);
                    mPdfViewCtrl.refresh(_pageIndex, rect);
                    mLastAnnot = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }
        mLastAnnot = null;
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        deleteAnnot(annot, addUndo, result);
    }

    public void modifyAnnotColor(int color) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) return;
        mModifyColor = color & 0xFFFFFF;
        try {
            mModifyOpacity = (int) (((Highlight) annot).getOpacity() * 255f);

            mModifyAnnotColor = mModifyColor;
            if (annot.getBorderColor() != mModifyAnnotColor) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Highlight) annot).setOpacity(mModifyOpacity / 255f);
                PDFViewCtrl.lock();
                annot.resetAppearanceStream();
                PDFViewCtrl.unlock();
                mPaintBbox.setColor(mModifyAnnotColor | 0xFF000000);
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = (int) annot.getBorderColor() & 0xFFFFFF;
            mModifyOpacity = opacity;

            mModifyAnnotColor = mModifyColor;
            if ((int)(((Highlight) annot).getOpacity() * 255f) != mModifyOpacity) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Highlight) annot).setOpacity(mModifyOpacity / 255f);
                PDFViewCtrl.lock();
                annot.resetAppearanceStream();
                PDFViewCtrl.unlock();
                mPaintBbox.setColor(mModifyAnnotColor | 0xFF000000);
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }


    private void modifyAnnot(final Annot annot, int color, int opacity, DateTime modifyDate, final boolean addUndo, final Event.Callback callback) {

        try {
            final PDFPage page = annot.getPage();
            if (page == null) {
                if (callback != null) {
                    callback.result(null, false);
                }
                return;
            }

            final HighlightModifyUndoItem undoItem = new HighlightModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = page.getIndex();
            undoItem.mColor = color;
            undoItem.mOpacity = opacity  / 255f;
            undoItem.mModifiedDate = (modifyDate == null) ? AppDmUtil.currentDateToDocumentDate() : modifyDate;

            undoItem.mRedoColor = color;
            undoItem.mRedoOpacity =  opacity / 255f;
            undoItem.mRedoContents = annot.getContent();

            undoItem.mUndoColor = mUndoColor;
            undoItem.mUndoOpacity = mUndoOpacity / 255f;
            undoItem.mUndoContents = mUndoContents;

            undoItem.mPaintBbox = mPaintBbox;

            HighlightEvent event = new HighlightEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Highlight) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);
                        if (addUndo) {
                           ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        } else {
                            try {
                                if (mPdfViewCtrl.isPageVisible(page.getIndex())) {
                                    RectF annotRectF = new RectF(AppUtil.toRectF(annot.getRect()));
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, page.getIndex());
                                    mPdfViewCtrl.refresh(page.getIndex(), AppDmUtil.rectFToRect(annotRectF));
                                }
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    if (callback != null) {
                        callback.result(null, success);
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

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final RectF annotRectF = AppUtil.toRectF(annot.getRect());
            final PDFPage page = annot.getPage();
            if (page == null) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }
            final int pageIndex = page.getIndex();
            final HighlightDeleteUndoItem undoItem = new HighlightDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.quadPointsArray = ((Highlight)annot).getQuadPoints();

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            HighlightEvent event = new HighlightEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Highlight) annot, mPdfViewCtrl);
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
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(deviceRectF));
                            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                                mLastAnnot = null;
                            }
                        } else {
                            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
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
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    private void invalidateForToolModify(Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
            RectF rectF = new RectF(AppUtil.toRectF(annot.getRect()));
            RectF pvRect = new RectF();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, pvRect, pageIndex);
            Rect rect = rectRoundOut(pvRect, mBBoxSpace);
            rect.inset(-mPaintBoxOutset, -mPaintBoxOutset);
            mPdfViewCtrl.refresh(pageIndex, rect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Rect rectRoundOut(RectF rectF, int roundSize) {
        Rect rect = new Rect();
        rectF.roundOut(rect);
        rect.inset(-roundSize, -roundSize);
        return rect;
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier, boolean addUndo, Event.Callback result) {
        if (mToolHandler != null) {
            if (!(contentSupplier instanceof TextMarkupContent)) {
                mToolHandler.setFromSelector(true);
            }
            mToolHandler.addAnnot(pageIndex, addUndo, contentSupplier, result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    private HighlightToolHandler mToolHandler;

    public void setToolHandler(HighlightToolHandler toolHandler) {
        mToolHandler = toolHandler;
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }
        try {
            mUndoColor = (int) annot.getBorderColor();
            mUndoOpacity = (int) (((Highlight) annot).getOpacity() * 255f);
            mUndoContents = annot.getContent();
            if (content.getContents() != null) {
                annot.setContent(content.getContents());
            } else {
                annot.setContent("");
            }


            if (mLastAnnot == annot) {
                mPaintBbox.setColor(content.getColor() | 0xFF000000);
            }
            modifyAnnot(annot, content.getColor(), content.getOpacity(), content.getModifiedDate(), addUndo, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (!(annot instanceof Highlight)) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this) return;
        try {
            int annotPageIndex = annot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                mRectF.set(AppUtil.toRectF(annot.getRect()));
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(mRectF, deviceRt, annotPageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(deviceRt, mRectF, annotPageIndex);
                if (mIsEditProperty) {
                    RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mRectF);
                    mAnnotPropertyBar.update(rectF);
                }
                mAnnotMenu.update(mRectF);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void removeProbarListener() {
        mPropertyChangeListener = null;
    }
}
