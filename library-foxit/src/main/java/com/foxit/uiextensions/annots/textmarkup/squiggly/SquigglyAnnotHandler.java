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
package com.foxit.uiextensions.annots.textmarkup.squiggly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Squiggly;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupUtil;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.security.acl.AclEntry;
import java.util.ArrayList;


class SquigglyAnnotHandler implements AnnotHandler {

    private PropertyBar mAnnotPropertyBar;
    private Context mContext;

    private PDFViewCtrl mPdfViewCtrl;

    private AppAnnotUtil mAppAnnotUtil;
    private AnnotMenu mAnnotMenu;
    private Annot mLastAnnot;
    private Paint mPaintBbox;

    private int mBBoxSpace;

    private RectF mDrawLocal_tmpF;

    private int mModifyColor;
    private int mModifyOpacity;
    private int mModifyAnnotColor;

    private ArrayList<Integer> mMenuItems;
    private SquigglyToolHandler mSquigglyToolHandler;

    private boolean mIsEditProperty;
    private boolean mIsAnnotModified;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public SquigglyAnnotHandler(Context context, PDFViewCtrl pdfViewer) {
        mContext = context;

        mPdfViewCtrl = pdfViewer;
        mAppAnnotUtil = AppAnnotUtil.getInstance(context);

        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(mAppAnnotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(mAppAnnotUtil.getAnnotBBoxPathEffect());

        mDrawLocal_tmpF = new RectF();

        mMenuItems = new ArrayList<Integer>();
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotPropertyBar;
    }

    public void setToolHandler(SquigglyToolHandler toolHandler) {
        mSquigglyToolHandler = toolHandler;
    }

    @Override
    public int getType() {
        return Annot.e_Squiggly;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
            return new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF bbox = getAnnotBBox(annot);
        return bbox.contains(point.x, point.y);
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_SQUIGGLY.length];

    public int getPBCustomColor() {
        return PropertyBar.PB_COLORS_SQUIGGLY[0];
    }

    private int mTmpUndoColor;
    private int mTmpUndoOpacity;
    private String  mTmpUndoContents;
    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((Squiggly) annot).getOpacity() * 255f);

            mPaintBbox.setColor(mTmpUndoColor | 0xFF000000);

