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
package com.foxit.uiextensions.annots.ink;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractAnnotHandler;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.modules.annotations.AnnotationsConfig;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


public class InkAnnotHandler extends AbstractAnnotHandler {
    protected InkToolHandler mToolHandler;
    protected InkAnnotUtil mUtil;
    protected ArrayList<Integer> mMenuText;
    protected String mSubject = "Pencil";

    protected float mBackOpacity;
    protected int mBackColor;
    protected ArrayList<ArrayList<PointF>> mOldInkLists;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public InkAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager,InkToolHandler toolHandler, InkAnnotUtil util) {
        super(context, pdfViewCtrl, Annot.e_Ink);
        mToolHandler = toolHandler;
        mColor = mToolHandler.getColor();
        mOpacity = mToolHandler.getOpacity();
        mThickness = mToolHandler.getThickness();
        mUtil = util;
        mUiExtensionsManager = uiExtensionsManager;
        mMenuText = new ArrayList<Integer>();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected AbstractToolHandler getToolHandler() {
        return mToolHandler;
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
        if (uiExtensionsManager == null) return false;

        Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
        AnnotationsConfig annotConfig = config.modules.getAnnotConfig();
        if (annotConfig.isLoadDrawPencil() == false) return false;
        return super.onTouchEvent(pageIndex, e, annot);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
        if (uiExtensionsManager == null) return false;

        Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
        AnnotationsConfig annotConfig = config.modules.getAnnotConfig();
        if (annotConfig.isLoadDrawPencil() == false) return false;
        return super.onSingleTapConfirmed(pageIndex, motionEvent, annot);
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        return AppAnnotUtil.isSameAnnot(curAnnot, annot) ? false : true;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
        if (uiExtensionsManager == null) return false;

        Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
        AnnotationsConfig annotConfig = config.modules.getAnnotConfig();
        if (annotConfig.isLoadDrawPencil() == false) return false;
        return super.onLongPress(pageIndex, motionEvent, annot);
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            mColor = (int) annot.getBorderColor();
            mOpacity = AppDmUtil.opacity255To100((int) (((Ink) annot).getOpacity() * 255f + 0.5f));
            mThickness = annot.getBorderInfo().getWidth();

            mBackColor = mColor;
            mBackOpacity = ((Ink) annot).getOpacity();
            mOldInkLists = InkAnnotUtil.generateInkList(((Ink) annot).getInkList());
            super.onAnnotSelected(annot, reRender);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        if (!mIsModified) {
            super.onAnnotDeselected(annot, reRender);
        } else {
            InkModifyUndoItem undoItem = new InkModifyUndoItem(this, mPdfViewCtrl);
            undoItem.setCurrentValue(mSelectedAnnot);
            try {
                undoItem.mPath = ((Ink)mSelectedAnnot).getInkList();
                undoItem.mInkLists = InkAnnotUtil.generateInkList(undoItem.mPath);
                undoItem.mOldColor = mBackColor;
                undoItem.mOldOpacity = mBackOpacity;
                undoItem.mOldBBox = new RectF(mBackRect);
                undoItem.mOldLineWidth = mBackThickness;

                undoItem.mOldInkLists = InkAnnotUtil.cloneInkList(mOldInkLists);
                undoItem.mOldPath = new com.foxit.sdk.common.Path();
                for (int li = 0; li < mOldInkLists.size(); li++) { //li: line index
                    ArrayList<PointF> line = mOldInkLists.get(li);
                    int size = line.size();
                    if (size == 1) {
                        undoItem.mOldPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                        undoItem.mOldPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                    } else {
                        for (int pi = 0; pi < size; pi++) {//pi: point index
                            if (pi == 0) {
                                undoItem.mOldPath.moveTo(AppUtil.toFxPointF(line.get(pi)));
                            } else {
                                undoItem.mOldPath.lineTo(AppUtil.toFxPointF(line.get(pi)));
                            }
                        }
                    }
                }

            } catch (PDFException e) {
                e.printStackTrace();
            }
            modifyAnnot(mSelectedAnnot, undoItem, false, true, reRender, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mSelectedAnnot != ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        resetStatus();
                    }
                }
            });
            dismissPopupMenu();
            hidePropertyBar();
        }
    }

    @Override
    public void addAnnot(int pageIndex, final AnnotContent content, boolean addUndo, final Event.Callback result) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final InkAnnotContent inkAnnotContent = (InkAnnotContent) content;
            final Ink annot = (Ink) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Ink, AppUtil.toFxRectF(inkAnnotContent.getBBox())), Annot.e_Ink);
            InkAddUndoItem undoItem = new InkAddUndoItem(this, mPdfViewCtrl);
            undoItem.setCurrentValue(inkAnnotContent);
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();

            ArrayList<ArrayList<PointF>> lines = ((InkAnnotContent) content).getInkLisk();
            if (lines != null) {
                undoItem.mPath = new com.foxit.sdk.common.Path();
                for (int i = 0; i < lines.size(); i++) {
                    ArrayList<PointF> line = lines.get(i);
                    int size = line.size();
                    if (size == 1) {
                        undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                        undoItem.mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                    } else {
                        for (int j = 0; j < size; j++) {
                            if (j == 0) {
                                undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(j)));
                            } else {
                                undoItem.mPath.lineTo(AppUtil.toFxPointF(line.get(j)));
                            }
                        }
                    }
                }
            }
            undoItem.mInkLists = InkAnnotUtil.cloneInkList(lines);
            addAnnot(pageIndex, annot, undoItem, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected Annot addAnnot(int pageIndex, RectF bbox, final int color, final int opacity, final float thickness,
                             final ArrayList<ArrayList<PointF>> lines, Event.Callback result) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final Ink annot = (Ink) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Ink, AppUtil.toFxRectF(bbox)), Annot.e_Ink);

            InkAddUndoItem undoItem = new InkAddUndoItem(this, mPdfViewCtrl);
            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mBBox = new RectF(bbox);
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mFlags = Annot.e_FlagPrint;
            undoItem.mSubject = mSubject;
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mColor = color;
            undoItem.mOpacity = opacity / 255f;
            undoItem.mLineWidth = thickness;
            undoItem.mPath = new com.foxit.sdk.common.Path();
            for (int li = 0; li < lines.size(); li++) { //li: line index
                ArrayList<PointF> line = lines.get(li);
                int size = line.size();
                if (size == 1) {
                    undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                    undoItem.mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                } else {
                    for (int pi = 0; pi < size; pi++) {//pi: point index
                        if (pi == 0) {
                            undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(pi)));
                        } else {
                            undoItem.mPath.lineTo(AppUtil.toFxPointF(line.get(pi)));

                        }
                    }
                }
            }
            undoItem.mInkLists = InkAnnotUtil.cloneInkList(lines);
            addAnnot(pageIndex, annot, undoItem, true, result);
            return annot;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void addAnnot(int pageIndex, Annot annot, InkAddUndoItem undoItem, boolean addUndo, final Event.Callback result) {
        final InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (Ink) annot, mPdfViewCtrl);
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots() ||
                undoItem.isFromEraser) {
            if (result != null) {
                result.result(event, true);
            }
            return;
        }
        handleAddAnnot(pageIndex, annot, event, addUndo, new IAnnotTaskResult<PDFPage, Annot, Void>() {
            @Override
            public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                if (result != null) {
                    result.result(event, true);
                }
            }
        });
    }

    @Override
    public Annot handleAddAnnot(final int pageIndex, final Annot annot, final EditAnnotEvent addEvent, final boolean addUndo,
                                final IAnnotTaskResult<PDFPage, Annot, Void> result) {
        try {
            final PDFPage page = annot.getPage();

            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    try{
                        if (success) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
                            if (addUndo) {
                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(addEvent.mUndoItem);
                            }
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                RectF pvRect = getBBox(mPdfViewCtrl, annot);
                                final Rect tv_rect1 = new Rect();
                                pvRect.roundOut(tv_rect1);
                                mPdfViewCtrl.refresh(pageIndex, tv_rect1);
                            }

                        }

                        if (result != null) {
                            result.onResult(success, page, annot, null);
                        }
                    }catch (Exception e){

                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return annot;
    }

    @Override
    public void modifyAnnot(final Annot annot, final AnnotContent content, boolean addUndo, final Event.Callback result) {
        try {
            InkModifyUndoItem undoItem = new InkModifyUndoItem(this, mPdfViewCtrl);
            undoItem.setOldValue(annot);
            undoItem.mOldPath = ((Ink)annot).getInkList();
            undoItem.mOldInkLists = InkAnnotUtil.generateInkList(undoItem.mOldPath );
            undoItem.setCurrentValue(content);
            if (content instanceof InkAnnotContent) {
                ArrayList<ArrayList<PointF>> lines = ((InkAnnotContent) content).getInkLisk();
                if (lines != null) {
                    undoItem.mPath = new com.foxit.sdk.common.Path();
                    for (int i = 0; i < lines.size(); i++) {
                        ArrayList<PointF> line = lines.get(i);
                        int size = line.size();
                        if (size == 1) {
                            undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                            undoItem.mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                        } else {
                            for (int j = 0; j < size; j++) {
                                if (j == 0) {
                                    undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(j)));
                                } else {
                                    undoItem.mPath.lineTo(AppUtil.toFxPointF(line.get(j)));
                                }
                            }
                        }
                    }
                }
                undoItem.mInkLists = InkAnnotUtil.cloneInkList(lines);
            }

            modifyAnnot(annot, undoItem, false, addUndo, true, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void modifyAnnot(Annot annot, InkUndoItem undoItem, boolean useOldValue, boolean addUndo, boolean reRender,
                               final Event.Callback result) {
        InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Ink) annot, mPdfViewCtrl);
        event.useOldValue = useOldValue;
        if (undoItem.isFromEraser) {
            if (result != null) {
                result.result(event, true);
            }
            return;
        }
        handleModifyAnnot(annot, event, addUndo, reRender,
                new IAnnotTaskResult<PDFPage, Annot, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, final Event.Callback result) {
        InkDeleteUndoItem undoItem = new InkDeleteUndoItem(this, mPdfViewCtrl);
        undoItem.setCurrentValue(annot);
        try {
            undoItem.mPath = ((Ink)annot).getInkList();
            undoItem.mInkLists = InkAnnotUtil.generateInkList(undoItem.mPath);
            undoItem.mGroupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);
        } catch (PDFException e) {
            e.printStackTrace();
        }

        removeAnnot(annot, undoItem, addUndo, result);
    }

    protected void removeAnnot(Annot annot, final InkDeleteUndoItem undoItem, boolean addUndo, final Event.Callback result) {
        InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Ink) annot, mPdfViewCtrl);
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()
                || undoItem.isFromEraser) {
            try {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(annot.getPage(), annot);
                if (result != null) {
                    result.result(event, true);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return;
        }

        handleRemoveAnnot(annot, event, addUndo,
                new IAnnotTaskResult<PDFPage, Void, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage page, Void p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }

                        if (success) {

                            if (undoItem.mGroupNMList.size() >= 2) {
                                ArrayList<String> newGroupList = new ArrayList<>(undoItem.mGroupNMList);
                                newGroupList.remove(undoItem.mNM);
                                if (newGroupList.size() >= 2)
                                    GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, newGroupList);
                                else
                                    GroupManager.getInstance().unGroup(page, newGroupList.get(0));
                            }
                        }
                    }
                });
    }

    @Override
    protected ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot) {
        return InkAnnotUtil.generatePathData(mPdfViewCtrl, pageIndex, (Ink) annot);
    }

    @Override
    protected void transformAnnot(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, Matrix matrix) {
        RectF bbox = getBBox(pdfViewCtrl, annot);
        matrix.mapRect(bbox);
        pdfViewCtrl.convertPageViewRectToPdfRect(bbox, bbox, pageIndex);

        transformLines(pdfViewCtrl, pageIndex, (Ink) annot, matrix);

        try {
            annot.move(AppUtil.toFxRectF(bbox));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void resetStatus() {
        mBackRect = null;
        mBackThickness = 0.0f;
        mSelectedAnnot = null;
        mIsModified = false;
    }

    @Override
    protected void showPopupMenu(final Annot annot) {
        try {
            Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
            if (curAnnot == null || curAnnot.isEmpty() || curAnnot.getType() != Annot.e_Ink) return;

            reloadPopupMenuString((Ink) curAnnot);
            mAnnotMenu.setMenuItems(mMenuText);
            RectF bbox = AppUtil.toRectF(curAnnot.getRect());
            int pageIndex = curAnnot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
            mAnnotMenu.show(bbox);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int flag) {
                    if (annot == null) return;
                    if (flag == AnnotMenu.AM_BT_COMMENT) { // comment
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotReply.showComments(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annot);
                    } else if (flag == AnnotMenu.AM_BT_REPLY) { // reply
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annot);
                    } else if (flag == AnnotMenu.AM_BT_DELETE) { // delete
                        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            removeAnnot(annot, true, null);
                        }
                    } else if (flag == AnnotMenu.AM_BT_STYLE) { // line color
                        dismissPopupMenu();
                        showPropertyBar(PropertyBar.PROPERTY_COLOR);
                    } else if (flag == AnnotMenu.AM_BT_FLATTEN) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                    }
                }
            });
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void dismissPopupMenu() {
        mAnnotMenu.setListener(null);
        mAnnotMenu.dismiss();
    }

    @Override
    protected void showPropertyBar(long curProperty) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || annot.isEmpty() || !(annot instanceof Ink)) return;
        long properties = getSupportedProperties();

        mPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
        mPropertyBar.setPropertyChangeListener(this);
        setPropertyBarProperties(mPropertyBar);
        mPropertyBar.reset(properties);

        try {
            RectF bbox = AppUtil.toRectF(annot.getRect());
            int pageIndex = annot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
            RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), bbox);
            mPropertyBar.show(rectF, false);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint, Annot annot) {
        super.setPaintProperty(pdfViewCtrl, pageIndex, paint, annot);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Style.STROKE);

    }

    @Override
    protected long getSupportedProperties() {
        return mUtil.getSupportedProperties();
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        propertyBar.setColors(colors);
        super.setPropertyBarProperties(propertyBar);
    }

    protected void reloadPopupMenuString(Ink ink) {
        mMenuText.clear();

        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            mMenuText.add(AnnotMenu.AM_BT_STYLE);
            mMenuText.add(AnnotMenu.AM_BT_COMMENT);
            mMenuText.add(AnnotMenu.AM_BT_REPLY);
            mMenuText.add(AnnotMenu.AM_BT_FLATTEN);
            if (!(AppAnnotUtil.isLocked(ink) || AppAnnotUtil.isReadOnly(ink))) {
                mMenuText.add(AnnotMenu.AM_BT_DELETE);
            }
        } else {
            mMenuText.add(AnnotMenu.AM_BT_COMMENT);
        }
    }

    private void transformLines(PDFViewCtrl pdfViewCtrl, int pageIndex, Ink annot, Matrix matrix) {
        try {
            float[] tmp = {0, 0};
            com.foxit.sdk.common.Path path = annot.getInkList();
            for (int i = 0; i < path.getPointCount(); i++) {
                PointF pt = AppUtil.toPointF(path.getPoint(i));
                pdfViewCtrl.convertPdfPtToPageViewPt(pt, pt, pageIndex);
                tmp[0] = pt.x;
                tmp[1] = pt.y;
                matrix.mapPoints(tmp);
                pt.set(tmp[0], tmp[1]);
                pdfViewCtrl.convertPageViewPtToPdfPt(pt, pt, pageIndex);

                path.setPoint(i, AppUtil.toFxPointF(pt), path.getPointType(i));
            }

            annot.setInkList(path);
            annot.resetAppearanceStream();
        } catch (PDFException e) {

        }

    }

}