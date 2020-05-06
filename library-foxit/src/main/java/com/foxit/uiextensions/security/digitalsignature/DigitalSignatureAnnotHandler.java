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
package com.foxit.uiextensions.security.digitalsignature;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.form.FormFillerEvent;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.annots.form.FormFillerUtil;
import com.foxit.uiextensions.annots.form.undo.FormFillerDeleteUndoItem;
import com.foxit.uiextensions.annots.form.undo.FormFillerModifyUndoItem;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.modules.panzoom.PanZoomModule;
import com.foxit.uiextensions.modules.signature.SignatureDataUtil;
import com.foxit.uiextensions.modules.signature.SignatureFragment;
import com.foxit.uiextensions.modules.signature.SignatureListPicker;
import com.foxit.uiextensions.modules.signature.SignatureMixListPopup;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class DigitalSignatureAnnotHandler implements AnnotHandler {
    private static final int DEFAULT_COLOR = PropertyBar.PB_COLORS_TOOL_GROUP_1[6];
    private static final int DEFAULT_THICKNESS = 4;

    /*
     *  LT     T     RT
     *   1-----2-----3
     *   |	         |
     *   |	         |
     * L 8           4 R
     *   |           |
     *   |           |
     *   7-----6-----5
     *   LB    B     RB
     *   */
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

    private Paint mFrmPaint;// outline
    private Paint mCtlPtPaint;

    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private DigitalSignatureSecurityHandler mSignatureHandler;
    private Paint mPaintBbox;
    private Paint mBitmapPaint;

    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;
    private int mBBoxSpace = 0;
    private String mDsgPath;
    private int mPageIndex = -1;
    private boolean mIsShowAnnotMenu = true;

    private Bitmap mOriginBitmap;
    private Bitmap mRotateBitmap;
    private PropertyBar mPropertyBar;
    private SignatureMixListPopup mMixListPopup;
    private Rect mBitmapRect;
    private RectF mTempAnnotRect;
    private Annot mLastAnnot;
    private SignatureFragment mFragment;
    private UITextEditDialog mWillSignDialog;
    private UISaveAsDialog mSaveAsDialog;
    private Signature mSignature = null;

    private boolean mIsModify = false;
    private boolean mIsLongPressTouchEvent = false;

    public DigitalSignatureAnnotHandler(Context dmContext, ViewGroup parent, PDFViewCtrl pdfViewCtrl, DigitalSignatureSecurityHandler securityHandler) {
        mContext = dmContext;
        mPdfViewCtrl = pdfViewCtrl;
        mSignatureHandler = securityHandler;
        mParent = parent;

        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
        mMixListPopup = new SignatureMixListPopup(mContext, mParent, mPdfViewCtrl, mInkCallback, true);

        mBitmapRect = new Rect();
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
        mBitmapPaint.setColor(0xFF000000);

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mFrmPaint.setColor(DEFAULT_COLOR | 0xFF000000);

        mCtlPtPaint = new Paint();
        mDownPoint = new PointF();
        mLastPoint = new PointF();

        init();
    }

    private void init() {
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(AppAnnotUtil.getInstance(mContext).getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(AppAnnotUtil.getBBoxPathEffect2());
        mPaintBbox.setColor(0xFF4e4d4d);
        mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        mMenuItems = new ArrayList<Integer>();
    }


    @Override
    public int getType() {
        return AnnotHandler.TYPE_FORMFIELD_SIGNATURE;
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
        RectF rectF = getAnnotBBox(annot);
        if (mPdfViewCtrl != null) {
            try {
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, annot.getPage().getIndex());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rectF.contains(point.x, point.y);
    }


    @Override
    public void onAnnotSelected(final Annot annot, final boolean needInvalid) {
        if (!(annot instanceof Widget)) return;

        try {
            Field field = ((Widget) annot).getField();
            if (field.isEmpty() || field.getType() != Field.e_TypeSignature) return;
            mLastAnnot = annot;
            mTempAnnotRect = AppUtil.toRectF(annot.getRect());
            mPageIndex = annot.getPage().getIndex();

            if (TextUtils.isEmpty(annot.getUniqueID())) {
                annot.setUniqueID(AppDmUtil.randomUUID(null));
            }

            if (mIsLongPressTouchEvent) {
                onAnnotSeletedByLongPress(annot, needInvalid);
            } else {
                onAnnotSelectedBySingleTap(annot, needInvalid);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void onAnnotSelectedBySingleTap(final Annot annot, final boolean needInvalid) {
        try {
            Field field = ((Widget) annot).getField();
            if (field.isEmpty() || field.getType() != Field.e_TypeSignature) return;

            Signature signature = new Signature(field);
            mMenuItems.clear();
            if (signature.isSigned()) {
                mMenuItems.add(AnnotMenu.AM_BT_VERIFY_SIGNATURE);
                mMenuItems.add(AnnotMenu.AM_BT_CANCEL);
                mAnnotMenu.setMenuItems(mMenuItems);

                mAnnotMenu.setListener(new AnnotMenu.ClickListener() {

                    @Override
                    public void onAMClick(int btType) {

                        if (btType == AnnotMenu.AM_BT_VERIFY_SIGNATURE) {
                            try {
                                Field field = ((Widget) annot).getField();
                                if (field.isEmpty() || field.getType() != Field.e_TypeSignature)
                                    return;

                                Signature signature = new Signature(field);
                                mSignatureHandler.verifySignature(signature);
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        } else if (btType == AnnotMenu.AM_BT_CANCEL) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        }
                    }
                });

                if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                    RectF annotRect = AppUtil.toRectF(annot.getRect());
                    RectF pageViewRect = new RectF();
                    RectF displayRect = new RectF();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, pageViewRect, mPageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, displayRect, mPageIndex);
                    Rect rect = rectRoundOut(displayRect, 0);
                    mPdfViewCtrl.refresh(mPageIndex, rect);
                    mAnnotMenu.show(displayRect);
                }
            } else {
                mSignature = signature;
                showSignDialog();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private void onAnnotSeletedByLongPress(final Annot annot, final boolean needInvalid) {
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);
        mCtlPtDeltyXY = AppDisplay.getInstance(mContext).dp2px(mCtlPtDeltyXY);
        try {
            mPageViewRect.set(AppUtil.toRectF(annot.getRect()));
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);

            mMenuItems.clear();
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                if (!(AppAnnotUtil.isLocked(annot) || FormFillerUtil.isReadOnly(annot))) {
                    String uid = annot.getUniqueID();
                    if (!TextUtils.isEmpty(uid) && uid.contains(FormFillerModule.ID_TAG)) {
                        mMenuItems.add(AnnotMenu.AM_BT_DELETE);
                    }
                }
            }
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    if (btType == AnnotMenu.AM_BT_DELETE) {
                        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            deleteAnnot(annot, true, null);
                        }
                    }
                }
            });
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            mAnnotMenu.show(menuRect);

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(mPageViewRect));
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        final DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        if (annot == documentManager.getCurrentAnnot()) {
            documentManager.setCurrentAnnot(null);
        }

        try {
            final PDFPage page = annot.getPage();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final int pageIndex = page.getIndex();

            final FormFillerDeleteUndoItem undoItem = new FormFillerDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mFieldName = ((Widget) annot).getField().getName();
            undoItem.mFieldType = ((Widget) annot).getField().getType();
            undoItem.mValue = ((Widget) annot).getField().getValue();
            undoItem.mRotation = ((Widget) annot).getMKRotation();

            documentManager.onAnnotWillDelete(page, annot);
            FormFillerEvent event = new FormFillerEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Widget) annot, mPdfViewCtrl);
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

    private void modifyAnnot(final int pageIndex, final Annot annot, RectF bboxRect,
                             boolean isModifyJni, final boolean addUndo, final Event.Callback result) {
        try {
            final FormFillerModifyUndoItem undoItem = new FormFillerModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mBBox = new RectF(bboxRect);
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

            undoItem.mRedoBbox = new RectF(bboxRect);
            undoItem.mUndoBbox = new RectF(mTempAnnotRect);

            if (isModifyJni) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(addUndo);

                FormFillerEvent event = new FormFillerEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Widget) annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (addUndo) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            }
                            RectF tempRectF = mTempAnnotRect;
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {

                                try {
                                    RectF annotRectF = AppUtil.toRectF(annot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                    annotRectF.union(tempRectF);
                                    annotRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);
            }

            if (isModifyJni) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
            }
            mIsModify = true;
            if (!isModifyJni) {
                RectF oldRect = AppUtil.toRectF(annot.getRect());
                annot.setFlags(annot.getFlags());
                annot.move(AppUtil.toFxRectF(bboxRect));
                annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                    mPdfViewCtrl.convertPdfRectToPageViewRect(oldRect, oldRect, pageIndex);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                    annotRectF.union(oldRect);
                    annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        if (annot == null || !(annot instanceof Widget)) return;
        try {
            if (((Widget) annot).getField().getType() != Field.e_TypeSignature) return;
            int pageIndex = annot.getPage().getIndex();

            if (needInvalid && mIsModify) {
                // must calculate BBox again
                RectF rectF = AppUtil.toRectF(annot.getRect());
                if (mTempAnnotRect.equals(rectF)) {
                    modifyAnnot(pageIndex, annot, rectF, false, false, null);
                } else {
                    modifyAnnot(pageIndex, annot, rectF, true, true, null);
                }
            } else if (mIsModify) {
                annot.move(AppUtil.toFxRectF(mTempAnnotRect));
                annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            }

            RectF pdfRect = AppUtil.toRectF(annot.getRect());
            RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
            if (mPdfViewCtrl.isPageVisible(pageIndex) && needInvalid) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                Rect rect = rectRoundOut(viewRect, 10);
                mPdfViewCtrl.refresh(pageIndex, rect);
            }
            clearData();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
    }

    private PointF mDocViewerPt = new PointF(0, 0);
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mPageDrawRect = new RectF();
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);
    private float mThickness = DEFAULT_THICKNESS;

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!mIsLongPressTouchEvent) return false;

        // in pageView evX and evY
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float evX = point.x;
        float evY = point.y;

        int action = motionEvent.getAction();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        mThickness = thicknessOnPageView(pageIndex, DEFAULT_THICKNESS);
                        RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                        RectF pdfRect = AppUtil.toRectF(annot.getRect());
                        mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                        mCurrentCtr = isTouchControlPoint(pageViewBBox, evX, evY);

                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);
                        mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());

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
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
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
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
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
                        float _lineWidth = DEFAULT_THICKNESS;
                        viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF bboxRect = new RectF(viewDrawBox);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, bboxRect, pageIndex);

                            modifyAnnot(pageIndex, annot, bboxRect, false, false, null);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }

                        } else {
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }
                        }

                        ret = true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    return ret;
                default:
            }
            return false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

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
    RectF mMapBounds = new RectF();

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
        try {
            Field field = ((Widget) annot).getField();
            if (field.isEmpty() || field.getType() != Field.e_TypeSignature) return false;
            Signature signature = new Signature(field);
            if (signature.isSigned()) return false;

            mIsLongPressTouchEvent = true;
            return onPressEvent(pageIndex, motionEvent, annot);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        mIsLongPressTouchEvent = false;
        return onPressEvent(pageIndex, motionEvent, annot);
    }

    private boolean onPressEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try {
            DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            if (annot == documentManager.getCurrentAnnot()) {

                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                    if (!canTouch(annot)) {
                        documentManager.setCurrentAnnot(null);
                    }
                    return true;
                } else {
                    documentManager.setCurrentAnnot(null);
                    return true;
                }
            } else {
                if (canTouch(annot)) {
                    documentManager.setCurrentAnnot(annot);
                    return true;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean canTouch(Annot annot) {
        try {
            if (!(annot instanceof Widget))
                return false;

            Field field = ((Widget) annot).getField();
            if (field.isEmpty() || field.getType() != Field.e_TypeSignature)
                return false;

            Signature signature = new Signature(field);
            if (signature.isSigned()) {
                return true;
            } else {
                if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canSigning())
                    return false;

                if (FormFillerUtil.isReadOnly(annot))
                    return false;
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    private Rect mTmpRect = new Rect();

    private Rect rectRoundOut(RectF rectF, int roundSize) {
        rectF.roundOut(mTmpRect);
        mTmpRect.inset(-roundSize, -roundSize);
        return mTmpRect;
    }

    private RectF mTmpRectF = new RectF();

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || annot.isEmpty() || !(annot instanceof Widget)) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this)
            return;

        if (mIsLongPressTouchEvent) {
            onDrawByLongPress(annot, pageIndex, canvas);
        } else {
            onDrawBySingleTap(annot, pageIndex, canvas);
        }
    }

    private void onDrawBySingleTap(Annot annot, int pageIndex, Canvas canvas) {
        try {
            if (mLastAnnot == annot && annot.getPage().getIndex() == pageIndex) {
                com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
                mTmpRectF.set(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                mPdfViewCtrl.convertPdfRectToPageViewRect(mTmpRectF, mTmpRectF, pageIndex);
                Rect rectBBox = rectRoundOut(mTmpRectF, mBBoxSpace);
                canvas.save();
                canvas.drawRect(rectBBox, mPaintBbox);

                if (mOriginBitmap != null) {
                    int rotation = (annot.getPage().getRotation() + mPdfViewCtrl.getViewRotation()) % 4;
                    if (rotation != 0){
                        if (rotation == 1) {
                            rotation = 90;
                        } else if (rotation == 2) {
                            rotation = 180;
                        } else if (rotation == 3) {
                            rotation = 270;
                        } else {
                            rotation = 0;
                        }
                        if (mRotateBitmap == null){
                            mRotateBitmap = rotateBitmap(mOriginBitmap, rotation);
                        }
                        Rect rect = new Rect(0, 0, mRotateBitmap.getWidth(), mRotateBitmap.getHeight());
                        canvas.drawBitmap(mRotateBitmap, rect, mTmpRectF, mBitmapPaint);
                    } else {
                        canvas.drawBitmap(mOriginBitmap, mBitmapRect, mTmpRectF, mBitmapPaint);
                        mRotateBitmap = mOriginBitmap;
                    }

                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private Bitmap rotateBitmap(Bitmap origin, float degrees) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, true);
        if (newBM.equals(origin)) {
            return newBM;
        }
        return newBM;
    }

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private void onDrawByLongPress(Annot annot, int pageIndex, Canvas canvas) {
        try {
            int annotPageIndex = annot.getPage().getIndex();
            if (AppAnnotUtil.equals(mLastAnnot, annot) && annotPageIndex == pageIndex) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                float thickness = thicknessOnPageView(pageIndex, DEFAULT_THICKNESS);
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
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    drawControlPoints(canvas, mBBoxInOnDraw);
                    // add Control Imaginary
                    drawControlImaginary(canvas, mBBoxInOnDraw);
                }
                canvas.restore();
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
            mCtlPtPaint.setColor(DEFAULT_COLOR);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    private Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);

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

        canvas.drawPath(mImaginaryPath, mFrmPaint);
    }

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }


    public void onDrawForControls(Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (mIsLongPressTouchEvent) {
            onDrawForControlsByLongPress(canvas, annot);
        } else {
            onDrawForControlsBySingleTap(canvas, annot);
        }
    }

    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    private void onDrawForControlsByLongPress(Canvas canvas, Annot annot) {
        if (annot != null
                && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
            try {
                int annotPageIndex = annot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                    float thickness = thicknessOnPageView(annotPageIndex, DEFAULT_THICKNESS);
                    mViewDrawRect.set(AppUtil.toRectF(annot.getRect()));
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRect, mViewDrawRect, annotPageIndex);
                    mViewDrawRect.inset(thickness / 2f, thickness / 2f);
                    if (mLastOper == OPER_SCALE_LT) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_T) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RT) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.y;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_R) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RB) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_B) {
                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_LB) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.y;
                    } else if (mLastOper == OPER_SCALE_L) {
                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    }
                    mDocViewerBBox.inset(-thickness / 2f, -thickness / 2f);
                    if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {
                        mDocViewerBBox = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);

                        float dx = mLastPoint.x - mDownPoint.x;
                        float dy = mLastPoint.y - mDownPoint.y;

                        mDocViewerBBox.offset(dx, dy);
                    }

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);
                    mAnnotMenu.update(mDocViewerBBox);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    private void onDrawForControlsBySingleTap(Canvas canvas, Annot annot) {
        try {
            if (annot != null && (annot.getType() == Annot.e_Widget)) {
                int pageIndex = annot.getPage().getIndex();
                com.foxit.sdk.common.fxcrt.RectF rect = annot.getRect();
                mTmpRectF.set(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mTmpRectF, mTmpRectF, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mTmpRectF, mTmpRectF, pageIndex);
                    mAnnotMenu.update(mTmpRectF);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void showSignDialog() {
        HashMap<String, Object> map = SignatureDataUtil.getRecentDsgSignData(mContext);
        if (map != null && !TextUtils.isEmpty((String) map.get("dsgPath")) && map.get("rect") != null && map.get("bitmap") != null) {
            mInkCallback.onSuccess(false, (Bitmap) map.get("bitmap"), (Rect) map.get("rect"), (Integer) map.get("color"), (String) map.get("dsgPath"));
            return;
        }
        showDrawViewFragment();
    }

    public void showSignList() {
        if (AppDisplay.getInstance(mContext).isPad()) {
            showMixListPopupPad(new RectF(), mLastAnnot);
        } else {
            showMixListPopupPhone(mLastAnnot);
        }
    }

    private void showMixListPopupPad(final RectF rectF, final Annot annot) {
        final SignatureListPicker listPicker = new SignatureListPicker(mContext, mParent, mPdfViewCtrl, mInkCallback, true);
        listPicker.init(new SignatureListPicker.ISignListPickerDismissCallback() {
            @Override
            public void onDismiss(boolean isShowAnnotMenu) {
                mIsShowAnnotMenu = isShowAnnotMenu;
                mPropertyBar.dismiss();
            }
        });
        listPicker.loadData();
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(0);
        mPropertyBar.addContentView(listPicker.getRootView());
        listPicker.getRootView().getLayoutParams().height = AppDisplay.getInstance(mContext).dp2px(460);
        mPropertyBar.setDismissListener(new PropertyBar.DismissListener() {
            @Override
            public void onDismiss() {
                if (listPicker.getBaseItemsSize() == 0) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
                listPicker.dismiss();
                if (mOriginBitmap != null) {
                    if (mIsShowAnnotMenu) {
                        showAnnotMenu(annot);
                    } else {
                        mAnnotMenu.dismiss();
                        mIsShowAnnotMenu = true;
                    }
                }
            }
        });
        mPropertyBar.show(rectF, true);
    }

    private void showMixListPopupPhone(final Annot annot) {
        if (mMixListPopup == null) {
            mMixListPopup = new SignatureMixListPopup(mContext, mParent, mPdfViewCtrl, mInkCallback, true);
        }
        mMixListPopup.setSignatureListEvent(new SignatureMixListPopup.ISignatureListEvent() {
            @Override
            public void onSignatureListDismiss() {
                if (mOriginBitmap != null) {
                    showAnnotMenu(annot);
                }
            }
        });
        mMixListPopup.show();
    }

    private void showAnnotMenu(Annot annot) {
        if (annot == null || annot.isEmpty())
            return;

        try {
            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF annotRect = AppUtil.toRectF(annot.getRect());
                RectF pageViewRect = new RectF();
                RectF displayRect = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, pageViewRect, pageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, displayRect, pageIndex);
                mAnnotMenu.show(displayRect);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void clearData() {
        if (mFragment != null && mFragment.isAttached()) {
            Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context != null) {
                try {
                    FragmentManager manager = ((FragmentActivity) context).getSupportFragmentManager();
                    manager.popBackStack();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (mOriginBitmap != null) {
            if (!mOriginBitmap.isRecycled())
                mOriginBitmap.recycle();
            mOriginBitmap = null;
        }

        if (mRotateBitmap != null) {
            if (!mRotateBitmap.isRecycled())
                mRotateBitmap.recycle();
            mRotateBitmap = null;
        }

        mDsgPath = null;
        mPageIndex = -1;
        mBitmapRect.setEmpty();
        mTempAnnotRect.setEmpty();
        mAnnotMenu.dismiss();
        mMenuItems.clear();
        mLastAnnot = null;
        mIsLongPressTouchEvent = false;
        mCtlPtRadius = 5;
        mCtlPtDeltyXY = 20;
        mIsModify = false;
    }

    private void showDrawViewFragment() {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }
        FragmentActivity act = ((FragmentActivity) context);
        mFragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");
        if (mFragment == null) {
            mFragment = new SignatureFragment();
            mFragment.init(mContext, mParent, mPdfViewCtrl, true);
        }
        mFragment.setInkCallback(mInkCallback);
        AppDialogManager.getInstance().showAllowManager(mFragment, act.getSupportFragmentManager(), "InkSignFragment", null);
    }

    private SignatureFragment.SignatureInkCallback mInkCallback = new SignatureFragment.SignatureInkCallback() {

        @Override
        public void onBackPressed() {
            if (mPdfViewCtrl.getUIExtensionsManager() == null) {
                return;
            }
            Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) {
                return;
            }

            FragmentActivity act = ((FragmentActivity) context);
            SignatureFragment fragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");

            if (fragment == null) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
            } else {
                fragment.dismiss();

                List<String> list = SignatureDataUtil.getRecentKeys(mContext);
                if (list != null && list.size() > 0) {
                    if (mOriginBitmap != null) {
                        showAnnotMenu(mLastAnnot);
                    }
                } else {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            }

        }

        @Override
        public void onSuccess(boolean isFromFragment, final Bitmap bitmap, final Rect rect, final int color, final String dsgPath) {
            if (isFromFragment) {
                if (mPdfViewCtrl.getUIExtensionsManager() == null) {
                    return;
                }
                Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                if (context == null) {
                    return;
                }
                try {
                    FragmentActivity act = ((FragmentActivity) context);
                    SignatureFragment fragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");
                    fragment.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                AppThreadManager.getInstance().getMainThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showSignOnPage(bitmap, rect, color, dsgPath);
                    }
                }, 300);
            } else {
                showSignOnPage(bitmap, rect, color, dsgPath);
            }
        }

        private void showSignOnPage(Bitmap bitmap, Rect rect, int color, String dsgPath) {
            if (bitmap == null || rect.isEmpty()) {
                return;
            }
            if (bitmap.getWidth() < rect.width() || bitmap.getHeight() < rect.height()) {
                return;
            }
            if (mPageIndex < 0) {
                return;
            }
            if (mOriginBitmap != null) {
                if (!mOriginBitmap.isRecycled())
                    mOriginBitmap.recycle();
                mOriginBitmap = null;
            }
            if (mRotateBitmap != null) {
                if (!mRotateBitmap.isRecycled())
                    mRotateBitmap.recycle();
                mRotateBitmap = null;
            }

            mBitmapRect.set(0, 0, rect.width(), rect.height());
            int t = rect.top;
            int b = rect.bottom;
            int l = rect.left;
            int r = rect.right;
            int[] pixels = new int[rect.width() * rect.height()];
            bitmap.getPixels(pixels, 0, r - l, l, t, r - l, b - t);
            for (int i = 0; i < pixels.length; i++) {
                if (0xFFFFFFFF == pixels[i]) {
                    pixels[i] = 0x0;
                }
            }
            mOriginBitmap = Bitmap.createBitmap(pixels, rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
            bitmap.recycle();
            bitmap = null;
            mDsgPath = dsgPath;

            mMenuItems.clear();
            mMenuItems.add(AnnotMenu.AM_BT_SIGNATURE);
            mMenuItems.add(AnnotMenu.AM_BT_SIGN_LIST);
            mMenuItems.add(AnnotMenu.AM_BT_DELETE);
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    if (btType == AnnotMenu.AM_BT_SIGNATURE) {
                        doSign();
                    } else if (btType == AnnotMenu.AM_BT_DELETE) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    } else if (btType == AnnotMenu.AM_BT_SIGN_LIST) {
                        showSignList();
                    }
                }
            });
            mAnnotMenu.setShowAlways(true);

            try {
                if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                    RectF annotRect = AppUtil.toRectF(mLastAnnot.getRect());
                    RectF pageViewRect = new RectF();
                    RectF displayRect = new RectF();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, pageViewRect, mPageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, displayRect, mPageIndex);
                    Rect rectRount = rectRoundOut(displayRect, 0);
                    mPdfViewCtrl.refresh(mPageIndex, rectRount);
                    mAnnotMenu.show(displayRect);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };

    private boolean doSign() {
        if (mOriginBitmap == null) return false;
        mAnnotMenu.dismiss();
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return false;
        final Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return false;

        if (mWillSignDialog == null || mWillSignDialog.getDialog().getOwnerActivity() == null) {
            mWillSignDialog = new UITextEditDialog(context);
            mWillSignDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) return;
                    mWillSignDialog.dismiss();
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            });
            mWillSignDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            });
            mWillSignDialog.getPromptTextView().setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_dialog_description));
            mWillSignDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_dialog_title));
            mWillSignDialog.getInputEditText().setVisibility(View.GONE);
        }
        mWillSignDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) return;
                mWillSignDialog.dismiss();
                sign2Doc(context);
            }
        });
        mWillSignDialog.show();
        return true;
    }

    private void sign2Doc(@NonNull Context context) {
        final Module dsgModule = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE);
        if (dsgModule != null) {
            if (mPdfViewCtrl.getUIExtensionsManager() == null) {
                return;
            }

            mSaveAsDialog = new UISaveAsDialog(context, "sign.pdf", "pdf", new UISaveAsDialog.ISaveAsOnOKClickCallBack() {
                @Override
                public void onOkClick(final String newFilePath) {
                    String tmpPath = newFilePath;
                    final File file = new File(newFilePath);
                    if (file.exists()) {
                        tmpPath = newFilePath + "_tmp.pdf";
                    }
                    DigitalSignatureUtil dsgUtil = ((DigitalSignatureModule) dsgModule).getDigitalSignatureUtil();
                    final String finalTmpPath = tmpPath;

                    IDigitalSignatureCreateCallBack signatureCreateCallBack = new IDigitalSignatureCreateCallBack() {
                        @Override
                        public void onCreateFinish(boolean success) {
                            mSignature = null;
                            if (!success) {
                                File file = new File(finalTmpPath);
                                file.delete();
                                return;
                            }

                            File newFile = new File(newFilePath);
                            File file = new File(finalTmpPath);
                            file.renameTo(newFile);

                            if (!mPdfViewCtrl.isPageVisible(mPageIndex)) {
                                clearData();
                                return;
                            }

                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().clearUndoRedo();
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(false);
                            PanZoomModule panZoomModule = (PanZoomModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PANZOOM);
                            if (panZoomModule != null) {
                                panZoomModule.exit();
                            }
                            mPdfViewCtrl.openDoc(newFilePath, null);
                            if (((DigitalSignatureModule) dsgModule).getDocPathChangeListener() != null)
                                ((DigitalSignatureModule) dsgModule).getDocPathChangeListener().onDocPathChange(newFilePath);
                            updateThumbnail(newFilePath);
                        }
                    };

                    if (mSignature != null && !mSignature.isEmpty()) {
                        try {
                            mSignature.setBitmap(mRotateBitmap);
                            dsgUtil.addCertSignature(tmpPath, mDsgPath, mSignature, mTempAnnotRect, mPageIndex, true, signatureCreateCallBack);
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    } else {
                        dsgUtil.addCertSignature(tmpPath, mDsgPath, mRotateBitmap, mTempAnnotRect, mPageIndex, signatureCreateCallBack);
                    }

                }

                @Override
                public void onCancelClick() {
                    mWillSignDialog.dismiss();
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            });
            mSaveAsDialog.showDialog();
        }
    }

    private void updateThumbnail(String path) {
        LocalModule module = (LocalModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager())
                .getModuleByName(Module.MODULE_NAME_LOCAL);
        if (module != null) {
            module.updateThumbnail(path);
        }
    }

    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
        if (newWidth != oldWidth || newHeight != oldHeight) {
            if (!AppDisplay.getInstance(mContext).isPad()) {

                if (mMixListPopup.getPopWindow() != null && mMixListPopup.getPopWindow().isShowing()) {
                    mMixListPopup.update(newWidth, newHeight);
                }
            }

            if (mSaveAsDialog != null && mSaveAsDialog.isShowing()) {
                mSaveAsDialog.setHeight(mSaveAsDialog.getDialogHeight());
                mSaveAsDialog.showDialog();
            }
        }
    }

    protected void onPagesRotated(boolean success, int[] pageIndexes, int rotation){
        if (success){
            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            AnnotHandler currentAnnotHandler = uiExtensionsManager.getCurrentAnnotHandler();
            Annot curAnnot = uiExtensionsManager.getDocumentManager().getCurrentAnnot();
            if (curAnnot != null && currentAnnotHandler == this) {
                try {
                    int pageIndex = curAnnot.getPage().getIndex();
                    for (int index: pageIndexes){
                        if (index == pageIndex){
                            if (mRotateBitmap != null) {
                                if (!mRotateBitmap.isRecycled())
                                    mRotateBitmap.recycle();
                                mRotateBitmap = null;
                            }
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                RectF viewRect = AppUtil.toRectF(curAnnot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                viewRect.inset(-40, -40);
                                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                            }
                            break;
                        }
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