            mAnnotPropertyBar.setArrowVisible(false);
            resetMenuItems(annot);
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    try {
                        if (btType == AnnotMenu.AM_BT_COPY) {
                            ClipboardManager clipboard = null;
                            clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
//                            String text = annot.getContent();
                            clipboard.setText(annot.getContent());
                            AppAnnotUtil.toastAnnotCopy(mContext);
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        } else if (btType == AnnotMenu.AM_BT_DELETE) {
                            deleteAnnot(annot, true, null);
                        } else if (btType == AnnotMenu.AM_BT_STYLE) {
                            mAnnotMenu.dismiss();
                            mIsEditProperty = true;
                            mAnnotPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
                            System.arraycopy(PropertyBar.PB_COLORS_SQUIGGLY, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = getPBCustomColor();
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (((Squiggly) annot).getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);
                            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
                            RectF annotRectF = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                            int _pageIndex = annot.getPage().getIndex();

                            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, _pageIndex);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, _pageIndex);
                            }
                            RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annotRectF);
                            mAnnotPropertyBar.show(rectF, false);
                            mAnnotPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
                        } else if (btType == AnnotMenu.AM_BT_COMMENT) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                            UIAnnotReply.showComments(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annot);
                        } else if (btType == AnnotMenu.AM_BT_REPLY) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                            UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annot);
                        } else if (AnnotMenu.AM_BT_FLATTEN == btType) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                            UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }

            });

            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
            RectF annotRectF = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
            int _pageIndex = annot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, _pageIndex);
                Rect rect = TextMarkupUtil.rectRoundOut(annotRectF, 0);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, _pageIndex);
                mPdfViewCtrl.refresh(_pageIndex, rect);
                if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
            }
            mAnnotMenu.show(annotRectF);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        mAnnotMenu.dismiss();
        try {
            if (mIsEditProperty) {
                mIsEditProperty = false;
            }

            if (mIsAnnotModified && needInvalid) {
                if (mTmpUndoColor != mModifyAnnotColor || mTmpUndoOpacity != mModifyOpacity) {
                    ModifyAnnot(annot, mModifyColor, mModifyOpacity, null, true, null);
                }
            } else if (mIsAnnotModified) {
                annot.setBorderColor(mTmpUndoColor);
                ((Squiggly) annot).setOpacity(mTmpUndoOpacity / 255f);
                annot.resetAppearanceStream();
            }
            mIsAnnotModified = false;
            if (needInvalid) {
                int _pageIndex = annot.getPage().getIndex();

                if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                    com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
                    RectF rectF = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, _pageIndex);
                    Rect rect = TextMarkupUtil.rectRoundOut(rectF, 0);
                    mPdfViewCtrl.refresh(_pageIndex, rect);
                    mLastAnnot = null;
                }
                return;
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
        mLastAnnot = null;
    }

    private void ModifyAnnot(final Annot annot, int color, int opacity, DateTime modifyDate, final boolean addUndo, final Event.Callback callback) {
        try {
            final PDFPage page = annot.getPage();

            if (null == page) return;
            if (modifyDate == null) {
                modifyDate = AppDmUtil.currentDateToDocumentDate();
                annot.setBorderColor(mModifyAnnotColor);
            } else {
                annot.setBorderColor(color);
                ((Squiggly) annot).setOpacity(opacity / 255f);
            }

            final int _pageIndex = page.getIndex();

            final SquigglyModifyUndoItem undoItem = new SquigglyModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = _pageIndex;
            undoItem.mColor = color;
            undoItem.mOpacity = opacity / 255f;
            undoItem.mModifiedDate = modifyDate;

            undoItem.mRedoColor = color;
            undoItem.mRedoOpacity = opacity / 255f;
            undoItem.mRedoContents = annot.getContent();

            undoItem.mUndoColor = mTmpUndoColor;
            undoItem.mUndoOpacity = mTmpUndoOpacity / 255f;
            undoItem.mUndoContents = mTmpUndoContents;

            undoItem.mPaintBbox = mPaintBbox;

            SquigglyEvent event = new SquigglyEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Squiggly) annot, mPdfViewCtrl);
            final EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        } else {
                            try {
                                if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                                    com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
                                    RectF annotRectF = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, _pageIndex);
                                    mPdfViewCtrl.refresh(_pageIndex, AppDmUtil.rectFToRect(annotRectF));
                                }
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (callback != null) {
                        callback.result(null, true);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {

            if (e.getLastError() ==Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final RectF annotRectF = AppUtil.toRectF(annot.getRect());
            final PDFPage page = annot.getPage();

            final int _pageIndex = page.getIndex();
            final RectF deviceRectF = new RectF();
            final SquigglyDeleteUndoItem undoItem = new SquigglyDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = _pageIndex;
            undoItem.mQuadPoints = ((Squiggly)annot).getQuadPoints();

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            SquigglyEvent event = new SquigglyEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Squiggly) annot, mPdfViewCtrl);
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

                        if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, _pageIndex);
                            mPdfViewCtrl.refresh(_pageIndex, AppDmUtil.rectFToRect(deviceRectF));
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

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (mSquigglyToolHandler != null) {
            if (content instanceof TextMarkupContent) {
                mSquigglyToolHandler.addAnnot(pageIndex, addUndo, null,
                        content.getBBox(), null, result);
            } else {
                TextMarkupContentAbs tmSelector = TextMarkupContentAbs.class.cast(content);
                SquigglyToolHandler.SelectInfo info = mSquigglyToolHandler.mSelectInfo;
                info.clear();
                info.mIsFromTS = true;
                info.mStartChar = tmSelector.getTextSelector().getStart();
                info.mEndChar = tmSelector.getTextSelector().getEnd();
                mSquigglyToolHandler.setFromSelector(true);
                mSquigglyToolHandler.selectCountRect(pageIndex, info);
                mSquigglyToolHandler.onSelectRelease(pageIndex, info, result);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) return;
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((Squiggly) annot).getOpacity() * 255f);
            mTmpUndoContents = annot.getContent();
            if (content.getContents() != null) {
                annot.setContent(content.getContents());
            } else {
                annot.setContent(null);
            }

            if (mLastAnnot == annot) {
                mPaintBbox.setColor(content.getColor());
            }
            ModifyAnnot(annot, content.getColor(), content.getOpacity(), content.getModifiedDate(), addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        deleteAnnot(annot, addUndo, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || !(annot instanceof Squiggly)) return;
        if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
        try {
            if (pageIndex != annot.getPage().getIndex()) return;

            if (AppAnnotUtil.equals(mLastAnnot, annot)) {
                RectF rectF = AppUtil.toRectF(annot.getRect());
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, deviceRt, pageIndex);
                Rect rectBBox = TextMarkupUtil.rectRoundOut(deviceRt, mBBoxSpace);
                canvas.save();
                canvas.drawRect(rectBBox, mPaintBbox);
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
        try{
            PointF pdfPt = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pdfPt)) {
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

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot == null || !(curAnnot instanceof Squiggly)) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this) return;

        try {
            int annotPageIndex = curAnnot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                com.foxit.sdk.common.fxcrt.RectF _rectF = curAnnot.getRect();
                RectF rectF = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, mDrawLocal_tmpF, annotPageIndex);
                RectF canvasRt = new RectF();
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDrawLocal_tmpF, canvasRt, annotPageIndex);
                if (mIsEditProperty) {
                    RectF rect = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), canvasRt);
                    mAnnotPropertyBar.update(rect);
                }
                mAnnotMenu.update(canvasRt);
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void resetMenuItems(Annot annot) {
        mMenuItems.clear();

//        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canCopy()) {
//            mMenuItems.add(AnnotMenu.AM_BT_COPY);
//        }
//        if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
//            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
//        } else {
//            mMenuItems.add(AnnotMenu.AM_BT_STYLE);
//            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
//            mMenuItems.add(AnnotMenu.AM_BT_REPLY);
//            mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
        try{
            if (AppAnnotUtil.isLocked(annot))   {
                int flags = annot.getFlags();
                annot.setFlags(flags-128);
            }

            if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                mMenuItems.add(AnnotMenu.AM_BT_DELETE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

//        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_Squiggly) == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                return true;
            }
        }
        return false;
    }

    public void modifyAnnotColor(int color) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = color & 0xFFFFFF;
            mModifyOpacity = (int) (((Squiggly) annot).getOpacity() * 255f);

            mModifyAnnotColor = mModifyColor;
            if (annot.getBorderColor() != mModifyAnnotColor) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Squiggly) annot).setOpacity(mModifyOpacity / 255f);
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
            return;
        }
    }

    private void invalidateForToolModify(Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
            RectF rectF = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
            RectF pvRect = new RectF();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, pvRect, pageIndex);
            Rect rect = TextMarkupUtil.rectRoundOut(pvRect, mBBoxSpace);
            rect.inset(-1, -1);
            mPdfViewCtrl.refresh(pageIndex, rect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = (int) annot.getBorderColor() & 0xFFFFFF;
            mModifyOpacity = opacity;

            mModifyAnnotColor = mModifyColor;
            if ((int)(((Squiggly) annot).getOpacity() * 255f) != mModifyOpacity) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((Squiggly) annot).setOpacity(mModifyOpacity / 255f);
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
            return;
        }
    }

    protected void removeProbarListener() {
        mPropertyChangeListener = null;
    }
}
